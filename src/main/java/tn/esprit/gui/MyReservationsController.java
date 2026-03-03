package tn.esprit.gui;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
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

    @FXML
    private javafx.scene.control.ScrollPane mainScroll;

    @FXML
    private VBox voyageList;
    @FXML
    private VBox hotelList;
    @FXML
    private VBox transportList;
    @FXML
    private VBox activiteList;

    private final ReservationVoyageServices rvService = new ReservationVoyageServices();
    private final ReservationHotelServices rhService = new ReservationHotelServices();
    private final ReservationTransportServices rtService = new ReservationTransportServices();
    private final ReservationActiviteServices raService = new ReservationActiviteServices();
    private final VoyageServices voyageService = new VoyageServices();
    private final HotelServices hotelService = new HotelServices();
    private final ActiviteServices activiteService = new ActiviteServices();

    private static MyReservationsController instance;

    @FXML
    public void initialize() {
        instance = this;
        if (mainScroll != null) {
            tn.esprit.utils.AnimationHelper.applyFadeIn(mainScroll);
        }
        User user = SessionHolder.getCurrentUser();
        if (user == null) {
            System.err.println("Aucun utilisateur connecte !");
            return;
        }
        loadAllReservations(user.getIdUser());
    }

    public static void refreshIfActive() {
        if (instance != null) {
            User user = SessionHolder.getCurrentUser();
            if (user != null) {
                instance.loadAllReservations(user.getIdUser());
            }
        }
    }

    private void loadAllReservations(int idUser) {
        voyageList.getChildren().clear();
        hotelList.getChildren().clear();
        transportList.getChildren().clear();
        activiteList.getChildren().clear();

        try {
            Map<Integer, String> voyageNames = new HashMap<>();
            for (Voyage v : voyageService.afficher()) {
                String name = (v.getTypeVoyage() != null ? v.getTypeVoyage() : "Voyage") + " — "
                        + (v.getDateDepart() != null ? v.getDateDepart().format(DATE_FMT) : "?");
                voyageNames.put(v.getIdVoyage(), name);
            }
            Map<Integer, String> hotelNames = new HashMap<>();
            for (Hotel h : hotelService.afficher())
                hotelNames.put(h.getIdHotel(), h.getNom() != null ? h.getNom() : "Hôtel");
            Map<Integer, String> activiteNames = new HashMap<>();
            for (Activite a : activiteService.afficher())
                activiteNames.put(a.getIdActivite(), a.getNom() != null ? a.getNom() : "Activité");

            List<ReservationVoyage> rvList = rvService.afficher().stream()
                    .filter(r -> r.getIdUser() == idUser).collect(Collectors.toList());
            for (ReservationVoyage r : rvList) {
                String voyageName = voyageNames.getOrDefault(r.getIdVoyage(), "Voyage #" + r.getIdVoyage());
                voyageList.getChildren().add(buildVoyageCard(r, voyageName));
            }
            if (rvList.isEmpty())
                voyageList.getChildren().add(emptyHint("Aucune réservation voyage."));

            List<ReservationHotel> rhList = rhService.afficher().stream().filter(r -> r.getIdUser() == idUser)
                    .collect(Collectors.toList());
            for (ReservationHotel r : rhList) {
                String hotelName = hotelNames.getOrDefault(r.getIdHotel(), "Hôtel #" + r.getIdHotel());
                hotelList.getChildren().add(buildCard("hotel", hotelName,
                        "Check-in : " + (r.getDateCheckin() != null ? r.getDateCheckin().format(DATE_FMT) : "—")
                                + "  ·  Check-out : "
                                + (r.getDateCheckout() != null ? r.getDateCheckout().format(DATE_FMT) : "—"),
                        null,
                        formatMoney(r.getPrixTotal())));
            }
            if (rhList.isEmpty())
                hotelList.getChildren().add(emptyHint("Aucune réservation hôtel."));

            List<ReservationTransport> rtList = rtService.afficher().stream().filter(r -> r.getIdUser() == idUser)
                    .collect(Collectors.toList());
            for (ReservationTransport r : rtList) {
                transportList.getChildren().add(buildCard("transport", "Réservation transport",
                        "Date : " + (r.getDateReservation() != null ? r.getDateReservation().format(DATE_FMT) : "—"),
                        "Statut : " + (r.getStatut() != null ? r.getStatut().name() : "—"),
                        formatMoney(r.getPrixTotal())));
            }
            if (rtList.isEmpty())
                transportList.getChildren().add(emptyHint("Aucune réservation transport."));

            List<ReservationActivite> raList = raService.afficher().stream().filter(r -> r.getIdUser() == idUser)
                    .collect(Collectors.toList());
            for (ReservationActivite r : raList) {
                String actName = activiteNames.getOrDefault(r.getIdActivite(), "Activité #" + r.getIdActivite());
                activiteList.getChildren().add(buildCard("activite", actName,
                        "Date : " + (r.getDateReservation() != null ? r.getDateReservation().format(DATE_FMT) : "—"),
                        "Statut : " + (r.getStatut() != null ? r.getStatut().name() : "—"),
                        formatMoney(r.getMontantTotal())));
            }
            if (raList.isEmpty())
                activiteList.getChildren().add(emptyHint("Aucune réservation activité."));

        } catch (SQLException e) {
            voyageList.getChildren().add(emptyHint("Erreur chargement."));
        }
    }

    /**
     * Builds a voyage reservation card with Annuler and (if EN_ATTENTE) Confirmer
     * buttons.
     */
    private HBox buildVoyageCard(ReservationVoyage r, String voyageName) {
        HBox card = new HBox(20);
        card.getStyleClass().addAll("my-reservation-card", "my-reservation-card-voyage");
        tn.esprit.utils.AnimationHelper.applyHoverScaleEffect(card);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setAlignment(Pos.CENTER_LEFT);

        // Left: info
        VBox left = new VBox(6);
        Label titleLabel = new Label(voyageName);
        titleLabel.getStyleClass().add("my-reservation-card-title");
        titleLabel.setWrapText(true);
        Label l1 = new Label(
                "Date : " + (r.getDateReservation() != null ? r.getDateReservation().format(DATE_FMT) : "—"));
        l1.getStyleClass().add("my-reservation-card-detail");
        Label statutLabel = new Label("Statut : " + (r.getStatut() != null ? r.getStatut().name() : "—"));
        statutLabel.getStyleClass().add("my-reservation-card-detail");
        left.getChildren().addAll(titleLabel, l1, statutLabel);
        HBox.setHgrow(left, Priority.ALWAYS);

        // Price
        Label priceLabel = new Label(formatMoney(r.getMontantTotal()));
        priceLabel.getStyleClass().add("my-reservation-card-price");

        // Confirm button (only shown when status is EN_ATTENTE)
        Button confirmBtn = new Button("✅ Confirmer");
        confirmBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;"
                + " -fx-font-size: 12px; -fx-padding: 5 12; -fx-background-radius: 6;"
                + " -fx-cursor: hand;");
        boolean enAttente = r.getStatut() == ReservationTransport.StatutReservation.EN_ATTENTE;
        confirmBtn.setVisible(enAttente);
        confirmBtn.setManaged(enAttente);
        confirmBtn.setOnAction(e -> confirmReservationVoyage(r, statutLabel, confirmBtn));

        // Cancel button
        Button cancelBtn = new Button("❌ Annuler");
        cancelBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;"
                + " -fx-font-size: 12px; -fx-padding: 5 12; -fx-background-radius: 6;"
                + " -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> cancelReservationVoyage(r.getIdReservationVoyage(), voyageName, card));

        card.getChildren().addAll(left, priceLabel, confirmBtn, cancelBtn);
        return card;
    }

    /**
     * Confirms a voyage reservation: sets status to CONFIRMEE in DB and updates the
     * card UI.
     */
    private void confirmReservationVoyage(ReservationVoyage r, Label statutLabel, Button confirmBtn) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la réservation");
        confirm.setHeaderText("Confirmer cette réservation ?");
        confirm.setContentText("Le statut passera à CONFIRMÉE.");
        confirm.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    rvService.updateStatut(r.getIdReservationVoyage(),
                            ReservationTransport.StatutReservation.CONFIRMEE);
                    r.setStatut(ReservationTransport.StatutReservation.CONFIRMEE);
                    statutLabel.setText("Statut : CONFIRMEE");
                    confirmBtn.setVisible(false);
                    confirmBtn.setManaged(false);
                    Alert ok = new Alert(Alert.AlertType.INFORMATION);
                    ok.setTitle("Confirmée");
                    ok.setContentText("Réservation confirmée avec succès !");
                    ok.showAndWait();
                } catch (SQLException ex) {
                    Alert err = new Alert(Alert.AlertType.ERROR);
                    err.setTitle("Erreur");
                    err.setContentText("Impossible de confirmer : " + ex.getMessage());
                    err.showAndWait();
                }
            }
        });
    }

    /**
     * Asks for confirmation then deletes the voyage reservation and removes the
     * card from the UI.
     */
    private void cancelReservationVoyage(int idReservation, String voyageName, HBox card) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Annuler la réservation");
        confirm.setHeaderText("Annuler : " + voyageName + " ?");
        confirm.setContentText("Cette action est irréversible. Voulez-vous continuer ?");
        confirm.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    rvService.supprimer(idReservation);
                    voyageList.getChildren().remove(card);
                    if (voyageList.getChildren().isEmpty()) {
                        voyageList.getChildren().add(emptyHint("Aucune réservation voyage."));
                    }
                    Alert ok = new Alert(Alert.AlertType.INFORMATION);
                    ok.setTitle("Annulée");
                    ok.setContentText("Réservation annulée avec succès.");
                    ok.showAndWait();
                } catch (SQLException ex) {
                    Alert err = new Alert(Alert.AlertType.ERROR);
                    err.setTitle("Erreur");
                    err.setContentText("Impossible d'annuler la réservation : " + ex.getMessage());
                    err.showAndWait();
                }
            }
        });
    }

    private String formatMoney(BigDecimal m) {
        if (m == null)
            return "—";
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
        tn.esprit.utils.AnimationHelper.applyHoverScaleEffect(card);
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
