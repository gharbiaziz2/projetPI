package tn.esprit.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.services.UserServices;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class LoginController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label emailError;
    @FXML private Label passwordError;
    @FXML private Label loginError;
    @FXML private javafx.scene.control.Button loginButton;
    @FXML private javafx.scene.control.Button goSignUpButton;
    @FXML private ImageView logoImage;
    @FXML private ImageView formLogoImage;
    @FXML private Label logoFallback;

    private final UserServices userServices = new UserServices();

    @FXML
    public void initialize() {
        URL logoUrl = getClass().getResource("/images/logo.png");
        if (logoUrl != null) {
            Image img = new Image(logoUrl.toExternalForm());
            logoImage.setImage(img);
            logoImage.setVisible(true);
            formLogoImage.setImage(img);
            formLogoImage.setVisible(true);
            if (logoFallback != null) logoFallback.setVisible(false);
        } else {
            logoImage.setVisible(false);
            formLogoImage.setVisible(false);
            if (logoFallback != null) {
                logoFallback.setVisible(true);
                logoFallback.setManaged(true);
            }
        }
        clearErrors();
    }

    private void clearErrors() {
        emailError.setVisible(false);
        emailError.setManaged(false);
        passwordError.setVisible(false);
        passwordError.setManaged(false);
        loginError.setVisible(false);
        loginError.setManaged(false);
    }

    /** Contrôle de saisie: email obligatoire et format valide, mot de passe obligatoire. */
    private boolean validateInputs() {
        clearErrors();
        boolean valid = true;
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email == null || email.isBlank()) {
            emailError.setText("L'email est obligatoire.");
            emailError.setVisible(true);
            emailError.setManaged(true);
            valid = false;
        } else if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            emailError.setText("Format d'email invalide.");
            emailError.setVisible(true);
            emailError.setManaged(true);
            valid = false;
        }

        if (password == null || password.isBlank()) {
            passwordError.setText("Le mot de passe est obligatoire.");
            passwordError.setVisible(true);
            passwordError.setManaged(true);
            valid = false;
        }

        return valid;
    }

    @FXML
    private void onLogin() {
        if (!validateInputs()) return;

        String email = emailField.getText().trim();
        String password = passwordField.getText();

        try {
            User user = userServices.findByEmail(email);
            if (user == null) {
                loginError.setText("Aucun compte associé à cet email.");
                loginError.setVisible(true);
                loginError.setManaged(true);
                return;
            }
            if (!user.getMotDePasse().equals(password)) {
                loginError.setText("Mot de passe incorrect.");
                loginError.setVisible(true);
                loginError.setManaged(true);
                return;
            }
            if (user.getStatut() != User.Statut.ACTIVE) {
                loginError.setText("Ce compte est désactivé. Contactez l'administrateur.");
                loginError.setVisible(true);
                loginError.setManaged(true);
                return;
            }
            // Success: ADMIN -> back office, CLIENT -> front, else welcome alert
            SessionHolder.setCurrentUser(user);
            Stage stage = (Stage) loginButton.getScene().getWindow();
            if (user.getRole() == User.Role.ADMIN) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/back.fxml"));
                Parent root = loader.load();
                Scene scene = new Scene(root, 1200, 700);
                scene.getStylesheets().add(getClass().getResource("/css/back.css").toExternalForm());
                stage.setScene(scene);
                stage.setTitle("CarthageVoyage - Administration");
            } else if (user.getRole() == User.Role.CLIENT) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/front.fxml"));
                Parent root = loader.load();
                Scene scene = new Scene(root, 1100, 700);
                scene.getStylesheets().add(getClass().getResource("/css/front.css").toExternalForm());
                stage.setScene(scene);
                stage.setTitle("CarthageVoyage");
            } else {
                Alert a = new Alert(Alert.AlertType.INFORMATION);
                a.setTitle("CarthageVoyage");
                a.setHeaderText("Connexion réussie");
                a.setContentText("Bienvenue, " + user.getPrenom() + " " + user.getNom() + " (" + user.getRole() + ").");
                a.showAndWait();
            }
        } catch (SQLException e) {
            loginError.setText("Erreur base de données. Réessayez.");
            loginError.setVisible(true);
            loginError.setManaged(true);
        } catch (IOException e) {
            loginError.setText("Erreur chargement interface.");
            loginError.setVisible(true);
            loginError.setManaged(true);
        }
    }

    @FXML
    private void goToSignUp() throws IOException {
        Stage stage = (Stage) goSignUpButton.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/signup.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 980, 700);
        scene.getStylesheets().add(getClass().getResource("/css/voyage.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("CarthageVoyage - Inscription");
    }
}
