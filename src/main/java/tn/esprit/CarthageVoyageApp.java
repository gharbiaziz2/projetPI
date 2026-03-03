package tn.esprit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import tn.esprit.services.ConfirmationHttpServer;

import java.io.IOException;

public class CarthageVoyageApp extends Application {

    private static CarthageVoyageApp instance;

    /** Used by LoginController for Google OAuth (open browser). */
    public static javafx.application.HostServices getAppHostServices() {
        return instance != null ? instance.getHostServices() : null;
    }

    @Override
    public void start(Stage stage) throws IOException {
        instance = this;
        // Start the background HTTP server (email confirmation link handler)
        ConfirmationHttpServer.start();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        Parent root = loader.load();

        // ── Responsive window setup ───────────────────────────────────────────
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();

        // Minimum dimensions — nothing will ever be hidden below these
        double minW = 1200;
        double minH = 760;

        // Initial scene size: 90% of screen (or min, whichever is bigger)
        double initW = Math.max(minW, screen.getWidth() * 0.90);
        double initH = Math.max(minH, screen.getHeight() * 0.90);

        Scene scene = new Scene(root, initW, initH);
        scene.getStylesheets().add(getClass().getResource("/css/voyage.css").toExternalForm());

        stage.setTitle("CarthageVoyage");
        stage.setMinWidth(minW);
        stage.setMinHeight(minH);
        stage.setScene(scene);

        // Centre on screen then go fullscreen-maximized
        stage.setX(screen.getMinX() + (screen.getWidth() - initW) / 2);
        stage.setY(screen.getMinY() + (screen.getHeight() - initH) / 2);
        stage.setMaximized(true); // ← fills the whole desktop on launch
        // ─────────────────────────────────────────────────────────────────────

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
