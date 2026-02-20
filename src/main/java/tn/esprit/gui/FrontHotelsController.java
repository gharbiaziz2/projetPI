package tn.esprit.gui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.entities.Hotel;
import tn.esprit.entities.ReservationHotel;
import tn.esprit.entities.User;
import tn.esprit.services.HotelServices;
import tn.esprit.services.ReservationHotelServices;
import tn.esprit.gui.DialogStyleHelper;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ResourceBundle;

public class FrontHotelsController implements Initializable {

    @FXML private FlowPane cardsContainer;

    private FrontController frontController;
    private final HotelServices hotelService = new HotelServices();
    private final ReservationHotelServices reservationHotelService = new ReservationHotelServices();

    public void setFrontController(FrontController c) { this.frontController = c; }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadCards();
    }

    public void loadCards() {
        cardsContainer.getChildren().clear();
        try {
            for (Hotel h : hotelService.afficher()) {
                VBox card = buildHotelCard(h);
                cardsContainer.getChildren().add(card);
            }
        } catch (SQLException e) {
            showError("Erreur", "Impossible de charger les hotels.");
        }
    }

    private VBox buildHotelCard(Hotel h) {
        VBox card = new VBox(10);
        card.getStyleClass().add("front-card");
        card.setPadding(new Insets(20));
        card.setPrefWidth(300);
        card.setMaxWidth(300);
        Label nameL = new Label(h.getNom());
        nameL.getStyleClass().add("card-title");
        Label locL = new Label((h.getVille() != null ? h.getVille() : "") + ", " + (h.getPays() != null ? h.getPays() : ""));
        locL.getStyleClass().add("card-dates");
        Label prixL = new Label((h.getPrixNuit() != null ? h.getPrixNuit().toString() : "0") + " DT / nuit");
        prixL.getStyleClass().add("card-price");
        String desc = h.getDescription();
        if (desc != null && desc.length() > 100) desc = desc.substring(0, 100) + "...";
        Label descL = new Label(desc != null ? desc : "");
        descL.setWrapText(true);
        descL.getStyleClass().add("card-statut");
        Button bookBtn = new Button("Reserver");
        bookBtn.getStyleClass().add("btn-reserve");
        bookBtn.setOnAction(e -> onBook(h));
        card.getChildren().addAll(nameL, locL, prixL, descL, bookBtn);
        return card;
    }

    private void onBook(Hotel hotel) {
        User user = SessionHolder.getCurrentUser();
        if (user == null) {
            showError("Connexion requise", "Veuillez vous connecter pour reserver.");
            return;
        }
        Dialog<ReservationHotel> d = new Dialog<>();
        d.setTitle("Réservation hôtel");
        d.setHeaderText("Choisissez vos dates");
        DialogStyleHelper.styleFrontDialogPane(d.getDialogPane());
        DatePicker checkIn = new DatePicker(LocalDate.now());
        DatePicker checkOut = new DatePicker(LocalDate.now().plusDays(1));
        DialogStyleHelper.styleDatePicker(checkIn);
        DialogStyleHelper.styleDatePicker(checkOut);
        GridPane g = DialogStyleHelper.buildGrid();
        Label hotelLabel = new Label(hotel.getNom());
        hotelLabel.getStyleClass().add("crud-dialog-field");
        DialogStyleHelper.addRow(g, 0, "Hôtel", hotelLabel);
        DialogStyleHelper.addRow(g, 1, "Check-in", checkIn);
        DialogStyleHelper.addRow(g, 2, "Check-out", checkOut);
        d.getDialogPane().setContent(g);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            LocalDate ci = checkIn.getValue();
            LocalDate co = checkOut.getValue();
            if (ci == null || co == null) return null;
            if (!co.isAfter(ci)) return null;
            ReservationHotel rh = new ReservationHotel();
            rh.setDateCheckin(ci);
            rh.setDateCheckout(co);
            long nights = ChronoUnit.DAYS.between(ci, co);
            rh.setPrixTotal(hotel.getPrixNuit() != null ? hotel.getPrixNuit().multiply(BigDecimal.valueOf(nights)) : BigDecimal.ZERO);
            rh.setIdUser(user.getIdUser());
            rh.setIdHotel(hotel.getIdHotel());
            return rh;
        });
        d.showAndWait().ifPresent(rh -> {
            try {
                reservationHotelService.ajouter(rh);
                showSuccess("Reservation enregistree.");
            } catch (SQLException e) {
                showError("Erreur", "Impossible d'enregistrer la reservation.");
            }
        });
    }

    private void showSuccess(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Succes");
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
