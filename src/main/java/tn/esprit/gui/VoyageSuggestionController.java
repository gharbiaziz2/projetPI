package tn.esprit.gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import tn.esprit.entities.VoyageSuggestion;
import tn.esprit.services.VoyageSuggestionServices;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

public class VoyageSuggestionController {
    @FXML private TableView<VoyageSuggestion> table;
    @FXML private TableColumn<VoyageSuggestion, String> colNom;
    @FXML private TableColumn<VoyageSuggestion, String> colEmail;
    @FXML private TableColumn<VoyageSuggestion, String> colDestination;
    @FXML private TableColumn<VoyageSuggestion, String> colVilleDepart;
    @FXML private TableColumn<VoyageSuggestion, String> colDate;
    @FXML private TableColumn<VoyageSuggestion, String> colBudget;
    @FXML private TableColumn<VoyageSuggestion, String> colMessage;
    @FXML private TableColumn<VoyageSuggestion, String> colStatut;
    @FXML private TextField searchField;

    private final VoyageSuggestionServices service = new VoyageSuggestionServices();
    private ObservableList<VoyageSuggestion> data = FXCollections.observableArrayList();
    private static final DateTimeFormatter D_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFullName()));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        colDestination.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDestination()));
        colVilleDepart.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDepartureCity()));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDesiredDate() != null ? c.getValue().getDesiredDate().format(D_FMT) : ""));
        colBudget.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getBudget())));
        colMessage.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMessage()));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));

        // Double-click to change status
        table.setRowFactory(tv -> {
            TableRow<VoyageSuggestion> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    changeStatus(row.getItem());
                }
            });
            return row;
        });

        searchField.textProperty().addListener((obs, o, n) -> filterTable(n));
        refresh();
    }

    public void refresh() {
        try {
            data.setAll(service.afficher());
            table.setItems(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void filterTable(String keyword) {
        if (keyword == null || keyword.isBlank()) { table.setItems(data); return; }
        String lower = keyword.toLowerCase();
        table.setItems(data.filtered(s ->
                (s.getFullName() != null && s.getFullName().toLowerCase().contains(lower)) ||
                (s.getDestination() != null && s.getDestination().toLowerCase().contains(lower)) ||
                (s.getEmail() != null && s.getEmail().toLowerCase().contains(lower))));
    }

    private void changeStatus(VoyageSuggestion s) {
        ChoiceDialog<String> dialog = new ChoiceDialog<>(s.getStatus(),
                "EN_ATTENTE", "TRAITEE", "REFUSEE");
        dialog.setTitle("Changer le statut");
        dialog.setHeaderText("Suggestion de " + s.getFullName());
        dialog.setContentText("Statut:");
        dialog.showAndWait().ifPresent(newStatus -> {
            try {
                service.updateStatus(s.getIdSuggestion(), newStatus);
                refresh();
            } catch (SQLException e) {
                showError("Erreur", e.getMessage());
            }
        });
    }

    @FXML
    private void onDelete() {
        VoyageSuggestion selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { showError("Sélection", "Veuillez sélectionner une suggestion."); return; }
        try {
            service.supprimer(selected.getIdSuggestion());
            refresh();
        } catch (SQLException e) {
            showError("Erreur", e.getMessage());
        }
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle(title); a.setContentText(msg); a.showAndWait();
    }
}
