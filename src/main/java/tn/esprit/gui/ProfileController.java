package tn.esprit.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import tn.esprit.entities.User;
import tn.esprit.services.UserServices;

import java.io.File;
import java.sql.SQLException;

public class ProfileController {

    @FXML private TextField fieldNom;
    @FXML private TextField fieldPrenom;
    @FXML private TextField fieldEmail;
    @FXML private PasswordField fieldPassword;
    @FXML private TextField fieldTelephone;
    @FXML private TextField fieldAdresse;
    @FXML private TextField fieldPhoto;
    @FXML private TextArea fieldBio;
    @FXML private Button btnBrowsePhoto;
    @FXML private Button btnSave;

    private final UserServices userServices = new UserServices();
    private User currentUser;
    private FrontController frontController;

    public void setFrontController(FrontController fc) {
        this.frontController = fc;
    }

    public void loadUser() {
        currentUser = SessionHolder.getCurrentUser();
        if (currentUser == null) return;
        fieldNom.setText(currentUser.getNom());
        fieldPrenom.setText(currentUser.getPrenom());
        fieldEmail.setText(currentUser.getEmail());
        fieldPassword.clear();
        fieldTelephone.setText(currentUser.getTelephone() != null ? currentUser.getTelephone() : "");
        fieldAdresse.setText(currentUser.getAdresse() != null ? currentUser.getAdresse() : "");
        fieldPhoto.setText(currentUser.getPhoto() != null ? currentUser.getPhoto() : "");
        fieldBio.setText(currentUser.getBio() != null ? currentUser.getBio() : "");
    }

    @FXML
    private void browsePhoto() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une photo");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        File f = fc.showOpenDialog(btnBrowsePhoto.getScene().getWindow());
        if (f != null) {
            fieldPhoto.setText(f.getAbsolutePath());
        }
    }

    @FXML
    private void saveProfile() {
        if (currentUser == null) return;
        String nom = fieldNom.getText();
        String prenom = fieldPrenom.getText();
        String email = fieldEmail.getText();
        if (nom == null || nom.isBlank() || prenom == null || prenom.isBlank()) {
            showError("Validation", "Nom et prénom obligatoires.");
            return;
        }
        if (email == null || email.isBlank()) {
            showError("Validation", "Email obligatoire.");
            return;
        }

        currentUser.setNom(nom.trim());
        currentUser.setPrenom(prenom.trim());
        currentUser.setEmail(email.trim());
        String pwd = fieldPassword.getText();
        if (pwd != null && !pwd.isBlank()) currentUser.setMotDePasse(pwd);
        currentUser.setTelephone(fieldTelephone.getText() != null && !fieldTelephone.getText().isBlank() ? fieldTelephone.getText().trim() : null);
        currentUser.setAdresse(fieldAdresse.getText() != null && !fieldAdresse.getText().isBlank() ? fieldAdresse.getText().trim() : null);
        currentUser.setPhoto(fieldPhoto.getText() != null && !fieldPhoto.getText().isBlank() ? fieldPhoto.getText().trim() : null);
        currentUser.setBio(fieldBio.getText() != null && !fieldBio.getText().isBlank() ? fieldBio.getText().trim() : null);

        try {
            userServices.modifier(currentUser);
            SessionHolder.setCurrentUser(currentUser);
            if (frontController != null) frontController.refreshAvatar();
            showInfo("Profil enregistré", "Vos modifications ont été enregistrées.");
        } catch (SQLException e) {
            showError("Erreur", "Impossible d'enregistrer le profil.");
        }
    }

    private void showError(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }
}
