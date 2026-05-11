package tn.esprit.gui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Modality;
import tn.esprit.entities.User;
import tn.esprit.entities.Voyage;
import tn.esprit.services.VoyageServices;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;

/**
 * Contrôleur pour permettre aux clients de proposer un voyage personnalisé
 */
public class VoyageSuggestionClientController implements Initializable {

    @FXML private TextField nomField;
    @FXML private DatePicker dateDepField;
    @FXML private DatePicker dateRetField;
    @FXML private TextField destinationField;
    @FXML private TextField villeDepField;
    @FXML private TextField budgetField;
    @FXML private TextArea descriptionArea;
    @FXML private Button submitButton;
    @FXML private Label messageLabel;

    private final VoyageServices voyageService = new VoyageServices();
    private User currentUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            showError("Erreur", "Vous devez être connecté pour proposer un voyage");
            return;
        }

        submitButton.setOnAction(e -> proposerVoyage());
    }

    @FXML
    private void proposerVoyage() {
        // Validation des champs
        if (nomField.getText().trim().isEmpty()) {
            showMessage("Le nom du voyage est requis", true);
            return;
        }
        if (dateDepField.getValue() == null) {
            showMessage("La date de départ est requise", true);
            return;
        }
        if (dateRetField.getValue() == null) {
            showMessage("La date de retour est requise", true);
            return;
        }
        if (dateRetField.getValue().isBefore(dateDepField.getValue())) {
            showMessage("La date de retour doit être après la date de départ", true);
            return;
        }
        if (budgetField.getText().trim().isEmpty()) {
            showMessage("Le budget est requis", true);
            return;
        }

        try {
            double budget = Double.parseDouble(budgetField.getText());
            if (budget <= 0) {
                showMessage("Le budget doit être positif", true);
                return;
            }

            // Créer le voyage
            Voyage voyage = new Voyage();
            voyage.setTypeVoyage(nomField.getText());
            voyage.setDateDepart(dateDepField.getValue());
            voyage.setDateRetour(dateRetField.getValue());
            voyage.setPrix(budget);
            voyage.setPlacesDisponibles(1); // Default: 1 personne
            voyage.setStatut("EN ATTENTE");
            voyage.setIdGuide(0); // No guide yet
            voyage.setImage("");
            voyage.setEstPropositionClient(true);
            voyage.setIdUserCreateur(currentUser.getIdUser());

            // Ajouter la proposition
            int idVoyage = voyageService.ajouterPropositionVoyage(voyage, currentUser.getIdUser());

            showMessage("✓ Votre proposition a été envoyée avec succès!\nL'admin va l'examiner et vous répondre bientôt.", false);
            clearForm();

        } catch (NumberFormatException e) {
            showMessage("Le budget doit être un nombre valide", true);
        } catch (SQLException e) {
            showMessage("Erreur lors de l'ajout de la proposition: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    private void clearForm() {
        nomField.clear();
        dateDepField.setValue(null);
        dateRetField.setValue(null);
        destinationField.clear();
        villeDepField.clear();
        budgetField.clear();
        descriptionArea.clear();
    }

    private void showMessage(String message, boolean isError) {
        messageLabel.setText(message);
        messageLabel.setStyle(isError ? "-fx-text-fill: #d32f2f;" : "-fx-text-fill: #388e3c;");
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.showAndWait();
    }
}
