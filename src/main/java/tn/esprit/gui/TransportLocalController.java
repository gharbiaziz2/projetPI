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
import tn.esprit.entities.TransportLocal;
import tn.esprit.entities.Voyage;
import tn.esprit.gui.DialogStyleHelper;
import tn.esprit.services.TransportLocalServices;
import tn.esprit.services.VoyageServices;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class TransportLocalController {

    @FXML private TableView<TransportLocal> table;
    @FXML private TableColumn<TransportLocal, String> colCompagnie;
    @FXML private TableColumn<TransportLocal, String> colTypeTransport;
    @FXML private TableColumn<TransportLocal, String> colPaysDepart;
    @FXML private TableColumn<TransportLocal, String> colPaysArrivee;
    @FXML private TableColumn<TransportLocal, String> colDateDepart;
    @FXML private TableColumn<TransportLocal, String> colDateRetour;
    @FXML private TableColumn<TransportLocal, BigDecimal> colPrix;
    @FXML private TableColumn<TransportLocal, Integer> colNbrPlaces;
    @FXML private TableColumn<TransportLocal, String> colIdVoyage;
    @FXML private TextField searchField;
    @FXML private Button btnFilter;

    private final TransportLocalServices service = new TransportLocalServices();
    private final VoyageServices voyageService = new VoyageServices();
    private final ObservableList<TransportLocal> list = FXCollections.observableArrayList();
    private final FilteredList<TransportLocal> filteredList = new FilteredList<>(list, p -> true);
    private TransportLocal.TypeTransport filterTypeTransport;
    private String filterPays;
    private static final DateTimeFormatter D_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        colCompagnie.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCompagnie()));
        colTypeTransport.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTypeTransport() != null ? c.getValue().getTypeTransport().name() : ""));
        colPaysDepart.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPaysDepart()));
        colPaysArrivee.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPaysArrivee()));
        colDateDepart.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDateDepart() != null ? c.getValue().getDateDepart().format(D_FMT) : ""));
        colDateRetour.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDateRetour() != null ? c.getValue().getDateRetour().format(D_FMT) : ""));
        colPrix.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPrix()));
        if (colNbrPlaces != null) colNbrPlaces.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getNbrPlaces()));
        colIdVoyage.setCellValueFactory(c -> new SimpleStringProperty(voyageDisplay.getOrDefault(c.getValue().getIdVoyage(), String.valueOf(c.getValue().getIdVoyage()))));
        table.setItems(filteredList);
        if (searchField != null) searchField.textProperty().addListener((o, ov, nv) -> applyFilter());
        refresh();
    }

    private Map<Integer, String> voyageDisplay = new HashMap<>();

    private void applyFilter() {
        String q = searchField != null ? (searchField.getText() == null ? "" : searchField.getText()).trim().toLowerCase() : "";
        filteredList.setPredicate(t -> {
            if (filterTypeTransport != null && t.getTypeTransport() != filterTypeTransport) return false;
            if (filterPays != null && !filterPays.isEmpty()) {
                String pd = t.getPaysDepart() != null ? t.getPaysDepart().toLowerCase() : "";
                String pa = t.getPaysArrivee() != null ? t.getPaysArrivee().toLowerCase() : "";
                if (!pd.contains(filterPays.toLowerCase()) && !pa.contains(filterPays.toLowerCase())) return false;
            }
            if (q.isEmpty()) return true;
            return (t.getCompagnie() != null && t.getCompagnie().toLowerCase().contains(q)) || (t.getTypeTransport() != null && t.getTypeTransport().name().toLowerCase().contains(q)) || (t.getPaysDepart() != null && t.getPaysDepart().toLowerCase().contains(q)) || (t.getPaysArrivee() != null && t.getPaysArrivee().toLowerCase().contains(q)) || voyageDisplay.getOrDefault(t.getIdVoyage(), "").toLowerCase().contains(q);
        });
    }

    @FXML
    private void onFilter() {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Filtrage");
        d.initModality(Modality.APPLICATION_MODAL);
        DialogStyleHelper.styleDialogPane(d.getDialogPane());
        ComboBox<TransportLocal.TypeTransport> typeCombo = new ComboBox<>(FXCollections.observableArrayList(TransportLocal.TypeTransport.values()));
        typeCombo.setPromptText("Tous les types");
        if (filterTypeTransport != null) typeCombo.getSelectionModel().select(filterTypeTransport);
        TextField paysF = new TextField(filterPays != null ? filterPays : "");
        paysF.setPromptText("Pays (départ ou arrivée, vide = tous)");
        DialogStyleHelper.styleCombo(typeCombo);
        DialogStyleHelper.styleField(paysF);
        javafx.scene.layout.GridPane g = DialogStyleHelper.buildGrid();
        DialogStyleHelper.addRow(g, 0, "Type transport", typeCombo);
        DialogStyleHelper.addRow(g, 1, "Pays", paysF);
        d.getDialogPane().setContent(g);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(btn -> btn);
        if (d.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            filterTypeTransport = typeCombo.getSelectionModel().getSelectedItem();
            filterPays = paysF.getText() != null && !paysF.getText().isBlank() ? paysF.getText().trim() : null;
            applyFilter();
        }
    }

    public void refresh() {
        try {
            voyageDisplay.clear();
            for (Voyage v : voyageService.afficher()) voyageDisplay.put(v.getIdVoyage(), (v.getTypeVoyage() != null ? v.getTypeVoyage() : "") + " " + (v.getDateDepart() != null ? v.getDateDepart().format(D_FMT) : ""));
            list.clear();
            list.addAll(service.afficher());
        } catch (SQLException e) { showError("Erreur", "Chargement impossible."); }
    }

    @FXML private void onAdd() {
        TransportLocal t = new TransportLocal();
        if (showDialog(t, "Ajouter transport")) {
            try {
                service.ajouter(t);
                refresh();
            } catch (SQLException e) { showError("Erreur", "Ajout impossible."); }
        }
    }

    @FXML private void onEdit() {
        TransportLocal t = table.getSelectionModel().getSelectedItem();
        if (t == null) { showError("Attention", "Selectionnez une ligne."); return; }
        if (showDialog(t, "Modifier transport")) {
            try {
                service.modifier(t);
                refresh();
            } catch (SQLException e) { showError("Erreur", "Modification impossible."); }
        }
    }

    @FXML private void onDelete() {
        TransportLocal t = table.getSelectionModel().getSelectedItem();
        if (t == null) { showError("Attention", "Selectionnez une ligne."); return; }
        if (confirm("Supprimer ce transport ?")) {
            try {
                service.supprimer(t.getIdTransport());
                refresh();
            } catch (SQLException e) { showError("Erreur", "Suppression impossible."); }
        }
    }

    private boolean showDialog(TransportLocal t, String title) {
        Dialog<TransportLocal> d = new Dialog<>();
        d.setTitle(title);
        d.initModality(Modality.APPLICATION_MODAL);
        DialogStyleHelper.styleDialogPane(d.getDialogPane());
        TextField compagnie = new TextField(t.getCompagnie());
        ComboBox<TransportLocal.TypeTransport> typeCombo = new ComboBox<>(FXCollections.observableArrayList(TransportLocal.TypeTransport.values()));
        typeCombo.getSelectionModel().select(t.getTypeTransport());
        TextField paysD = new TextField(t.getPaysDepart());
        TextField paysA = new TextField(t.getPaysArrivee());
        DatePicker dateD = new DatePicker(t.getDateDepart());
        DatePicker dateR = new DatePicker(t.getDateRetour());
        TextField prix = new TextField(t.getPrix() != null ? t.getPrix().toString() : "");
        TextField nbrPlacesF = new TextField(String.valueOf(t.getNbrPlaces() > 0 ? t.getNbrPlaces() : 10));
        ComboBox<Voyage> comboVoyage = new ComboBox<>();
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
            comboVoyage.getItems().addAll(voyageService.afficher());
        } catch (SQLException e) {}
        if (t.getIdVoyage() > 0) comboVoyage.getItems().stream().filter(v -> v.getIdVoyage() == t.getIdVoyage()).findFirst().ifPresent(comboVoyage.getSelectionModel()::select);
        else if (!comboVoyage.getItems().isEmpty()) comboVoyage.getSelectionModel().selectFirst();
        GridPane g = DialogStyleHelper.buildGrid();
        DialogStyleHelper.addRow(g, 0, "Compagnie *", compagnie);
        DialogStyleHelper.addRow(g, 1, "Type *", typeCombo);
        DialogStyleHelper.addRow(g, 2, "Pays départ *", paysD);
        DialogStyleHelper.addRow(g, 3, "Pays arrivée *", paysA);
        DialogStyleHelper.addRow(g, 4, "Date départ *", dateD);
        DialogStyleHelper.addRow(g, 5, "Date retour *", dateR);
        DialogStyleHelper.addRow(g, 6, "Prix *", prix);
        DialogStyleHelper.addRow(g, 7, "Nbr places *", nbrPlacesF);
        DialogStyleHelper.addRow(g, 8, "Voyage *", comboVoyage);
        DialogStyleHelper.styleField(compagnie); DialogStyleHelper.styleField(paysD); DialogStyleHelper.styleField(paysA); DialogStyleHelper.styleField(prix); DialogStyleHelper.styleField(nbrPlacesF);
        DialogStyleHelper.styleDatePicker(dateD); DialogStyleHelper.styleDatePicker(dateR);
        DialogStyleHelper.styleCombo(typeCombo); DialogStyleHelper.styleCombo(comboVoyage);
        VBox content = new VBox(new Label(title), g);
        content.getStyleClass().add("crud-dialog-content");
        ((Label) content.getChildren().get(0)).getStyleClass().add("crud-dialog-title");
        content.setSpacing(16);
        d.getDialogPane().setContent(content);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            if (compagnie.getText() == null || compagnie.getText().isBlank()) { showError("Validation", "Compagnie obligatoire."); return null; }
            if (typeCombo.getSelectionModel().getSelectedItem() == null) { showError("Validation", "Type obligatoire."); return null; }
            if (paysD.getText() == null || paysD.getText().isBlank()) { showError("Validation", "Pays départ obligatoire."); return null; }
            if (paysA.getText() == null || paysA.getText().isBlank()) { showError("Validation", "Pays arrivée obligatoire."); return null; }
            LocalDate dd = dateD.getValue();
            LocalDate dr = dateR.getValue();
            if (dd == null) { showError("Validation", "Date départ obligatoire."); return null; }
            if (dr == null) { showError("Validation", "Date retour obligatoire."); return null; }
            if (dr.isBefore(dd)) { showError("Validation", "Date retour après date départ."); return null; }
            if (prix.getText() == null || prix.getText().isBlank()) { showError("Validation", "Prix obligatoire."); return null; }
            BigDecimal p;
            try { p = new BigDecimal(prix.getText().trim()); if (p.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException(); } catch (Exception e) { showError("Validation", "Prix invalide (nombre >= 0)."); return null; }
            int nbrPlacesVal = 10;
            try {
                nbrPlacesVal = Integer.parseInt(nbrPlacesF.getText().trim());
                if (nbrPlacesVal < 0) throw new NumberFormatException();
            } catch (Exception e) { showError("Validation", "Nbr places invalide (entier >= 0)."); return null; }
            Voyage selVoyage = comboVoyage.getSelectionModel().getSelectedItem();
            if (selVoyage == null) { showError("Validation", "Voyage obligatoire."); return null; }
            t.setCompagnie(compagnie.getText().trim());
            t.setTypeTransport(typeCombo.getSelectionModel().getSelectedItem());
            t.setPaysDepart(paysD.getText().trim());
            t.setPaysArrivee(paysA.getText().trim());
            t.setDateDepart(dd);
            t.setDateRetour(dr);
            t.setPrix(p);
            t.setNbrPlaces(nbrPlacesVal);
            t.setIdVoyage(selVoyage.getIdVoyage());
            return t;
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
