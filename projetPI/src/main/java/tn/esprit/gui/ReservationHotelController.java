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
import tn.esprit.entities.Hotel;
import tn.esprit.entities.ReservationHotel;
import tn.esprit.entities.User;
import tn.esprit.services.HotelServices;
import tn.esprit.services.ReservationHotelServices;
import tn.esprit.services.UserServices;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class ReservationHotelController {

    @FXML private TableView<ReservationHotel> table;
    @FXML private TableColumn<ReservationHotel, String> colDateCheckin;
    @FXML private TableColumn<ReservationHotel, String> colDateCheckout;
    @FXML private TableColumn<ReservationHotel, BigDecimal> colPrixTotal;
    @FXML private TableColumn<ReservationHotel, String> colUser;
    @FXML private TableColumn<ReservationHotel, String> colHotel;
    @FXML private TextField searchField;
    @FXML private Button btnFilter;

    private final ReservationHotelServices service = new ReservationHotelServices();
    private final UserServices userService = new UserServices();
    private final HotelServices hotelService = new HotelServices();
    private final ObservableList<ReservationHotel> list = FXCollections.observableArrayList();
    private final FilteredList<ReservationHotel> filteredList = new FilteredList<>(list, p -> true);
    private static final DateTimeFormatter D_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private Map<Integer, String> userDisplay = new HashMap<>();
    private Map<Integer, String> hotelDisplay = new HashMap<>();
    private BigDecimal filterPrixMax;

    @FXML
    public void initialize() {
        colDateCheckin.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDateCheckin() != null ? c.getValue().getDateCheckin().format(D_FMT) : ""));
        colDateCheckout.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDateCheckout() != null ? c.getValue().getDateCheckout().format(D_FMT) : ""));
        colPrixTotal.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPrixTotal()));
        colUser.setCellValueFactory(c -> new SimpleStringProperty(userDisplay.getOrDefault(c.getValue().getIdUser(), "")));
        colHotel.setCellValueFactory(c -> new SimpleStringProperty(hotelDisplay.getOrDefault(c.getValue().getIdHotel(), "")));
        table.setItems(filteredList);
        if (searchField != null) searchField.textProperty().addListener((o, ov, nv) -> applyFilter());
        refresh();
    }

    private void applyFilter() {
        String q = searchField != null ? (searchField.getText() == null ? "" : searchField.getText()).trim().toLowerCase() : "";
        filteredList.setPredicate(r -> {
            if (filterPrixMax != null && (r.getPrixTotal() == null || r.getPrixTotal().compareTo(filterPrixMax) > 0)) return false;
            if (q.isEmpty()) return true;
            return (r.getPrixTotal() != null && r.getPrixTotal().toString().contains(q)) || userDisplay.getOrDefault(r.getIdUser(), "").toLowerCase().contains(q) || hotelDisplay.getOrDefault(r.getIdHotel(), "").toLowerCase().contains(q);
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
            try { filterPrixMax = prixMaxF.getText() != null && !prixMaxF.getText().isBlank() ? new BigDecimal(prixMaxF.getText().trim()) : null; } catch (Exception e) { filterPrixMax = null; }
            applyFilter();
        }
    }

    public void refresh() {
        try {
            userDisplay.clear();
            for (User u : userService.afficher()) userDisplay.put(u.getIdUser(), u.getNom() + " " + u.getPrenom());
            hotelDisplay.clear();
            for (Hotel h : hotelService.afficher()) hotelDisplay.put(h.getIdHotel(), h.getNom());
            list.clear();
            list.addAll(service.afficher());
        } catch (SQLException e) { showError("Erreur", "Chargement impossible."); }
    }

    @FXML private void onAdd() {
        ReservationHotel r = new ReservationHotel();
        if (showDialog(r, "Ajouter reservation hotel")) {
            try {
                service.ajouter(r);
                refresh();
            } catch (SQLException e) { showError("Erreur", "Ajout impossible."); }
        }
    }

    @FXML private void onEdit() {
        ReservationHotel r = table.getSelectionModel().getSelectedItem();
        if (r == null) { showError("Attention", "Selectionnez une ligne."); return; }
        if (showDialog(r, "Modifier reservation hotel")) {
            try {
                service.modifier(r);
                refresh();
            } catch (SQLException e) { showError("Erreur", "Modification impossible."); }
        }
    }

    @FXML private void onDelete() {
        ReservationHotel r = table.getSelectionModel().getSelectedItem();
        if (r == null) { showError("Attention", "Selectionnez une ligne."); return; }
        if (confirm("Supprimer cette reservation ?")) {
            try {
                service.supprimer(r.getIdReservationHotel());
                refresh();
            } catch (SQLException e) { showError("Erreur", "Suppression impossible."); }
        }
    }

    private boolean showDialog(ReservationHotel r, String title) {
        Dialog<ReservationHotel> d = new Dialog<>();
        d.setTitle(title);
        d.initModality(Modality.APPLICATION_MODAL);
        DialogStyleHelper.styleDialogPane(d.getDialogPane());
        DatePicker dateIn = new DatePicker(r.getDateCheckin());
        DatePicker dateOut = new DatePicker(r.getDateCheckout());
        TextField prix = new TextField(r.getPrixTotal() != null ? r.getPrixTotal().toString() : "");
        ComboBox<User> comboUser = new ComboBox<>();
        ComboBox<Hotel> comboHotel = new ComboBox<>();
        comboUser.setConverter(new StringConverter<User>() {
            @Override public String toString(User u) { return u == null ? "" : u.getNom() + " " + u.getPrenom(); }
            @Override public User fromString(String s) { return null; }
        });
        comboHotel.setConverter(new StringConverter<Hotel>() {
            @Override public String toString(Hotel h) { return h == null ? "" : h.getNom(); }
            @Override public Hotel fromString(String s) { return null; }
        });
        try {
            comboUser.getItems().addAll(userService.afficher());
            comboHotel.getItems().addAll(hotelService.afficher());
        } catch (SQLException e) {}
        if (r.getIdUser() > 0) comboUser.getItems().stream().filter(u -> u.getIdUser() == r.getIdUser()).findFirst().ifPresent(comboUser.getSelectionModel()::select);
        else if (!comboUser.getItems().isEmpty()) comboUser.getSelectionModel().selectFirst();
        if (r.getIdHotel() > 0) comboHotel.getItems().stream().filter(h -> h.getIdHotel() == r.getIdHotel()).findFirst().ifPresent(comboHotel.getSelectionModel()::select);
        else if (!comboHotel.getItems().isEmpty()) comboHotel.getSelectionModel().selectFirst();
        GridPane g = DialogStyleHelper.buildGrid();
        DialogStyleHelper.addRow(g, 0, "Date check-in *", dateIn);
        DialogStyleHelper.addRow(g, 1, "Date check-out *", dateOut);
        DialogStyleHelper.addRow(g, 2, "Prix total *", prix);
        DialogStyleHelper.addRow(g, 3, "User *", comboUser);
        DialogStyleHelper.addRow(g, 4, "Hotel *", comboHotel);
        DialogStyleHelper.styleField(prix);
        DialogStyleHelper.styleDatePicker(dateIn);
        DialogStyleHelper.styleDatePicker(dateOut);
        DialogStyleHelper.styleCombo(comboUser);
        DialogStyleHelper.styleCombo(comboHotel);
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
            if (din == null) { showError("Validation", "Date check-in obligatoire."); return null; }
            if (dout == null) { showError("Validation", "Date check-out obligatoire."); return null; }
            if (!dout.isAfter(din)) { showError("Validation", "Check-out doit être après check-in."); return null; }
            if (prix.getText() == null || prix.getText().isBlank()) { showError("Validation", "Prix total obligatoire."); return null; }
            BigDecimal p;
            try { p = new BigDecimal(prix.getText().trim()); if (p.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException(); } catch (Exception e) { showError("Validation", "Prix invalide (nombre >= 0)."); return null; }
            User selUser = comboUser.getSelectionModel().getSelectedItem();
            Hotel selHotel = comboHotel.getSelectionModel().getSelectedItem();
            if (selUser == null) { showError("Validation", "User obligatoire."); return null; }
            if (selHotel == null) { showError("Validation", "Hotel obligatoire."); return null; }
            r.setDateCheckin(din);
            r.setDateCheckout(dout);
            r.setPrixTotal(p);
            r.setIdUser(selUser.getIdUser());
            r.setIdHotel(selHotel.getIdHotel());
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
