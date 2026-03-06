package tn.esprit.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Ninja API Sentiment Analysis - analyzes text sentiment.
 * Config: ninja.api.key in config.properties
 * https://api-ninjas.com/api/sentiment
 */
public class SentimentAnalysisService {
    private static final String API_URL = "https://api.api-ninjas.com/v1/sentiment";
    private static final int MAX_TEXT_LENGTH = 2000;
    private final String apiKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SentimentAnalysisService() {
        Properties props = new Properties();
        try (var is = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) props.load(is);
        } catch (Exception e) { }
        this.apiKey = props.getProperty("ninja.api.key", "").trim();
    }

    /**
     * Analyzes text sentiment. Returns null on error or if API key missing.
     * @return SentimentResult with score (-1 to 1) and label, or null
     */
    public SentimentResult analyze(String text) {
        if (apiKey.isEmpty() || text == null || text.isBlank()) return null;
        String toAnalyze = text.length() > MAX_TEXT_LENGTH ? text.substring(0, MAX_TEXT_LENGTH) : text;
        try {
            String encoded = URLEncoder.encode(toAnalyze, StandardCharsets.UTF_8);
            var request = java.net.http.HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "?text=" + encoded))
                    .header("X-Api-Key", apiKey)
                    .GET()
                    .build();
            var response = java.net.http.HttpClient.newHttpClient()
                    .send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;
            JsonNode root = objectMapper.readTree(response.body());
            double score = root.path("score").asDouble(0);
            String sentiment = root.path("sentiment").asText("NEUTRAL");
            return new SentimentResult(score, sentiment);
        } catch (Exception e) {
            System.err.println("SentimentAnalysis error: " + e.getMessage());
            return null;
        }
    }

    public static class SentimentResult {
        public final double score;
        public final String sentiment;

        public SentimentResult(double score, String sentiment) {
            this.score = score;
            this.sentiment = sentiment != null ? sentiment : "NEUTRAL";
        }

        public boolean isPositive() {
            return "POSITIVE".equals(sentiment) || "WEAK_POSITIVE".equals(sentiment);
        }

        public boolean isNegative() {
            return "NEGATIVE".equals(sentiment) || "WEAK_NEGATIVE".equals(sentiment);
        }
    }
}
