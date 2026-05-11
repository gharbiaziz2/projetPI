package tn.esprit.gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import tn.esprit.entities.User;
import tn.esprit.entities.Voyage;
import tn.esprit.services.VoyageServices;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Contrôleur pour l'interface frontend où les clients peuvent proposer de nouveaux voyages.
 * Les propositions doivent être approuvées par l'admin pour être visibles.
 */
public class FrontVoyageController {
    @FXML private TableView<Voyage> tablePropositions;
    @FXML private TableColumn<Voyage, String> colType;
    @FXML private TableColumn<Voyage, String> colDates;
    @FXML private TableColumn<Voyage, String> colPrix;
    @FXML private TableColumn<Voyage, String> colStatut;
    @FXML private TextField searchField;
    @FXML private Label lblUserName;

    private final VoyageServices voyageService = new VoyageServices();
    private ObservableList<Voyage> mesPropositions = FXCollections.observableArrayList();

    private static final DateTimeFormatter D_FMT_DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private User currentUser;

    @FXML
    public void initialize() {
        // Récupérer l'utilisateur courant
        currentUser = SessionManager.getCurrentUser();
        if (currentUser != null && lblUserName != null) {
            lblUserName.setText("Mes propositions de voyage - " + currentUser.getNom() + " " + currentUser.getPrenom());
        }

        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTypeVoyage()));
        colDates.setCellValueFactory(c -> {
            LocalDate dd = c.getValue().getDateDepart();
            LocalDate dr = c.getValue().getDateRetour();
            String dates = "";
            if (dd != null && dr != null) {
                dates = dd.format(D_FMT_DISPLAY) + " → " + dr.format(D_FMT_DISPLAY);
            }
            return new SimpleStringProperty(dates);
        });
        colPrix.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f TND", c.getValue().getPrix())));
        colStatut.setCellValueFactory(c -> {
            String statut = c.getValue().getStatut();
            return new SimpleStringProperty(statut != null ? statut : "UNKN");
        });

        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> filterPropositions(n));
        }
        refresh();
    }

    public void refresh() {
        try {
            List<Voyage> tousVoyages = voyageService.afficher();
            mesPropositions.clear();
            for (Voyage v : tousVoyages) {
                if (v.isEstPropositionClient() && currentUser != null && v.getIdUserCreateur() == currentUser.getIdUser()) {
                    mesPropositions.add(v);
                }
            }
            tablePropositions.setItems(mesPropositions);
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur", "Impossible de charger vos propositions.");
        }
    }

    private void filterPropositions(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            tablePropositions.setItems(mesPropositions);
            return;
        }
        String lower = keyword.toLowerCase();
        tablePropositions.setItems(mesPropositions.filtered(v ->
                (v.getTypeVoyage() != null && v.getTypeVoyage().toLowerCase().contains(lower))));
    }

    @FXML
    private void onProposerVoyage() {
        if (currentUser == null) {
            showError("Erreur", "Vous devez être connecté pour proposer un voyage.");
            return;
        }
        Voyage nouveauVoyage = showVoyageDialog(null);
        if (nouveauVoyage != null) {
            try {
                nouveauVoyage.setEstPropositionClient(true);
                nouveauVoyage.setIdUserCreateur(currentUser.getIdUser());
                nouveauVoyage.setStatut("EN ATTENTE");
                voyageService.ajouter(nouveauVoyage);
                refresh();
                showInfo("Succès", "Votre proposition de voyage a été envoyée à l'administration pour approbation.");
            } catch (SQLException e) {
                showError("Erreur", "Impossible d'ajouter la proposition: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onModifierProposition() {
        Voyage selected = tablePropositions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Sélection", "Veuillez sélectionner une proposition.");
            return;
        }
        if (!selected.getStatut().equals("EN ATTENTE")) {
            showError("Erreur", "Vous ne pouvez modifier que les propositions en attente d'approbation.");
            return;
        }
        Voyage updated = showVoyageDialog(selected);
        if (updated != null) {
            try {
                voyageService.modifier(updated);
                refresh();
                showInfo("Succès", "Proposition modifiée avec succès.");
            } catch (SQLException e) {
                showError("Erreur", "Impossible de modifier la proposition: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onSupprimerProposition() {
        Voyage selected = tablePropositions.getSelectionModel().getSelectedItem();
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
                voyageService.supprimer(selected.getIdVoyage());
                refresh();
                showInfo("Succès", "Proposition supprimée.");
            } catch (SQLException e) {
                showError("Erreur", e.getMessage());
            }
        }
    }

    private Voyage showVoyageDialog(Voyage existing) {
        Dialog<Voyage> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Proposer un voyage" : "Modifier la proposition");
        dialog.initModality(Modality.APPLICATION_MODAL);
        DialogStyleHelper.styleFrontDialogPane(dialog.getDialogPane());

        TextField tfType = new TextField();
        DatePicker dpDepart = new DatePicker();
        DatePicker dpRetour = new DatePicker();
        TextField tfPrix = new TextField();
        Spinner<Integer> spPlaces = new Spinner<>(1, 100, 10);
        TextField tfImage = new TextField();
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
            }
        });

        if (existing != null) {
            tfType.setText(existing.getTypeVoyage());
            if (existing.getDateDepart() != null) dpDepart.setValue(existing.getDateDepart());
            if (existing.getDateRetour() != null) dpRetour.setValue(existing.getDateRetour());
            tfPrix.setText(String.valueOf(existing.getPrix()));
            spPlaces.getValueFactory().setValue(existing.getPlacesDisponibles());
            tfImage.setText(existing.getImage() != null ? existing.getImage() : "");
        }

        GridPane g = DialogStyleHelper.buildGrid();
        DialogStyleHelper.addRow(g, 0, "Type de voyage *", tfType);
        DialogStyleHelper.addRow(g, 1, "Date de départ *", dpDepart);
        DialogStyleHelper.addRow(g, 2, "Date de retour *", dpRetour);
        DialogStyleHelper.addRow(g, 3, "Prix (TND) *", tfPrix);
        DialogStyleHelper.addRow(g, 4, "Places disponibles *", spPlaces);
        DialogStyleHelper.addRow(g, 5, "Image", tfImage);
        DialogStyleHelper.addRow(g, 6, "", btnBrowseImage);

        dialog.getDialogPane().setContent(g);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                if (tfType.getText() == null || tfType.getText().isBlank()) {
                    showError("Validation", "Le type de voyage est obligatoire.");
                    return null;
                }
                if (dpDepart.getValue() == null) {
                    showError("Validation", "La date de départ est obligatoire.");
                    return null;
                }
                if (dpRetour.getValue() == null) {
                    showError("Validation", "La date de retour est obligatoire.");
                    return null;
                }
                if (!dpRetour.getValue().isAfter(dpDepart.getValue())) {
                    showError("Validation", "La date de retour doit être après la date de départ.");
                    return null;
                }
                if (tfPrix.getText() == null || tfPrix.getText().isBlank()) {
                    showError("Validation", "Le prix est obligatoire.");
                    return null;
                }
                
                double prix;
                try {
                    prix = Double.parseDouble(tfPrix.getText());
                    if (prix < 0) throw new NumberFormatException();
                } catch (Exception e) {
                    showError("Validation", "Prix invalide (nombre >= 0).");
                    return null;
                }

                Voyage v = new Voyage();
                if (existing != null) {
                    v.setIdVoyage(existing.getIdVoyage());
                    v.setStatut(existing.getStatut());
                    v.setIdUserCreateur(existing.getIdUserCreateur());
                    v.setEstPropositionClient(existing.isEstPropositionClient());
                } else {
                    v.setStatut("EN ATTENTE");
                    v.setEstPropositionClient(true);
                    v.setIdUserCreateur(currentUser.getIdUser());
                }
                
                v.setTypeVoyage(tfType.getText());
                v.setDateDepart(dpDepart.getValue());
                v.setDateRetour(dpRetour.getValue());
                v.setPrix(prix);
                v.setPlacesDisponibles(spPlaces.getValue());
                v.setImage(tfImage.getText());
                return v;
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
