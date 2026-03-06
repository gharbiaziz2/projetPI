package tn.esprit.gui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import tn.esprit.entities.Activite;
import tn.esprit.entities.Hotel;
import tn.esprit.entities.ReservationHotel;
import tn.esprit.entities.User;
import tn.esprit.services.ActiviteServices;
import tn.esprit.services.FavoriteHotelService;
import tn.esprit.services.HotelServices;
import tn.esprit.services.MapboxService;
import tn.esprit.services.ReservationHotelServices;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import tn.esprit.gui.DialogStyleHelper;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.time.temporal.ChronoUnit;
import java.util.ResourceBundle;

public class FrontHotelsController implements Initializable {

    @FXML private FlowPane cardsContainer;

    private FrontController frontController;
    private final HotelServices hotelService = new HotelServices();
    private final ReservationHotelServices reservationHotelService = new ReservationHotelServices();
    private final FavoriteHotelService favoriteHotelService = new FavoriteHotelService();
    private final ActiviteServices activiteService = new ActiviteServices();
    private final MapboxService mapboxService = new MapboxService();

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

        VBox content = new VBox(10);
        if (h.getImage() != null && !h.getImage().isBlank()) {
            try {
                ImageView imgView = new ImageView();
                imgView.setFitWidth(260);
                imgView.setFitHeight(140);
                imgView.setPreserveRatio(true);
                String imgSrc = h.getImage();
                if (imgSrc.startsWith("http://") || imgSrc.startsWith("https://")) {
                    imgView.setImage(new Image(imgSrc, true));
                } else {
                    File f = new File(imgSrc);
                    if (f.exists()) imgView.setImage(new Image(f.toURI().toString(), true));
                }
                if (imgView.getImage() != null) content.getChildren().add(imgView);
            } catch (Exception ignored) { /* invalid path/URL, skip image */ }
        }

        HBox titleRow = new HBox();
        titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label nameL = new Label(h.getNom());
        nameL.getStyleClass().add("card-title");
        nameL.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(nameL, javafx.scene.layout.Priority.ALWAYS);
        Button starBtn = createStarButton(h);
        titleRow.getChildren().addAll(nameL, starBtn);
        content.getChildren().add(titleRow);

        Label locL = new Label((h.getVille() != null ? h.getVille() : "") + ", " + (h.getPays() != null ? h.getPays() : ""));
        locL.getStyleClass().add("card-dates");
        content.getChildren().add(locL);

        if (h.getLatitude() != null && h.getLongitude() != null) {
            Label coordsL = new Label("📍 " + String.format("%.4f, %.4f", h.getLatitude(), h.getLongitude()));
            coordsL.getStyleClass().add("card-statut");
            coordsL.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
            content.getChildren().add(coordsL);
        }

        Label prixL = new Label((h.getPrixNuit() != null ? h.getPrixNuit().toString() : "0") + " DT / nuit");
        prixL.getStyleClass().add("card-price");
        content.getChildren().add(prixL);

        String desc = h.getDescription();
        if (desc != null && desc.length() > 100) desc = desc.substring(0, 100) + "...";
        Label descL = new Label(desc != null ? desc : "");
        descL.setWrapText(true);
        descL.getStyleClass().add("card-statut");
        content.getChildren().add(descL);

        HBox btnRow = new HBox(8);
        btnRow.setAlignment(Pos.CENTER_LEFT);
        Button bookBtn = new Button("Reserver");
        bookBtn.getStyleClass().add("btn-reserve");
        bookBtn.setOnAction(e -> onBook(h));
        Button mapBtn = new Button("Voir sur la carte");
        mapBtn.getStyleClass().add("btn-reserve");
        mapBtn.setOnAction(e -> onShowMap(h));
        mapBtn.setDisable(h.getLatitude() == null || h.getLongitude() == null);
        btnRow.getChildren().addAll(bookBtn, mapBtn);
        content.getChildren().add(btnRow);

