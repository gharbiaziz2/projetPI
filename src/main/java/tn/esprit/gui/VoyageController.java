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
import javafx.util.StringConverter;
import tn.esprit.entities.Activite;
import tn.esprit.entities.Destination;
import tn.esprit.entities.User;
import tn.esprit.entities.Voyage;
import tn.esprit.entities.VoyageActivite;
import tn.esprit.entities.VoyageDestination;
import tn.esprit.services.ActiviteServices;
import tn.esprit.services.DestinationServices;
import tn.esprit.services.UserServices;
import tn.esprit.services.VoyageActiviteServices;
import tn.esprit.services.VoyageDestinationServices;
import tn.esprit.services.VoyageServices;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VoyageController {

    @FXML private TableView<Voyage> table;
    @FXML private TableColumn<Voyage, String> colTypeVoyage;
    @FXML private TableColumn<Voyage, String> colDateDepart;
    @FXML private TableColumn<Voyage, String> colDateRetour;
    @FXML private TableColumn<Voyage, BigDecimal> colPrix;
    @FXML private TableColumn<Voyage, Number> colPlaces;
    @FXML private TableColumn<Voyage, String> colStatut;
    @FXML private TableColumn<Voyage, String> colDestination;
    @FXML private TableColumn<Voyage, String> colActivites;
    @FXML private TableColumn<Voyage, String> colIdGuide;
    @FXML private TextField searchField;
    @FXML private Button btnFilter;

    private final VoyageServices service = new VoyageServices();
    private final UserServices userService = new UserServices();
    private final VoyageDestinationServices voyageDestinationService = new VoyageDestinationServices();
    private final VoyageActiviteServices voyageActiviteService = new VoyageActiviteServices();
    private final DestinationServices destinationService = new DestinationServices();
    private final ActiviteServices activiteService = new ActiviteServices();
    private final ObservableList<Voyage> list = FXCollections.observableArrayList();
    private final FilteredList<Voyage> filteredList = new FilteredList<>(list, p -> true);
    private static final DateTimeFormatter D_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private String filterStatut;
    private Map<Integer, String> guideDisplay = new HashMap<>();
    private Map<Integer, String> destinationDisplay = new HashMap<>();
    private Map<Integer, String> activiteDisplay = new HashMap<>();

    /** Holder for dialog result: selected destinations and activities. */
    public static class VoyageDialogResult {
        public final List<Destination> destinations;
        public final List<Activite> activites;
        public VoyageDialogResult(List<Destination> destinations, List<Activite> activites) {
            this.destinations = destinations != null ? destinations : new ArrayList<>();
            this.activites = activites != null ? activites : new ArrayList<>();
        }
    }

    @FXML
    public void initialize() {
        colTypeVoyage.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTypeVoyage()));
        colDateDepart.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDateDepart() != null ? c.getValue().getDateDepart().format(D_FMT) : ""));
        colDateRetour.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDateRetour() != null ? c.getValue().getDateRetour().format(D_FMT) : ""));
        colPrix.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPrix()));
        colPlaces.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPlacesDisponibles()));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatut() != null ? c.getValue().getStatut() : ""));
        colDestination.setCellValueFactory(c -> new SimpleStringProperty(destinationDisplay.getOrDefault(c.getValue().getIdVoyage(), "—")));
        colActivites.setCellValueFactory(c -> new SimpleStringProperty(activiteDisplay.getOrDefault(c.getValue().getIdVoyage(), "—")));
        colIdGuide.setCellValueFactory(c -> new SimpleStringProperty(guideDisplay.getOrDefault(c.getValue().getIdGuide(), String.valueOf(c.getValue().getIdGuide()))));
        table.setItems(filteredList);
        if (searchField != null) searchField.textProperty().addListener((o, ov, nv) -> applyFilter());
        refresh();
    }

    private void applyFilter() {
        String q = searchField != null ? (searchField.getText() == null ? "" : searchField.getText()).trim().toLowerCase() : "";
        filteredList.setPredicate(v -> {
            if (filterStatut != null && !filterStatut.isEmpty() && (v.getStatut() == null || !v.getStatut().toLowerCase().contains(filterStatut.toLowerCase()))) return false;
            if (q.isEmpty()) return true;
            String type = v.getTypeVoyage() != null ? v.getTypeVoyage().toLowerCase() : "";
            String statut = v.getStatut() != null ? v.getStatut().toLowerCase() : "";
            String guide = guideDisplay.getOrDefault(v.getIdGuide(), "").toLowerCase();
            String dest = destinationDisplay.getOrDefault(v.getIdVoyage(), "").toLowerCase();
            String act = activiteDisplay.getOrDefault(v.getIdVoyage(), "").toLowerCase();
            return type.contains(q) || statut.contains(q) || guide.contains(q) || dest.contains(q) || act.contains(q);
        });
    }

    @FXML
    private void onFilter() {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Filtrage");
        d.initModality(Modality.APPLICATION_MODAL);
        DialogStyleHelper.styleDialogPane(d.getDialogPane());
        TextField statutF = new TextField(filterStatut != null ? filterStatut : "");
        statutF.setPromptText("Statut (vide = tous)");
        DialogStyleHelper.styleField(statutF);
        javafx.scene.layout.GridPane g = DialogStyleHelper.buildGrid();
        DialogStyleHelper.addRow(g, 0, "Statut", statutF);
        d.getDialogPane().setContent(g);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(btn -> btn);
        if (d.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            filterStatut = statutF.getText() != null && !statutF.getText().isBlank() ? statutF.getText().trim() : null;
            applyFilter();
        }
    }

    public void refresh() {
        try {
            guideDisplay.clear();
            destinationDisplay.clear();
            activiteDisplay.clear();
            for (User u : userService.afficher()) guideDisplay.put(u.getIdUser(), u.getNom() + " " + u.getPrenom());
            list.clear();
            list.addAll(service.afficher());
            for (Voyage v : list) {
                List<Destination> dests = voyageDestinationService.getDestinationsForVoyage(v.getIdVoyage());
                String destSummary = dests.isEmpty() ? "—" : dests.stream()
                        .map(d -> (d.getPaysDepart() != null ? d.getPaysDepart() : "?") + " → " + (d.getPaysArrivee() != null ? d.getPaysArrivee() : "?"))
                        .collect(Collectors.joining(", "));
                destinationDisplay.put(v.getIdVoyage(), destSummary);
                List<Activite> activites = voyageActiviteService.getActivitesForVoyage(v.getIdVoyage());
                String actSummary = activites.isEmpty() ? "—" : activites.stream()
                        .map(a -> a.getNom() != null ? a.getNom() : "?")
                        .collect(Collectors.joining(", "));
                activiteDisplay.put(v.getIdVoyage(), actSummary);
            }
        } catch (SQLException e) { showError("Erreur", "Chargement impossible."); }
    }

    @FXML private void onAdd() {
        Voyage v = new Voyage();
        VoyageDialogResult result = showDialog(v, "Ajouter voyage");
        if (result != null) {
            try {
                int newId = service.ajouterAndReturnId(v);
                for (Destination d : result.destinations) {
                    voyageDestinationService.ajouter(new VoyageDestination(newId, d.getIdDestination()));
                }
                for (Activite a : result.activites) {
                    voyageActiviteService.ajouter(new VoyageActivite(newId, a.getIdActivite()));
                }
                refresh();
            } catch (IllegalArgumentException e) { showError("Validation", e.getMessage()); }
            catch (SQLException e) { showError("Erreur", "Ajout impossible."); }
        }
    }

    @FXML private void onEdit() {
        Voyage v = table.getSelectionModel().getSelectedItem();
        if (v == null) { showError("Attention", "Selectionnez une ligne."); return; }
        VoyageDialogResult result = showDialog(v, "Modifier voyage");
        if (result != null) {
            try {
                service.modifier(v);
                voyageDestinationService.supprimerByVoyage(v.getIdVoyage());
                voyageActiviteService.supprimerByVoyage(v.getIdVoyage());
                for (Destination d : result.destinations) {
                    voyageDestinationService.ajouter(new VoyageDestination(v.getIdVoyage(), d.getIdDestination()));
                }
                for (Activite a : result.activites) {
                    voyageActiviteService.ajouter(new VoyageActivite(v.getIdVoyage(), a.getIdActivite()));
                }
                refresh();
            } catch (IllegalArgumentException e) { showError("Validation", e.getMessage()); }
            catch (SQLException e) { showError("Erreur", "Modification impossible."); }
        }
    }

    @FXML private void onDelete() {
        Voyage v = table.getSelectionModel().getSelectedItem();
        if (v == null) { showError("Attention", "Selectionnez une ligne."); return; }
        if (confirm("Supprimer ce voyage ?")) {
            try {
                service.supprimer(v.getIdVoyage());
                refresh();
            } catch (SQLException e) { showError("Erreur", "Suppression impossible."); }
        }
    }

    /** Returns selected destinations and activities on OK, or null on Cancel. Voyage v is updated in place. */
    private VoyageDialogResult showDialog(Voyage v, String title) {
        Dialog<VoyageDialogResult> d = new Dialog<>();
        d.setTitle(title);
        d.initModality(Modality.APPLICATION_MODAL);
        DialogStyleHelper.styleDialogPane(d.getDialogPane());
        TextField type = new TextField(v.getTypeVoyage());
        DatePicker dateD = new DatePicker(v.getDateDepart());
        DatePicker dateR = new DatePicker(v.getDateRetour());
        TextField prix = new TextField(v.getPrix() != null ? v.getPrix().toString() : "");
        TextField places = new TextField(v.getPlacesDisponibles() > 0 ? String.valueOf(v.getPlacesDisponibles()) : "");
        TextField statut = new TextField(v.getStatut());
        ComboBox<User> comboGuide = new ComboBox<>();
        comboGuide.setConverter(new StringConverter<User>() {
            @Override public String toString(User u) { return u == null ? "" : u.getNom() + " " + u.getPrenom(); }
            @Override public User fromString(String s) { return null; }
        });
        try {
            comboGuide.getItems().addAll(userService.getGuides());
        } catch (SQLException e) {}
        if (v.getIdGuide() > 0) comboGuide.getItems().stream().filter(u -> u.getIdUser() == v.getIdGuide()).findFirst().ifPresent(comboGuide.getSelectionModel()::select);
        else if (!comboGuide.getItems().isEmpty()) comboGuide.getSelectionModel().selectFirst();

        ListView<Destination> listDestinations = new ListView<>();
        listDestinations.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listDestinations.setPrefHeight(120);
        try {
            listDestinations.getItems().addAll(destinationService.afficher());
            listDestinations.setCellFactory(lv -> new ListCell<Destination>() {
                @Override protected void updateItem(Destination dest, boolean empty) {
                    super.updateItem(dest, empty);
                    setText(dest == null || empty ? "" : (dest.getPaysDepart() != null ? dest.getPaysDepart() : "?") + " → " + (dest.getPaysArrivee() != null ? dest.getPaysArrivee() : "?"));
                }
            });
            if (v.getIdVoyage() > 0) {
                List<Destination> currentDests = voyageDestinationService.getDestinationsForVoyage(v.getIdVoyage());
                for (int i = 0; i < listDestinations.getItems().size(); i++) {
                    int id = listDestinations.getItems().get(i).getIdDestination();
                    if (currentDests.stream().anyMatch(dest -> dest.getIdDestination() == id))
                        listDestinations.getSelectionModel().select(i);
                }
            }
        } catch (SQLException e) {}

        ListView<Activite> listActivites = new ListView<>();
        listActivites.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listActivites.setPrefHeight(120);
        try {
            listActivites.getItems().addAll(activiteService.afficher());
            listActivites.setCellFactory(lv -> new ListCell<Activite>() {
                @Override protected void updateItem(Activite act, boolean empty) {
                    super.updateItem(act, empty);
                    setText(act == null || empty ? "" : (act.getNom() != null ? act.getNom() : "?") + (act.getPrix() != null ? " - " + act.getPrix() + " DT" : ""));
                }
            });
            if (v.getIdVoyage() > 0) {
                List<Activite> currentActivites = voyageActiviteService.getActivitesForVoyage(v.getIdVoyage());
                for (int i = 0; i < listActivites.getItems().size(); i++) {
                    int id = listActivites.getItems().get(i).getIdActivite();
                    if (currentActivites.stream().anyMatch(act -> act.getIdActivite() == id))
                        listActivites.getSelectionModel().select(i);
                }
            }
        } catch (SQLException e) {}

        GridPane g = DialogStyleHelper.buildGrid();
        DialogStyleHelper.addRow(g, 0, "Type voyage *", type);
        DialogStyleHelper.addRow(g, 1, "Date départ *", dateD);
        DialogStyleHelper.addRow(g, 2, "Date retour *", dateR);
        DialogStyleHelper.addRow(g, 3, "Prix *", prix);
        DialogStyleHelper.addRow(g, 4, "Places *", places);
        DialogStyleHelper.addRow(g, 5, "Statut *", statut);
        DialogStyleHelper.addRow(g, 6, "Guide *", comboGuide);
        Label destLabel = new Label("Destinations (multi)");
        destLabel.getStyleClass().add("crud-dialog-label");
        g.add(destLabel, 0, 7);
        g.add(listDestinations, 1, 7);
        Label actLabel = new Label("Activités (multi)");
        actLabel.getStyleClass().add("crud-dialog-label");
        g.add(actLabel, 0, 8);
        g.add(listActivites, 1, 8);
        DialogStyleHelper.styleField(type);
        DialogStyleHelper.styleField(prix);
        DialogStyleHelper.styleField(places);
        DialogStyleHelper.styleField(statut);
        DialogStyleHelper.styleDatePicker(dateD);
        DialogStyleHelper.styleDatePicker(dateR);
        DialogStyleHelper.styleCombo(comboGuide);
        VBox content = new VBox(new Label(title), g);
        content.getStyleClass().add("crud-dialog-content");
        ((Label) content.getChildren().get(0)).getStyleClass().add("crud-dialog-title");
        content.setSpacing(16);
        d.getDialogPane().setContent(content);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            if (type.getText() == null || type.getText().isBlank()) { showError("Validation", "Type obligatoire."); return null; }
            LocalDate dd = dateD.getValue();
            LocalDate dr = dateR.getValue();
            if (dd == null) { showError("Validation", "Date départ obligatoire."); return null; }
            if (dr == null) { showError("Validation", "Date retour obligatoire."); return null; }
            if (dr.isBefore(dd)) { showError("Validation", "Date retour doit être après date départ."); return null; }
            if (prix.getText() == null || prix.getText().isBlank()) { showError("Validation", "Prix obligatoire."); return null; }
            if (places.getText() == null || places.getText().isBlank()) { showError("Validation", "Places obligatoires."); return null; }
            BigDecimal p;
            try { p = new BigDecimal(prix.getText().trim()); if (p.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException(); } catch (Exception e) { showError("Validation", "Prix invalide (nombre >= 0)."); return null; }
            int pl;
            try { pl = Integer.parseInt(places.getText().trim()); if (pl <= 0) throw new NumberFormatException(); } catch (Exception e) { showError("Validation", "Places invalide (entier > 0)."); return null; }
            if (statut.getText() == null || statut.getText().isBlank()) { showError("Validation", "Statut obligatoire."); return null; }
            User selGuide = comboGuide.getSelectionModel().getSelectedItem();
            if (selGuide == null) { showError("Validation", "Guide obligatoire."); return null; }
            v.setTypeVoyage(type.getText().trim());
            v.setDateDepart(dd);
            v.setDateRetour(dr);
            v.setPrix(p);
            v.setPlacesDisponibles(pl);
            v.setStatut(statut.getText().trim());
            v.setIdGuide(selGuide.getIdUser());
            return new VoyageDialogResult(
                    new ArrayList<>(listDestinations.getSelectionModel().getSelectedItems()),
                    new ArrayList<>(listActivites.getSelectionModel().getSelectedItems()));
        });
        return d.showAndWait().orElse(null);
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
