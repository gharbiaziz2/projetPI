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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import tn.esprit.entities.HotelChambre;
import tn.esprit.entities.ReservationChambre;
import tn.esprit.entities.User;
import tn.esprit.services.HotelChambreServices;
import tn.esprit.services.ReservationHotelServices;
import tn.esprit.services.UserServices;

public class ReservationHotelController {

    @FXML private TableView<ReservationChambre> table;
    @FXML private TableColumn<ReservationChambre, String> colDateCheckin;
    @FXML private TableColumn<ReservationChambre, String> colDateCheckout;
    @FXML private TableColumn<ReservationChambre, Double> colPrixTotal;
    @FXML private TableColumn<ReservationChambre, String> colUser;
    @FXML private TableColumn<ReservationChambre, String> colHotel;
    @FXML private TextField searchField;
    @FXML private Button btnFilter;

    private final ReservationHotelServices service = new ReservationHotelServices();
    private final UserServices userService = new UserServices();
    private final HotelChambreServices chambreService = new HotelChambreServices();
    private final ObservableList<ReservationChambre> list = FXCollections.observableArrayList();
    private final FilteredList<ReservationChambre> filteredList = new FilteredList<>(list, p -> true);
    private static final DateTimeFormatter D_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private Map<Integer, String> userDisplay = new HashMap<>();
    private Map<Integer, String> chambreDisplay = new HashMap<>();
    private Double filterPrixMax;

