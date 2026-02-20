package tn.esprit.gui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.entities.Forum;
import tn.esprit.entities.User;
import tn.esprit.entities.Voyage;
import tn.esprit.services.ForumServices;
import tn.esprit.services.UserServices;
import tn.esprit.services.VoyageServices;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class FrontForumController implements Initializable {

    @FXML private VBox postsContainer;

    private FrontController frontController;
    private final ForumServices forumService = new ForumServices();
    private final UserServices userService = new UserServices();
    private final VoyageServices voyageService = new VoyageServices();

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private Map<Integer, String> userNames = new HashMap<>();
    private Map<Integer, String> voyageLabels = new HashMap<>();

    public void setFrontController(FrontController c) { this.frontController = c; }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadPosts();
    }

    public void loadPosts() {
        postsContainer.getChildren().clear();
        try {
            userNames.clear();
            for (User u : userService.afficher()) userNames.put(u.getIdUser(), u.getNom() + " " + u.getPrenom());
            voyageLabels.clear();
            for (Voyage v : voyageService.afficher()) {
                String lab = (v.getTypeVoyage() != null ? v.getTypeVoyage() : "") + " " + (v.getDateDepart() != null ? v.getDateDepart().toString() : "");
                voyageLabels.put(v.getIdVoyage(), lab);
            }
            for (Forum f : forumService.afficher()) {
                VBox card = new VBox(8);
                card.getStyleClass().add("front-card");
                card.setPadding(new Insets(16));
                Label userL = new Label(userNames.getOrDefault(f.getIdUser(), "?"));
                userL.getStyleClass().add("card-title");
                Label contentL = new Label(f.getContenu() != null ? f.getContenu() : "");
                contentL.setWrapText(true);
                Label dateL = new Label(f.getDateEnvoi() != null ? f.getDateEnvoi().format(DT_FMT) : "");
                dateL.getStyleClass().add("card-statut");
                Label voyageL = new Label("Voyage: " + voyageLabels.getOrDefault(f.getIdVoyage(), "?"));
                voyageL.getStyleClass().add("card-statut");
                card.getChildren().addAll(userL, contentL, dateL, voyageL);
                postsContainer.getChildren().add(card);
            }
        } catch (SQLException e) {
            showError("Erreur", "Impossible de charger le forum.");
        }
    }

    @FXML
    private void onAddPost() {
        User user = SessionHolder.getCurrentUser();
        if (user == null) {
            showError("Connexion requise", "Veuillez vous connecter pour poster.");
            return;
        }
        Dialog<Forum> d = new Dialog<>();
        d.setTitle("Nouveau message");
        d.setHeaderText("Publier un message");
        TextArea contenu = new TextArea();
        contenu.setPromptText("Votre message...");
        contenu.setPrefRowCount(4);
        contenu.setWrapText(true);
        ComboBox<Voyage> voyageCombo = new ComboBox<>();
        voyageCombo.setConverter(new javafx.util.StringConverter<Voyage>() {
            @Override
            public String toString(Voyage v) {
                if (v == null) return "";
                return (v.getTypeVoyage() != null ? v.getTypeVoyage() : "") + " - " + (v.getDateDepart() != null ? v.getDateDepart().toString() : "");
            }
            @Override
            public Voyage fromString(String s) { return null; }
        });
        try {
            voyageCombo.getItems().addAll(voyageService.afficher());
            if (!voyageCombo.getItems().isEmpty()) voyageCombo.getSelectionModel().selectFirst();
        } catch (SQLException e) {}
        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(10);
        g.add(new Label("Message *"), 0, 0);
        g.add(contenu, 1, 0);
        g.add(new Label("Voyage *"), 0, 1);
        g.add(voyageCombo, 1, 1);
        d.getDialogPane().setContent(g);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            if (contenu.getText() == null || contenu.getText().isBlank()) return null;
            Voyage v = voyageCombo.getSelectionModel().getSelectedItem();
            if (v == null) return null;
            Forum f = new Forum();
            f.setContenu(contenu.getText().trim());
            f.setDateEnvoi(LocalDateTime.now());
            f.setIdUser(user.getIdUser());
            f.setIdVoyage(v.getIdVoyage());
            return f;
        });
        d.showAndWait().ifPresent(f -> {
            try {
                forumService.ajouter(f);
                showSuccess("Message publie.");
                loadPosts();
            } catch (SQLException e) {
                showError("Erreur", "Impossible de publier.");
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
