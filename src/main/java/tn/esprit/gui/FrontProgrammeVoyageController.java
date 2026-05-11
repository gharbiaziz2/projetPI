package tn.esprit.gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import tn.esprit.entities.ProgrammeVoyage;
import tn.esprit.entities.User;
import tn.esprit.entities.Voyage;
import tn.esprit.services.ProgrammeVoyageServices;
import tn.esprit.services.UserServices;
import tn.esprit.services.VoyageServices;

import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Contrôleur pour l'interface frontend où les clients peuvent proposer des programmes de voyage.
 * Les propositions doivent être approuvées par l'admin.
 */
public class FrontProgrammeVoyageController {
    @FXML private TableView<ProgrammeVoyage> tablePropositions;
    @FXML private TableColumn<ProgrammeVoyage, String> colVoyage;
    @FXML private TableColumn<ProgrammeVoyage, String> colTitre;
    @FXML private TableColumn<ProgrammeVoyage, String> colJour;
    @FXML private TableColumn<ProgrammeVoyage, String> colStatut;
    @FXML private TextField searchField;
    @FXML private Label lblUserName;

    private final ProgrammeVoyageServices programmeService = new ProgrammeVoyageServices();
    private final VoyageServices voyageService = new VoyageServices();
    private final UserServices userService = new UserServices();
    private ObservableList<ProgrammeVoyage> mesPropositions = FXCollections.observableArrayList();
    private List<Voyage> voyages;
    private User currentUser;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    public void initialize() {
        // Récupérer l'utilisateur courant
        currentUser = SessionManager.getCurrentUser();
        if (currentUser != null && lblUserName != null) {
            lblUserName.setText("Mes propositions - " + currentUser.getNom() + " " + currentUser.getPrenom());
        }

        colVoyage.setCellValueFactory(c -> {
            int idVoyage = c.getValue().getIdVoyage();
            String name = String.valueOf(idVoyage);
            if (voyages != null) {
                for (Voyage v : voyages) {
                    if (v.getIdVoyage() == idVoyage) { 
                        name = v.getTypeVoyage(); 
                        break; 
                    }
                }
            }
            return new SimpleStringProperty(name);
        });
        colTitre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitre()));
        colJour.setCellValueFactory(c -> new SimpleStringProperty("Jour " + c.getValue().getJour()));
        colStatut.setCellValueFactory(c -> {
            String statut = c.getValue().getStatut();
            String styleClass = switch(statut) {
                case "ACCEPTÉ" -> "status-accepted";
                case "REFUSÉ" -> "status-rejected";
                default -> "status-pending";
            };
            return new SimpleStringProperty(statut);
        });

        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> filterPropositions(n));
        }
        refresh();
    }

    public void refresh() {
        try {
            voyages = voyageService.afficher();
            // Récupérer uniquement les propositions du client connecté
            List<ProgrammeVoyage> toutesPropositions = programmeService.afficher();
            mesPropositions.clear();
            for (ProgrammeVoyage p : toutesPropositions) {
                if (p.isEstPropositionClient() && currentUser != null && p.getIdUserCreateur() == currentUser.getIdUser()) {
                    mesPropositions.add(p);
                }
            }
            tablePropositions.setItems(mesPropositions);
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur", "Impossible de charger les propositions.");
        }
    }

    private void filterPropositions(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            tablePropositions.setItems(mesPropositions);
            return;
        }
        String lower = keyword.toLowerCase();
        tablePropositions.setItems(mesPropositions.filtered(p ->
                (p.getTitre() != null && p.getTitre().toLowerCase().contains(lower)) ||
                (p.getDescription() != null && p.getDescription().toLowerCase().contains(lower))));
    }

    @FXML
    private void onProposerProgramme() {
        if (currentUser == null) {
            showError("Erreur", "Vous devez être connecté pour proposer un programme.");
            return;
        }
        ProgrammeVoyage nouvelle = showPropositionDialog(null);
        if (nouvelle != null) {
            try {
                nouvelle.setEstPropositionClient(true);
                nouvelle.setIdUserCreateur(currentUser.getIdUser());
                nouvelle.setStatut("EN ATTENTE");
                programmeService.ajouter(nouvelle);
                refresh();
                showInfo("Succès", "Votre proposition a été envoyée à l'administration pour approbation.");
            } catch (SQLException e) {
                showError("Erreur", "Impossible d'ajouter la proposition: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onModifierProposition() {
        ProgrammeVoyage selected = tablePropositions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Sélection", "Veuillez sélectionner une proposition.");
            return;
        }
        if (!selected.getStatut().equals("EN ATTENTE")) {
            showError("Erreur", "Vous ne pouvez modifier que les propositions en attente d'approbation.");
            return;
        }
        ProgrammeVoyage updated = showPropositionDialog(selected);
        if (updated != null) {
            try {
                programmeService.modifier(updated);
                refresh();
                showInfo("Succès", "Proposition modifiée avec succès.");
            } catch (SQLException e) {
                showError("Erreur", "Impossible de modifier la proposition: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onSupprimerProposition() {
        ProgrammeVoyage selected = tablePropositions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Sélection", "Veuillez sélectionner une proposition.");
            return;
        }
        if (!selected.getStatut().equals("EN ATTENTE")) {
            showError("Erreur", "Vous ne pouvez supprimer que les propositions en attente.");
            return;
        }
        if (confirm("Êtes-vous sûr de vouloir supprimer cette proposition?")) {
            try {
                programmeService.supprimer(selected.getIdProgramme());
                refresh();
                showInfo("Succès", "Proposition supprimée.");
            } catch (SQLException e) {
                showError("Erreur", e.getMessage());
            }
        }
    }

    private ProgrammeVoyage showPropositionDialog(ProgrammeVoyage existing) {
        Dialog<ProgrammeVoyage> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Proposer un programme" : "Modifier la proposition");
        dialog.initModality(Modality.APPLICATION_MODAL);
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
                if (v.getIdVoyage() == existing.getIdVoyage()) { 
                    voyageCombo.getSelectionModel().select(v); 
                    break; 
                }
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
        DialogStyleHelper.addRow(g, 0, "Voyage *", voyageCombo);
        DialogStyleHelper.addRow(g, 1, "Jour *", spJour);
        DialogStyleHelper.addRow(g, 2, "Titre *", tfTitre);
        DialogStyleHelper.addRow(g, 3, "Description", tfDescription);
        DialogStyleHelper.addRow(g, 4, "Lieu *", tfLieu);
        DialogStyleHelper.addRow(g, 5, "Heure début", tfHeureDebut);
        DialogStyleHelper.addRow(g, 6, "Heure fin", tfHeureFin);
        DialogStyleHelper.addRow(g, 7, "Image", tfImage);

        dialog.getDialogPane().setContent(g);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                Voyage selVoyage = voyageCombo.getSelectionModel().getSelectedItem();
                if (selVoyage == null) { 
                    showError("Validation", "Sélectionnez un voyage."); 
                    return null; 
                }
                if (tfTitre.getText() == null || tfTitre.getText().isBlank()) {
                    showError("Validation", "Le titre est obligatoire.");
                    return null;
                }
                if (tfLieu.getText() == null || tfLieu.getText().isBlank()) {
                    showError("Validation", "Le lieu est obligatoire.");
                    return null;
                }
                
                ProgrammeVoyage p = new ProgrammeVoyage();
                if (existing != null) {
                    p.setIdProgramme(existing.getIdProgramme());
                    p.setStatut(existing.getStatut());
                    p.setIdUserCreateur(existing.getIdUserCreateur());
                    p.setEstPropositionClient(existing.isEstPropositionClient());
                } else {
                    p.setStatut("EN ATTENTE");
                    p.setEstPropositionClient(true);
                    p.setIdUserCreateur(currentUser.getIdUser());
                }
                
                p.setIdVoyage(selVoyage.getIdVoyage());
                p.setJour(spJour.getValue());
                p.setTitre(tfTitre.getText());
                p.setDescription(tfDescription.getText());
                p.setLieu(tfLieu.getText());
                try { 
                    if (!tfHeureDebut.getText().isBlank()) 
                        p.setHeureDebut(LocalTime.parse(tfHeureDebut.getText(), TIME_FMT)); 
                } catch (Exception ignored) {}
                try { 
                    if (!tfHeureFin.getText().isBlank()) 
                        p.setHeureFin(LocalTime.parse(tfHeureFin.getText(), TIME_FMT)); 
                } catch (Exception ignored) {}
                p.setImage(tfImage.getText());
                return p;
            }
            return null;
        });
        return dialog.showAndWait().orElse(null);
    }

    private boolean confirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setContentText(msg);
        a.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        return a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setContentText(msg);
        a.showAndWait();
    }
}
