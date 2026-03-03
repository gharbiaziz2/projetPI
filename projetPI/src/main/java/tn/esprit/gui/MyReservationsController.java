package tn.esprit.gui;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import tn.esprit.entities.*;
import tn.esprit.services.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MyReservationsController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private VBox voyageList;
    @FXML private VBox hotelList;
    @FXML private VBox transportList;
    @FXML private VBox activiteList;

    private final ReservationVoyageServices rvService = new ReservationVoyageServices();
    private final ReservationHotelServices rhService = new ReservationHotelServices();
    private final ReservationTransportServices rtService = new ReservationTransportServices();
    private final ReservationActiviteServices raService = new ReservationActiviteServices();
    private final VoyageServices voyageService = new VoyageServices();
    private final HotelServices hotelService = new HotelServices();
    private final ActiviteServices activiteService = new ActiviteServices();

    @FXML
    public void initialize() {
        User user = SessionHolder.getCurrentUser();
        if (user == null) return;
        int idUser = user.getIdUser();
        voyageList.getChildren().clear();
        hotelList.getChildren().clear();
        transportList.getChildren().clear();
        activiteList.getChildren().clear();

        try {
            Map<Integer, String> voyageNames = new HashMap<>();
            for (Voyage v : voyageService.afficher()) {
                String name = (v.getTypeVoyage() != null ? v.getTypeVoyage() : "Voyage") + " — " + (v.getDateDepart() != null ? v.getDateDepart().format(DATE_FMT) : "?");
                voyageNames.put(v.getIdVoyage(), name);
            }
            Map<Integer, String> hotelNames = new HashMap<>();
            for (Hotel h : hotelService.afficher()) hotelNames.put(h.getIdHotel(), h.getNom() != null ? h.getNom() : "Hôtel");
            Map<Integer, String> activiteNames = new HashMap<>();
            for (Activite a : activiteService.afficher()) activiteNames.put(a.getIdActivite(), a.getNom() != null ? a.getNom() : "Activité");

            List<ReservationVoyage> rvList = rvService.afficher().stream().filter(r -> r.getIdUser() == idUser).collect(Collectors.toList());
            for (ReservationVoyage r : rvList) {
                String voyageName = voyageNames.getOrDefault(r.getIdVoyage(), "Voyage #" + r.getIdVoyage());
                voyageList.getChildren().add(buildCard("voyage", voyageName,
                        "Date : " + (r.getDateReservation() != null ? r.getDateReservation().format(DATE_FMT) : "—"),
                        "Statut : " + (r.getStatut() != null ? r.getStatut().name() : "—"),
                        formatMoney(r.getMontantTotal())));
            }
            if (rvList.isEmpty()) voyageList.getChildren().add(emptyHint("Aucune réservation voyage."));

            List<ReservationHotel> rhList = rhService.afficher().stream().filter(r -> r.getIdUser() == idUser).collect(Collectors.toList());
            for (ReservationHotel r : rhList) {
                String hotelName = hotelNames.getOrDefault(r.getIdHotel(), "Hôtel #" + r.getIdHotel());
                hotelList.getChildren().add(buildCard("hotel", hotelName,
                        "Check-in : " + (r.getDateCheckin() != null ? r.getDateCheckin().format(DATE_FMT) : "—") + "  ·  Check-out : " + (r.getDateCheckout() != null ? r.getDateCheckout().format(DATE_FMT) : "—"),
                        null,
                        formatMoney(r.getPrixTotal())));
            }
            if (rhList.isEmpty()) hotelList.getChildren().add(emptyHint("Aucune réservation hôtel."));

            List<ReservationTransport> rtList = rtService.afficher().stream().filter(r -> r.getIdUser() == idUser).collect(Collectors.toList());
            for (ReservationTransport r : rtList) {
                transportList.getChildren().add(buildCard("transport", "Réservation transport",
                        "Date : " + (r.getDateReservation() != null ? r.getDateReservation().format(DATE_FMT) : "—"),
                        "Statut : " + (r.getStatut() != null ? r.getStatut().name() : "—"),
                        formatMoney(r.getPrixTotal())));
            }
            if (rtList.isEmpty()) transportList.getChildren().add(emptyHint("Aucune réservation transport."));

            List<ReservationActivite> raList = raService.afficher().stream().filter(r -> r.getIdUser() == idUser).collect(Collectors.toList());
            for (ReservationActivite r : raList) {
                String actName = activiteNames.getOrDefault(r.getIdActivite(), "Activité #" + r.getIdActivite());
                activiteList.getChildren().add(buildCard("activite", actName,
                        "Date : " + (r.getDateReservation() != null ? r.getDateReservation().format(DATE_FMT) : "—"),
                        "Statut : " + (r.getStatut() != null ? r.getStatut().name() : "—"),
                        formatMoney(r.getMontantTotal())));
            }
            if (raList.isEmpty()) activiteList.getChildren().add(emptyHint("Aucune réservation activité."));

        } catch (SQLException e) {
            voyageList.getChildren().add(emptyHint("Erreur chargement."));
        }
    }

    private String formatMoney(BigDecimal m) {
        if (m == null) return "—";
        return m + " TND";
    }

    private Label emptyHint(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("my-reservations-empty-hint");
        return l;
    }

    private HBox buildCard(String type, String title, String line1, String line2, String price) {
        HBox card = new HBox(20);
        card.getStyleClass().addAll("my-reservation-card", "my-reservation-card-" + type);
        card.setPadding(new Insets(16, 20, 16, 20));

        VBox left = new VBox(6);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("my-reservation-card-title");
        titleLabel.setWrapText(true);
        left.getChildren().add(titleLabel);
        if (line1 != null) {
            Label l1 = new Label(line1);
            l1.getStyleClass().add("my-reservation-card-detail");
            left.getChildren().add(l1);
        }
        if (line2 != null) {
            Label l2 = new Label(line2);
            l2.getStyleClass().add("my-reservation-card-detail");
            left.getChildren().add(l2);
        }
        HBox.setHgrow(left, Priority.ALWAYS);

        Label priceLabel = new Label(price);
        priceLabel.getStyleClass().add("my-reservation-card-price");
        card.getChildren().addAll(left, priceLabel);
        return card;
    }
}
