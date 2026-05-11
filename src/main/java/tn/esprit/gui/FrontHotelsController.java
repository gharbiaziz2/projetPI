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
import tn.esprit.entities.HotelChambre;
import tn.esprit.entities.ReservationChambre;
import tn.esprit.entities.User;
import tn.esprit.services.ActiviteServices;
import tn.esprit.services.FavoriteHotelService;
import tn.esprit.services.HotelChambreServices;
import tn.esprit.services.HotelServices;
import tn.esprit.services.MapboxService;
import tn.esprit.services.ReservationHotelServices;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;


import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.time.temporal.ChronoUnit;
import java.util.ResourceBundle;

public class FrontHotelsController implements Initializable {

    @FXML private FlowPane cardsContainer;


    private final HotelServices hotelService = new HotelServices();
    private final ReservationHotelServices reservationHotelService = new ReservationHotelServices();
    private final HotelChambreServices hotelChambreService = new HotelChambreServices();
    private final FavoriteHotelService favoriteHotelService = new FavoriteHotelService();
    private final ActiviteServices activiteService = new ActiviteServices();
    private final MapboxService mapboxService = new MapboxService();

    private FrontController frontController;

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

        Label prixL = new Label(h.getPrixNuit() + " DT / nuit");
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
        
        // Get available chambers for this hotel
        List<HotelChambre> chambers = new java.util.ArrayList<>();
        try {
            chambers = hotelChambreService.getAvailableByHotel(hotel.getIdHotel());
        } catch (SQLException e) {
            showError("Erreur", "Impossible de charger les chambres disponibles.");
            return;
        }
        
        if (chambers.isEmpty()) {
            showError("Chambres indisponibles", "Aucune chambre disponible dans cet hôtel.");
            return;
        }
        
        Dialog<ReservationChambre> d = new Dialog<>();
        d.setTitle("Réservation hôtel");
        d.setHeaderText("Choisissez vos dates et votre chambre");
        DialogStyleHelper.styleFrontDialogPane(d.getDialogPane());
        DatePicker checkIn = new DatePicker(LocalDate.now());
        DatePicker checkOut = new DatePicker(LocalDate.now().plusDays(1));
        DialogStyleHelper.styleDatePicker(checkIn);
        DialogStyleHelper.styleDatePicker(checkOut);
        
        ComboBox<HotelChambre> chamberCombo = new ComboBox<>();
        chamberCombo.getItems().addAll(chambers);
        chamberCombo.setCellFactory(lv -> new ListCell<HotelChambre>() {
            @Override
            protected void updateItem(HotelChambre chamber, boolean empty) {
                super.updateItem(chamber, empty);
                if (empty || chamber == null) {
                    setText(null);
                } else {
                    setText(chamber.getNumeroChambre() + " - " + chamber.getTypeChambre() + " (" + chamber.getPrixChambre() + " DT)");
                }
            }
        });
        chamberCombo.setButtonCell(new ListCell<HotelChambre>() {
            @Override
            protected void updateItem(HotelChambre chamber, boolean empty) {
                super.updateItem(chamber, empty);
                if (empty || chamber == null) {
                    setText(null);
                } else {
                    setText(chamber.getNumeroChambre() + " - " + chamber.getTypeChambre() + " (" + chamber.getPrixChambre() + " DT)");
                }
            }
        });
        if (!chambers.isEmpty()) {
            chamberCombo.getSelectionModel().select(0);
        }
        
        Label totalLabel = new Label("Total: 0 DT");
        
        // Update total when dates or chamber change
        checkIn.valueProperty().addListener((obs, oldVal, newVal) -> updateTotal(checkIn, checkOut, chamberCombo, totalLabel));
        checkOut.valueProperty().addListener((obs, oldVal, newVal) -> updateTotal(checkIn, checkOut, chamberCombo, totalLabel));
        chamberCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateTotal(checkIn, checkOut, chamberCombo, totalLabel));
        updateTotal(checkIn, checkOut, chamberCombo, totalLabel);
        
        GridPane g = DialogStyleHelper.buildGrid();
        Label hotelLabel = new Label(hotel.getNom());
        hotelLabel.getStyleClass().add("crud-dialog-field");
        DialogStyleHelper.addRow(g, 0, "Hôtel", hotelLabel);
        DialogStyleHelper.addRow(g, 1, "Check-in", checkIn);
        DialogStyleHelper.addRow(g, 2, "Check-out", checkOut);
        DialogStyleHelper.addRow(g, 3, "Chambre", chamberCombo);
        DialogStyleHelper.addRow(g, 4, "Prix total", totalLabel);
        
        d.getDialogPane().setContent(g);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            LocalDate ci = checkIn.getValue();
            LocalDate co = checkOut.getValue();
            HotelChambre selected = chamberCombo.getSelectionModel().getSelectedItem();
            if (ci == null || co == null || selected == null) return null;
            if (!co.isAfter(ci)) return null;
            ReservationChambre rh = new ReservationChambre();
            rh.setDateDebut(ci);
            rh.setDateFin(co);
            long nights = ChronoUnit.DAYS.between(ci, co);
            double totalPrice = selected.getPrixChambre() * nights;
            rh.setMontantTotal(totalPrice);
            rh.setIdUser(user.getIdUser());
            rh.setIdChambre(selected.getIdChambre());
            rh.setStatut("EN_ATTENTE");
            return rh;
        });
        d.showAndWait().ifPresent(rh -> {
            try {
                if (rh.getIdChambre() == 0) {
                    showError("Validation", "Veuillez sélectionner une chambre.");
                    return;
                }
                if (rh.getDateDebut() == null || rh.getDateFin() == null) {
                    showError("Validation", "Les dates sont requises.");
                    return;
                }
                if (rh.getIdUser() == 0) {
                    showError("Validation", "Utilisateur non trouvé.");
                    return;
                }
                reservationHotelService.ajouter(rh);
                showSuccess("Réservation de chambre enregistrée avec succès. Montant total: " + rh.getMontantTotal() + " DT");
            } catch (SQLException e) {
                e.printStackTrace();
                showError("Erreur SQL", "Impossible d'enregistrer la réservation:\n" + e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                showError("Erreur", "Erreur lors de la réservation:\n" + e.getMessage());
            }
        });
    }
    
    private void updateTotal(DatePicker checkIn, DatePicker checkOut, ComboBox<HotelChambre> chamberCombo, Label totalLabel) {
        LocalDate ci = checkIn.getValue();
        LocalDate co = checkOut.getValue();
        HotelChambre selected = chamberCombo.getSelectionModel().getSelectedItem();
        
        if (ci != null && co != null && co.isAfter(ci) && selected != null) {
            long nights = ChronoUnit.DAYS.between(ci, co);
            double total = selected.getPrixChambre() * nights;
            totalLabel.setText(String.format("Total: %.2f DT", total));
        } else {
            totalLabel.setText("Total: 0 DT");
        }
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
