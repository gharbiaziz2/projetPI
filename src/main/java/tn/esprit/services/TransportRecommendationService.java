package tn.esprit.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import tn.esprit.entities.TransportLocal;
import tn.esprit.entities.Voyage;

import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

/**
 * Uses OpenRouter LLM API to recommend the best transport for a voyage.
 * Config: openrouter.api.key in config.properties
 */
public class TransportRecommendationService {

    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String MODEL = "meta-llama/llama-3.2-3b-instruct:free";
    private final String apiKey;
    private final HttpClient httpClient = HttpClient.newBuilder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TransportRecommendationService() {
        String key = System.getenv("OPENROUTER_API_KEY");
        if (key == null) key = System.getProperty("openrouter.api.key");
        if (key == null || key.isBlank()) {
            Properties props = new Properties();
            try (var is = getClass().getClassLoader().getResourceAsStream("config.properties")) {
                if (is != null) props.load(new InputStreamReader(is, StandardCharsets.UTF_8));
            } catch (Exception ignored) { }
            key = props.getProperty("openrouter.api.key", "").trim();
        } else {
            key = key.trim();
        }
        this.apiKey = key;
    }

    public static class Recommendation {
        public final int index;
        public final String reason;

        public Recommendation(int index, String reason) {
            this.index = index;
            this.reason = reason != null ? reason : "";
        }
    }

    /**
     * Recommends the best transport from the list for the given voyage.
     * Uses OpenRouter LLM when available; falls back to local heuristic on API error (e.g. 429).
     * @return Recommendation with index and reason, or null if none
     */
    public Recommendation recommendTransport(Voyage voyage, List<TransportLocal> transports) {
        if (transports == null || transports.isEmpty()) return null;
        if (transports.size() == 1) return new Recommendation(0, "Seule option disponible.");
        Recommendation apiResult = null;
        if (!apiKey.isEmpty()) {
            try {
                apiResult = recommendTransportViaApi(voyage, transports);
            } catch (Exception e) {
                if (e.getMessage() != null && !e.getMessage().contains("429"))
                    System.err.println("OpenRouter: " + e.getMessage());
            }
        }
        if (apiResult != null && apiResult.index >= 0) return apiResult;
        return recommendTransportFallback(transports);
    }

    private Recommendation recommendTransportViaApi(Voyage voyage, List<TransportLocal> transports) throws Exception {
            StringBuilder transportList = new StringBuilder();
            for (int i = 0; i < transports.size(); i++) {
                TransportLocal t = transports.get(i);
                String type = t.getTypeTransport() != null ? t.getTypeTransport().name() : "?";
                String prix = t.getPrix() != null ? t.getPrix().toString() : "?";
                String places = String.valueOf(t.getNbrPlaces());
                transportList.append(i + 1).append(". ").append(t.getCompagnie())
                        .append(" | Type: ").append(type)
                        .append(" | Prix: ").append(prix).append(" DT")
                        .append(" | Places: ").append(places);
                if (t.getPaysDepart() != null || t.getPaysArrivee() != null) {
                    transportList.append(" | ").append(t.getPaysDepart()).append(" → ").append(t.getPaysArrivee());
                }
                transportList.append("\n");
            }
            String voyageInfo = "Voyage: " + (voyage.getNomVoyage() != null ? voyage.getNomVoyage() : "?")
                    + " | Prix: " + (voyage.getPrix() != null ? voyage.getPrix() : "?") + " DT"
                    + " | Dates: " + (voyage.getDateDepart() != null ? voyage.getDateDepart() : "?")
                    + " - " + (voyage.getDateRetour() != null ? voyage.getDateRetour() : "?");

            String prompt = "Tu es un conseiller voyage. Recommande le MEILLEUR transport pour ce voyage.\n\n" +
                    "Voyage: " + voyageInfo + "\n\n" +
                    "Options de transport (numérotées 1 à " + transports.size() + "):\n" + transportList +
                    "\nRègle: Réponds sur 2 lignes. Ligne 1: UNIQUEMENT le numéro (1, 2, 3...). Ligne 2: une courte phrase expliquant POURQUOI (max 15 mots). " +
                    "Considère prix, confort, disponibilité des places et adéquation avec le voyage.";

            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", MODEL);
            body.put("max_tokens", 50);
            body.put("temperature", 0.3);
            ArrayNode messages = objectMapper.createArrayNode();
            ObjectNode msg = objectMapper.createObjectNode();
            msg.put("role", "user");
            msg.put("content", prompt);
            messages.add(msg);
            body.set("messages", messages);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                if (response.statusCode() == 429)
                    throw new RuntimeException("429 rate limit");
                throw new RuntimeException("HTTP " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) return null;
            String content = choices.get(0).path("message").path("content").asText("").trim();
            int idx = parseRecommendedIndex(content, transports.size());
            String reason = parseReason(content);
            return new Recommendation(idx >= 0 ? idx : 0, reason.isEmpty() ? "Recommandation adaptée à votre voyage." : reason);
    }

    private String parseReason(String content) {
        if (content == null || content.isEmpty()) return "";
        String[] lines = content.split("\\r?\\n");
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            line = line.replaceAll("^[0-9]+[.)\\s]*", "").trim();
            if (line.length() > 5) return line;
        }
        return "";
    }

    /** Fallback: pick transport with most places, then lowest price (when API fails e.g. 429). */
    private Recommendation recommendTransportFallback(List<TransportLocal> transports) {
        int best = 0;
        int bestPlaces = transports.get(0).getNbrPlaces();
        java.math.BigDecimal bestPrix = transports.get(0).getPrix();
        for (int i = 1; i < transports.size(); i++) {
            TransportLocal t = transports.get(i);
            boolean better = t.getNbrPlaces() > bestPlaces;
            if (!better && t.getNbrPlaces() == bestPlaces && t.getPrix() != null && bestPrix != null)
                better = t.getPrix().compareTo(bestPrix) < 0;
            if (better) {
                best = i;
                bestPlaces = t.getNbrPlaces();
                bestPrix = t.getPrix();
            }
        }
        TransportLocal t = transports.get(best);
        String reason;
        if (bestPlaces > 0 && t.getPrix() != null) {
            reason = bestPlaces > 3 ? "Plus de places disponibles (" + bestPlaces + ") et bon rapport qualité-prix."
                    : "Meilleur rapport places/prix pour ce voyage.";
        } else {
            reason = "Meilleure option disponible.";
        }
        return new Recommendation(best, reason);
    }

    private int parseRecommendedIndex(String content, int maxSize) {
        if (content == null || content.isEmpty()) return -1;
        content = content.replaceAll("[^0-9]", " ").trim();
        String[] parts = content.split("\\s+");
        for (String p : parts) {
            if (!p.isEmpty()) {
                try {
                    int n = Integer.parseInt(p);
                    if (n >= 1 && n <= maxSize) return n - 1;
                } catch (NumberFormatException ignored) { }
            }
        }
        return -1;
    }
}