        card.getChildren().add(content);
        return card;
    }

    private void onShowMap(Hotel hotel) {
        if (hotel.getLatitude() == null || hotel.getLongitude() == null) {
            showError("Emplacement manquant", "Cet hôtel n'a pas de coordonnées GPS.");
            return;
        }
        double lat = hotel.getLatitude();
        double lon = hotel.getLongitude();
        List<MapboxService.MapMarker> activities = new java.util.ArrayList<>();
        List<MapboxService.MapMarker> transports = new java.util.ArrayList<>();
        List<MapboxService.MapMarker> otherHotels = new java.util.ArrayList<>();
        try {
            for (Activite a : activiteService.getNearby(lat, lon, 25)) {
                activities.add(new MapboxService.MapMarker(a.getNom(), a.getLatitude(), a.getLongitude()));
            }
        } catch (SQLException ignored) { }
        try {
            for (Hotel h : hotelService.getNearby(lat, lon, hotel.getIdHotel(), 25)) {
                otherHotels.add(new MapboxService.MapMarker(h.getNom(), h.getLatitude(), h.getLongitude()));
            }
        } catch (SQLException ignored) { }
        for (MapboxService.TransportPlace p : mapboxService.searchNearby(lat, lon, "bus station", 5)) {
            transports.add(new MapboxService.MapMarker(p.name, p.lat, p.lon));
        }
        for (MapboxService.TransportPlace p : mapboxService.searchNearby(lat, lon, "airport", 3)) {
            transports.add(new MapboxService.MapMarker(p.name, p.lat, p.lon));
        }
        String html = mapboxService.buildInteractiveMapHtml(lat, lon, hotel.getNom() != null ? hotel.getNom() : "Hôtel", activities, transports, otherHotels);
        if (html == null) {
            showError("Configuration", "Clé Mapbox non configurée.");
            return;
        }
        try {
            File tmp = File.createTempFile("hotel_map_", ".html");
            Files.writeString(tmp.toPath(), html);
            java.awt.Desktop.getDesktop().browse(tmp.toURI());
            showSuccess("La carte s'ouvre dans votre navigateur. Vous pouvez zoomer et déplacer la carte.");
        } catch (IOException e) {
            showError("Erreur", "Impossible d'ouvrir la carte.");
        }
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

    private Button createStarButton(Hotel h) {
        Button starBtn = new Button();
        starBtn.getStyleClass().add("favorite-star-btn");
        starBtn.setCursor(javafx.scene.Cursor.HAND);
        starBtn.setTooltip(new Tooltip("Cliquez pour ajouter/retirer des favoris"));
        updateStarAppearance(starBtn, h);
        starBtn.setOnAction(e -> {
            User user = SessionHolder.getCurrentUser();
            if (user == null) {
                showError("Connexion requise", "Connectez-vous pour ajouter des favoris.");
                return;
            }
            try {
                favoriteHotelService.toggleFavorite(user.getIdUser(), h.getIdHotel());
                updateStarAppearance(starBtn, h);
            } catch (SQLException ex) {
                showError("Erreur", "Impossible de modifier le favori.");
            }
        });
        return starBtn;
    }

    private void updateStarAppearance(Button starBtn, Hotel h) {
        User user = SessionHolder.getCurrentUser();
        boolean favorite = false;
        if (user != null) {
            try {
                favorite = favoriteHotelService.isFavorite(user.getIdUser(), h.getIdHotel());
            } catch (SQLException ignored) { }
        }
        starBtn.setText(favorite ? "★" : "☆");
        starBtn.setStyle(favorite ? "-fx-text-fill: #f59e0b; -fx-font-size: 20px; -fx-background-color: transparent; -fx-cursor: hand;" : "-fx-text-fill: #94a3b8; -fx-font-size: 20px; -fx-background-color: transparent; -fx-cursor: hand;");
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setContentText(msg);
        a.showAndWait();
    }
}
