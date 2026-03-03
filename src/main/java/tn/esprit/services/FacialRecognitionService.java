package tn.esprit.services;

import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import tn.esprit.entities.User;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for facial recognition login. Captures from camera, detects face,
 * compares with stored user photos using histogram correlation.
 */
public class FacialRecognitionService {

    private static final int FACE_SIZE = 100;
    private static final double MATCH_THRESHOLD = 0.6; // Correlation threshold (0-1, higher = stricter)
    private static final String CASCADE_PATH = "cascade/haarcascade_frontalface_default.xml";

    private final UserServices userServices = new UserServices();
    private CascadeClassifier faceCascade;

    static {
        try {
            OpenCV.loadShared();
        } catch (Throwable t) {
            System.err.println("OpenCV load: " + t.getMessage());
        }
    }

    public FacialRecognitionService() {
        faceCascade = loadCascade();
    }

    private CascadeClassifier loadCascade() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(CASCADE_PATH)) {
            if (is == null) return null;
            File tmp = File.createTempFile("haarcascade_", ".xml");
            tmp.deleteOnExit();
            Files.copy(is, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            CascadeClassifier cc = new CascadeClassifier(tmp.getAbsolutePath());
            return cc.empty() ? null : cc;
        } catch (IOException e) {
            System.err.println("Cascade load error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Captures from camera, detects face, compares with users who have photos.
     * Returns the matched User or null.
     */
    public User recognizeAndLogin() throws SQLException {
        if (faceCascade == null || faceCascade.empty()) {
            System.err.println("Face cascade not loaded");
            return null;
        }
        VideoCapture capture = new VideoCapture(0, Videoio.CAP_DSHOW);
        if (!capture.isOpened()) {
            capture = new VideoCapture(0);
        }
        if (!capture.isOpened()) {
            return null;
        }
        Mat frame = new Mat();
        User matched = null;
        try {
            for (int i = 0; i < 30; i++) {
                if (capture.read(frame) && !frame.empty()) {
                    break;
                }
                try { Thread.sleep(50); } catch (InterruptedException e) { break; }
            }
            if (!frame.empty()) {
                matched = matchFace(frame);
            }
        } finally {
            capture.release();
            frame.release();
        }
        return matched;
    }

    /**
     * Matches the face in the given frame against all users with photos.
     */
    private User matchFace(Mat frame) throws SQLException {
        MatOfRect faces = new MatOfRect();
        Mat gray = new Mat();
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(gray, gray);
        faceCascade.detectMultiScale(gray, faces, 1.1, 5, 0, new Size(30, 30), new Size());
        gray.release();

        Rect[] faceArray = faces.toArray();
        faces.release();
        if (faceArray.length == 0) return null;

        Rect largestFace = faceArray[0];
        for (Rect r : faceArray) {
            if (r.width * r.height > largestFace.width * largestFace.height) {
                largestFace = r;
            }
        }

        Mat faceRoi = new Mat(frame, largestFace);
        Mat faceResized = new Mat();
        Imgproc.resize(faceRoi, faceResized, new Size(FACE_SIZE, FACE_SIZE));
        Mat faceGray = new Mat();
        Imgproc.cvtColor(faceResized, faceGray, Imgproc.COLOR_BGR2GRAY);
        faceRoi.release();
        faceResized.release();

        try {
            List<User> usersWithPhotos = new ArrayList<>();
            for (User u : userServices.afficher()) {
                if (u.getPhoto() != null && !u.getPhoto().isBlank()
                        && u.getStatut() == User.Statut.ACTIVE) {
                    usersWithPhotos.add(u);
                }
            }
            User bestMatch = null;
            double bestScore = MATCH_THRESHOLD;

            for (User user : usersWithPhotos) {
                File photoFile = new File(user.getPhoto());
                if (!photoFile.exists()) continue;

                Mat userMat = Imgcodecs.imread(photoFile.getAbsolutePath());
                if (userMat == null || userMat.empty()) continue;

                MatOfRect userFaces = new MatOfRect();
                Mat userGray = new Mat();
                Imgproc.cvtColor(userMat, userGray, Imgproc.COLOR_BGR2GRAY);
                Imgproc.equalizeHist(userGray, userGray);
                faceCascade.detectMultiScale(userGray, userFaces, 1.1, 5, 0, new Size(30, 30), new Size());

                Rect[] uFaces = userFaces.toArray();
                userGray.release();
                userFaces.release();

                for (Rect r : uFaces) {
                    Mat userFaceRoi = new Mat(userMat, r);
                    Mat userFaceResized = new Mat();
                    Imgproc.resize(userFaceRoi, userFaceResized, new Size(FACE_SIZE, FACE_SIZE));
                    Mat userFaceGray = new Mat();
                    Imgproc.cvtColor(userFaceResized, userFaceGray, Imgproc.COLOR_BGR2GRAY);

                    double score = compareFaceHistograms(faceGray, userFaceGray);

                    userFaceRoi.release();
                    userFaceResized.release();
                    userFaceGray.release();

                    if (score > bestScore) {
                        bestScore = score;
                        bestMatch = user;
                    }
                }
                userMat.release();
            }

            return bestMatch;
        } finally {
            faceGray.release();
        }
    }

    private double compareFaceHistograms(Mat face1, Mat face2) {
        Mat hist1 = new Mat();
        Mat hist2 = new Mat();
        List<Mat> images = List.of(face1);
        MatOfInt channels = new MatOfInt(0);
        MatOfFloat ranges = new MatOfFloat(0, 256);
        Imgproc.calcHist(images, channels, new Mat(), hist1, new MatOfInt(256), ranges, false);
        images = List.of(face2);
        Imgproc.calcHist(images, channels, new Mat(), hist2, new MatOfInt(256), ranges, false);

        Core.normalize(hist1, hist1, 0, 1, Core.NORM_MINMAX);
        Core.normalize(hist2, hist2, 0, 1, Core.NORM_MINMAX);

        double corr = Imgproc.compareHist(hist1, hist2, Imgproc.HISTCMP_CORREL);

        hist1.release();
        hist2.release();
        return corr;
    }
}
