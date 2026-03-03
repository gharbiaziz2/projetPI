package tn.esprit.gui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.esprit.services.CohereService;

import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;

public class FrontChatbotController implements Initializable {

    @FXML private VBox chatContainer;
    @FXML private TextField inputField;
    @FXML private Button sendBtn;
    @FXML private ScrollPane chatScroll;

    private FrontController frontController;
    private final List<Map<String, String>> chatHistory = new ArrayList<>();
    private static final String SYSTEM_PROMPT = "Tu es un assistant de CarthageVoyage, une plateforme de tourisme en Tunisie. Réponds en français. Aide les utilisateurs sur les voyages, activités, hôtels, transport et destinations (Tunis, Carthage, Sidi Bou Said, etc.). Sois concis et utile.";

    public void setFrontController(FrontController c) { this.frontController = c; }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        addWelcomeMessage();
    }

    private void addWelcomeMessage() {
        Label welcome = new Label("Bonjour ! Je suis l'assistant CarthageVoyage. Comment puis-je vous aider ?");
        welcome.setStyle("-fx-font-size: 14px; -fx-text-fill: #475569;");
        welcome.setWrapText(true);
        VBox msgBox = new VBox(4);
        msgBox.setAlignment(Pos.CENTER_LEFT);
        msgBox.setPadding(new Insets(12));
        msgBox.setStyle("-fx-background-color: #eff6ff; -fx-background-radius: 12; -fx-max-width: 600;");
        msgBox.getChildren().add(welcome);
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().add(msgBox);
        chatContainer.getChildren().add(row);
    }

    @FXML
    private void sendMessage() {
        String text = inputField != null ? (inputField.getText() != null ? inputField.getText().trim() : "") : "";
        if (text.isEmpty()) return;

        inputField.clear();
        sendBtn.setDisable(true);

        addMessage(text, "user");
        chatHistory.add(Map.of("role", "user", "content", text));

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<Map<String, String>> messages = new ArrayList<>();
                messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
                messages.addAll(chatHistory);

                CohereService service = new CohereService();
                String reply = service.chat(messages);
                chatHistory.add(Map.of("role", "assistant", "content", reply));

                Platform.runLater(() -> {
                    addMessage(reply, "assistant");
                    sendBtn.setDisable(false);
                    scrollToBottom();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    addMessage("Désolé, une erreur s'est produite. Réessayez.", "assistant");
                    sendBtn.setDisable(false);
                });
            }
        });
    }

    private void addMessage(String text, String role) {
        Label content = new Label(text);
        content.setStyle("-fx-font-size: 14px; -fx-text-fill: " + ("user".equals(role) ? "#1a2332" : "#475569") + ";");
        content.setWrapText(true);

        VBox msgBox = new VBox(4);
        msgBox.setPadding(new Insets(12));
        msgBox.setMaxWidth(600);
        msgBox.getChildren().add(content);

        if ("user".equals(role)) {
            msgBox.setStyle("-fx-background-color: #2980b9; -fx-background-radius: 12;");
            content.setStyle("-fx-font-size: 14px; -fx-text-fill: white;");
            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_RIGHT);
            row.getChildren().add(msgBox);
            chatContainer.getChildren().add(row);
        } else {
            msgBox.setStyle("-fx-background-color: #eff6ff; -fx-background-radius: 12;");
            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.getChildren().add(msgBox);
            chatContainer.getChildren().add(row);
        }
    }

    private void scrollToBottom() {
        if (chatScroll != null) {
            chatScroll.setVvalue(1.0);
        }
    }
}
