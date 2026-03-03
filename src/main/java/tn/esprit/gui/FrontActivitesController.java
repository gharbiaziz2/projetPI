package tn.esprit.gui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Activite;
import tn.esprit.services.ActiviteServices;

import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class FrontActivitesController implements Initializable {

    @FXML private FlowPane cardsContainer;

    private FrontController frontController;
    private final ActiviteServices activiteService = new ActiviteServices();

    public void setFrontController(FrontController c) { this.frontController = c; }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadCards();
    }

    public void loadCards() {
        cardsContainer.getChildren().clear();
        try {
            for (Activite a : activiteService.afficher()) {
                VBox card = buildActiviteCard(a);
                cardsContainer.getChildren().add(card);
            }
        } catch (SQLException e) {
            cardsContainer.getChildren().add(new Label("Impossible de charger les activités."));
        }
    }

    private VBox buildActiviteCard(Activite a) {
        VBox card = new VBox(10);
        card.getStyleClass().add("front-card");
        card.setPadding(new Insets(20));
        card.setPrefWidth(300);
        card.setMaxWidth(300);
        if (a.getPhoto() != null && !a.getPhoto().isBlank()) {
            try {
                String path = a.getPhoto();
                if (!path.startsWith("file:") && !path.startsWith("http")) path = "file:" + path;
                ImageView imgView = new ImageView(new Image(path, true));
                imgView.setFitWidth(260);
                imgView.setFitHeight(140);
                imgView.setPreserveRatio(true);
                imgView.setStyle("-fx-background-radius: 8;");
                card.getChildren().add(imgView);
            } catch (Exception ignored) { }
        }
        Label nameL = new Label(a.getNom() != null ? a.getNom() : "Activité");
        nameL.getStyleClass().add("card-title");
        Label prixL = new Label("Prix: " + (a.getPrix() != null ? a.getPrix().toString() : "0") + " DT");
        prixL.getStyleClass().add("card-price");
        Label dureeL = new Label("Durée: " + a.getDuree() + " min");
        dureeL.getStyleClass().add("card-dates");
        String coords = (a.getLatitude() != null && a.getLongitude() != null)
                ? String.format("%.4f, %.4f", a.getLatitude(), a.getLongitude()) : "—";
        Label coordsL = new Label("📍 Lat/Lon: " + coords);
        coordsL.getStyleClass().add("card-statut");
        coordsL.setWrapText(true);
        Label dateL = new Label("📅 Date: " + (a.getDate() != null ? a.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—"));
        dateL.getStyleClass().add("card-statut");
        String desc = a.getDescription();
        if (desc != null && desc.length() > 120) desc = desc.substring(0, 120) + "...";
        Label descL = new Label(desc != null ? desc : "");
        descL.setWrapText(true);
        descL.getStyleClass().add("card-statut");
        card.getChildren().addAll(nameL, prixL, dureeL, coordsL, dateL, descL);
        return card;
    }
}
