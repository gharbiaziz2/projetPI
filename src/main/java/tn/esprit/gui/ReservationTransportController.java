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
import tn.esprit.entities.User;
import tn.esprit.services.ReservationTransportServices;
import tn.esprit.services.UserServices;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class ReservationTransportController {

    @FXML private TableView<ReservationTransport> table;
    @FXML private TableColumn<ReservationTransport, String> colDateReservation;
    @FXML private TableColumn<ReservationTransport, String> colStatut;
    @FXML private TableColumn<ReservationTransport, Double> colPrixTotal;
    @FXML private TableColumn<ReservationTransport, String> colUser;
    @FXML private TextField searchField;
    @FXML private Button btnFilter;

    private final ReservationTransportServices service = new ReservationTransportServices();
    private final UserServices userService = new UserServices();
    private final ObservableList<ReservationTransport> list = FXCollections.observableArrayList();
    private final FilteredList<ReservationTransport> filteredList = new FilteredList<>(list, p -> true);
    private static final DateTimeFormatter D_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private Map<Integer, String> userDisplay = new HashMap<>();
    private ReservationTransport.StatutReservation filterStatut;

    @FXML
    public void initialize() {
        colDateReservation.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDateReservation() != null ? c.getValue().getDateReservation().format(D_FMT) : ""));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatut() != null ? c.getValue().getStatut().name() : ""));
        colPrixTotal.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPrixTotal()));
        colUser.setCellValueFactory(c -> new SimpleStringProperty(userDisplay.getOrDefault(c.getValue().getIdUser(), "")));
        table.setItems(filteredList);
        if (searchField != null) searchField.textProperty().addListener((o, ov, nv) -> applyFilter());
        refresh();
    }

    private void applyFilter() {
        String q = searchField != null ? (searchField.getText() == null ? "" : searchField.getText()).trim().toLowerCase() : "";
        filteredList.setPredicate(r -> {
            if (filterStatut != null && r.getStatut() != filterStatut) return false;
            if (q.isEmpty()) return true;
            return (r.getStatut() != null && r.getStatut().name().toLowerCase().contains(q)) || String.valueOf(r.getPrixTotal()).contains(q) || userDisplay.getOrDefault(r.getIdUser(), "").toLowerCase().contains(q);
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
            list.clear();
            list.addAll(service.afficher());
        } catch (SQLException e) { showError("Erreur", "Chargement impossible."); }
    }

    @FXML private void onAdd() {
        ReservationTransport r = new ReservationTransport();
        if (showDialog(r, "Ajouter reservation transport")) {
            try {
                service.ajouter(r);
                refresh();
            } catch (SQLException e) { showError("Erreur", "Ajout impossible."); }
        }
    }

    @FXML private void onEdit() {
        ReservationTransport r = table.getSelectionModel().getSelectedItem();
        if (r == null) { showError("Attention", "Selectionnez une ligne."); return; }
        if (showDialog(r, "Modifier reservation transport")) {
            try {
                service.modifier(r);
                refresh();
            } catch (SQLException e) { showError("Erreur", "Modification impossible."); }
        }
    }

    @FXML private void onDelete() {
        ReservationTransport r = table.getSelectionModel().getSelectedItem();
        if (r == null) { showError("Attention", "Selectionnez une ligne."); return; }
        if (confirm("Supprimer cette reservation ?")) {
            try {
                service.supprimer(r.getIdReservationTransport());
                refresh();
            } catch (SQLException e) { showError("Erreur", "Suppression impossible."); }
        }
    }

    private boolean showDialog(ReservationTransport r, String title) {
        Dialog<ReservationTransport> d = new Dialog<>();
        d.setTitle(title);
        d.initModality(Modality.APPLICATION_MODAL);
        DialogStyleHelper.styleDialogPane(d.getDialogPane());
        DatePicker dateResa = new DatePicker(r.getDateReservation());
        ComboBox<ReservationTransport.StatutReservation> statutCombo = new ComboBox<>(FXCollections.observableArrayList(ReservationTransport.StatutReservation.values()));
        statutCombo.getSelectionModel().select(r.getStatut());
        TextField prix = new TextField(r.getPrixTotal() > 0 ? String.valueOf(r.getPrixTotal()) : "");
        ComboBox<User> comboUser = new ComboBox<>();
        comboUser.setConverter(new StringConverter<User>() {
            @Override public String toString(User u) { return u == null ? "" : u.getNom() + " " + u.getPrenom(); }
            @Override public User fromString(String s) { return null; }
        });
        try {
            comboUser.getItems().addAll(userService.afficher());
        } catch (SQLException e) {}
        if (r.getIdUser() > 0) comboUser.getItems().stream().filter(u -> u.getIdUser() == r.getIdUser()).findFirst().ifPresent(comboUser.getSelectionModel()::select);
        else if (!comboUser.getItems().isEmpty()) comboUser.getSelectionModel().selectFirst();
        GridPane g = DialogStyleHelper.buildGrid();
        DialogStyleHelper.addRow(g, 0, "Date réservation *", dateResa);
        DialogStyleHelper.addRow(g, 1, "Statut *", statutCombo);
        DialogStyleHelper.addRow(g, 2, "Prix total *", prix);
        DialogStyleHelper.addRow(g, 3, "User *", comboUser);
        DialogStyleHelper.styleField(prix);
        DialogStyleHelper.styleDatePicker(dateResa);
        DialogStyleHelper.styleCombo(statutCombo);
        DialogStyleHelper.styleCombo(comboUser);
        VBox content = new VBox(new Label(title), g);
        content.getStyleClass().add("crud-dialog-content");
        ((Label) content.getChildren().get(0)).getStyleClass().add("crud-dialog-title");
        content.setSpacing(16);
        d.getDialogPane().setContent(content);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            LocalDate dr = dateResa.getValue();
            if (dr == null) { showError("Validation", "Date réservation obligatoire."); return null; }
            if (statutCombo.getSelectionModel().getSelectedItem() == null) { showError("Validation", "Statut obligatoire."); return null; }
            if (prix.getText() == null || prix.getText().isBlank()) { showError("Validation", "Prix total obligatoire."); return null; }
            double p;
            try { p = Double.parseDouble(prix.getText().trim()); if (p < 0) throw new NumberFormatException(); } catch (Exception e) { showError("Validation", "Prix invalide (nombre >= 0)."); return null; }
            User selUser = comboUser.getSelectionModel().getSelectedItem();
            if (selUser == null) { showError("Validation", "User obligatoire."); return null; }
            r.setDateReservation(dr);
            r.setStatut(statutCombo.getSelectionModel().getSelectedItem());
            r.setPrixTotal(p);
            r.setIdUser(selUser.getIdUser());
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