    @FXML
    public void initialize() {
        colDateCheckin.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDateDebut() != null ? c.getValue().getDateDebut().format(D_FMT) : ""));
        colDateCheckout.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDateFin() != null ? c.getValue().getDateFin().format(D_FMT) : ""));
        colPrixTotal.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getMontantTotal()));
        colUser.setCellValueFactory(c -> new SimpleStringProperty(userDisplay.getOrDefault(c.getValue().getIdUser(), "")));
        colHotel.setCellValueFactory(c -> new SimpleStringProperty(chambreDisplay.getOrDefault(c.getValue().getIdChambre(), "")));
        table.setItems(filteredList);
        if (searchField != null) searchField.textProperty().addListener((o, ov, nv) -> applyFilter());
        refresh();
    }

    private void applyFilter() {
        String q = searchField != null ? (searchField.getText() == null ? "" : searchField.getText()).trim().toLowerCase() : "";
        filteredList.setPredicate(r -> {
            if (filterPrixMax != null && r.getMontantTotal() > filterPrixMax) return false;
            if (q.isEmpty()) return true;
            return String.valueOf(r.getMontantTotal()).contains(q) || userDisplay.getOrDefault(r.getIdUser(), "").toLowerCase().contains(q) || chambreDisplay.getOrDefault(r.getIdChambre(), "").toLowerCase().contains(q);
        });
    }

    @FXML
    private void onFilter() {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Filtrage");
        d.initModality(Modality.APPLICATION_MODAL);
        DialogStyleHelper.styleDialogPane(d.getDialogPane());
        TextField prixMaxF = new TextField(filterPrixMax != null ? filterPrixMax.toString() : "");
        prixMaxF.setPromptText("Prix max (vide = tous)");
        DialogStyleHelper.styleField(prixMaxF);
        GridPane g = DialogStyleHelper.buildGrid();
        DialogStyleHelper.addRow(g, 0, "Prix max", prixMaxF);
        d.getDialogPane().setContent(g);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(btn -> btn);
        if (d.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try { filterPrixMax = prixMaxF.getText() != null && !prixMaxF.getText().isBlank() ? Double.parseDouble(prixMaxF.getText().trim()) : null; } catch (Exception e) { filterPrixMax = null; }
            applyFilter();
        }
    }

    public void refresh() {
        try {
            userDisplay.clear();
            for (User u : userService.afficher()) userDisplay.put(u.getIdUser(), u.getNom() + " " + u.getPrenom());
            chambreDisplay.clear();
            for (HotelChambre c : chambreService.afficher()) chambreDisplay.put(c.getIdChambre(), c.getNumeroChambre() + " (" + c.getTypeChambre() + ")");
            list.clear();
            list.addAll(service.afficher());
        } catch (SQLException e) { showError("Erreur", "Chargement impossible."); }
    }

    @FXML private void onAdd() {
        ReservationChambre r = new ReservationChambre();
        r.setDateDebut(java.time.LocalDate.now());
        r.setDateFin(java.time.LocalDate.now().plusDays(1));
        if (showDialog(r, "Ajouter reservation chambre")) {
            try {
                service.ajouter(r);
                refresh();
            } catch (SQLException e) { showError("Erreur", "Ajout impossible."); }
        }
    }

    @FXML private void onEdit() {
        ReservationChambre r = table.getSelectionModel().getSelectedItem();
        if (r == null) { showError("Attention", "Selectionnez une ligne."); return; }
        if (showDialog(r, "Modifier reservation chambre")) {
            try {
                service.modifier(r);
                refresh();
            } catch (SQLException e) { showError("Erreur", "Modification impossible."); }
        }
    }

    @FXML private void onDelete() {
        ReservationChambre r = table.getSelectionModel().getSelectedItem();
        if (r == null) { showError("Attention", "Selectionnez une ligne."); return; }
        if (confirm("Supprimer cette reservation ?")) {
            try {
                service.supprimer(r.getIdReservationChambre());
                refresh();
            } catch (SQLException e) { showError("Erreur", "Suppression impossible."); }
        }
    }

    private boolean showDialog(ReservationChambre r, String title) {
        Dialog<ReservationChambre> d = new Dialog<>();
        d.setTitle(title);
        d.initModality(Modality.APPLICATION_MODAL);
        DialogStyleHelper.styleDialogPane(d.getDialogPane());
        DatePicker dateIn = new DatePicker(r.getDateDebut() != null ? r.getDateDebut() : java.time.LocalDate.now());
        DatePicker dateOut = new DatePicker(r.getDateFin() != null ? r.getDateFin() : java.time.LocalDate.now().plusDays(1));
        TextField prix = new TextField(r.getMontantTotal() > 0 ? String.valueOf(r.getMontantTotal()) : "");
        TextField statut = new TextField(r.getStatut() != null ? r.getStatut() : "");
        ComboBox<User> comboUser = new ComboBox<>();
        ComboBox<HotelChambre> comboChambre = new ComboBox<>();
        Label lblCalcul = new Label("Total auto-calculé");
        comboUser.setConverter(new javafx.util.StringConverter<User>() {
            @Override public String toString(User u) { return u == null ? "" : u.getNom() + " " + u.getPrenom(); }
            @Override public User fromString(String s) { return null; }
        });
        comboChambre.setConverter(new javafx.util.StringConverter<HotelChambre>() {
            @Override public String toString(HotelChambre c) { return c == null ? "" : c.getNumeroChambre() + " (" + c.getTypeChambre() + ")"; }
            @Override public HotelChambre fromString(String s) { return null; }
        });
        
        // Calcul automatique du montant
        Runnable calculateTotal = () -> {
            LocalDate din = dateIn.getValue();
            LocalDate dout = dateOut.getValue();
            HotelChambre sel = comboChambre.getSelectionModel().getSelectedItem();
            if (din != null && dout != null && sel != null && dout.isAfter(din)) {
                long days = java.time.temporal.ChronoUnit.DAYS.between(din, dout);
                double total = sel.getPrixChambre() * days;
                prix.setText(String.format("%.2f", total));
                lblCalcul.setText("Total: " + String.format("%.2f", total) + " (" + days + " jours × " + sel.getPrixChambre() + ")");
            }
        };
        
        dateIn.setOnAction(e -> calculateTotal.run());
        dateOut.setOnAction(e -> calculateTotal.run());
        comboChambre.setOnAction(e -> calculateTotal.run());
        
        try {
            comboUser.getItems().addAll(userService.afficher());
            comboChambre.getItems().addAll(chambreService.afficher());
        } catch (SQLException e) {}
        if (r.getIdUser() > 0) comboUser.getItems().stream().filter(u -> u.getIdUser() == r.getIdUser()).findFirst().ifPresent(comboUser.getSelectionModel()::select);
        else if (!comboUser.getItems().isEmpty()) comboUser.getSelectionModel().selectFirst();
        if (r.getIdChambre() > 0) comboChambre.getItems().stream().filter(c -> c.getIdChambre() == r.getIdChambre()).findFirst().ifPresent(comboChambre.getSelectionModel()::select);
        else if (!comboChambre.getItems().isEmpty()) comboChambre.getSelectionModel().selectFirst();
        calculateTotal.run();
        
        GridPane g = DialogStyleHelper.buildGrid();
        DialogStyleHelper.addRow(g, 0, "Date debut *", dateIn);
        DialogStyleHelper.addRow(g, 1, "Date fin *", dateOut);
        DialogStyleHelper.addRow(g, 2, "Montant total *", prix);
        DialogStyleHelper.addRow(g, 3, "", lblCalcul);
        DialogStyleHelper.addRow(g, 4, "Statut", statut);
        DialogStyleHelper.addRow(g, 5, "User *", comboUser);
        DialogStyleHelper.addRow(g, 6, "Chambre *", comboChambre);
        DialogStyleHelper.styleField(prix);
        DialogStyleHelper.styleField(statut);
        DialogStyleHelper.styleDatePicker(dateIn);
        DialogStyleHelper.styleDatePicker(dateOut);
        DialogStyleHelper.styleCombo(comboUser);
        DialogStyleHelper.styleCombo(comboChambre);
        VBox content = new VBox(new Label(title), g);
        content.getStyleClass().add("crud-dialog-content");
        ((Label) content.getChildren().get(0)).getStyleClass().add("crud-dialog-title");
        content.setSpacing(16);
        d.getDialogPane().setContent(content);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            LocalDate din = dateIn.getValue();
            LocalDate dout = dateOut.getValue();
            if (din == null) { showError("Validation", "Date debut obligatoire."); return null; }
            if (dout == null) { showError("Validation", "Date fin obligatoire."); return null; }
            if (!dout.isAfter(din)) { showError("Validation", "Date fin doit être après date debut."); return null; }
            if (prix.getText() == null || prix.getText().isBlank()) { showError("Validation", "Montant total obligatoire."); return null; }
            double p;
            try { p = Double.parseDouble(prix.getText().trim()); if (p < 0) throw new NumberFormatException(); } catch (Exception e) { showError("Validation", "Prix invalide (nombre >= 0)."); return null; }
            User selUser = comboUser.getSelectionModel().getSelectedItem();
            HotelChambre selChambre = comboChambre.getSelectionModel().getSelectedItem();
            if (selUser == null) { showError("Validation", "User obligatoire."); return null; }
            if (selChambre == null) { showError("Validation", "Chambre obligatoire."); return null; }
            r.setDateDebut(din);
            r.setDateFin(dout);
            r.setMontantTotal(p);
            r.setStatut(statut.getText() != null ? statut.getText().trim() : "");
            r.setIdUser(selUser.getIdUser());
            r.setIdChambre(selChambre.getIdChambre());
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
