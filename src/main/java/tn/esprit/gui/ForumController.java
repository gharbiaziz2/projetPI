package tn.esprit.gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.util.StringConverter;
import tn.esprit.entities.Forum;
import tn.esprit.entities.User;
import tn.esprit.entities.Voyage;
import tn.esprit.services.ForumServices;
import tn.esprit.services.TranslationService;
import tn.esprit.services.UserServices;
import tn.esprit.services.VoyageServices;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class ForumController {

    @FXML private TableView<Forum> table;
    @FXML private TableColumn<Forum, String> colContenu;
    @FXML private TableColumn<Forum, String> colDateEnvoi;
    @FXML private TableColumn<Forum, String> colIdUser;
    @FXML private TableColumn<Forum, String> colIdVoyage;
    @FXML private TextField searchField;
    @FXML private Button btnFilter;
    @FXML private Button btnTranslate;

    private final ForumServices service = new ForumServices();
    private final TranslationService translationService = new TranslationService();
    private final UserServices userService = new UserServices();
    private final VoyageServices voyageService = new VoyageServices();
    private final ObservableList<Forum> list = FXCollections.observableArrayList();
    private final FilteredList<Forum> filteredList = new FilteredList<>(list, p -> true);
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private Map<Integer, String> userDisplay = new HashMap<>();
    private Map<Integer, String> voyageDisplay = new HashMap<>();

    @FXML
    public void initialize() {
        colContenu.setCellValueFactory(c -> new SimpleStringProperty(truncate(c.getValue().getContenu(), 50)));
        colDateEnvoi.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDateEnvoi() != null ? c.getValue().getDateEnvoi().format(DT_FMT) : ""));
        colIdUser.setCellValueFactory(c -> new SimpleStringProperty(userDisplay.getOrDefault(c.getValue().getIdUser(), String.valueOf(c.getValue().getIdUser()))));
        colIdVoyage.setCellValueFactory(c -> new SimpleStringProperty(voyageDisplay.getOrDefault(c.getValue().getIdVoyage(), String.valueOf(c.getValue().getIdVoyage()))));
        table.setItems(filteredList);
        if (searchField != null) searchField.textProperty().addListener((o, ov, nv) -> applyFilter());
        refresh();
    }

    private String filterContenu;

    private void applyFilter() {
        String q = searchField != null ? (searchField.getText() == null ? "" : searchField.getText()).trim().toLowerCase() : "";
        filteredList.setPredicate(f -> {
            if (filterContenu != null && !filterContenu.isEmpty() && (f.getContenu() == null || !f.getContenu().toLowerCase().contains(filterContenu.toLowerCase()))) return false;
            if (q.isEmpty()) return true;
            return (f.getContenu() != null && f.getContenu().toLowerCase().contains(q)) || userDisplay.getOrDefault(f.getIdUser(), "").toLowerCase().contains(q) || voyageDisplay.getOrDefault(f.getIdVoyage(), "").toLowerCase().contains(q);
        });
    }

    @FXML
    private void onFilter() {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Filtrage");
        d.initModality(Modality.APPLICATION_MODAL);
        DialogStyleHelper.styleDialogPane(d.getDialogPane());
        TextField contenuF = new TextField(filterContenu != null ? filterContenu : "");
        contenuF.setPromptText("Contenu contient (vide = tous)");
        DialogStyleHelper.styleField(contenuF);
        GridPane g = DialogStyleHelper.buildGrid();
        DialogStyleHelper.addRow(g, 0, "Contenu contient", contenuF);
        d.getDialogPane().setContent(g);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(btn -> btn);
        if (d.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            filterContenu = contenuF.getText() != null && !contenuF.getText().isBlank() ? contenuF.getText().trim() : null;
            applyFilter();
        }
    }

    public void refresh() {
        try {
            userDisplay.clear();
            for (User u : userService.afficher()) userDisplay.put(u.getIdUser(), u.getNom() + " " + u.getPrenom());
            voyageDisplay.clear();
            for (Voyage v : voyageService.afficher()) voyageDisplay.put(v.getIdVoyage(), (v.getTypeVoyage() != null ? v.getTypeVoyage() : "") + " " + (v.getDateDepart() != null ? v.getDateDepart().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : ""));
            list.clear();
            list.addAll(service.afficher());
        } catch (SQLException e) { showError("Erreur", "Chargement impossible."); }
    }

    @FXML private void onAdd() {
        Forum f = new Forum();
        f.setDateEnvoi(LocalDateTime.now());
        if (showDialog(f, "Ajouter forum")) {
            try {
                service.ajouter(f);
                refresh();
            } catch (SQLException e) { showError("Erreur", "Ajout impossible."); }
        }
    }

    @FXML private void onEdit() {
        Forum f = table.getSelectionModel().getSelectedItem();
        if (f == null) { showError("Attention", "Selectionnez une ligne."); return; }
        if (showDialog(f, "Modifier forum")) {
            try {
                service.modifier(f);
                refresh();
            } catch (SQLException e) { showError("Erreur", "Modification impossible."); }
        }
    }

    @FXML private void onTranslate() {
        Forum f = table.getSelectionModel().getSelectedItem();
        if (f == null) { showError("Attention", "Sélectionnez un message."); return; }
        String text = f.getContenu();
        if (text == null || text.isBlank()) { showError("Attention", "Aucun contenu à traduire."); return; }
        Dialog<String> d = new Dialog<>();
        d.setTitle("Traduction");
        d.setHeaderText("Choisir la langue cible");
        ButtonType toEn = new ButtonType("Traduire en anglais", ButtonBar.ButtonData.OK_DONE);
        ButtonType toFr = new ButtonType("Traduire en français", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(toEn, toFr, ButtonType.CANCEL);
        d.setResultConverter(btn -> btn == toEn ? "en" : (btn == toFr ? "fr" : null));
        d.showAndWait().ifPresent(target -> {
            String source = "en".equals(target) ? "fr" : "en";
            String translated = translationService.translate(text, source, target);
            if (translated != null && !translated.isBlank()) {
                Alert a = new Alert(Alert.AlertType.INFORMATION);
                a.setTitle("Traduction");
                a.setHeaderText("en".equals(target) ? "Traduction en anglais" : "Traduction en français");
                a.setContentText(translated);
                a.showAndWait();
            } else {
                showError("Traduction", "Impossible de traduire. Vérifiez votre connexion.");
            }
        });
    }

    @FXML private void onDelete() {
        Forum f = table.getSelectionModel().getSelectedItem();
        if (f == null) { showError("Attention", "Selectionnez une ligne."); return; }
        if (confirm("Supprimer ce message ?")) {
            try {
                service.supprimer(f.getIdForum());
                refresh();
            } catch (SQLException e) { showError("Erreur", "Suppression impossible."); }
        }
    }

    private boolean showDialog(Forum f, String title) {
        Dialog<Forum> d = new Dialog<>();
        d.setTitle(title);
        d.initModality(Modality.APPLICATION_MODAL);
        DialogStyleHelper.styleDialogPane(d.getDialogPane());
        TextArea contenu = new TextArea(f.getContenu());
        contenu.setPrefRowCount(4);
        contenu.setPrefWidth(260);
        contenu.getStyleClass().add("crud-dialog-field");
        ComboBox<User> comboUser = new ComboBox<>();
        ComboBox<Voyage> comboVoyage = new ComboBox<>();
        comboUser.setConverter(new StringConverter<User>() {
            @Override public String toString(User u) { return u == null ? "" : u.getNom() + " " + u.getPrenom(); }
            @Override public User fromString(String s) { return null; }
        });
        comboVoyage.setConverter(new StringConverter<Voyage>() {
            @Override public String toString(Voyage v) {
                if (v == null) return "";
                String type = v.getTypeVoyage() != null ? v.getTypeVoyage() : "";
                String date = v.getDateDepart() != null ? v.getDateDepart().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
                return type + " - " + date;
            }
            @Override public Voyage fromString(String s) { return null; }
        });
        try {
            comboUser.getItems().addAll(userService.afficher());
            comboVoyage.getItems().addAll(voyageService.afficher());
        } catch (SQLException e) {}
        if (f.getIdUser() > 0) comboUser.getItems().stream().filter(u -> u.getIdUser() == f.getIdUser()).findFirst().ifPresent(comboUser.getSelectionModel()::select);
        else if (!comboUser.getItems().isEmpty()) comboUser.getSelectionModel().selectFirst();
        if (f.getIdVoyage() > 0) comboVoyage.getItems().stream().filter(v -> v.getIdVoyage() == f.getIdVoyage()).findFirst().ifPresent(comboVoyage.getSelectionModel()::select);
        else if (!comboVoyage.getItems().isEmpty()) comboVoyage.getSelectionModel().selectFirst();
        DialogStyleHelper.styleCombo(comboUser);
        DialogStyleHelper.styleCombo(comboVoyage);
        GridPane g = DialogStyleHelper.buildGrid();
        DialogStyleHelper.addRow(g, 0, "Contenu *", contenu);
        DialogStyleHelper.addRow(g, 1, "User *", comboUser);
        DialogStyleHelper.addRow(g, 2, "Voyage *", comboVoyage);
        VBox content = new VBox(new Label(title), g);
        content.getStyleClass().add("crud-dialog-content");
        ((Label) content.getChildren().get(0)).getStyleClass().add("crud-dialog-title");
        content.setSpacing(16);
        d.getDialogPane().setContent(content);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            if (contenu.getText() == null || contenu.getText().isBlank()) { showError("Validation", "Contenu obligatoire."); return null; }
            User selUser = comboUser.getSelectionModel().getSelectedItem();
            Voyage selVoyage = comboVoyage.getSelectionModel().getSelectedItem();
            if (selUser == null) { showError("Validation", "User obligatoire."); return null; }
            if (selVoyage == null) { showError("Validation", "Voyage obligatoire."); return null; }
            f.setContenu(contenu.getText().trim());
            f.setIdUser(selUser.getIdUser());
            f.setIdVoyage(selVoyage.getIdVoyage());
            if (f.getIdForum() == 0) f.setDateEnvoi(LocalDateTime.now());
            return f;
        });
        return d.showAndWait().orElse(null) != null;
    }

    private static String truncate(String s, int max) {
        if (s == null || s.isEmpty()) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private boolean confirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setContentText(msg);
        a.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        return a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void showError(String t, String m) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(t);
        a.setContentText(m);
        a.showAndWait();
    }
}
