package tn.esprit.gui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Activite;
import tn.esprit.services.ActiviteServices;

import java.net.URL;
import java.sql.SQLException;
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
        Label nameL = new Label(a.getNom() != null ? a.getNom() : "Activité");
        nameL.getStyleClass().add("card-title");
        Label prixL = new Label("Prix: " + (a.getPrix() != null ? a.getPrix().toString() : "0") + " DT");
        prixL.getStyleClass().add("card-price");
        Label dureeL = new Label("Durée: " + a.getDuree() + " min");
        dureeL.getStyleClass().add("card-dates");
        String desc = a.getDescription();
        if (desc != null && desc.length() > 120) desc = desc.substring(0, 120) + "...";
        Label descL = new Label(desc != null ? desc : "");
        descL.setWrapText(true);
        descL.getStyleClass().add("card-statut");
        card.getChildren().addAll(nameL, prixL, dureeL, descL);
        return card;
    }
}
