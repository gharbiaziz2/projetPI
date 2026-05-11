package tn.esprit.gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import tn.esprit.entities.ProgrammeVoyage;
import tn.esprit.entities.User;
import tn.esprit.entities.Voyage;
import tn.esprit.services.ProgrammeVoyageServices;
import tn.esprit.services.UserServices;
import tn.esprit.services.VoyageServices;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Dashboard admin pour gérer les propositions de voyages et ajouter des programmes
 */
public class VoyagePropositionAdminController implements Initializable {

    @FXML private TableView<Voyage> propositionsTable;
    @FXML private TableColumn<Voyage, String> colTypeVoyage;
    @FXML private TableColumn<Voyage, String> colDateDepart;
    @FXML private TableColumn<Voyage, String> colDateRetour;
    @FXML private TableColumn<Voyage, String> colPrix;
    @FXML private TableColumn<Voyage, String> colClient;
    @FXML private TableColumn<Voyage, String> colStatut;
    @FXML private Button acceptButton;
    @FXML private Button refuseButton;
    @FXML private Button addProgrammeButton;
    @FXML private TableView<ProgrammeVoyage> programmeTable;
    @FXML private TableColumn<ProgrammeVoyage, String> colJour;
    @FXML private TableColumn<ProgrammeVoyage, String> colTitre;
    @FXML private TableColumn<ProgrammeVoyage, String> colLieu;
    @FXML private TableColumn<ProgrammeVoyage, String> colHeures;
    @FXML private Label selectedVoyageLabel;

