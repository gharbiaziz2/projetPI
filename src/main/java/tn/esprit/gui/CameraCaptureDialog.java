package tn.esprit.gui;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Dialog to capture a photo from the webcam. Shows live preview and a capture button.
 */
public class CameraCaptureDialog {

    private static final int PREVIEW_WIDTH = 480;
    private static final int PREVIEW_HEIGHT = 360;

    private VideoCapture capture;
    private ScheduledExecutorService timer;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private String capturedPath;

    static {
        try {
            OpenCV.loadShared();
        } catch (Throwable t) {
            System.err.println("OpenCV load: " + t.getMessage());
        }
    }

    /**
     * Opens the camera capture dialog. Returns the path to the saved image, or null if cancelled/failed.
     */
    public String showAndCapture(Stage owner) {
        javafx.scene.control.Dialog<String> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Prendre une photo");
        dialog.setHeaderText("Positionnez-vous face à la caméra");
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(PREVIEW_WIDTH);
        imageView.setFitHeight(PREVIEW_HEIGHT);
        imageView.setPreserveRatio(true);

        javafx.scene.control.ButtonType captureType = new javafx.scene.control.ButtonType("Capturer", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(captureType, javafx.scene.control.ButtonType.CANCEL);

        VBox content = new VBox(16, imageView);
        content.setAlignment(javafx.geometry.Pos.CENTER);
        content.setPadding(new javafx.geometry.Insets(20));
        content.setStyle("-fx-background-color: #2c3e50;");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(PREVIEW_WIDTH + 80);

        dialog.setResultConverter(btn -> {
            if (btn == captureType) {
                captureAndSave();
            }
            stopCamera();
            return btn == captureType ? capturedPath : null;
        });

        dialog.setOnShown(e -> startCamera(imageView));
        dialog.setOnHidden(e -> stopCamera());

        return dialog.showAndWait().orElse(null);
    }

    private void startCamera(ImageView imageView) {
        if (running.getAndSet(true)) return;
        capture = new VideoCapture(0, Videoio.CAP_DSHOW);
        if (!capture.isOpened()) {
            capture = new VideoCapture(0);
        }
        if (!capture.isOpened()) {
            Platform.runLater(() -> {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Erreur");
                alert.setHeaderText("Impossible d'accéder à la caméra");
                alert.setContentText("Vérifiez que la webcam est connectée et non utilisée par une autre application.");
                alert.showAndWait();
            });
            running.set(false);
            return;
        }
        Runnable frameGrabber = () -> {
            Mat frame = new Mat();
            if (capture.read(frame) && !frame.empty()) {
                org.opencv.core.Core.flip(frame, frame, 1);
                Image img = matToImage(frame);
                if (img != null) {
                    final Image fImg = img;
                    Platform.runLater(() -> imageView.setImage(fImg));
                }
                frame.release();
            }
        };
        timer = Executors.newSingleThreadScheduledExecutor();
        timer.scheduleAtFixedRate(frameGrabber, 0, 50, TimeUnit.MILLISECONDS);
    }

    private void stopCamera() {
        if (!running.getAndSet(false)) return;
        try {
            if (timer != null) {
                timer.shutdown();
                timer.awaitTermination(500, TimeUnit.MILLISECONDS);
            }
            if (capture != null) {
                capture.release();
                capture = null;
            }
        } catch (InterruptedException ignored) {
        }
    }

    private void captureAndSave() {
        if (capture == null || !capture.isOpened()) return;
        Mat frame = new Mat();
        if (capture.read(frame) && !frame.empty()) {
            org.opencv.core.Core.flip(frame, frame, 1);
            try {
                File dir = new File(System.getProperty("user.dir"), "user_photos");
                dir.mkdirs();
                File output = new File(dir, "capture_" + System.currentTimeMillis() + ".jpg");
                Imgcodecs.imwrite(output.getAbsolutePath(), frame);
                capturedPath = output.getAbsolutePath();
            } catch (Exception e) {
                System.err.println("Capture save error: " + e.getMessage());
            }
            frame.release();
        }
    }

    private Image matToImage(Mat mat) {
        try {
            MatOfByte buffer = new MatOfByte();
            Imgcodecs.imencode(".png", mat, buffer);
            return new Image(new java.io.ByteArrayInputStream(buffer.toArray()));
        } catch (Exception e) {
            return null;
        }
    }
}
