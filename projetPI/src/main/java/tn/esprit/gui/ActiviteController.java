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
import tn.esprit.entities.Activite;
import tn.esprit.gui.DialogStyleHelper;
import tn.esprit.services.ActiviteServices;

import java.math.BigDecimal;
import java.sql.SQLException;

public class ActiviteController {

    @FXML private TableView<Activite> table;
    @FXML private TableColumn<Activite, String> colNom;
    @FXML private TableColumn<Activite, String> colDescription;
    @FXML private TableColumn<Activite, BigDecimal> colPrix;
    @FXML private TableColumn<Activite, Number> colDuree;
    @FXML private TextField searchField;
    @FXML private Button btnFilter;

    private final ActiviteServices service = new ActiviteServices();
    private final ObservableList<Activite> list = FXCollections.observableArrayList();
    private final FilteredList<Activite> filteredList = new FilteredList<>(list, p -> true);
    private BigDecimal filterPrixMax;
    private Integer filterDureeMin;

    @FXML
    public void initialize() {
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNom()));
        colDescription.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription() != null ? c.getValue().getDescription() : ""));
        colPrix.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPrix()));
        colDuree.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getDuree()));
        table.setItems(filteredList);
        if (searchField != null) searchField.textProperty().addListener((o, ov, nv) -> applyFilter());
        refresh();
    }

    private void applyFilter() {
        String q = searchField != null ? (searchField.getText() == null ? "" : searchField.getText()).trim().toLowerCase() : "";
        filteredList.setPredicate(a -> {
            if (filterPrixMax != null && (a.getPrix() == null || a.getPrix().compareTo(filterPrixMax) > 0)) return false;
            if (filterDureeMin != null && a.getDuree() < filterDureeMin) return false;
            if (q.isEmpty()) return true;
            return (a.getNom() != null && a.getNom().toLowerCase().contains(q)) || (a.getDescription() != null && a.getDescription().toLowerCase().contains(q)) || (a.getPrix() != null && a.getPrix().toString().contains(q));
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
        TextField dureeMinF = new TextField(filterDureeMin != null ? filterDureeMin.toString() : "");
        dureeMinF.setPromptText("Durée min en min (vide = tous)");
        DialogStyleHelper.styleField(prixMaxF);
        DialogStyleHelper.styleField(dureeMinF);
        GridPane g = DialogStyleHelper.buildGrid();
        DialogStyleHelper.addRow(g, 0, "Prix max", prixMaxF);
        DialogStyleHelper.addRow(g, 1, "Durée min (min)", dureeMinF);
        d.getDialogPane().setContent(g);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(btn -> btn);
        if (d.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try { filterPrixMax = prixMaxF.getText() != null && !prixMaxF.getText().isBlank() ? new BigDecimal(prixMaxF.getText().trim()) : null; } catch (Exception e) { filterPrixMax = null; }
            try { filterDureeMin = dureeMinF.getText() != null && !dureeMinF.getText().isBlank() ? Integer.parseInt(dureeMinF.getText().trim()) : null; } catch (Exception e) { filterDureeMin = null; }
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
        Activite a = new Activite();
        if (showDialog(a, "Ajouter activite")) {
            try {
                service.ajouter(a);
                refresh();
            } catch (SQLException e) { showError("Erreur", "Ajout impossible."); }
        }
    }

    @FXML private void onEdit() {
        Activite a = table.getSelectionModel().getSelectedItem();
        if (a == null) { showError("Attention", "Selectionnez une ligne."); return; }
        if (showDialog(a, "Modifier activite")) {
            try {
                service.modifier(a);
                refresh();
            } catch (SQLException e) { showError("Erreur", "Modification impossible."); }
        }
    }

    @FXML private void onDelete() {
        Activite a = table.getSelectionModel().getSelectedItem();
        if (a == null) { showError("Attention", "Selectionnez une ligne."); return; }
        if (confirm("Supprimer cette activite ?")) {
            try {
                service.supprimer(a.getIdActivite());
                refresh();
            } catch (SQLException e) { showError("Erreur", "Suppression impossible."); }
        }
    }

    private boolean showDialog(Activite a, String title) {
        Dialog<Activite> d = new Dialog<>();
        d.setTitle(title);
        d.initModality(Modality.APPLICATION_MODAL);
        DialogStyleHelper.styleDialogPane(d.getDialogPane());
        TextField nom = new TextField(a.getNom());
        TextField desc = new TextField(a.getDescription());
        TextField prix = new TextField(a.getPrix() != null ? a.getPrix().toString() : "");
        TextField duree = new TextField(a.getDuree() > 0 ? String.valueOf(a.getDuree()) : "");
        GridPane g = DialogStyleHelper.buildGrid();
        DialogStyleHelper.addRow(g, 0, "Nom *", nom);
        DialogStyleHelper.addRow(g, 1, "Description", desc);
        DialogStyleHelper.addRow(g, 2, "Prix *", prix);
        DialogStyleHelper.addRow(g, 3, "Durée (min) *", duree);
        DialogStyleHelper.styleField(nom);
        DialogStyleHelper.styleField(desc);
        DialogStyleHelper.styleField(prix);
        DialogStyleHelper.styleField(duree);
        VBox content = new VBox(new Label(title), g);
        content.getStyleClass().add("crud-dialog-content");
        ((Label) content.getChildren().get(0)).getStyleClass().add("crud-dialog-title");
        content.setSpacing(16);
        d.getDialogPane().setContent(content);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            if (nom.getText() == null || nom.getText().isBlank()) { showError("Validation", "Nom obligatoire."); return null; }
            if (prix.getText() == null || prix.getText().isBlank()) { showError("Validation", "Prix obligatoire."); return null; }
            if (duree.getText() == null || duree.getText().isBlank()) { showError("Validation", "Durée obligatoire."); return null; }
            BigDecimal p; int du;
            try { p = new BigDecimal(prix.getText().trim()); if (p.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException(); } catch (Exception e) { showError("Validation", "Prix invalide (nombre >= 0)."); return null; }
            try { du = Integer.parseInt(duree.getText().trim()); if (du <= 0) throw new NumberFormatException(); } catch (Exception e) { showError("Validation", "Durée invalide (entier > 0)."); return null; }
            a.setNom(nom.getText().trim());
            a.setDescription(desc.getText() != null && !desc.getText().isBlank() ? desc.getText().trim() : null);
            a.setPrix(p);
            a.setDuree(du);
            return a;
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
