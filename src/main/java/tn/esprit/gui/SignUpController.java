package tn.esprit.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.services.UserServices;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.regex.Pattern;

public class SignUpController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+\\s\\-]{8,20}$");
    private static final int MIN_PASSWORD_LENGTH = 6;

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<User.Role> roleCombo;
    @FXML private TextField telephoneField;
    @FXML private TextField adresseField;
    @FXML private Label photoLabel;
    @FXML private javafx.scene.control.Button browsePhotoButton;
    @FXML private Label nomError;
    @FXML private Label prenomError;
    @FXML private Label emailError;
    @FXML private Label passwordError;
    @FXML private Label confirmPasswordError;
    @FXML private Label roleError;
    @FXML private Label telephoneError;
    @FXML private Label photoError;
    @FXML private Label signUpError;
    @FXML private javafx.scene.control.Button signUpButton;
    @FXML private javafx.scene.control.Button goLoginButton;
    @FXML private ImageView logoImage;
    @FXML private ImageView formLogoImage;
    @FXML private Label logoFallback;

    private final UserServices userServices = new UserServices();
    private String selectedPhotoPath;

    @FXML
    public void initialize() {
        // Role: only CLIENT and GUIDE_TOURISTIQUE (ADMIN cannot be chosen in sign up)
        roleCombo.getItems().setAll(User.Role.CLIENT, User.Role.GUIDE_TOURISTIQUE);
        roleCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(User.Role r) {
                if (r == null) return "";
                return r == User.Role.CLIENT ? "Client" : "Guide touristique";
            }
            @Override
            public User.Role fromString(String s) {
                if (s == null || s.isEmpty()) return null;
                if ("Client".equals(s)) return User.Role.CLIENT;
                if ("Guide touristique".equals(s)) return User.Role.GUIDE_TOURISTIQUE;
                return null;
            }
        });

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
        nomError.setVisible(false);
        nomError.setManaged(false);
        prenomError.setVisible(false);
        prenomError.setManaged(false);
        emailError.setVisible(false);
        emailError.setManaged(false);
        passwordError.setVisible(false);
        passwordError.setManaged(false);
        confirmPasswordError.setVisible(false);
        confirmPasswordError.setManaged(false);
        roleError.setVisible(false);
        roleError.setManaged(false);
        telephoneError.setVisible(false);
        telephoneError.setManaged(false);
        photoError.setVisible(false);
        photoError.setManaged(false);
        signUpError.setVisible(false);
        signUpError.setManaged(false);
    }

    /** Contrôle de saisie: champs obligatoires, format email, mot de passe, téléphone optionnel. */
    private boolean validateInputs() {
        clearErrors();
        boolean valid = true;

        String nom = nomField.getText();
        if (nom == null || nom.isBlank()) {
            nomError.setText("Le nom est obligatoire.");
            nomError.setVisible(true);
            nomError.setManaged(true);
            valid = false;
        }

        String prenom = prenomField.getText();
        if (prenom == null || prenom.isBlank()) {
            prenomError.setText("Le prénom est obligatoire.");
            prenomError.setVisible(true);
            prenomError.setManaged(true);
            valid = false;
        }

        String email = emailField.getText();
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

        String password = passwordField.getText();
        if (password == null || password.isBlank()) {
            passwordError.setText("Le mot de passe est obligatoire.");
            passwordError.setVisible(true);
            passwordError.setManaged(true);
            valid = false;
        } else if (password.length() < MIN_PASSWORD_LENGTH) {
            passwordError.setText("Le mot de passe doit contenir au moins " + MIN_PASSWORD_LENGTH + " caractères.");
            passwordError.setVisible(true);
            passwordError.setManaged(true);
            valid = false;
        }

        String confirm = confirmPasswordField.getText();
        if (confirm == null || !confirm.equals(password)) {
            confirmPasswordError.setText("Les mots de passe ne correspondent pas.");
            confirmPasswordError.setVisible(true);
            confirmPasswordError.setManaged(true);
            valid = false;
        }

        if (roleCombo.getSelectionModel().getSelectedItem() == null) {
            roleError.setText("Veuillez choisir un rôle.");
            roleError.setVisible(true);
            roleError.setManaged(true);
            valid = false;
        }

        String phone = telephoneField.getText();
        if (phone != null && !phone.isBlank() && !PHONE_PATTERN.matcher(phone.trim()).matches()) {
            telephoneError.setText("Format de téléphone invalide.");
            telephoneError.setVisible(true);
            telephoneError.setManaged(true);
            valid = false;
        }

        return valid;
    }

    @FXML
    private void onBrowsePhoto() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une photo");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        File f = fc.showOpenDialog(browsePhotoButton.getScene().getWindow());
        if (f != null) {
            selectedPhotoPath = f.getAbsolutePath();
            photoLabel.setText(f.getName());
        }
    }

    @FXML
    private void onSignUp() {
        if (!validateInputs()) return;

        String email = emailField.getText().trim();
        try {
            if (userServices.findByEmail(email) != null) {
                emailError.setText("Un compte existe déjà avec cet email.");
                emailError.setVisible(true);
                emailError.setManaged(true);
                return;
            }
        } catch (SQLException e) {
            signUpError.setText("Erreur base de données. Réessayez.");
            signUpError.setVisible(true);
            signUpError.setManaged(true);
            return;
        }

        User u = new User();
        u.setNom(nomField.getText().trim());
        u.setPrenom(prenomField.getText().trim());
        u.setEmail(email);
        u.setMotDePasse(passwordField.getText());
        u.setRole(roleCombo.getSelectionModel().getSelectedItem());
        u.setStatut(User.Statut.ACTIVE);
        u.setDateCreation(LocalDate.now());
        u.setTelephone(telephoneField.getText() != null && !telephoneField.getText().isBlank() ? telephoneField.getText().trim() : null);
        u.setAdresse(adresseField.getText() != null && !adresseField.getText().isBlank() ? adresseField.getText().trim() : null);
        u.setPhoto(selectedPhotoPath != null && !selectedPhotoPath.isBlank() ? selectedPhotoPath : null);
        u.setBio(null);

        try {
            userServices.ajouter(u);
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("CarthageVoyage");
            a.setHeaderText("Inscription réussie");
            a.setContentText("Vous pouvez maintenant vous connecter.");
            a.showAndWait();
            goToLogin();
        } catch (SQLException e) {
            signUpError.setText("Erreur lors de l'inscription. Réessayez.");
            signUpError.setVisible(true);
            signUpError.setManaged(true);
        } catch (IOException e) {
            signUpError.setText("Erreur lors du chargement. Réessayez.");
            signUpError.setVisible(true);
            signUpError.setManaged(true);
        }
    }

    @FXML
    private void goToLogin() throws IOException {
        Stage stage = (Stage) goLoginButton.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 960, 620);
        scene.getStylesheets().add(getClass().getResource("/css/voyage.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("CarthageVoyage");
    }
}
