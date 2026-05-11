package tn.esprit.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Cohere Chat API for chatbot. Config: cohere.api.key in config.properties
 */
public class CohereService {

    private static final String CHAT_URL = "https://api.cohere.ai/v2/chat";
    private final String apiKey;
    private final HttpClient httpClient = HttpClient.newBuilder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CohereService() throws IOException {
        Properties props = new Properties();
        try (var is = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) props.load(is);
        }
        this.apiKey = props.getProperty("cohere.api.key", "").trim();
    }

    /**
     * Sends a chat message and returns the assistant's response.
     * @param messages List of [role, content] where role is "user" or "assistant"
     */
    public String chat(List<Map<String, String>> messages) {
        if (apiKey.isEmpty()) {
            System.err.println("Cohere: API key not configured");
            return "Clé API non configurée.";
        }
        try {
            String jsonBody = buildRequestBody(messages);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CHAT_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("Cohere error: " + response.statusCode() + " " + response.body());
                return "Erreur du service. Réessayez.";
            }
            return parseResponse(response.body());
        } catch (Exception e) {
            System.err.println("Cohere chat error: " + e.getMessage());
            return "Erreur de connexion. Réessayez.";
        }
    }

    private String buildRequestBody(List<Map<String, String>> messages) throws Exception {
        StringBuilder sb = new StringBuilder("{\"model\":\"command-a-03-2025\",\"messages\":[");
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) sb.append(",");
            Map<String, String> m = messages.get(i);
            String role = m.get("role");
            String content = m.get("content");
            if (content == null) content = "";
            content = content.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
            sb.append("{\"role\":\"").append(role).append("\",\"content\":\"").append(content).append("\"}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String parseResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode msg = root.path("message");
            if (msg.isMissingNode()) {
                JsonNode text = root.path("text");
                return text.isMissingNode() ? "" : text.asText();
            }
            JsonNode content = msg.path("content");
            if (content.isArray() && content.size() > 0) {
                JsonNode first = content.get(0);
                if (first.has("text")) return first.get("text").asText();
            }
            if (content.isTextual()) return content.asText();
            return msg.path("text").asText("");
        } catch (Exception e) {
            System.err.println("Cohere parse error: " + e.getMessage());
            return "";
        }
    }
}
