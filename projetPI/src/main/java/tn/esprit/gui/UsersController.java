package tn.esprit.gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.services.UserServices;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class UsersController {

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> colNom;
    @FXML private TableColumn<User, String> colPrenom;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colStatut;
    @FXML private TableColumn<User, String> colDateCreation;
    @FXML private TableColumn<User, String> colTelephone;
    @FXML private TableColumn<User, String> colAdresse;
    @FXML private TableColumn<User, String> colPhoto;
    @FXML private TableColumn<User, String> colBio;
    @FXML private TextField searchField;
    @FXML private Button btnFilter;

    private final UserServices userServices = new UserServices();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final ObservableList<User> userList = FXCollections.observableArrayList();
    private final FilteredList<User> filteredList = new FilteredList<>(userList, p -> true);
    private User.Role filterRole;
    private User.Statut filterStatut;

    @FXML
    public void initialize() {
        colNom.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNom()));
        colPrenom.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getPrenom()));
        colEmail.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEmail()));
        colRole.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getRole().name()));
        colStatut.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatut().name()));
        colDateCreation.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDateCreation() != null ? cell.getValue().getDateCreation().format(DATE_FMT) : ""));
        colTelephone.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTelephone() != null ? cell.getValue().getTelephone() : ""));
        colAdresse.setCellValueFactory(cell -> new SimpleStringProperty(truncate(cell.getValue().getAdresse(), 30)));
        colPhoto.setCellValueFactory(cell -> new SimpleStringProperty(truncate(cell.getValue().getPhoto(), 25)));
        colBio.setCellValueFactory(cell -> new SimpleStringProperty(truncate(cell.getValue().getBio(), 30)));
        userTable.setItems(filteredList);
        if (searchField != null) searchField.textProperty().addListener((o, ov, nv) -> applyFilter());
        refresh();
    }

    private void applyFilter() {
        String q = searchField != null ? (searchField.getText() == null ? "" : searchField.getText()).trim().toLowerCase() : "";
        filteredList.setPredicate(u -> {
            if (filterRole != null && u.getRole() != filterRole) return false;
            if (filterStatut != null && u.getStatut() != filterStatut) return false;
            if (q.isEmpty()) return true;
            String nom = u.getNom() != null ? u.getNom().toLowerCase() : "";
            String prenom = u.getPrenom() != null ? u.getPrenom().toLowerCase() : "";
            String email = u.getEmail() != null ? u.getEmail().toLowerCase() : "";
            return nom.contains(q) || prenom.contains(q) || email.contains(q);
        });
    }

    @FXML
    private void onFilter() {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Filtrage");
        d.initModality(Modality.APPLICATION_MODAL);
        DialogStyleHelper.styleDialogPane(d.getDialogPane());
        ComboBox<User.Role> roleCombo = new ComboBox<>(FXCollections.observableArrayList(User.Role.values()));
        roleCombo.setPromptText("Tous les rôles");
        if (filterRole != null) roleCombo.getSelectionModel().select(filterRole);
        ComboBox<User.Statut> statutCombo = new ComboBox<>(FXCollections.observableArrayList(User.Statut.values()));
        statutCombo.setPromptText("Tous les statuts");
        if (filterStatut != null) statutCombo.getSelectionModel().select(filterStatut);
        javafx.scene.layout.GridPane g = DialogStyleHelper.buildGrid();
        DialogStyleHelper.addRow(g, 0, "Rôle", roleCombo);
        DialogStyleHelper.addRow(g, 1, "Statut", statutCombo);
        DialogStyleHelper.styleCombo(roleCombo);
        DialogStyleHelper.styleCombo(statutCombo);
        d.getDialogPane().setContent(g);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(btn -> btn);
        if (d.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            filterRole = roleCombo.getSelectionModel().getSelectedItem();
            filterStatut = statutCombo.getSelectionModel().getSelectedItem();
            applyFilter();
        }
    }

    public void refresh() {
        try {
            userList.clear();
            userList.addAll(userServices.afficher());
        } catch (SQLException e) {
            showError("Erreur", "Impossible de charger les utilisateurs.");
        }
    }

    @FXML
    private void onAdd() {
        User u = new User();
        u.setStatut(User.Statut.ACTIVE);
        u.setDateCreation(LocalDate.now());
        u.setRole(User.Role.CLIENT);
        if (showUserDialog(u, "Ajouter un utilisateur")) {
            try {
                userServices.ajouter(u);
                refresh();
            } catch (SQLException e) {
                showError("Erreur", "Impossible d'ajouter l'utilisateur.");
            }
        }
    }

    @FXML
    private void onEdit() {
        User u = userTable.getSelectionModel().getSelectedItem();
        if (u == null) {
            showError("Attention", "Sélectionnez un utilisateur.");
            return;
        }
        if (showUserDialog(u, "Modifier l'utilisateur")) {
            try {
                userServices.modifier(u);
                refresh();
            } catch (SQLException e) {
                showError("Erreur", "Impossible de modifier l'utilisateur.");
            }
        }
    }

    @FXML
    private void onDelete() {
        User u = userTable.getSelectionModel().getSelectedItem();
        if (u == null) {
            showError("Attention", "Sélectionnez un utilisateur.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer");
        confirm.setHeaderText("Supprimer cet utilisateur ?");
        confirm.setContentText(u.getNom() + " " + u.getPrenom() + " - " + u.getEmail());
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                userServices.supprimer(u.getIdUser());
                refresh();
            } catch (SQLException e) {
                showError("Erreur", "Impossible de supprimer l'utilisateur.");
            }
        }
    }

    private boolean showUserDialog(User u, String title) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.initModality(Modality.APPLICATION_MODAL);
        DialogStyleHelper.styleDialogPane(dialog.getDialogPane());

        TextField nom = new TextField(u.getNom());
        TextField prenom = new TextField(u.getPrenom());
        TextField email = new TextField(u.getEmail());
        email.setEditable(u.getIdUser() == 0);
        PasswordField pwd = new PasswordField();
        pwd.setPromptText(u.getIdUser() != 0 ? "Laisser vide pour inchangé" : "Mot de passe *");
        ComboBox<User.Role> role = new ComboBox<>(FXCollections.observableArrayList(User.Role.values()));
        role.getSelectionModel().select(u.getRole());
        ComboBox<User.Statut> statut = new ComboBox<>(FXCollections.observableArrayList(User.Statut.values()));
        statut.getSelectionModel().select(u.getStatut());
        TextField telephone = new TextField(u.getTelephone() != null ? u.getTelephone() : "");
        TextField adresse = new TextField(u.getAdresse() != null ? u.getAdresse() : "");
        TextField photo = new TextField(u.getPhoto() != null ? u.getPhoto() : "");
        photo.setPromptText("URL ou chemin du fichier");
        TextArea bio = new TextArea(u.getBio() != null ? u.getBio() : "");
        bio.setPromptText("Biographie (optionnel)");
        bio.setPrefRowCount(3);
        bio.setWrapText(true);
        Label dateCreationLabel = new Label(u.getDateCreation() != null ? u.getDateCreation().format(DATE_FMT) : "—");
        dateCreationLabel.getStyleClass().add("crud-dialog-label");

        javafx.scene.layout.GridPane grid = DialogStyleHelper.buildGrid();
        DialogStyleHelper.addRow(grid, 0, "Nom *", nom);
        DialogStyleHelper.addRow(grid, 1, "Prénom *", prenom);
        DialogStyleHelper.addRow(grid, 2, "Email *", email);
        DialogStyleHelper.addRow(grid, 3, "Mot de passe", pwd);
        DialogStyleHelper.addRow(grid, 4, "Rôle", role);
        DialogStyleHelper.addRow(grid, 5, "Statut", statut);
        DialogStyleHelper.addRow(grid, 6, "Date création", dateCreationLabel);
        DialogStyleHelper.addRow(grid, 7, "Téléphone", telephone);
        DialogStyleHelper.addRow(grid, 8, "Adresse", adresse);
        DialogStyleHelper.addRow(grid, 9, "Photo", photo);
        DialogStyleHelper.addRow(grid, 10, "Bio", bio);

        DialogStyleHelper.styleField(nom);
        DialogStyleHelper.styleField(prenom);
        DialogStyleHelper.styleField(email);
        DialogStyleHelper.styleField(pwd);
        DialogStyleHelper.styleCombo(role);
        DialogStyleHelper.styleCombo(statut);
        DialogStyleHelper.styleField(telephone);
        DialogStyleHelper.styleField(adresse);
        DialogStyleHelper.styleField(photo);
        bio.getStyleClass().add("crud-dialog-field");
        bio.setPrefWidth(260);

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(new Label(title), grid);
        content.getStyleClass().add("crud-dialog-content");
        ((Label) content.getChildren().get(0)).getStyleClass().add("crud-dialog-title");
        content.setSpacing(16);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            if (nom.getText() == null || nom.getText().isBlank() || prenom.getText() == null || prenom.getText().isBlank()) {
                showError("Validation", "Nom et prénom obligatoires.");
                return null;
            }
            if (email.getText() == null || email.getText().isBlank()) {
                showError("Validation", "Email obligatoire.");
                return null;
            }
            String emailVal = email.getText().trim();
            if (!emailVal.contains("@") || emailVal.indexOf("@") == 0 || emailVal.indexOf("@") == emailVal.length() - 1) {
                showError("Validation", "Email invalide (doit contenir @ avec un domaine).");
                return null;
            }
            if (u.getIdUser() == 0 && (pwd.getText() == null || pwd.getText().isBlank())) {
                showError("Validation", "Mot de passe obligatoire pour un nouvel utilisateur.");
                return null;
            }
            if (u.getIdUser() == 0 && pwd.getText() != null && pwd.getText().length() < 6) {
                showError("Validation", "Le mot de passe doit contenir au moins 6 caractères.");
                return null;
            }
            if (role.getSelectionModel().getSelectedItem() == null) {
                showError("Validation", "Rôle obligatoire.");
                return null;
            }
            if (statut.getSelectionModel().getSelectedItem() == null) {
                showError("Validation", "Statut obligatoire.");
                return null;
            }
            u.setNom(nom.getText().trim());
            u.setPrenom(prenom.getText().trim());
            u.setEmail(emailVal);
            if (pwd.getText() != null && !pwd.getText().isBlank()) u.setMotDePasse(pwd.getText());
            u.setRole(role.getSelectionModel().getSelectedItem());
            u.setStatut(statut.getSelectionModel().getSelectedItem());
            u.setTelephone(telephone.getText() != null && !telephone.getText().isBlank() ? telephone.getText().trim() : null);
            u.setAdresse(adresse.getText() != null && !adresse.getText().isBlank() ? adresse.getText().trim() : null);
            u.setPhoto(photo.getText() != null && !photo.getText().isBlank() ? photo.getText().trim() : null);
            u.setBio(bio.getText() != null && !bio.getText().isBlank() ? bio.getText().trim() : null);
            if (u.getIdUser() == 0) u.setDateCreation(LocalDate.now());
            return u;
        });

        Optional<User> result = dialog.showAndWait();
        return result.isPresent();
    }

    private static String truncate(String s, int maxLen) {
        if (s == null || s.isEmpty()) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }

    private void showError(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }
}
