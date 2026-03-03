package tn.esprit.gui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Notification;
import tn.esprit.services.NotificationServices;

import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class FrontNotificationsController implements Initializable {

    @FXML private VBox notificationsContainer;

    private FrontController frontController;
    private final NotificationServices notificationService = new NotificationServices();
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public void setFrontController(FrontController c) { this.frontController = c; }

    /** Called when the notifications page is opened. Marks all as read and refreshes the badge. */
    public void onNotificationsPageOpened() {
        if (SessionHolder.getCurrentUser() == null) return;
        int userId = SessionHolder.getCurrentUser().getIdUser();
        try {
            notificationService.marquerToutesCommeLu(userId);
            if (frontController != null) frontController.refreshNotificationBadge();
        } catch (SQLException ignored) { }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadNotifications();
    }

    public void loadNotifications() {
        notificationsContainer.getChildren().clear();
        if (SessionHolder.getCurrentUser() == null) return;
        int userId = SessionHolder.getCurrentUser().getIdUser();
        try {
            List<Notification> list = notificationService.getByUserId(userId);
            if (list.isEmpty()) {
                Label empty = new Label("Aucune notification.");
                empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px;");
                notificationsContainer.getChildren().add(empty);
                return;
            }
            for (Notification n : list) {
                VBox card = new VBox(8);
                card.getStyleClass().add("front-card");
                card.setPadding(new Insets(16));
                Label msgL = new Label(n.getMessage() != null ? n.getMessage() : "");
                msgL.setWrapText(true);
                Label dateL = new Label(n.getDateCreation() != null ? n.getDateCreation().format(DT_FMT) : "");
                dateL.getStyleClass().add("card-statut");
                card.getChildren().addAll(msgL, dateL);
                notificationsContainer.getChildren().add(card);
            }
        } catch (SQLException e) {
            Label err = new Label("Impossible de charger les notifications.");
            err.setStyle("-fx-text-fill: #c0392b;");
            notificationsContainer.getChildren().add(err);
        }
    }
}
