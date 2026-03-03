package tn.esprit.gui;

import javafx.beans.property.SimpleObjectProperty;
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
import tn.esprit.entities.ReservationTransport;
import tn.esprit.entities.ReservationVoyage;
import tn.esprit.gui.DialogStyleHelper;
import tn.esprit.entities.User;
import tn.esprit.entities.Voyage;
import tn.esprit.services.ReservationVoyageServices;
import tn.esprit.services.UserServices;
import tn.esprit.services.VoyageServices;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class ReservationVoyageController {

    @FXML private TableView<ReservationVoyage> table;
    @FXML private TableColumn<ReservationVoyage, String> colDateReservation;
    @FXML private TableColumn<ReservationVoyage, String> colStatut;
    @FXML private TableColumn<ReservationVoyage, BigDecimal> colMontant;
    @FXML private TableColumn<ReservationVoyage, String> colUser;
    @FXML private TableColumn<ReservationVoyage, String> colVoyage;
    @FXML private TextField searchField;
    @FXML private Button btnFilter;

    private final ReservationVoyageServices service = new ReservationVoyageServices();
    private final UserServices userService = new UserServices();
    private final VoyageServices voyageService = new VoyageServices();
    private final ObservableList<ReservationVoyage> list = FXCollections.observableArrayList();
    private final FilteredList<ReservationVoyage> filteredList = new FilteredList<>(list, p -> true);
    private static final DateTimeFormatter D_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private Map<Integer, String> userDisplay = new HashMap<>();
    private Map<Integer, String> voyageDisplay = new HashMap<>();
    private ReservationTransport.StatutReservation filterStatut;

    @FXML
    public void initialize() {
        colDateReservation.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDateReservation() != null ? c.getValue().getDateReservation().format(D_FMT) : ""));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatut() != null ? c.getValue().getStatut().name() : ""));
        colMontant.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getMontantTotal()));
        colUser.setCellValueFactory(c -> new SimpleStringProperty(userDisplay.getOrDefault(c.getValue().getIdUser(), "")));
        colVoyage.setCellValueFactory(c -> new SimpleStringProperty(voyageDisplay.getOrDefault(c.getValue().getIdVoyage(), "")));
        table.setItems(filteredList);
        if (searchField != null) searchField.textProperty().addListener((o, ov, nv) -> applyFilter());
        refresh();
    }

    private void applyFilter() {
        String q = searchField != null ? (searchField.getText() == null ? "" : searchField.getText()).trim().toLowerCase() : "";
        filteredList.setPredicate(r -> {
            if (filterStatut != null && r.getStatut() != filterStatut) return false;
            if (q.isEmpty()) return true;
            return (r.getStatut() != null && r.getStatut().name().toLowerCase().contains(q)) || (r.getMontantTotal() != null && r.getMontantTotal().toString().contains(q)) || userDisplay.getOrDefault(r.getIdUser(), "").toLowerCase().contains(q) || voyageDisplay.getOrDefault(r.getIdVoyage(), "").toLowerCase().contains(q);
        });
    }

    @FXML
    private void onFilter() {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Filtrage");
        d.initModality(Modality.APPLICATION_MODAL);
        DialogStyleHelper.styleDialogPane(d.getDialogPane());
        ComboBox<ReservationTransport.StatutReservation> statutCombo = new ComboBox<>(FXCollections.observableArrayList(ReservationTransport.StatutReservation.values()));
        statutCombo.setPromptText("Tous les statuts");
        if (filterStatut != null) statutCombo.getSelectionModel().select(filterStatut);
        DialogStyleHelper.styleCombo(statutCombo);
        GridPane g = DialogStyleHelper.buildGrid();
        DialogStyleHelper.addRow(g, 0, "Statut", statutCombo);
        d.getDialogPane().setContent(g);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(btn -> btn);
        if (d.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            filterStatut = statutCombo.getSelectionModel().getSelectedItem();
            applyFilter();
        }
    }

    public void refresh() {
        try {
            userDisplay.clear();
            for (User u : userService.afficher()) userDisplay.put(u.getIdUser(), u.getNom() + " " + u.getPrenom());
            voyageDisplay.clear();
            for (Voyage v : voyageService.afficher()) voyageDisplay.put(v.getIdVoyage(), (v.getTypeVoyage() != null ? v.getTypeVoyage() : "") + " " + (v.getDateDepart() != null ? v.getDateDepart().format(D_FMT) : ""));
            list.clear();
            list.addAll(service.afficher());
        } catch (SQLException e) { showError("Erreur", "Chargement impossible."); }
    }

    @FXML private void onAdd() {
        ReservationVoyage r = new ReservationVoyage();
        if (showDialog(r, "Ajouter reservation voyage")) {
            try {
                service.ajouter(r);
                refresh();
            } catch (SQLException e) { showError("Erreur", "Ajout impossible."); }
        }
    }

    @FXML private void onEdit() {
        ReservationVoyage r = table.getSelectionModel().getSelectedItem();
        if (r == null) { showError("Attention", "Selectionnez une ligne."); return; }
        if (showDialog(r, "Modifier reservation voyage")) {
            try {
                service.modifier(r);
                refresh();
            } catch (SQLException e) { showError("Erreur", "Modification impossible."); }
        }
    }

    @FXML private void onDelete() {
        ReservationVoyage r = table.getSelectionModel().getSelectedItem();
        if (r == null) { showError("Attention", "Selectionnez une ligne."); return; }
        if (confirm("Supprimer cette reservation ?")) {
            try {
                service.supprimer(r.getIdReservationVoyage());
                refresh();
            } catch (SQLException e) { showError("Erreur", "Suppression impossible."); }
        }
    }

    private boolean showDialog(ReservationVoyage r, String title) {
        Dialog<ReservationVoyage> d = new Dialog<>();
        d.setTitle(title);
        d.initModality(Modality.APPLICATION_MODAL);
        DialogStyleHelper.styleDialogPane(d.getDialogPane());
        DatePicker dateResa = new DatePicker(r.getDateReservation());
        ComboBox<ReservationTransport.StatutReservation> statutCombo = new ComboBox<>(FXCollections.observableArrayList(ReservationTransport.StatutReservation.values()));
        statutCombo.getSelectionModel().select(r.getStatut());
        TextField montant = new TextField(r.getMontantTotal() != null ? r.getMontantTotal().toString() : "");
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
                String date = v.getDateDepart() != null ? v.getDateDepart().format(D_FMT) : "";
                return type + " - " + date;
            }
            @Override public Voyage fromString(String s) { return null; }
        });
        try {
            comboUser.getItems().addAll(userService.afficher());
            comboVoyage.getItems().addAll(voyageService.afficher());
        } catch (SQLException e) {}
        if (r.getIdUser() > 0) comboUser.getItems().stream().filter(u -> u.getIdUser() == r.getIdUser()).findFirst().ifPresent(comboUser.getSelectionModel()::select);
        else if (!comboUser.getItems().isEmpty()) comboUser.getSelectionModel().selectFirst();
        if (r.getIdVoyage() > 0) comboVoyage.getItems().stream().filter(v -> v.getIdVoyage() == r.getIdVoyage()).findFirst().ifPresent(comboVoyage.getSelectionModel()::select);
        else if (!comboVoyage.getItems().isEmpty()) comboVoyage.getSelectionModel().selectFirst();
        GridPane g = DialogStyleHelper.buildGrid();
        DialogStyleHelper.addRow(g, 0, "Date *", dateResa);
        DialogStyleHelper.addRow(g, 1, "Statut *", statutCombo);
        DialogStyleHelper.addRow(g, 2, "Montant *", montant);
        DialogStyleHelper.addRow(g, 3, "User *", comboUser);
        DialogStyleHelper.addRow(g, 4, "Voyage *", comboVoyage);
        DialogStyleHelper.styleField(montant);
        DialogStyleHelper.styleDatePicker(dateResa);
        DialogStyleHelper.styleCombo(statutCombo);
        DialogStyleHelper.styleCombo(comboUser);
        DialogStyleHelper.styleCombo(comboVoyage);
        VBox content = new VBox(new Label(title), g);
        content.getStyleClass().add("crud-dialog-content");
        ((Label) content.getChildren().get(0)).getStyleClass().add("crud-dialog-title");
        content.setSpacing(16);
        d.getDialogPane().setContent(content);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            LocalDate dr = dateResa.getValue();
            if (dr == null) { showError("Validation", "Date obligatoire."); return null; }
            if (statutCombo.getSelectionModel().getSelectedItem() == null) { showError("Validation", "Statut obligatoire."); return null; }
            if (montant.getText() == null || montant.getText().isBlank()) { showError("Validation", "Montant obligatoire."); return null; }
            BigDecimal m;
            try { m = new BigDecimal(montant.getText().trim()); if (m.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException(); } catch (Exception e) { showError("Validation", "Montant invalide (nombre >= 0)."); return null; }
            User selUser = comboUser.getSelectionModel().getSelectedItem();
            Voyage selVoyage = comboVoyage.getSelectionModel().getSelectedItem();
            if (selUser == null) { showError("Validation", "User obligatoire."); return null; }
            if (selVoyage == null) { showError("Validation", "Voyage obligatoire."); return null; }
            r.setDateReservation(dr);
            r.setStatut(statutCombo.getSelectionModel().getSelectedItem());
            r.setMontantTotal(m);
            r.setIdUser(selUser.getIdUser());
            r.setIdVoyage(selVoyage.getIdVoyage());
            return r;
        });
        return d.showAndWait().orElse(null) != null;
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
