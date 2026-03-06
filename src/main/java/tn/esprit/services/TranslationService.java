package tn.esprit.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Free translation via MyMemory REST API (no API key required).
 * https://mymemory.translated.net/doc/spec.php
 */
public class TranslationService {

    private static final String API_URL = "https://api.mymemory.translated.net/get";
    private final HttpClient httpClient = HttpClient.newBuilder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Translates text from source to target language.
     * @param text Text to translate
     * @param sourceLang Source language code (e.g. "fr", "en", "ar")
     * @param targetLang Target language code
     * @return Translated text, or null on error
     */
    public String translate(String text, String sourceLang, String targetLang) {
        if (text == null || text.isBlank()) return null;
        if (sourceLang == null || targetLang == null) return null;
        try {
            String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String langpair = URLEncoder.encode(sourceLang + "|" + targetLang, StandardCharsets.UTF_8);
            String url = API_URL + "?q=" + encoded + "&langpair=" + langpair;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode responseData = root.path("responseData");
            if (responseData.isMissingNode()) return null;
            JsonNode translated = responseData.path("translatedText");
            return translated.isMissingNode() ? null : translated.asText();
        } catch (Exception e) {
            System.err.println("Translation error: " + e.getMessage());
            return null;
        }
    }
}
