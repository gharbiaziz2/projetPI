package tn.esprit.gui;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import tn.esprit.entities.Hotel;

import java.io.File;
import tn.esprit.gui.DialogStyleHelper;
import tn.esprit.services.HotelServices;

import java.math.BigDecimal;
import java.sql.SQLException;

public class HotelController {

    @FXML private TableView<Hotel> table;
    @FXML private TableColumn<Hotel, String> colNom;
    @FXML private TableColumn<Hotel, String> colPays;
    @FXML private TableColumn<Hotel, String> colVille;
    @FXML private TableColumn<Hotel, BigDecimal> colPrixNuit;
    @FXML private TableColumn<Hotel, Double> colLongitude;
    @FXML private TableColumn<Hotel, Double> colLatitude;
    @FXML private TableColumn<Hotel, String> colImage;
    @FXML private TableColumn<Hotel, String> colDescription;
    @FXML private TextField searchField;
    @FXML private Button btnFilter;

    private final HotelServices service = new HotelServices();
    private final ObservableList<Hotel> list = FXCollections.observableArrayList();
    private final FilteredList<Hotel> filteredList = new FilteredList<>(list, p -> true);
    private String filterPays;
    private String filterVille;

    @FXML
    public void initialize() {
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNom()));
        colPays.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPays()));
        colVille.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getVille()));
        colPrixNuit.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPrixNuit()));
        colLongitude.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getLongitude()));
        colLatitude.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getLatitude()));
        colImage.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getImage() != null ? c.getValue().getImage() : ""));
        colDescription.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription() != null ? c.getValue().getDescription() : ""));
        table.setItems(filteredList);
        if (searchField != null) searchField.textProperty().addListener((o, ov, nv) -> applyFilter());
        refresh();
    }

    private void applyFilter() {
        String q = searchField != null ? (searchField.getText() == null ? "" : searchField.getText()).trim().toLowerCase() : "";
        filteredList.setPredicate(h -> {
            if (filterPays != null && !filterPays.isEmpty() && (h.getPays() == null || !h.getPays().toLowerCase().contains(filterPays.toLowerCase()))) return false;
            if (filterVille != null && !filterVille.isEmpty() && (h.getVille() == null || !h.getVille().toLowerCase().contains(filterVille.toLowerCase()))) return false;
            if (q.isEmpty()) return true;
            return (h.getNom() != null && h.getNom().toLowerCase().contains(q)) || (h.getVille() != null && h.getVille().toLowerCase().contains(q)) || (h.getPays() != null && h.getPays().toLowerCase().contains(q)) || (h.getDescription() != null && h.getDescription().toLowerCase().contains(q));
        });
    }

    @FXML
    private void onFilter() {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Filtrage");
        d.initModality(Modality.APPLICATION_MODAL);
        DialogStyleHelper.styleDialogPane(d.getDialogPane());
        TextField paysF = new TextField(filterPays != null ? filterPays : "");
        paysF.setPromptText("Pays (vide = tous)");
        TextField villeF = new TextField(filterVille != null ? filterVille : "");
        villeF.setPromptText("Ville (vide = toutes)");
        DialogStyleHelper.styleField(paysF);
        DialogStyleHelper.styleField(villeF);
        GridPane g = DialogStyleHelper.buildGrid();
        DialogStyleHelper.addRow(g, 0, "Pays", paysF);
        DialogStyleHelper.addRow(g, 1, "Ville", villeF);
        d.getDialogPane().setContent(g);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(btn -> btn);
        if (d.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            filterPays = paysF.getText() != null && !paysF.getText().isBlank() ? paysF.getText().trim() : null;
            filterVille = villeF.getText() != null && !villeF.getText().isBlank() ? villeF.getText().trim() : null;
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
        Hotel h = new Hotel();
        if (showDialog(h, "Ajouter hôtel")) {
            try {
                service.ajouter(h);
                refresh();
            } catch (SQLException e) { showError("Erreur", "Ajout impossible."); }
        }
    }

    @FXML private void onEdit() {
        Hotel h = table.getSelectionModel().getSelectedItem();
        if (h == null) { showError("Attention", "Sélectionnez une ligne."); return; }
        if (showDialog(h, "Modifier hôtel")) {
            try {
                service.modifier(h);
                refresh();
            } catch (SQLException e) { showError("Erreur", "Modification impossible."); }
        }
    }

    @FXML private void onDelete() {
        Hotel h = table.getSelectionModel().getSelectedItem();
        if (h == null) { showError("Attention", "Sélectionnez une ligne."); return; }
        if (confirm("Supprimer cet hôtel ?")) {
            try {
                service.supprimer(h.getIdHotel());
                refresh();
            } catch (SQLException e) { showError("Erreur", "Suppression impossible."); }
        }
    }

    private boolean showDialog(Hotel h, String title) {
        Dialog<Hotel> d = new Dialog<>();
        d.setTitle(title);
        d.initModality(Modality.APPLICATION_MODAL);
        DialogStyleHelper.styleDialogPane(d.getDialogPane());
        TextField nom = new TextField(h.getNom());
        TextField pays = new TextField(h.getPays());
        TextField ville = new TextField(h.getVille());
        TextField prix = new TextField(h.getPrixNuit() != null ? h.getPrixNuit().toString() : "");
        TextField longitude = new TextField(h.getLongitude() != null ? h.getLongitude().toString() : "");
        TextField latitude = new TextField(h.getLatitude() != null ? h.getLatitude().toString() : "");
        TextField image = new TextField(h.getImage());
        image.setPromptText("Parcourir pour selectionner...");
        Button browseImage = new Button("Parcourir");
        browseImage.setOnAction(ev -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Choisir une image");
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp"));
            File f = fc.showOpenDialog(browseImage.getScene().getWindow());
            if (f != null) image.setText(f.getAbsolutePath());
        });
        HBox imageBox = new HBox(8);
        imageBox.getChildren().addAll(image, browseImage);
        imageBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        TextField desc = new TextField(h.getDescription());
        GridPane g = DialogStyleHelper.buildGrid();
        DialogStyleHelper.addRow(g, 0, "Nom *", nom);
        DialogStyleHelper.addRow(g, 1, "Pays *", pays);
        DialogStyleHelper.addRow(g, 2, "Ville *", ville);
        DialogStyleHelper.addRow(g, 3, "Prix/nuit *", prix);
        DialogStyleHelper.addRow(g, 4, "Longitude", longitude);
        DialogStyleHelper.addRow(g, 5, "Latitude", latitude);
        DialogStyleHelper.addRow(g, 6, "Image", imageBox);
        DialogStyleHelper.addRow(g, 7, "Description", desc);
        DialogStyleHelper.styleField(nom);
        DialogStyleHelper.styleField(pays);
        DialogStyleHelper.styleField(ville);
        DialogStyleHelper.styleField(prix);
        DialogStyleHelper.styleField(longitude);
        DialogStyleHelper.styleField(latitude);
        DialogStyleHelper.styleField(image);
        DialogStyleHelper.styleField(desc);
        VBox content = new VBox(new Label(title), g);
        content.getStyleClass().add("crud-dialog-content");
        ((Label) content.getChildren().get(0)).getStyleClass().add("crud-dialog-title");
        content.setSpacing(16);
        d.getDialogPane().setContent(content);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            if (nom.getText() == null || nom.getText().isBlank()) { showError("Validation", "Nom obligatoire."); return null; }
            if (pays.getText() == null || pays.getText().isBlank()) { showError("Validation", "Pays obligatoire."); return null; }
            if (ville.getText() == null || ville.getText().isBlank()) { showError("Validation", "Ville obligatoire."); return null; }
            if (prix.getText() == null || prix.getText().isBlank()) { showError("Validation", "Prix/nuit obligatoire."); return null; }
            BigDecimal p;
            try { p = new BigDecimal(prix.getText().trim()); if (p.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException(); } catch (Exception e) { showError("Validation", "Prix invalide (nombre >= 0)."); return null; }
            h.setNom(nom.getText().trim());
            h.setPays(pays.getText().trim());
            h.setVille(ville.getText().trim());
            h.setPrixNuit(p);
            Double lon = null, lat = null;
            try { if (!longitude.getText().trim().isEmpty()) lon = Double.parseDouble(longitude.getText().trim()); } catch (Exception ignored) {}
            try { if (!latitude.getText().trim().isEmpty()) lat = Double.parseDouble(latitude.getText().trim()); } catch (Exception ignored) {}
            h.setLongitude(lon);
            h.setLatitude(lat);
            h.setImage(image.getText() != null && !image.getText().isBlank() ? image.getText().trim() : null);
            h.setDescription(desc.getText() != null && !desc.getText().isBlank() ? desc.getText().trim() : null);
            return h;
        });
        return d.showAndWait().orElse(null) != null;
    }

    private boolean confirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.OK, ButtonType.CANCEL);
        return a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void showError(String t, String m) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(t);
        a.setContentText(m);
        a.showAndWait();
    }
}