    private final VoyageServices voyageService = new VoyageServices();
    private final UserServices userService = new UserServices();
    private final ProgrammeVoyageServices programmeService = new ProgrammeVoyageServices();
    private final ObservableList<Voyage> propositions = FXCollections.observableArrayList();
    private final ObservableList<ProgrammeVoyage> programmes = FXCollections.observableArrayList();
    private Voyage selectedVoyage;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupPropositionsTable();
        setupProgrammeTable();
        setupButtonActions();
        loadPropositions();
    }

    private void setupPropositionsTable() {
        colTypeVoyage.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTypeVoyage()));
        colDateDepart.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDateDepart().toString()));
        colDateRetour.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDateRetour().toString()));
        colPrix.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPrix() + " DT"));
        colClient.setCellValueFactory(c -> new SimpleStringProperty(getClientName(c.getValue().getIdUserCreateur())));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatut()));

        propositionsTable.setItems(propositions);
        propositionsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedVoyage = newVal;
            if (selectedVoyage != null) {
                selectedVoyageLabel.setText("Voyage sélectionné: " + selectedVoyage.getTypeVoyage());
                loadProgrammes();
                acceptButton.setDisable(false);
                refuseButton.setDisable(false);
                addProgrammeButton.setDisable(false);
            } else {
                selectedVoyageLabel.setText("Aucun voyage sélectionné");
                acceptButton.setDisable(true);
                refuseButton.setDisable(true);
                addProgrammeButton.setDisable(true);
                programmes.clear();
            }
        });
    }

    private void setupProgrammeTable() {
        colJour.setCellValueFactory(c -> new SimpleStringProperty("Jour " + c.getValue().getJour()));
        colTitre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitre()));
        colLieu.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLieu()));
        colHeures.setCellValueFactory(c -> {
            LocalTime debut = c.getValue().getHeureDebut();
            LocalTime fin = c.getValue().getHeureFin();
            String heures = (debut != null ? debut.format(TIME_FMT) : "??") + " - " +
                           (fin != null ? fin.format(TIME_FMT) : "??");
            return new SimpleStringProperty(heures);
        });
        programmeTable.setItems(programmes);
    }

    private void setupButtonActions() {
        acceptButton.setOnAction(e -> accepterProposition());
        refuseButton.setOnAction(e -> refuserProposition());
        addProgrammeButton.setOnAction(e -> afficherDialogAjoutProgramme());
    }

    private void loadPropositions() {
        try {
            propositions.clear();
            List<Voyage> props = voyageService.obtenirPropositionsEnAttente();
            propositions.addAll(props);
        } catch (SQLException e) {
            showError("Erreur", "Impossible de charger les propositions: " + e.getMessage());
        }
    }

    private void loadProgrammes() {
        if (selectedVoyage == null) return;
        try {
            programmes.clear();
            List<ProgrammeVoyage> progs = programmeService.getByVoyage(selectedVoyage.getIdVoyage());
            programmes.addAll(progs);
        } catch (SQLException e) {
            showError("Erreur", "Impossible de charger les programmes: " + e.getMessage());
        }
    }

    @FXML
    private void accepterProposition() {
        if (selectedVoyage == null) {
            showError("Erreur", "Veuillez sélectionner une proposition");
            return;
        }

        // Afficher un dialog pour sélectionner le guide
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Accepter la proposition");
        dialog.setHeaderText("Sélectionner un guide pour ce voyage");
        dialog.initModality(Modality.APPLICATION_MODAL);

        GridPane pane = new GridPane();
        pane.setHgap(10);
        pane.setVgap(10);
        pane.setPadding(new Insets(20));

        try {
            List<User> guides = userService.getUsersByRole(User.Role.GUIDE_TOURISTIQUE);
            ComboBox<User> guideCombo = new ComboBox<>();
            guideCombo.setItems(FXCollections.observableArrayList(guides));
            if (!guides.isEmpty()) guideCombo.getSelectionModel().selectFirst();

            pane.add(new Label("Guide:"), 0, 0);
            pane.add(guideCombo, 1, 0);

            dialog.getDialogPane().setContent(pane);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            dialog.setResultConverter(btn -> {
                if (btn == ButtonType.OK && guideCombo.getValue() != null) {
                    return guideCombo.getValue().getIdUser();
                }
                return null;
            });

            Integer idGuide = dialog.showAndWait().orElse(null);
            if (idGuide != null) {
                voyageService.accepterPropositionVoyage(selectedVoyage.getIdVoyage(), idGuide);
                showInfo("Succès", "✓ Proposition acceptée!");
                loadPropositions();
                selectedVoyage = null;
                propositionsTable.getSelectionModel().clearSelection();
            }
        } catch (SQLException e) {
            showError("Erreur", "Erreur lors de l'acceptation: " + e.getMessage());
        }
    }

    @FXML
    private void refuserProposition() {
        if (selectedVoyage == null) {
            showError("Erreur", "Veuillez sélectionner une proposition");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer le refus");
        confirm.setHeaderText("Êtes-vous sûr de refuser cette proposition?");
        confirm.setContentText("Cette action ne peut pas être annulée.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                voyageService.refuserPropositionVoyage(selectedVoyage.getIdVoyage());
                showInfo("Succès", "✓ Proposition refusée");
                loadPropositions();
                selectedVoyage = null;
                propositionsTable.getSelectionModel().clearSelection();
            } catch (SQLException e) {
                showError("Erreur", "Erreur lors du refus: " + e.getMessage());
            }
        }
    }

    @FXML
    private void afficherDialogAjoutProgramme() {
        if (selectedVoyage == null) {
            showError("Erreur", "Veuillez sélectionner un voyage");
            return;
        }

        Dialog<ProgrammeVoyage> dialog = new Dialog<>();
        dialog.setTitle("Ajouter un programme");
        dialog.setHeaderText("Ajouter une activité au programme du voyage");
        dialog.initModality(Modality.APPLICATION_MODAL);

        GridPane pane = new GridPane();
        pane.setHgap(10);
        pane.setVgap(10);
        pane.setPadding(new Insets(20));

        Spinner<Integer> jourSpinner = new Spinner<>(1, 30, 1);
        TextField titreField = new TextField();
        titreField.setPromptText("Titre de l'activité");
        TextField lieuField = new TextField();
        lieuField.setPromptText("Lieu");
        Spinner<Integer> heureDebSpinner = new Spinner<>(0, 23, 9);
        Spinner<Integer> heureFinSpinner = new Spinner<>(0, 23, 18);
        TextArea descArea = new TextArea();
        descArea.setPromptText("Description");
        descArea.setPrefRowCount(3);

        pane.add(new Label("Jour:"), 0, 0);
        pane.add(jourSpinner, 1, 0);
        pane.add(new Label("Titre:"), 0, 1);
        pane.add(titreField, 1, 1);
        pane.add(new Label("Lieu:"), 0, 2);
        pane.add(lieuField, 1, 2);
        pane.add(new Label("Heure début:"), 0, 3);
        pane.add(heureDebSpinner, 1, 3);
        pane.add(new Label("Heure fin:"), 0, 4);
        pane.add(heureFinSpinner, 1, 4);
        pane.add(new Label("Description:"), 0, 5);
        pane.add(descArea, 1, 5);

        dialog.getDialogPane().setContent(pane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && !titreField.getText().isEmpty()) {
                ProgrammeVoyage prog = new ProgrammeVoyage();
                prog.setIdVoyage(selectedVoyage.getIdVoyage());
                prog.setJour(jourSpinner.getValue());
                prog.setTitre(titreField.getText());
                prog.setLieu(lieuField.getText());
                prog.setDescription(descArea.getText());
                prog.setHeureDebut(LocalTime.of(heureDebSpinner.getValue(), 0));
                prog.setHeureFin(LocalTime.of(heureFinSpinner.getValue(), 0));
                prog.setImage("");
                return prog;
            }
            return null;
        });

        ProgrammeVoyage newProg = dialog.showAndWait().orElse(null);
        if (newProg != null) {
            try {
                programmeService.ajouter(newProg);
                showInfo("Succès", "✓ Programme ajouté!");
                loadProgrammes();
            } catch (SQLException e) {
                showError("Erreur", "Erreur lors de l'ajout du programme: " + e.getMessage());
            }
        }
    }

    private String getClientName(int idUser) {
        try {
            User user = userService.getById(idUser);
            return user != null ? user.getNom() + " " + user.getPrenom() : "Inconnu";
        } catch (SQLException e) {
            return "Erreur";
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.showAndWait();
    }
}
