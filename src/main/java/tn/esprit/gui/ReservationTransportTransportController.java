package tn.esprit.gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.util.StringConverter;
import tn.esprit.entities.ReservationTransport;
import tn.esprit.entities.ReservationTransportTransport;
import tn.esprit.entities.TransportLocal;
import tn.esprit.entities.User;
import tn.esprit.services.ReservationTransportServices;
import tn.esprit.services.ReservationTransportTransportServices;
import tn.esprit.services.TransportLocalServices;
import tn.esprit.services.UserServices;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class ReservationTransportTransportController {

    @FXML private TableView<ReservationTransportTransport> table;
    @FXML private TableColumn<ReservationTransportTransport, String> colReservationTransport;
    @FXML private TableColumn<ReservationTransportTransport, String> colTransport;

    private final ReservationTransportTransportServices service = new ReservationTransportTransportServices();
    private final ReservationTransportServices resaTransportService = new ReservationTransportServices();
    private final TransportLocalServices transportService = new TransportLocalServices();
    private final UserServices userService = new UserServices();
    private final ObservableList<ReservationTransportTransport> list = FXCollections.observableArrayList();
    private static final DateTimeFormatter D_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private Map<Integer, String> resaDisplay = new HashMap<>();
    private Map<Integer, String> transportDisplay = new HashMap<>();

    @FXML
    public void initialize() {
        colReservationTransport.setCellValueFactory(c -> new SimpleStringProperty(resaDisplay.getOrDefault(c.getValue().getIdReservationTransport(), "")));
        colTransport.setCellValueFactory(c -> new SimpleStringProperty(transportDisplay.getOrDefault(c.getValue().getIdTransport(), "")));
        table.setItems(list);
        refresh();
    }

    public void refresh() {
        try {
            resaDisplay.clear();
            for (ReservationTransport r : resaTransportService.afficher()) {
                String userStr = "";
                for (User u : userService.afficher()) if (u.getIdUser() == r.getIdUser()) { userStr = u.getNom() + " " + u.getPrenom(); break; }
                resaDisplay.put(r.getIdReservationTransport(), (r.getDateReservation() != null ? r.getDateReservation().format(D_FMT) : "") + " - " + userStr);
            }
            transportDisplay.clear();
            for (TransportLocal t : transportService.afficher()) transportDisplay.put(t.getIdTransport(), (t.getCompagnie() != null ? t.getCompagnie() : "") + " " + (t.getTypeTransport() != null ? t.getTypeTransport().name() : ""));
            list.clear();
            list.addAll(service.afficher());
        } catch (SQLException e) { showError("Erreur", "Chargement impossible."); }
    }

    @FXML private void onAdd() {
        ReservationTransportTransport rtt = new ReservationTransportTransport();
        if (showDialog(rtt, "Lier reservation transport et transport")) {
            try {
                service.ajouter(rtt);
                refresh();
            } catch (SQLException e) { showError("Erreur", "Ajout impossible (doublon ?)."); }
        }
    }

    @FXML private void onDelete() {
        ReservationTransportTransport rtt = table.getSelectionModel().getSelectedItem();
        if (rtt == null) { showError("Attention", "Selectionnez une ligne."); return; }
        if (confirm("Supprimer ce lien ?")) {
            try {
                service.supprimer(rtt.getIdReservationTransport(), rtt.getIdTransport());
                refresh();
            } catch (SQLException e) { showError("Erreur", "Suppression impossible."); }
        }
    }

    private boolean showDialog(ReservationTransportTransport rtt, String title) {
        Dialog<ReservationTransportTransport> d = new Dialog<>();
        d.setTitle(title);
        d.initModality(Modality.APPLICATION_MODAL);
        DialogStyleHelper.styleDialogPane(d.getDialogPane());
        Map<Integer, String> userNames = new HashMap<>();
        try {
            for (User u : userService.afficher()) userNames.put(u.getIdUser(), u.getNom() + " " + u.getPrenom());
        } catch (SQLException e) {}
        ComboBox<ReservationTransport> comboResa = new ComboBox<>();
        ComboBox<TransportLocal> comboTransport = new ComboBox<>();
        comboResa.setConverter(new StringConverter<ReservationTransport>() {
            @Override public String toString(ReservationTransport r) {
                if (r == null) return "";
                String date = r.getDateReservation() != null ? r.getDateReservation().format(D_FMT) : "";
                return date + " - " + userNames.getOrDefault(r.getIdUser(), "");
            }
            @Override public ReservationTransport fromString(String s) { return null; }
        });
        comboTransport.setConverter(new StringConverter<TransportLocal>() {
            @Override public String toString(TransportLocal t) {
                if (t == null) return "";
                String comp = t.getCompagnie() != null ? t.getCompagnie() : "";
                String type = t.getTypeTransport() != null ? t.getTypeTransport().name() : "";
                return comp + " - " + type;
            }
            @Override public TransportLocal fromString(String s) { return null; }
        });
        try {
            comboResa.getItems().addAll(resaTransportService.afficher());
            comboTransport.getItems().addAll(transportService.afficher());
        } catch (SQLException e) {}
        if (!comboResa.getItems().isEmpty()) comboResa.getSelectionModel().selectFirst();
        if (!comboTransport.getItems().isEmpty()) comboTransport.getSelectionModel().selectFirst();
        DialogStyleHelper.styleCombo(comboResa);
        DialogStyleHelper.styleCombo(comboTransport);
        GridPane g = DialogStyleHelper.buildGrid();
        DialogStyleHelper.addRow(g, 0, "Réservation transport *", comboResa);
        DialogStyleHelper.addRow(g, 1, "Transport *", comboTransport);
        VBox content = new VBox(new Label(title), g);
        content.getStyleClass().add("crud-dialog-content");
        ((Label) content.getChildren().get(0)).getStyleClass().add("crud-dialog-title");
        content.setSpacing(16);
        d.getDialogPane().setContent(content);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            ReservationTransport selResa = comboResa.getSelectionModel().getSelectedItem();
            TransportLocal selTransport = comboTransport.getSelectionModel().getSelectedItem();
            if (selResa == null) { showError("Validation", "Reservation transport obligatoire."); return null; }
            if (selTransport == null) { showError("Validation", "Transport obligatoire."); return null; }
            rtt.setIdReservationTransport(selResa.getIdReservationTransport());
            rtt.setIdTransport(selTransport.getIdTransport());
            return rtt;
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
