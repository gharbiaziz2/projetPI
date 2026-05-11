package tn.esprit.gui;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.DatePicker;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import tn.esprit.entities.Activite;

import tn.esprit.services.ActiviteServices;
import tn.esprit.services.NotificationServices;

import javafx.stage.FileChooser;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;

public class ActiviteController {

    @FXML private TableView<Activite> table;
    @FXML private TableColumn<Activite, String> colNom;
    @FXML private TableColumn<Activite, String> colDescription;
    @FXML private TableColumn<Activite, Double> colPrix;
    @FXML private TableColumn<Activite, Number> colDuree;
    @FXML private TableColumn<Activite, String> colLatitude;
    @FXML private TableColumn<Activite, String> colLongitude;
    @FXML private TableColumn<Activite, LocalDate> colDate;
    @FXML private TextField searchField;
    @FXML private Button btnFilter;

    private final ActiviteServices service = new ActiviteServices();
    private final NotificationServices notificationService = new NotificationServices();
    private final ObservableList<Activite> list = FXCollections.observableArrayList();
    private final FilteredList<Activite> filteredList = new FilteredList<>(list, p -> true);
    private Double filterPrixMax;
    private Integer filterDureeMin;

    @FXML
    public void initialize() {
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNom()));
        colDescription.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription() != null ? c.getValue().getDescription() : ""));
        colPrix.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPrix()));
        colDuree.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getDuree()));
        colLatitude.setCellValueFactory(c -> new SimpleStringProperty(formatCoord(c.getValue().getLatitude())));
        colLongitude.setCellValueFactory(c -> new SimpleStringProperty(formatCoord(c.getValue().getLongitude())));
        colDate.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getDateActivite()));
        table.setItems(filteredList);
        if (searchField != null) searchField.textProperty().addListener((o, ov, nv) -> applyFilter());
        refresh();
    }

    private void applyFilter() {
        String q = searchField != null ? (searchField.getText() == null ? "" : searchField.getText()).trim().toLowerCase() : "";
        filteredList.setPredicate(a -> {
            if (filterPrixMax != null && a.getPrix() > filterPrixMax) return false;
            if (filterDureeMin != null && a.getDuree() < filterDureeMin) return false;
            if (q.isEmpty()) return true;
            return (a.getNom() != null && a.getNom().toLowerCase().contains(q)) || (a.getDescription() != null && a.getDescription().toLowerCase().contains(q)) || String.valueOf(a.getPrix()).contains(q);
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
            try { filterPrixMax = prixMaxF.getText() != null && !prixMaxF.getText().isBlank() ? Double.parseDouble(prixMaxF.getText().trim()) : null; } catch (Exception e) { filterPrixMax = null; }
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
                String msg = "Nouvelle activité : " + a.getNom() + (a.getPrix() > 0 ? " (" + a.getPrix() + " TND)" : "") + ".";
                notificationService.notifyAllClients(msg);
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
        TextField prix = new TextField(a.getPrix() > 0 ? String.valueOf(a.getPrix()) : "");
        TextField duree = new TextField(a.getDuree() > 0 ? String.valueOf(a.getDuree()) : "");
        TextField latitude = new TextField(a.getLatitude() != null ? String.valueOf(a.getLatitude()) : "");
        latitude.setPromptText("ex: 36.8065");
        TextField longitude = new TextField(a.getLongitude() != null ? String.valueOf(a.getLongitude()) : "");
        longitude.setPromptText("ex: 10.1815");
        DatePicker datePicker = new DatePicker(a.getDateActivite());
        datePicker.setPromptText("jj/mm/aaaa");
        TextField photo = new TextField(a.getPhoto() != null ? a.getPhoto() : "");
        photo.setPromptText("Chemin ou Parcourir...");
        Button browsePhoto = new Button("Parcourir");
        browsePhoto.setOnAction(ev -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Choisir une photo");
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
            File f = fc.showOpenDialog(browsePhoto.getScene().getWindow());
            if (f != null) photo.setText(f.getAbsolutePath());
        });
        HBox photoBox = new HBox(8);
        photoBox.getChildren().addAll(photo, browsePhoto);
        photoBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        DialogStyleHelper.styleField(photo);
        GridPane g = DialogStyleHelper.buildGrid();
        DialogStyleHelper.addRow(g, 0, "Nom *", nom);
        DialogStyleHelper.addRow(g, 1, "Description", desc);
        DialogStyleHelper.addRow(g, 2, "Prix *", prix);
        DialogStyleHelper.addRow(g, 3, "Durée (min) *", duree);
        DialogStyleHelper.addRow(g, 4, "Latitude", latitude);
        DialogStyleHelper.addRow(g, 5, "Longitude", longitude);
        DialogStyleHelper.addRow(g, 6, "Date", datePicker);
        DialogStyleHelper.addRow(g, 7, "Photo", photoBox);
        DialogStyleHelper.styleField(nom);
        DialogStyleHelper.styleField(desc);
        DialogStyleHelper.styleField(prix);
        DialogStyleHelper.styleField(duree);
        DialogStyleHelper.styleField(latitude);
        DialogStyleHelper.styleField(longitude);
        DialogStyleHelper.styleDatePicker(datePicker);
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
            double p; int du;
            try { p = Double.parseDouble(prix.getText().trim()); if (p < 0) throw new NumberFormatException(); } catch (Exception e) { showError("Validation", "Prix invalide (nombre >= 0)."); return null; }
            try { du = Integer.parseInt(duree.getText().trim()); if (du <= 0) throw new NumberFormatException(); } catch (Exception e) { showError("Validation", "Durée invalide (entier > 0)."); return null; }
            a.setNom(nom.getText().trim());
            a.setDescription(desc.getText() != null && !desc.getText().isBlank() ? desc.getText().trim() : null);
            a.setPrix(p);
            a.setDuree(du);
            Double latVal = null, lonVal = null;
            try { if (latitude.getText() != null && !latitude.getText().isBlank()) latVal = Double.parseDouble(latitude.getText().trim()); } catch (Exception ignored) {}
            try { if (longitude.getText() != null && !longitude.getText().isBlank()) lonVal = Double.parseDouble(longitude.getText().trim()); } catch (Exception ignored) {}
            a.setLatitude(latVal);
            a.setLongitude(lonVal);
            a.setDateActivite(datePicker.getValue());
            a.setPhoto(photo.getText() != null && !photo.getText().isBlank() ? photo.getText().trim() : null);
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

    private String formatCoord(Double value) {
        return value != null ? String.format("%.4f", value) : "";
    }

    private void showError(String t, String m) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(t);
        a.setContentText(m);
        a.showAndWait();
    }
}
