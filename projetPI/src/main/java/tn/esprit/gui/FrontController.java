package tn.esprit.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import tn.esprit.entities.User;

import java.io.IOException;
import java.net.URL;

public class FrontController {

    @FXML private Button userMenuButton;
    @FXML private Button btnMyReservations;
    @FXML private ImageView userAvatar;
    @FXML private Label userAvatarFallback;
    @FXML private javafx.scene.layout.StackPane avatarFallbackPane;
    @FXML private StackPane contentStack;

    private ContextMenu userContextMenu;

    @FXML
    public void initialize() {
        buildUserContextMenu();
        User user = SessionHolder.getCurrentUser();
        if (user != null) {
            loadUserAvatar(user);
        }
        goHome();
    }

    private void buildUserContextMenu() {
        userContextMenu = new ContextMenu();
        MenuItem itemProfile = new MenuItem("Modifier le profil");
        itemProfile.setOnAction(e -> goModifyProfile());
        MenuItem itemLogout = new MenuItem("Déconnexion");
        itemLogout.setOnAction(e -> logout());
        userContextMenu.getItems().addAll(itemProfile, itemLogout);
    }

    @FXML
    private void showUserMenu() {
        if (userContextMenu != null && userMenuButton != null) {
            javafx.geometry.Bounds b = userMenuButton.localToScreen(userMenuButton.getBoundsInLocal());
            userContextMenu.show(userMenuButton, b.getMinX(), b.getMaxY());
        }
    }

    private void loadUserAvatar(User user) {
        String photo = user.getPhoto();
        if (photo != null && !photo.isBlank()) {
            try {
                String path = photo;
                if (!path.startsWith("file:") && !path.startsWith("http")) {
                    path = "file:" + path;
                }
                userAvatar.setImage(new Image(path));
                userAvatar.setVisible(true);
                if (avatarFallbackPane != null) avatarFallbackPane.setVisible(false);
            } catch (Exception e) {
                showFallbackAvatar(user);
            }
        } else {
            showFallbackAvatar(user);
        }
    }

    private void showFallbackAvatar(User user) {
        userAvatar.setVisible(false);
        if (avatarFallbackPane != null) avatarFallbackPane.setVisible(true);
        userAvatarFallback.setVisible(true);
        String initial = "";
        if (user.getPrenom() != null && !user.getPrenom().isEmpty()) {
            initial = user.getPrenom().substring(0, 1).toUpperCase();
        } else if (user.getNom() != null && !user.getNom().isEmpty()) {
            initial = user.getNom().substring(0, 1).toUpperCase();
        } else {
            initial = "?";
        }
        userAvatarFallback.setText(initial);
    }

    @FXML
    private void goHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/front_home.fxml"));
            Parent root = loader.load();
            Label welcome = (Label) root.lookup("#welcomeLabel");
            User user = SessionHolder.getCurrentUser();
            if (welcome != null && user != null) {
                welcome.setText("Bienvenue, " + user.getPrenom() + " " + user.getNom());
            }
            contentStack.getChildren().setAll(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goVoyages() {
        loadFrontPage("/fxml/front_voyages.fxml");
    }

    @FXML
    private void goActivites() {
        loadFrontPage("/fxml/front_activites.fxml");
    }

    @FXML
    private void goHotels() {
        loadFrontPage("/fxml/front_hotels.fxml");
    }

    @FXML
    private void goTransport() {
        loadFrontPage("/fxml/front_transport.fxml");
    }

    @FXML
    private void goForum() {
        loadFrontPage("/fxml/front_forum.fxml");
    }

    @FXML
    private void goMyReservations() {
        loadFrontPage("/fxml/front_my_reservations.fxml");
    }

    private void loadFrontPage(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Object ctrl = loader.getController();
            if (ctrl instanceof FrontVoyagesController) ((FrontVoyagesController) ctrl).setFrontController(this);
            else if (ctrl instanceof FrontActivitesController) ((FrontActivitesController) ctrl).setFrontController(this);
            else if (ctrl instanceof FrontHotelsController) ((FrontHotelsController) ctrl).setFrontController(this);
            else if (ctrl instanceof FrontTransportController) ((FrontTransportController) ctrl).setFrontController(this);
            else if (ctrl instanceof FrontForumController) ((FrontForumController) ctrl).setFrontController(this);
            contentStack.getChildren().setAll(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goModifyProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/front_profile.fxml"));
            Parent root = loader.load();
            ProfileController ctrl = loader.getController();
            if (ctrl != null) {
                ctrl.setFrontController(this);
                ctrl.loadUser();
            }
            contentStack.getChildren().setAll(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void logout() {
        SessionHolder.clear();
        Stage stage = (Stage) userMenuButton.getScene().getWindow();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Scene scene = new Scene(root, 960, 620);
            scene.getStylesheets().add(getClass().getResource("/css/voyage.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("CarthageVoyage");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Called by ProfileController after save to refresh avatar. */
    public void refreshAvatar() {
        User user = SessionHolder.getCurrentUser();
        if (user != null) loadUserAvatar(user);
    }
}
