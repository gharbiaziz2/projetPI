package tn.esprit.gui;

import javafx.application.Platform;
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
import tn.esprit.CarthageVoyageApp;
import tn.esprit.entities.User;
import tn.esprit.services.FacialRecognitionService;
import tn.esprit.services.GoogleOAuthService;
import tn.esprit.services.UserServices;
import tn.esprit.services.VonageSmsService;
import tn.esprit.util.PasswordUtil;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
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
    @FXML private javafx.scene.control.Button googleLoginButton;
    @FXML private javafx.scene.control.Button faceRecognitionButton;
    @FXML private javafx.scene.control.Button forgotPasswordButton;
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
            if (PasswordUtil.isGoogleOAuthSentinel(user.getMotDePasse())) {
                loginError.setText("Ce compte utilise la connexion Google. Utilisez « Se connecter avec Google ».");
                loginError.setVisible(true);
                loginError.setManaged(true);
                return;
            }
            if (!PasswordUtil.verify(password, user.getMotDePasse())) {
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
    private void onGoogleLogin() {
        clearErrors();
        if (googleLoginButton != null) googleLoginButton.setDisable(true);
        try {
            GoogleOAuthService oauth = new GoogleOAuthService();
            String authUrl = oauth.getAuthorizationUrl();
            CompletableFuture<String> codeFuture = oauth.startLocalServerAndWaitForCode();

            var hostServices = CarthageVoyageApp.getAppHostServices();
            if (hostServices != null) {
                hostServices.showDocument(authUrl);
            } else {
                try {
                    java.awt.Desktop.getDesktop().browse(java.net.URI.create(authUrl));
                } catch (Exception e) {
                    if (googleLoginButton != null) googleLoginButton.setDisable(false);
                    loginError.setText("Impossible d'ouvrir le navigateur.");
                    loginError.setVisible(true);
                    loginError.setManaged(true);
                    return;
                }
            }

            codeFuture.thenApplyAsync(code -> {
                try {
                    return oauth.exchangeCodeAndGetUserInfo(code);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).thenAcceptAsync(googleUser -> {
                try {
                    User user = userServices.findOrCreateFromGoogle(
                            googleUser.getEmail(), googleUser.getNom(), googleUser.getPrenom(), googleUser.getPhotoUrl());
                    if (user.getStatut() != User.Statut.ACTIVE) {
                        Platform.runLater(() -> {
                            if (googleLoginButton != null) googleLoginButton.setDisable(false);
                            loginError.setText("Ce compte est désactivé. Contactez l'administrateur.");
                            loginError.setVisible(true);
                            loginError.setManaged(true);
                        });
                        return;
                    }
                    SessionHolder.setCurrentUser(user);
                    Stage stage = (Stage) loginButton.getScene().getWindow();
                    if (user.getRole() == User.Role.ADMIN) {
                        Platform.runLater(() -> navigateTo(stage, "/fxml/back.fxml", "/css/back.css", 1200, 700, "CarthageVoyage - Administration"));
                    } else if (user.getRole() == User.Role.CLIENT) {
                        Platform.runLater(() -> navigateTo(stage, "/fxml/front.fxml", "/css/front.css", 1100, 700, "CarthageVoyage"));
                    } else {
                        Platform.runLater(() -> {
                            Alert a = new Alert(Alert.AlertType.INFORMATION);
                            a.setTitle("CarthageVoyage");
                            a.setHeaderText("Connexion réussie");
                            a.setContentText("Bienvenue, " + user.getPrenom() + " " + user.getNom() + " (" + user.getRole() + ").");
                            a.showAndWait();
                        });
                    }
                } catch (SQLException e) {
                    Platform.runLater(() -> showLoginError("Erreur base de données. Réessayez."));
                }
            }, Platform::runLater).exceptionally(ex -> {
                Platform.runLater(() -> {
                    if (googleLoginButton != null) googleLoginButton.setDisable(false);
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    if (msg != null && msg.contains("error=access_denied")) {
                        loginError.setText("Connexion annulée.");
                    } else {
                        loginError.setText("Erreur lors de la connexion Google. Réessayez.");
                    }
                    loginError.setVisible(true);
                    loginError.setManaged(true);
                });
                return null;
            });
        } catch (IOException e) {
            if (googleLoginButton != null) googleLoginButton.setDisable(false);
            loginError.setText("Impossible de charger la configuration Google. Vérifiez config/google-credentials.json");
            loginError.setVisible(true);
            loginError.setManaged(true);
        }
    }

    private void navigateTo(Stage stage, String fxml, String css, int w, int h, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Scene scene = new Scene(root, w, h);
            scene.getStylesheets().add(getClass().getResource(css).toExternalForm());
            stage.setScene(scene);
            stage.setTitle(title);
        } catch (IOException e) {
            loginError.setText("Erreur chargement interface.");
            loginError.setVisible(true);
            loginError.setManaged(true);
        }
    }

    private void showLoginError(String message) {
        if (googleLoginButton != null) googleLoginButton.setDisable(false);
        if (faceRecognitionButton != null) faceRecognitionButton.setDisable(false);
        loginError.setText(message);
        loginError.setVisible(true);
        loginError.setManaged(true);
    }

    @FXML
    private void onFaceRecognition() {
        clearErrors();
        if (faceRecognitionButton != null) faceRecognitionButton.setDisable(true);
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                User user = new FacialRecognitionService().recognizeAndLogin();
                Platform.runLater(() -> {
                    if (faceRecognitionButton != null) faceRecognitionButton.setDisable(false);
                    if (user == null) {
                        loginError.setText("Reconnaissance échouée. Vérifiez que votre visage est visible et que vous avez ajouté une photo lors de l'inscription.");
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
                    SessionHolder.setCurrentUser(user);
                    Stage stage = (Stage) loginButton.getScene().getWindow();
                    if (user.getRole() == User.Role.ADMIN) {
                        navigateTo(stage, "/fxml/back.fxml", "/css/back.css", 1200, 700, "CarthageVoyage - Administration");
                    } else if (user.getRole() == User.Role.CLIENT) {
                        navigateTo(stage, "/fxml/front.fxml", "/css/front.css", 1100, 700, "CarthageVoyage");
                    } else {
                        Alert a = new Alert(Alert.AlertType.INFORMATION);
                        a.setTitle("CarthageVoyage");
                        a.setHeaderText("Connexion réussie");
                        a.setContentText("Bienvenue, " + user.getPrenom() + " " + user.getNom() + " (" + user.getRole() + ").");
                        a.showAndWait();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (faceRecognitionButton != null) faceRecognitionButton.setDisable(false);
                    loginError.setText("Erreur reconnaissance faciale. Vérifiez que la caméra est disponible.");
                    loginError.setVisible(true);
                    loginError.setManaged(true);
                });
            }
        });
    }

    @FXML
    private void onForgotPassword() {
        clearErrors();
        String email = emailField != null ? emailField.getText() : null;
        if (email == null || email.isBlank()) {
            loginError.setText("Veuillez entrer votre email pour récupérer le mot de passe.");
            loginError.setVisible(true);
            loginError.setManaged(true);
            return;
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            loginError.setText("Format d'email invalide.");
            loginError.setVisible(true);
            loginError.setManaged(true);
            return;
        }
        try {
            User user = userServices.findByEmail(email.trim());
            if (user == null) {
                loginError.setText("Aucun compte associé à cet email.");
                loginError.setVisible(true);
                loginError.setManaged(true);
                return;
            }
            if (PasswordUtil.isGoogleOAuthSentinel(user.getMotDePasse())) {
                loginError.setText("Ce compte utilise la connexion Google. Pas de mot de passe à réinitialiser.");
                loginError.setVisible(true);
                loginError.setManaged(true);
                return;
            }
            if (user.getTelephone() == null || user.getTelephone().isBlank()) {
                loginError.setText("Aucun numéro de téléphone enregistré. Contactez l'administrateur.");
                loginError.setVisible(true);
                loginError.setManaged(true);
                return;
            }
            String tempPassword = userServices.resetPasswordToTemp(email.trim());
            if (tempPassword == null) {
                loginError.setText("Impossible de réinitialiser le mot de passe.");
                loginError.setVisible(true);
                loginError.setManaged(true);
                return;
            }
            VonageSmsService sms = new VonageSmsService();
            String message = "CarthageVoyage - Votre mot de passe temporaire : " + tempPassword + " - Changez-le après connexion.";
            boolean sent = sms.sendSms(user.getTelephone(), message);
            if (sent) {
                Alert a = new Alert(Alert.AlertType.INFORMATION);
                a.setTitle("CarthageVoyage");
                a.setHeaderText("Mot de passe envoyé");
                a.setContentText("Un mot de passe temporaire a été envoyé par SMS au numéro enregistré. Utilisez-le pour vous connecter, puis modifiez-le dans votre profil.");
                a.showAndWait();
            } else {
                loginError.setText("Erreur envoi SMS (identifiants Vonage invalides). Ajoutez vonage.api.secret dans config.properties depuis https://dashboard.nexmo.com");
                loginError.setVisible(true);
                loginError.setManaged(true);
            }
        } catch (SQLException e) {
            loginError.setText("Erreur base de données. Réessayez.");
            loginError.setVisible(true);
            loginError.setManaged(true);
        } catch (java.io.IOException e) {
            loginError.setText("Configuration Vonage manquante. Vérifiez config.properties.");
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
