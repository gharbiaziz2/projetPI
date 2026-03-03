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
import tn.esprit.entities.Destination;
import tn.esprit.gui.DialogStyleHelper;
import tn.esprit.services.DestinationServices;

import java.sql.SQLException;

public class DestinationController {

    @FXML private TableView<Destination> table;
    @FXML private TableColumn<Destination, String> colPaysDepart;
    @FXML private TableColumn<Destination, String> colPaysArrivee;
    @FXML private TableColumn<Destination, String> colDescription;
    @FXML private TextField searchField;
    @FXML private Button btnFilter;

    private final DestinationServices service = new DestinationServices();
    private final ObservableList<Destination> list = FXCollections.observableArrayList();
    private final FilteredList<Destination> filteredList = new FilteredList<>(list, p -> true);
    private String filterPaysDepart;
    private String filterPaysArrivee;

    @FXML
    public void initialize() {
        colPaysDepart.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPaysDepart()));
        colPaysArrivee.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPaysArrivee()));
        colDescription.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription() != null ? c.getValue().getDescription() : ""));
        table.setItems(filteredList);
        if (searchField != null) searchField.textProperty().addListener((o, ov, nv) -> applyFilter());
        refresh();
    }

    private void applyFilter() {
        String q = searchField != null ? (searchField.getText() == null ? "" : searchField.getText()).trim().toLowerCase() : "";
        filteredList.setPredicate(d -> {
            if (filterPaysDepart != null && !filterPaysDepart.isEmpty() && (d.getPaysDepart() == null || !d.getPaysDepart().toLowerCase().contains(filterPaysDepart.toLowerCase()))) return false;
            if (filterPaysArrivee != null && !filterPaysArrivee.isEmpty() && (d.getPaysArrivee() == null || !d.getPaysArrivee().toLowerCase().contains(filterPaysArrivee.toLowerCase()))) return false;
            if (q.isEmpty()) return true;
            return (d.getPaysDepart() != null && d.getPaysDepart().toLowerCase().contains(q)) || (d.getPaysArrivee() != null && d.getPaysArrivee().toLowerCase().contains(q)) || (d.getDescription() != null && d.getDescription().toLowerCase().contains(q));
        });
    }

    @FXML
    private void onFilter() {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Filtrage");
        d.initModality(Modality.APPLICATION_MODAL);
        DialogStyleHelper.styleDialogPane(d.getDialogPane());
        TextField paysDep = new TextField(filterPaysDepart != null ? filterPaysDepart : "");
        paysDep.setPromptText("Pays départ (vide = tous)");
        TextField paysArr = new TextField(filterPaysArrivee != null ? filterPaysArrivee : "");
        paysArr.setPromptText("Pays arrivée (vide = tous)");
        DialogStyleHelper.styleField(paysDep);
        DialogStyleHelper.styleField(paysArr);
        GridPane g = DialogStyleHelper.buildGrid();
        DialogStyleHelper.addRow(g, 0, "Pays départ", paysDep);
        DialogStyleHelper.addRow(g, 1, "Pays arrivée", paysArr);
        d.getDialogPane().setContent(g);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(btn -> btn);
        if (d.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            filterPaysDepart = paysDep.getText() != null && !paysDep.getText().isBlank() ? paysDep.getText().trim() : null;
            filterPaysArrivee = paysArr.getText() != null && !paysArr.getText().isBlank() ? paysArr.getText().trim() : null;
            applyFilter();
        }
    }

    public void refresh() {
        try {
            list.clear();
            list.addAll(service.afficher());
        } catch (SQLException e) { showError("Erreur", "Chargement impossible."); }
    }

    @FXML private void onAdd() {
        Destination d = new Destination();
        if (showDialog(d, "Ajouter destination")) {
            try {
                service.ajouter(d);
                refresh();
            } catch (SQLException e) { showError("Erreur", "Ajout impossible."); }
        }
    }

    @FXML private void onEdit() {
        Destination d = table.getSelectionModel().getSelectedItem();
        if (d == null) { showError("Attention", "Selectionnez une ligne."); return; }
        if (showDialog(d, "Modifier destination")) {
            try {
                service.modifier(d);
                refresh();
            } catch (SQLException e) { showError("Erreur", "Modification impossible."); }
        }
    }

    @FXML private void onDelete() {
        Destination d = table.getSelectionModel().getSelectedItem();
        if (d == null) { showError("Attention", "Selectionnez une ligne."); return; }
        if (confirm("Supprimer cette destination ?")) {
            try {
                service.supprimer(d.getIdDestination());
                refresh();
            } catch (SQLException e) { showError("Erreur", "Suppression impossible."); }
        }
    }

    private boolean showDialog(Destination d, String title) {
        Dialog<Destination> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.initModality(Modality.APPLICATION_MODAL);
        DialogStyleHelper.styleDialogPane(dialog.getDialogPane());
        TextField paysD = new TextField(d.getPaysDepart());
        TextField paysA = new TextField(d.getPaysArrivee());
        TextField desc = new TextField(d.getDescription());
        GridPane g = DialogStyleHelper.buildGrid();
        DialogStyleHelper.addRow(g, 0, "Pays départ *", paysD);
        DialogStyleHelper.addRow(g, 1, "Pays arrivée *", paysA);
        DialogStyleHelper.addRow(g, 2, "Description", desc);
        DialogStyleHelper.styleField(paysD);
        DialogStyleHelper.styleField(paysA);
        DialogStyleHelper.styleField(desc);
        VBox content = new VBox(new Label(title), g);
        content.getStyleClass().add("crud-dialog-content");
        ((Label) content.getChildren().get(0)).getStyleClass().add("crud-dialog-title");
        content.setSpacing(16);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            if (paysD.getText() == null || paysD.getText().isBlank()) { showError("Validation", "Pays depart obligatoire."); return null; }
            if (paysA.getText() == null || paysA.getText().isBlank()) { showError("Validation", "Pays arrivee obligatoire."); return null; }
            d.setPaysDepart(paysD.getText().trim());
            d.setPaysArrivee(paysA.getText().trim());
            d.setDescription(desc.getText() != null && !desc.getText().isBlank() ? desc.getText().trim() : null);
            return d;
        });
        return dialog.showAndWait().orElse(null) != null;
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
