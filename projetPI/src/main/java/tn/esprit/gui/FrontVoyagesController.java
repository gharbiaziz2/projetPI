package tn.esprit.gui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.entities.*;
import tn.esprit.services.*;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class FrontVoyagesController implements Initializable {

    @FXML private FlowPane cardsContainer;

    private FrontController frontController;
    private final VoyageServices voyageService = new VoyageServices();
    private final UserServices userService = new UserServices();
    private final VoyageDestinationServices voyageDestinationService = new VoyageDestinationServices();
    private final ReservationVoyageServices reservationVoyageService = new ReservationVoyageServices();
    private final ReservationHotelServices reservationHotelService = new ReservationHotelServices();
    private final ReservationActiviteServices reservationActiviteService = new ReservationActiviteServices();
    private final ReservationTransportServices reservationTransportService = new ReservationTransportServices();
    private final HotelServices hotelService = new HotelServices();
    private final VoyageActiviteServices voyageActiviteService = new VoyageActiviteServices();
    private final TransportLocalServices transportLocalService = new TransportLocalServices();
    private final ReservationTransportTransportServices reservationTransportTransportService = new ReservationTransportTransportServices();

    private static final DateTimeFormatter D_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void setFrontController(FrontController c) { this.frontController = c; }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadCards();
    }

    public void loadCards() {
        cardsContainer.getChildren().clear();
        try {
            List<Voyage> voyages = voyageService.afficher();
            java.util.Map<Integer, String> guideNames = new java.util.HashMap<>();
            for (User u : userService.afficher()) guideNames.put(u.getIdUser(), u.getNom() + " " + u.getPrenom());
            for (Voyage v : voyages) {
                List<Destination> dests = voyageDestinationService.getDestinationsForVoyage(v.getIdVoyage());
                List<Activite> activites = voyageActiviteService.getActivitesForVoyage(v.getIdVoyage());
                String guideName = guideNames.getOrDefault(v.getIdGuide(), "");
                VBox card = buildVoyageCard(v, guideName, dests, activites);
                cardsContainer.getChildren().add(card);
            }
        } catch (SQLException e) {
            showError("Erreur", "Impossible de charger les voyages.");
        }
    }

    private VBox buildVoyageCard(Voyage v, String guideName, List<Destination> dests, List<Activite> activites) {
        VBox card = new VBox(12);
        card.getStyleClass().add("front-card");
        card.setPadding(new Insets(20));
        card.setPrefWidth(320);
        card.setMaxWidth(320);

        Label typeL = new Label(v.getTypeVoyage() != null ? v.getTypeVoyage() : "Voyage");
        typeL.getStyleClass().add("card-title");
        String dates = (v.getDateDepart() != null ? v.getDateDepart().format(D_FMT) : "?") + " → " + (v.getDateRetour() != null ? v.getDateRetour().format(D_FMT) : "?");
        Label datesL = new Label(dates);
        datesL.getStyleClass().add("card-dates");
        Label prixL = new Label("Prix: " + (v.getPrix() != null ? v.getPrix().toString() : "0") + " DT");
        prixL.getStyleClass().add("card-price");
        Label placesL = new Label("Places: " + v.getPlacesDisponibles());
        Label guideL = new Label("Guide: " + guideName);
        StringBuilder destStr = new StringBuilder();
        if (dests.isEmpty()) destStr.append("—");
        else for (int i = 0; i < dests.size(); i++) {
            Destination d = dests.get(i);
            if (i > 0) destStr.append(", ");
            destStr.append(d.getPaysDepart() != null ? d.getPaysDepart() : "?").append(" → ").append(d.getPaysArrivee() != null ? d.getPaysArrivee() : "?");
        }
        Label destL = new Label("Destination: " + destStr.toString());
        destL.setWrapText(true);
        destL.getStyleClass().add("card-dates");
        StringBuilder actStr = new StringBuilder("Activités: ");
        if (activites == null || activites.isEmpty()) actStr.append("—");
        else for (int i = 0; i < activites.size(); i++) {
            if (i > 0) actStr.append(", ");
            actStr.append(activites.get(i).getNom() != null ? activites.get(i).getNom() : "?");
        }
        Label activitesL = new Label(actStr.toString());
        activitesL.setWrapText(true);
        activitesL.getStyleClass().add("card-statut");
        if (v.getStatut() != null && !v.getStatut().isEmpty()) {
            Label statutL = new Label(v.getStatut());
            statutL.getStyleClass().add("card-statut");
            card.getChildren().addAll(typeL, datesL, prixL, placesL, guideL, destL, activitesL, statutL);
        } else {
            card.getChildren().addAll(typeL, datesL, prixL, placesL, guideL, destL, activitesL);
        }
        Button reserveBtn = new Button("Réserver");
        reserveBtn.getStyleClass().add("btn-reserve");
        reserveBtn.setOnAction(e -> onReserve(v));
        card.getChildren().add(reserveBtn);
        return card;
    }

    private void onReserve(Voyage voyage) {
        User user = SessionHolder.getCurrentUser();
        if (user == null) {
            showError("Connexion requise", "Veuillez vous connecter pour réserver.");
            return;
        }
        try {
            if (reservationVoyageService.existsByUserAndVoyage(user.getIdUser(), voyage.getIdVoyage())) {
                showError("Déjà réservé", "Vous avez déjà réservé ce voyage. Vous ne pouvez pas le réserver à nouveau.");
                return;
            }
        } catch (SQLException e) {
            showError("Erreur", "Impossible de vérifier vos réservations.");
            return;
        }
        if (!confirmVoyageReservation(voyage, user)) return;
        try {
            ReservationVoyage rv = new ReservationVoyage();
            rv.setDateReservation(LocalDate.now());
            rv.setStatut(ReservationTransport.StatutReservation.CONFIRMEE);
            rv.setMontantTotal(voyage.getPrix());
            rv.setIdUser(user.getIdUser());
            rv.setIdVoyage(voyage.getIdVoyage());
            reservationVoyageService.ajouter(rv);
            showSuccess("Réservation voyage enregistrée.");
        } catch (SQLException ex) {
            if ("DUPLICATE_VOYAGE".equals(ex.getMessage())) {
                showError("Déjà réservé", "Vous avez déjà réservé ce voyage.");
                return;
            }
            showError("Erreur", "Impossible d'enregistrer la réservation voyage.");
            return;
        }
        // Step 2: Hotel popup
        showHotelReservationPopup(user);
        // Step 3: Activity popup
        showActivityReservationPopup(voyage, user);
        // Step 4: Transport popup (optional)
        showTransportReservationPopup(voyage, user);
    }

    private boolean confirmVoyageReservation(Voyage v, User user) {
        StringBuilder content = new StringBuilder();
        content.append("Voyage: ").append(v.getTypeVoyage()).append("\n");
        content.append("Dates: ").append(v.getDateDepart() != null ? v.getDateDepart().format(D_FMT) : "").append(" - ").append(v.getDateRetour() != null ? v.getDateRetour().format(D_FMT) : "").append("\n");
        content.append("Prix: ").append(v.getPrix()).append(" DT\n");
        try {
            List<Activite> activites = voyageActiviteService.getActivitesForVoyage(v.getIdVoyage());
            if (activites != null && !activites.isEmpty()) {
                content.append("Activités incluses: ");
                for (int i = 0; i < activites.size(); i++) {
                    if (i > 0) content.append(", ");
                    content.append(activites.get(i).getNom()).append(" (").append(activites.get(i).getPrix() != null ? activites.get(i).getPrix() : "0").append(" DT)");
                }
                content.append("\n");
            }
        } catch (SQLException ignored) {}
        content.append("Client: ").append(user.getNom()).append(" ").append(user.getPrenom());
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirmer réservation");
        a.setHeaderText("Réserver ce voyage ?");
        a.setContentText(content.toString());
        a.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        return a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void showHotelReservationPopup(User user) {
        try {
            List<Hotel> hotels = hotelService.afficher();
            if (hotels.isEmpty()) return;
            Dialog<ReservationHotel> d = new Dialog<>();
            d.setTitle("Réservation hôtel (optionnel)");
            d.setHeaderText("Souhaitez-vous réserver un hôtel ?");
            DialogStyleHelper.styleFrontDialogPane(d.getDialogPane());
            javafx.scene.control.DatePicker checkIn = new javafx.scene.control.DatePicker(LocalDate.now());
            javafx.scene.control.DatePicker checkOut = new javafx.scene.control.DatePicker(LocalDate.now().plusDays(1));
            DialogStyleHelper.styleDatePicker(checkIn);
            DialogStyleHelper.styleDatePicker(checkOut);
            ComboBox<Hotel> hotelCombo = new ComboBox<>();
            hotelCombo.getItems().addAll(hotels);
            hotelCombo.setConverter(new javafx.util.StringConverter<Hotel>() {
                @Override public String toString(Hotel h) { return h == null ? "" : h.getNom() + " - " + h.getVille(); }
                @Override public Hotel fromString(String s) { return null; }
            });
            DialogStyleHelper.styleCombo(hotelCombo);
            if (!hotels.isEmpty()) hotelCombo.getSelectionModel().selectFirst();
            GridPane g = DialogStyleHelper.buildGrid();
            DialogStyleHelper.addRow(g, 0, "Hôtel", hotelCombo);
            DialogStyleHelper.addRow(g, 1, "Check-in", checkIn);
            DialogStyleHelper.addRow(g, 2, "Check-out", checkOut);
            d.getDialogPane().setContent(g);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            d.setResultConverter(btn -> {
                if (btn != ButtonType.OK) return null;
                Hotel h = hotelCombo.getSelectionModel().getSelectedItem();
                if (h == null) return null;
                LocalDate ci = checkIn.getValue();
                LocalDate co = checkOut.getValue();
                if (ci == null || co == null || !co.isAfter(ci)) return null;
                ReservationHotel rh = new ReservationHotel();
                rh.setDateCheckin(ci);
                rh.setDateCheckout(co);
                rh.setPrixTotal(h.getPrixNuit() != null ? h.getPrixNuit().multiply(BigDecimal.valueOf(java.time.temporal.ChronoUnit.DAYS.between(ci, co))) : BigDecimal.ZERO);
                rh.setIdUser(user.getIdUser());
                rh.setIdHotel(h.getIdHotel());
                return rh;
            });
            d.showAndWait().ifPresent(rh -> {
                try {
                    reservationHotelService.ajouter(rh);
                    showSuccess("Réservation hôtel enregistrée.");
                } catch (SQLException e) { showError("Erreur", "Réservation hôtel impossible."); }
            });
        } catch (SQLException e) {}
    }

    private void showActivityReservationPopup(Voyage voyage, User user) {
        try {
            List<Activite> activites = voyageActiviteService.getActivitesForVoyage(voyage.getIdVoyage());
            if (activites.isEmpty()) return;
            Dialog<List<Activite>> d = new Dialog<>();
            d.setTitle("Réservation activités (optionnel)");
            d.setHeaderText("Sélectionnez une ou plusieurs activités");
            DialogStyleHelper.styleFrontDialogPane(d.getDialogPane());
            ListView<Activite> listView = new ListView<>();
            listView.getItems().addAll(activites);
            listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            listView.setCellFactory(lv -> new ListCell<Activite>() {
                @Override protected void updateItem(Activite a, boolean empty) {
                    super.updateItem(a, empty);
                    setText(a == null || empty ? "" : a.getNom() + " - " + (a.getPrix() != null ? a.getPrix() + " DT" : ""));
                }
            });
            d.getDialogPane().setContent(listView);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            d.setResultConverter(btn -> btn == ButtonType.OK ? new ArrayList<>(listView.getSelectionModel().getSelectedItems()) : null);
            d.showAndWait().ifPresent(selected -> {
                for (Activite a : selected) {
                    try {
                        ReservationActivite ra = new ReservationActivite();
                        ra.setDateReservation(LocalDate.now());
                        ra.setStatut(ReservationTransport.StatutReservation.CONFIRMEE);
                        ra.setMontantTotal(a.getPrix());
                        ra.setIdUser(user.getIdUser());
                        ra.setIdVoyage(voyage.getIdVoyage());
                        ra.setIdActivite(a.getIdActivite());
                        reservationActiviteService.ajouter(ra);
                    } catch (SQLException ex) {}
                }
                if (!selected.isEmpty()) showSuccess("Réservation(s) activité(s) enregistrée(s).");
            });
        } catch (SQLException e) {}
    }

    private void showTransportReservationPopup(Voyage voyage, User user) {
        try {
            // Show ALL transports so user always has options (voyage-linked or unassigned)
            List<TransportLocal> allTransports = transportLocalService.getByVoyageOrUnassigned(voyage.getIdVoyage());
            if (allTransports.isEmpty()) {
                allTransports = transportLocalService.afficher();
            }
            if (allTransports.isEmpty()) return;
            final List<TransportLocal> transportList = allTransports;
            javafx.scene.control.DatePicker datePicker = new javafx.scene.control.DatePicker(LocalDate.now());
            DialogStyleHelper.styleDatePicker(datePicker);
            ComboBox<TransportLocal> combo = new ComboBox<>();
            combo.setConverter(new javafx.util.StringConverter<TransportLocal>() {
                @Override public String toString(TransportLocal t) {
                    return t == null ? "" : t.getCompagnie() + " - " + t.getTypeTransport() + " - " + t.getPrix() + " DT";
                }
                @Override public TransportLocal fromString(String s) { return null; }
            });
            DialogStyleHelper.styleCombo(combo);
            java.util.Set<Integer> bookedTransportIds = new java.util.HashSet<>();
            java.util.function.Consumer<LocalDate> fillCombo = chosenDate -> {
                if (chosenDate == null) return;
                try {
                    bookedTransportIds.clear();
                    bookedTransportIds.addAll(reservationTransportTransportService.getTransportIdsBookedForDate(chosenDate));
                } catch (SQLException ignored) {}
                combo.getItems().clear();
                for (TransportLocal t : transportList) {
                    if (!bookedTransportIds.contains(t.getIdTransport()))
                        combo.getItems().add(t);
                }
                if (!combo.getItems().isEmpty()) combo.getSelectionModel().selectFirst();
            };
            datePicker.valueProperty().addListener((o, oldVal, newVal) -> fillCombo.accept(newVal));
            fillCombo.accept(LocalDate.now());
            Dialog<Pair<TransportLocal, LocalDate>> d = new Dialog<>();
            d.setTitle("Réservation transport (optionnel)");
            d.setHeaderText("Choisissez la date et un transport disponible");
            DialogStyleHelper.styleFrontDialogPane(d.getDialogPane());
            GridPane g = DialogStyleHelper.buildGrid();
            DialogStyleHelper.addRow(g, 0, "Date", datePicker);
            DialogStyleHelper.addRow(g, 1, "Transport", combo);
            d.getDialogPane().setContent(g);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            d.setResultConverter(btn -> {
                if (btn != ButtonType.OK) return null;
                TransportLocal t = combo.getSelectionModel().getSelectedItem();
                if (t == null) return null;
                LocalDate chosenDate = datePicker.getValue();
                if (chosenDate == null) return null;
                return new Pair<>(t, chosenDate);
            });
            d.showAndWait().ifPresent(pair -> {
                TransportLocal t = pair.getKey();
                LocalDate chosenDate = pair.getValue();
                try {
                    ReservationTransport rt = new ReservationTransport();
                    rt.setDateReservation(chosenDate);
                    rt.setStatut(ReservationTransport.StatutReservation.CONFIRMEE);
                    rt.setPrixTotal(t.getPrix());
                    rt.setIdUser(user.getIdUser());
                    int idResa = reservationTransportService.ajouterAndReturnId(rt);
                    reservationTransportTransportService.ajouter(new tn.esprit.entities.ReservationTransportTransport(idResa, t.getIdTransport()));
                    showSuccess("Réservation transport enregistrée.");
                } catch (SQLException e) {
                    showError("Erreur", "Réservation transport impossible.");
                }
            });
        } catch (SQLException e) {}
    }

    private void showSuccess(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Succès");
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setContentText(msg);
        a.showAndWait();
    }
}
