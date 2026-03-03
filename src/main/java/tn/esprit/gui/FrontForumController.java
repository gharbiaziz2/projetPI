package tn.esprit.gui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.entities.Forum;
import tn.esprit.entities.ForumComment;
import tn.esprit.entities.User;
import tn.esprit.entities.Voyage;
import tn.esprit.services.ForumCommentServices;
import tn.esprit.services.ForumReactionService;
import tn.esprit.services.ForumServices;
import tn.esprit.services.ProfanityFilterService;
import tn.esprit.services.TranslationService;
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
    private final ForumCommentServices commentService = new ForumCommentServices();
    private final ForumReactionService reactionService = new ForumReactionService();
    private final ProfanityFilterService profanityFilter = new ProfanityFilterService();
    private final UserServices userService = new UserServices();
    private final VoyageServices voyageService = new VoyageServices();

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private Map<Integer, String> userNames = new HashMap<>();
    private Map<Integer, String> voyageLabels = new HashMap<>();
    private final TranslationService translationService = new TranslationService();

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
                VBox card = buildForumPostCard(f);
                postsContainer.getChildren().add(card);
            }
        } catch (SQLException e) {
            showError("Erreur", "Impossible de charger le forum.");
        }
    }

    private VBox buildForumPostCard(Forum f) {
        VBox card = new VBox(8);
        card.getStyleClass().add("front-card");
        card.setPadding(new Insets(16));
        Label userL = new Label(userNames.getOrDefault(f.getIdUser(), "?"));
        userL.getStyleClass().add("card-title");
        String originalText = f.getContenu() != null ? f.getContenu() : "";
        Label contentL = new Label(originalText);
        contentL.setWrapText(true);
        HBox btnRow = new HBox(6);
        btnRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Button btnEn = new Button("→ EN");
        Button btnFr = new Button("→ FR");
        Button btnOriginal = new Button("Original");
        for (Button b : new Button[]{btnEn, btnFr, btnOriginal}) {
            b.getStyleClass().add("link-button");
            b.setStyle("-fx-font-size: 11px; -fx-padding: 2 6;");
        }
        btnEn.setOnAction(e -> translateInline(contentL, originalText, "fr", "en"));
        btnFr.setOnAction(e -> translateInline(contentL, originalText, "en", "fr"));
        btnOriginal.setOnAction(e -> contentL.setText(originalText));
        btnRow.getChildren().addAll(btnEn, btnFr, btnOriginal);
        Label dateL = new Label(f.getDateEnvoi() != null ? f.getDateEnvoi().format(DT_FMT) : "");
        dateL.getStyleClass().add("card-statut");
        Label voyageL = new Label("Voyage: " + voyageLabels.getOrDefault(f.getIdVoyage(), "?"));
        voyageL.getStyleClass().add("card-statut");

        // Like/Dislike row for post
        HBox postReactionRow = buildReactionRow(f.getIdForum(), true, null);
        card.getChildren().addAll(userL, contentL, btnRow, dateL, voyageL, postReactionRow);

        // Comments section
        VBox commentsSection = new VBox(6);
        commentsSection.getStyleClass().add("comments-section");
        commentsSection.setStyle("-fx-padding: 8 0 0 0; -fx-border-color: derive(-fx-base, 20%); -fx-border-width: 1 0 0 0;");
        try {
            for (ForumComment c : commentService.getByForumId(f.getIdForum())) {
                commentsSection.getChildren().add(buildCommentCard(c, commentsSection));
            }
        } catch (SQLException e) { /* ignore */ }
        TextField addCommentField = new TextField();
        addCommentField.setPromptText("Ajouter un commentaire...");
        addCommentField.setMaxWidth(Double.MAX_VALUE);
        Button btnAddComment = new Button("Ajouter commentaire");
        btnAddComment.getStyleClass().add("link-button");
        HBox addCommentRow = new HBox(8);
        addCommentRow.getChildren().addAll(addCommentField, btnAddComment);
        addCommentRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        btnAddComment.setOnAction(e -> {
            User user = SessionHolder.getCurrentUser();
            if (user == null) {
                showError("Connexion requise", "Veuillez vous connecter pour commenter.");
                return;
            }
            String text = addCommentField.getText();
            if (text == null || text.isBlank()) return;
            if (profanityFilter.hasProfanity(text)) {
                showError("Message refuse", "Votre commentaire contient des mots inappropriés. Veuillez modifier votre texte.");
                return;
            }
            try {
                ForumComment nc = new ForumComment();
                nc.setIdForum(f.getIdForum());
                nc.setIdUser(user.getIdUser());
                nc.setContenu(text.trim());
                nc.setDateCreation(LocalDateTime.now());
                commentService.ajouter(nc);
                addCommentField.clear();
                loadPosts();
            } catch (SQLException ex) {
                showError("Erreur", "Impossible d'ajouter le commentaire.");
            }
        });
        commentsSection.getChildren().add(addCommentRow);
        card.getChildren().add(commentsSection);
        return card;
    }

    private HBox buildCommentCard(ForumComment c, VBox parentComments) {
        HBox h = new HBox(8);
        h.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        h.setStyle("-fx-background-color: derive(-fx-base, 5%); -fx-padding: 6 10; -fx-background-radius: 4;");
        Label contentL = new Label(c.getContenu() != null ? c.getContenu() : "");
        contentL.setWrapText(true);
        Label metaL = new Label(userNames.getOrDefault(c.getIdUser(), "?") + " • " + (c.getDateCreation() != null ? c.getDateCreation().format(DT_FMT) : ""));
        metaL.getStyleClass().add("card-statut");
        metaL.setStyle("-fx-font-size: 11px;");
        HBox reactionRow = buildReactionRow(c.getIdForum(), false, c.getIdComment());
        HBox actions = new HBox(4);
        User user = SessionHolder.getCurrentUser();
        boolean isOwner = user != null && user.getIdUser() == c.getIdUser();
        if (isOwner) {
            Button btnEdit = new Button("Modifier");
            Button btnDel = new Button("Supprimer");
            for (Button b : new Button[]{btnEdit, btnDel}) {
                b.getStyleClass().add("link-button");
                b.setStyle("-fx-font-size: 11px; -fx-padding: 2 6;");
            }
            btnEdit.setOnAction(e -> editComment(c, parentComments));
            btnDel.setOnAction(e -> deleteComment(c, parentComments));
            actions.getChildren().addAll(btnEdit, btnDel);
        }
        VBox left = new VBox(4);
        left.getChildren().addAll(contentL, metaL, new HBox(8, reactionRow, actions));
        h.getChildren().add(left);
        return h;
    }

    private void editComment(ForumComment c, VBox parentComments) {
        TextInputDialog d = new TextInputDialog(c.getContenu());
        d.setTitle("Modifier le commentaire");
        d.setHeaderText("Modifier le commentaire");
        d.getEditor().setPrefWidth(350);
        d.showAndWait().ifPresent(text -> {
            if (text == null || text.isBlank()) return;
            if (profanityFilter.hasProfanity(text.trim())) {
                showError("Message refuse", "Votre commentaire contient des mots inappropriés. Veuillez modifier votre texte.");
                return;
            }
            try {
                c.setContenu(text.trim());
                commentService.modifier(c);
                loadPosts();
            } catch (SQLException e) {
                showError("Erreur", "Impossible de modifier.");
            }
        });
    }

    private void deleteComment(ForumComment c, VBox parentComments) {
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION);
        conf.setTitle("Confirmer");
        conf.setHeaderText("Supprimer ce commentaire ?");
        conf.showAndWait().filter(b -> b == ButtonType.OK).ifPresent(b -> {
            try {
                commentService.supprimer(c.getIdComment());
                loadPosts();
            } catch (SQLException e) {
                showError("Erreur", "Impossible de supprimer.");
            }
        });
    }

    private HBox buildReactionRow(int idForum, boolean isForum, Integer idComment) {
        HBox row = new HBox(8);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label likeL = new Label("👍");
        Label dislikeL = new Label("👎");
        Label countL = new Label();
        try {
            int uid = SessionHolder.getCurrentUser() != null ? SessionHolder.getCurrentUser().getIdUser() : -1;
            int userReaction = isForum
                ? reactionService.getUserForumReaction(uid, idForum)
                : reactionService.getUserCommentReaction(uid, idComment);
            Map<String, Integer> counts = isForum
                ? reactionService.getForumReactionCounts(idForum)
                : reactionService.getCommentReactionCounts(idComment);
            int likes = counts != null ? counts.getOrDefault("likes", 0) : 0;
            int dislikes = counts != null ? counts.getOrDefault("dislikes", 0) : 0;
            countL.setText(" " + likes + " 👍 / " + dislikes + " 👎 ");
            countL.getStyleClass().add("card-statut");
            String baseStyle = "-fx-cursor: hand;";
            likeL.setStyle(baseStyle + (userReaction == 1 ? " -fx-opacity: 1; -fx-font-weight: bold;" : ""));
            dislikeL.setStyle(baseStyle + (userReaction == -1 ? " -fx-opacity: 1; -fx-font-weight: bold;" : ""));
        } catch (SQLException e) {
            countL.setText(" 0 👍 / 0 👎 ");
            likeL.setStyle("-fx-cursor: hand;");
            dislikeL.setStyle("-fx-cursor: hand;");
        }
        User user = SessionHolder.getCurrentUser();
        int uid = user != null ? user.getIdUser() : -1;
        if (uid < 0) {
            likeL.setOnMouseClicked(e -> showError("Connexion requise", "Connectez-vous pour réagir."));
            dislikeL.setOnMouseClicked(e -> showError("Connexion requise", "Connectez-vous pour réagir."));
        } else {
            likeL.setOnMouseClicked(e -> setReaction(idForum, isForum, idComment, uid, 1));
            dislikeL.setOnMouseClicked(e -> setReaction(idForum, isForum, idComment, uid, -1));
        }
        row.getChildren().addAll(likeL, dislikeL, countL);
        return row;
    }

    private void setReaction(int idForum, boolean isForum, Integer idComment, int uid, int value) {
        try {
            if (isForum) {
                reactionService.setForumReaction(uid, idForum, value);
            } else {
                reactionService.setCommentReaction(uid, idComment, value);
            }
            loadPosts();
        } catch (SQLException e) {
            showError("Erreur", "Impossible d'enregistrer la réaction.");
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
            String msg = contenu.getText().trim();
            if (profanityFilter.hasProfanity(msg)) {
                showError("Message refuse", "Votre message contient des mots inappropriés. Veuillez modifier votre texte.");
                return null;
            }
            Voyage v = voyageCombo.getSelectionModel().getSelectedItem();
            if (v == null) return null;
            Forum f = new Forum();
            f.setContenu(msg);
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

    private void translateInline(Label contentL, String originalText, String source, String target) {
        if (originalText == null || originalText.isBlank()) return;
        String translated = translationService.translate(originalText, source, target);
        if (translated != null && !translated.isBlank()) {
            contentL.setText(translated);
        } else {
            showError("Traduction", "Impossible de traduire. Vérifiez votre connexion internet.");
        }
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
