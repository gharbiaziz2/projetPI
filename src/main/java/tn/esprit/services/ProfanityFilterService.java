package tn.esprit.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Ninja API Profanity Filter - detects bad words in text.
 * Config: ninja.api.key in config.properties
 * https://api-ninjas.com/api/profanityfilter
 */
public class ProfanityFilterService {
    private static final String API_URL = "https://api.api-ninjas.com/v1/profanityfilter";
    private static final int MAX_TEXT_LENGTH = 1000;
    private final String apiKey;

    public ProfanityFilterService() {
        Properties props = new Properties();
        try (var is = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) props.load(is);
        } catch (Exception e) {
            System.err.println("ProfanityFilter: could not load config.properties");
        }
        this.apiKey = props.getProperty("ninja.api.key", "").trim();
    }

    /**
     * Checks if the text contains profanity/bad words.
     * @param text Text to check (max 1000 chars)
     * @return true if profanity detected, false if clean or on API error (fails open)
     */
    public boolean hasProfanity(String text) {
        if (text == null || text.isBlank()) return false;
        if (apiKey.isEmpty()) return false;
        String toCheck = text.length() > MAX_TEXT_LENGTH ? text.substring(0, MAX_TEXT_LENGTH) : text;
        try {
            String encoded = URLEncoder.encode(toCheck, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "?text=" + encoded))
                    .header("X-Api-Key", apiKey)
                    .GET()
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return false;
            JsonNode root = new ObjectMapper().readTree(response.body());
            return root.path("has_profanity").asBoolean(false);
        } catch (Exception e) {
            System.err.println("ProfanityFilter error: " + e.getMessage());
            return false; // fail open - allow post if API unreachable
        }
    }
}
