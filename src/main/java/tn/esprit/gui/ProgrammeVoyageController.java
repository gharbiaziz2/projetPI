package tn.esprit.gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import tn.esprit.entities.ProgrammeVoyage;
import tn.esprit.entities.Voyage;
import tn.esprit.services.ProgrammeVoyageServices;
import tn.esprit.services.VoyageServices;

import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ProgrammeVoyageController {
    @FXML private TableView<ProgrammeVoyage> table;
    @FXML private TableColumn<ProgrammeVoyage, String> colVoyage;
    @FXML private TableColumn<ProgrammeVoyage, String> colJour;
    @FXML private TableColumn<ProgrammeVoyage, String> colTitre;
    @FXML private TableColumn<ProgrammeVoyage, String> colDescription;
    @FXML private TableColumn<ProgrammeVoyage, String> colLieu;
    @FXML private TableColumn<ProgrammeVoyage, String> colHeureDebut;
    @FXML private TableColumn<ProgrammeVoyage, String> colHeureFin;
    @FXML private TextField searchField;

    private final ProgrammeVoyageServices service = new ProgrammeVoyageServices();
    private final VoyageServices voyageService = new VoyageServices();
    private ObservableList<ProgrammeVoyage> data = FXCollections.observableArrayList();
    private List<Voyage> voyages;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    public void initialize() {
        colVoyage.setCellValueFactory(c -> {
            int idVoyage = c.getValue().getIdVoyage();
            String name = String.valueOf(idVoyage);
            if (voyages != null) {
                for (Voyage v : voyages) {
                    if (v.getIdVoyage() == idVoyage) { name = v.getTypeVoyage(); break; }
                }
            }
            return new SimpleStringProperty(name);
        });
        colJour.setCellValueFactory(c -> new SimpleStringProperty("Jour " + c.getValue().getJour()));
        colTitre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitre()));
        colDescription.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription()));
        colLieu.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLieu()));
        colHeureDebut.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getHeureDebut() != null ? c.getValue().getHeureDebut().format(TIME_FMT) : ""));
        colHeureFin.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getHeureFin() != null ? c.getValue().getHeureFin().format(TIME_FMT) : ""));

        searchField.textProperty().addListener((obs, o, n) -> filterTable(n));
        refresh();
    }

    public void refresh() {
        try {
            voyages = voyageService.afficher();
            data.setAll(service.afficher());
            table.setItems(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void filterTable(String keyword) {
        if (keyword == null || keyword.isBlank()) { table.setItems(data); return; }
        String lower = keyword.toLowerCase();
        table.setItems(data.filtered(p ->
                (p.getTitre() != null && p.getTitre().toLowerCase().contains(lower)) ||
                (p.getLieu() != null && p.getLieu().toLowerCase().contains(lower))));
    }

    @FXML
    private void onAdd() {
        ProgrammeVoyage p = showDialog(null);
        if (p != null) {
            try { service.ajouter(p); refresh(); } catch (SQLException e) { showError("Erreur", e.getMessage()); }
        }
    }

    @FXML
    private void onEdit() {
        ProgrammeVoyage selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { showError("Sélection", "Veuillez sélectionner un programme."); return; }
        ProgrammeVoyage updated = showDialog(selected);
        if (updated != null) {
            try { service.modifier(updated); refresh(); } catch (SQLException e) { showError("Erreur", e.getMessage()); }
        }
    }

    @FXML
    private void onDelete() {
        ProgrammeVoyage selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { showError("Sélection", "Veuillez sélectionner un programme."); return; }
        try { service.supprimer(selected.getIdProgramme()); refresh(); } catch (SQLException e) { showError("Erreur", e.getMessage()); }
    }

    private ProgrammeVoyage showDialog(ProgrammeVoyage existing) {
        Dialog<ProgrammeVoyage> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Ajouter Programme" : "Modifier Programme");
        DialogStyleHelper.styleFrontDialogPane(dialog.getDialogPane());

        ComboBox<Voyage> voyageCombo = new ComboBox<>();
        if (voyages != null) voyageCombo.getItems().addAll(voyages);
        voyageCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Voyage v, boolean empty) {
                super.updateItem(v, empty);
                setText(v == null || empty ? "" : v.getTypeVoyage() + " (" + v.getDateDepart() + ")");
            }
        });
        voyageCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Voyage v, boolean empty) {
                super.updateItem(v, empty);
                setText(v == null || empty ? "" : v.getTypeVoyage() + " (" + v.getDateDepart() + ")");
            }
        });

        Spinner<Integer> spJour = new Spinner<>(1, 30, 1);
        TextField tfTitre = new TextField();
        TextField tfDescription = new TextField();
        TextField tfLieu = new TextField();
        TextField tfHeureDebut = new TextField();
        tfHeureDebut.setPromptText("HH:mm");
        TextField tfHeureFin = new TextField();
        tfHeureFin.setPromptText("HH:mm");
        TextField tfImage = new TextField();

        if (existing != null) {
            for (Voyage v : voyageCombo.getItems()) {
                if (v.getIdVoyage() == existing.getIdVoyage()) { voyageCombo.getSelectionModel().select(v); break; }
            }
            spJour.getValueFactory().setValue(existing.getJour());
            tfTitre.setText(existing.getTitre());
            tfDescription.setText(existing.getDescription());
            tfLieu.setText(existing.getLieu());
            if (existing.getHeureDebut() != null) tfHeureDebut.setText(existing.getHeureDebut().format(TIME_FMT));
            if (existing.getHeureFin() != null) tfHeureFin.setText(existing.getHeureFin().format(TIME_FMT));
            tfImage.setText(existing.getImage());
        }

        GridPane g = DialogStyleHelper.buildGrid();
        DialogStyleHelper.addRow(g, 0, "Voyage", voyageCombo);
        DialogStyleHelper.addRow(g, 1, "Jour", spJour);
        DialogStyleHelper.addRow(g, 2, "Titre", tfTitre);
        DialogStyleHelper.addRow(g, 3, "Description", tfDescription);
        DialogStyleHelper.addRow(g, 4, "Lieu", tfLieu);
        DialogStyleHelper.addRow(g, 5, "Heure début", tfHeureDebut);
        DialogStyleHelper.addRow(g, 6, "Heure fin", tfHeureFin);
        DialogStyleHelper.addRow(g, 7, "Image", tfImage);

        dialog.getDialogPane().setContent(g);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                Voyage selVoyage = voyageCombo.getSelectionModel().getSelectedItem();
                if (selVoyage == null) { showError("Erreur", "Sélectionnez un voyage."); return null; }
                ProgrammeVoyage p = new ProgrammeVoyage();
                if (existing != null) p.setIdProgramme(existing.getIdProgramme());
                p.setIdVoyage(selVoyage.getIdVoyage());
                p.setJour(spJour.getValue());
                p.setTitre(tfTitre.getText());
                p.setDescription(tfDescription.getText());
                p.setLieu(tfLieu.getText());
                try { if (!tfHeureDebut.getText().isBlank()) p.setHeureDebut(LocalTime.parse(tfHeureDebut.getText(), TIME_FMT)); } catch (Exception ignored) {}
                try { if (!tfHeureFin.getText().isBlank()) p.setHeureFin(LocalTime.parse(tfHeureFin.getText(), TIME_FMT)); } catch (Exception ignored) {}
                p.setImage(tfImage.getText());
                return p;
            }
            return null;
        });
        return dialog.showAndWait().orElse(null);
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle(title); a.setContentText(msg); a.showAndWait();
    }
}
