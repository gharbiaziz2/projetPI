package tn.esprit.gui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.entities.ReservationTransport;
import tn.esprit.entities.ReservationTransportTransport;
import tn.esprit.entities.TransportLocal;
import tn.esprit.entities.User;
import tn.esprit.gui.DialogStyleHelper;
import tn.esprit.services.ReservationTransportServices;
import tn.esprit.services.ReservationTransportTransportServices;
import tn.esprit.services.TransportLocalServices;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class FrontTransportController implements Initializable {

    @FXML private FlowPane cardsContainer;

    private FrontController frontController;
    private final TransportLocalServices transportService = new TransportLocalServices();
    private final ReservationTransportServices reservationTransportService = new ReservationTransportServices();
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
            for (TransportLocal t : transportService.getAvailableTransports()) {
                VBox card = buildTransportCard(t);
                cardsContainer.getChildren().add(card);
            }
        } catch (SQLException e) {
            showError("Erreur", "Impossible de charger les transports.");
        }
    }

    private VBox buildTransportCard(TransportLocal t) {
        VBox card = new VBox(10);
        card.getStyleClass().add("front-card");
        card.setPadding(new Insets(20));
        card.setPrefWidth(300);
        card.setMaxWidth(300);
        Label compL = new Label(t.getCompagnie() != null ? t.getCompagnie() : "Transport");
        compL.getStyleClass().add("card-title");
        Label typeL = new Label("Type: " + (t.getTypeTransport() != null ? t.getTypeTransport().name() : ""));
        Label paysL = new Label((t.getPaysDepart() != null ? t.getPaysDepart() : "?") + " → " + (t.getPaysArrivee() != null ? t.getPaysArrivee() : "?"));
        paysL.getStyleClass().add("card-dates");
        String dates = (t.getDateDepart() != null ? t.getDateDepart().format(D_FMT) : "?") + " - " + (t.getDateRetour() != null ? t.getDateRetour().format(D_FMT) : "?");
        Label datesL = new Label(dates);
        Label prixL = new Label("Prix: " + (t.getPrix() != null ? t.getPrix().toString() : "0") + " DT");
        prixL.getStyleClass().add("card-price");
        int places = t.getNbrPlaces();
        Label placesL = new Label(places + " place" + (places > 1 ? "s" : "") + " disponible" + (places > 1 ? "s" : ""));
        placesL.getStyleClass().add("card-statut");
        Button bookBtn = new Button("Réserver");
        bookBtn.getStyleClass().add("btn-reserve");
        bookBtn.setOnAction(e -> onBook(t));
        card.getChildren().addAll(compL, typeL, paysL, datesL, prixL, placesL, bookBtn);
        return card;
    }

    private void onBook(TransportLocal transport) {
        User user = SessionHolder.getCurrentUser();
        if (user == null) {
            showError("Connexion requise", "Veuillez vous connecter pour réserver.");
            return;
        }
        if (transport.getNbrPlaces() <= 0) {
            showError("Indisponible", "Plus aucune place disponible pour ce transport.");
            return;
        }
        javafx.scene.control.DatePicker datePicker = new javafx.scene.control.DatePicker(LocalDate.now());
        DialogStyleHelper.styleDatePicker(datePicker);
        Dialog<LocalDate> d = new Dialog<>();
        d.setTitle("Réservation transport");
        d.setHeaderText(transport.getCompagnie() + " - " + transport.getTypeTransport() + " - " + transport.getPrix() + " DT");
        DialogStyleHelper.styleFrontDialogPane(d.getDialogPane());
        GridPane g = DialogStyleHelper.buildGrid();
        DialogStyleHelper.addRow(g, 0, "Date de réservation", datePicker);
        d.getDialogPane().setContent(g);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(btn -> btn == ButtonType.OK ? datePicker.getValue() : null);
        d.showAndWait().ifPresent(chosenDate -> {
            if (chosenDate == null) return;
            try {
                if (transport.getNbrPlaces() <= 0) {
                    showError("Indisponible", "Plus aucune place disponible.");
                    return;
                }
                ReservationTransport rt = new ReservationTransport();
                rt.setDateReservation(chosenDate);
                rt.setStatut(ReservationTransport.StatutReservation.CONFIRMEE);
                rt.setPrixTotal(transport.getPrix());
                rt.setIdUser(user.getIdUser());
                int idResa = reservationTransportService.ajouterAndReturnId(rt);
                reservationTransportTransportService.ajouter(new ReservationTransportTransport(idResa, transport.getIdTransport()));
                transportService.decrementerPlaces(transport.getIdTransport());
                loadCards();
                showSuccess("Réservation transport enregistrée.");
            } catch (SQLException e) {
                showError("Erreur", "Impossible d'enregistrer la réservation.");
            }
        });
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
