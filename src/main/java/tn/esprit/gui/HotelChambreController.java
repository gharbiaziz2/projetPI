package tn.esprit.gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import tn.esprit.entities.Hotel;
import tn.esprit.entities.HotelChambre;
import tn.esprit.services.HotelChambreServices;
import tn.esprit.services.HotelServices;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

public class HotelChambreController {
    @FXML private TableView<HotelChambre> table;
    @FXML private TableColumn<HotelChambre, String> colNumero;
    @FXML private TableColumn<HotelChambre, String> colType;
    @FXML private TableColumn<HotelChambre, String> colPrix;
    @FXML private TableColumn<HotelChambre, String> colHotel;
    @FXML private TableColumn<HotelChambre, String> colDisponible;
    @FXML private TableColumn<HotelChambre, String> colDescription;
    @FXML private TableColumn<HotelChambre, String> colImage;
    @FXML private TextField searchField;

    private final HotelChambreServices service = new HotelChambreServices();
    private final HotelServices hotelService = new HotelServices();
    private ObservableList<HotelChambre> data = FXCollections.observableArrayList();
    private List<Hotel> hotels;

    @FXML
    public void initialize() {
        colNumero.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNumeroChambre()));
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTypeChambre()));
        colPrix.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getPrixChambre())));
        colHotel.setCellValueFactory(c -> {
            int idHotel = c.getValue().getIdHotel();
            String hotelName = String.valueOf(idHotel);
            if (hotels != null) {
                for (Hotel h : hotels) {
                    if (h.getIdHotel() == idHotel) { hotelName = h.getNom(); break; }
                }
            }
            return new SimpleStringProperty(hotelName);
        });
        colDisponible.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isDisponible() ? "Oui" : "Non"));
        colDescription.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription()));
        colImage.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getImage()));

        searchField.textProperty().addListener((obs, o, n) -> filterTable(n));
        refresh();
    }

    public void refresh() {
        try {
            hotels = hotelService.afficher();
            data.setAll(service.afficher());
            table.setItems(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void filterTable(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            table.setItems(data);
            return;
        }
        String lower = keyword.toLowerCase();
        table.setItems(data.filtered(c ->
                (c.getNumeroChambre() != null && c.getNumeroChambre().toLowerCase().contains(lower)) ||
                (c.getTypeChambre() != null && c.getTypeChambre().toLowerCase().contains(lower))));
    }

    @FXML
    private void onAdd() {
        HotelChambre chambre = showDialog(null);
        if (chambre != null) {
            try {
                service.ajouter(chambre);
                refresh();
            } catch (SQLException e) {
                showError("Erreur", e.getMessage());
            }
        }
    }

    @FXML
    private void onEdit() {
        HotelChambre selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { showError("Sélection", "Veuillez sélectionner une chambre."); return; }
        HotelChambre updated = showDialog(selected);
        if (updated != null) {
            try {
                service.modifier(updated);
                refresh();
            } catch (SQLException e) {
                showError("Erreur", e.getMessage());
            }
        }
    }

    @FXML
    private void onDelete() {
        HotelChambre selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { showError("Sélection", "Veuillez sélectionner une chambre."); return; }
        try {
            service.supprimer(selected.getIdChambre());
            refresh();
        } catch (SQLException e) {
            showError("Erreur", e.getMessage());
        }
    }

    private HotelChambre showDialog(HotelChambre existing) {
        Dialog<HotelChambre> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Ajouter Chambre" : "Modifier Chambre");
        dialog.initModality(Modality.APPLICATION_MODAL);
        DialogStyleHelper.styleFrontDialogPane(dialog.getDialogPane());

        ComboBox<Hotel> hotelCombo = new ComboBox<>();
        if (hotels != null) hotelCombo.getItems().addAll(hotels);
        hotelCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Hotel h, boolean empty) {
                super.updateItem(h, empty);
                setText(h == null || empty ? "" : h.getNom() + " (" + h.getVille() + ")");
            }
        });
        hotelCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Hotel h, boolean empty) {
                super.updateItem(h, empty);
                setText(h == null || empty ? "" : h.getNom() + " (" + h.getVille() + ")");
            }
        });

        TextField tfNumero = new TextField();
        
        // ComboBox pour les types de chambre
        ComboBox<String> cbType = new ComboBox<>();
        cbType.setItems(FXCollections.observableArrayList("Simple", "Double", "Triple", "Suite", "Penthouse"));
        cbType.setEditable(false);
        
        TextField tfPrix = new TextField();
        TextField tfDescription = new TextField();
        TextField tfImage = new TextField();
        ImageView imagePreview = new ImageView();
        imagePreview.setFitWidth(150);
        imagePreview.setFitHeight(150);
        imagePreview.setPreserveRatio(true);
        
        Button btnBrowseImage = new Button("Parcourir les images");
        btnBrowseImage.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Sélectionner une image");
            fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif"),
                new FileChooser.ExtensionFilter("Tous", "*.*")
            );
            File file = fc.showOpenDialog(dialog.getOwner());
            if (file != null) {
                tfImage.setText(file.getAbsolutePath());
                try {
                    imagePreview.setImage(new javafx.scene.image.Image(file.toURI().toString()));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        
        CheckBox cbDisponible = new CheckBox("Disponible");
        cbDisponible.setSelected(true);

        if (existing != null) {
            tfNumero.setText(existing.getNumeroChambre());
            cbType.setValue(existing.getTypeChambre());
            tfPrix.setText(String.valueOf(existing.getPrixChambre()));
            tfDescription.setText(existing.getDescription());
            tfImage.setText(existing.getImage());
            cbDisponible.setSelected(existing.isDisponible());
            try {
                if (existing.getImage() != null && !existing.getImage().isEmpty()) {
                    imagePreview.setImage(new javafx.scene.image.Image(new File(existing.getImage()).toURI().toString()));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            for (Hotel h : hotelCombo.getItems()) {
                if (h.getIdHotel() == existing.getIdHotel()) { hotelCombo.getSelectionModel().select(h); break; }
            }
        }

        GridPane g = DialogStyleHelper.buildGrid();
        DialogStyleHelper.addRow(g, 0, "Hôtel", hotelCombo);
        DialogStyleHelper.addRow(g, 1, "Numéro chambre", tfNumero);
        DialogStyleHelper.addRow(g, 2, "Type chambre", cbType);
        DialogStyleHelper.addRow(g, 3, "Prix chambre", tfPrix);
        DialogStyleHelper.addRow(g, 4, "Description", tfDescription);
        DialogStyleHelper.addRow(g, 5, "Image", tfImage);
        DialogStyleHelper.addRow(g, 6, "", btnBrowseImage);
        
        VBox imageBox = new VBox(5, new Label("Aperçu:"), imagePreview);
        imageBox.setStyle("-fx-border-color: #ccc; -fx-padding: 5;");
        GridPane.setColumnSpan(imageBox, 2);
        g.add(imageBox, 0, 7);
        
        GridPane.setColumnSpan(cbDisponible, 2);
        g.add(cbDisponible, 0, 8);

        dialog.getDialogPane().setContent(g);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                Hotel selHotel = hotelCombo.getSelectionModel().getSelectedItem();
                if (selHotel == null) { showError("Erreur", "Sélectionnez un hôtel."); return null; }
                if (cbType.getValue() == null || cbType.getValue().isEmpty()) { showError("Erreur", "Sélectionnez un type de chambre."); return null; }
                double prix;
                try { prix = Double.parseDouble(tfPrix.getText()); } catch (Exception e) { showError("Erreur", "Prix invalide."); return null; }
                HotelChambre c = new HotelChambre(
                        existing != null ? existing.getIdChambre() : 0,
                        selHotel.getIdHotel(),
                        tfNumero.getText(), cbType.getValue(), prix,
                        tfDescription.getText(), tfImage.getText(), cbDisponible.isSelected()
                );
                return c;
            }
            return null;
        });
        return dialog.showAndWait().orElse(null);
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setContentText(msg);
        a.showAndWait();
    }
}
