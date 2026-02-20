package tn.esprit.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.services.UserServices;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;

public class BackController {

    @FXML private ImageView sidebarLogo;
    @FXML private Button menuDashboard;
    @FXML private Button menuUsers;
    @FXML private Button menuActivite;
    @FXML private Button menuDestination;
    @FXML private Button menuForum;
    @FXML private Button menuHotel;
    @FXML private Button menuReservationHotel;
    @FXML private Button menuReservationTransport;
    @FXML private Button menuReservationActivite;
    @FXML private Button menuReservationVoyage;
    @FXML private Button menuTransportLocal;
    @FXML private Button menuVoyage;
    @FXML private Label headerTitle;
    @FXML private StackPane contentStack;
    @FXML private Button btnLogout;

    private final UserServices userServices = new UserServices();

    @FXML
    public void initialize() {
        URL logoUrl = getClass().getResource("/images/logo.png");
        if (logoUrl != null) {
            sidebarLogo.setImage(new Image(logoUrl.toExternalForm()));
            sidebarLogo.setVisible(true);
        } else {
            sidebarLogo.setVisible(false);
        }
        goDashboard();
    }

    @FXML
    private void goDashboard() {
        setActiveMenu(menuDashboard);
        headerTitle.setText("Dashboard");
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/back_dashboard.fxml"));
            contentStack.getChildren().setAll(root);
            updateDashboardStats(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateDashboardStats(Parent dash) {
        try {
            long total = userServices.afficher().size();
            long clients = userServices.afficher().stream().filter(u -> u.getRole() == User.Role.CLIENT).count();
            long guides = userServices.afficher().stream().filter(u -> u.getRole() == User.Role.GUIDE_TOURISTIQUE).count();
            Node n1 = dash.lookup("#statUsers");
            Node n2 = dash.lookup("#statClients");
            Node n3 = dash.lookup("#statGuides");
            if (n1 instanceof Label) ((Label) n1).setText(String.valueOf(total));
            if (n2 instanceof Label) ((Label) n2).setText(String.valueOf(clients));
            if (n3 instanceof Label) ((Label) n3).setText(String.valueOf(guides));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goUsers() {
        setActiveMenu(menuUsers);
        loadContent("/fxml/back_users.fxml", "Users", "refresh");
    }

    @FXML private void goActivite() { setActiveMenu(menuActivite); loadContent("/fxml/back_activite.fxml", "Activite", "refresh"); }
    @FXML private void goDestination() { setActiveMenu(menuDestination); loadContent("/fxml/back_destination.fxml", "Destination", "refresh"); }
    @FXML private void goForum() { setActiveMenu(menuForum); loadContent("/fxml/back_forum.fxml", "Forum", "refresh"); }
    @FXML private void goHotel() { setActiveMenu(menuHotel); loadContent("/fxml/back_hotel.fxml", "Hotel", "refresh"); }
    @FXML private void goReservationHotel() { setActiveMenu(menuReservationHotel); loadContent("/fxml/back_reservationhotel.fxml", "Reservation Hotel", "refresh"); }
    @FXML private void goReservationTransport() { setActiveMenu(menuReservationTransport); loadContent("/fxml/back_reservationtransport.fxml", "Reservation Transport", "refresh"); }
    @FXML private void goReservationActivite() { setActiveMenu(menuReservationActivite); loadContent("/fxml/back_reservation_activite.fxml", "Reservation Activite", "refresh"); }
    @FXML private void goReservationVoyage() { setActiveMenu(menuReservationVoyage); loadContent("/fxml/back_reservation_voyage.fxml", "Reservation Voyage", "refresh"); }
    @FXML private void goTransportLocal() { setActiveMenu(menuTransportLocal); loadContent("/fxml/back_transportlocal.fxml", "Transport Local", "refresh"); }
    @FXML private void goVoyage() { setActiveMenu(menuVoyage); loadContent("/fxml/back_voyage.fxml", "Voyage", "refresh"); }

    private void loadContent(String fxmlPath, String title, String refreshMethod) {
        headerTitle.setText(title);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Object ctrl = loader.getController();
            if (ctrl != null && refreshMethod != null) {
                try {
                    ctrl.getClass().getMethod(refreshMethod).invoke(ctrl);
                } catch (Exception ignored) {}
            }
            contentStack.getChildren().setAll(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setActiveMenu(Button active) {
        Button[] all = { menuDashboard, menuUsers, menuActivite, menuDestination, menuForum, menuHotel,
                menuReservationHotel, menuReservationTransport, menuReservationActivite, menuReservationVoyage, menuTransportLocal, menuVoyage };
        for (Button b : all) if (b != null) b.getStyleClass().remove("active");
        if (active != null) active.getStyleClass().add("active");
    }

    @FXML
    private void toggleSidebar() {
        // Optional: collapse sidebar - could toggle visibility of left panel
    }

    @FXML
    private void logout() {
        SessionHolder.clear();
        Stage stage = (Stage) (btnLogout != null ? btnLogout.getScene().getWindow() : contentStack.getScene().getWindow());
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Scene scene = new Scene(root, 960, 620);
            try {
                scene.getStylesheets().add(getClass().getResource("/css/voyage.css").toExternalForm());
            } catch (Exception ignored) {}
            stage.setScene(scene);
            stage.setTitle("CarthageVoyage - Connexion");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
