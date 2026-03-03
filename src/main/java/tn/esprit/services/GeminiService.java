package tn.esprit.services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class GeminiService {

    private static final String API_KEY = loadApiKey();

    private static String loadApiKey() {
        try (var in = GeminiService.class.getResourceAsStream("/config.properties")) {
            var props = new java.util.Properties();
            props.load(in);
            return props.getProperty("gemini.api.key");
        } catch (Exception e) {
            throw new RuntimeException("Could not load Gemini API key from config.properties", e);
        }
    }

    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent";

    private final HttpClient httpClient;

    public GeminiService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

    public String getTravelResponse(String userMessage) {
        try {
            // 1. Combine System Prompt and User Message as requested
            String travelAgentPrompt = "Tu es un agent de voyage expert pour CarthageVoyage. "
                    + "Réponds de manière courte et utile uniquement aux questions sur les voyages et les destinations. "
                    + "Question: " + userMessage;

            // 2. Build the exact JSON structure: { "contents": [ { "parts": [ { "text":
            // "..." } ] } ] }
            JSONObject textPart = new JSONObject();
            textPart.put("text", travelAgentPrompt);

            JSONArray partsArray = new JSONArray();
            partsArray.put(textPart);

            JSONObject contentObject = new JSONObject();
            contentObject.put("parts", partsArray);

            JSONArray contentsArray = new JSONArray();
            contentsArray.put(contentObject);

            JSONObject requestBody = new JSONObject();
            requestBody.put("contents", contentsArray);

            // 3. Create the HTTP Request perfectly matching the cURL
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("X-goog-api-key", API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString(), StandardCharsets.UTF_8))
                    .build();

            // 4. Send the Request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // 5. Safely handle the response
            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                if (jsonResponse.has("candidates")) {
                    JSONArray candidates = jsonResponse.getJSONArray("candidates");
                    if (!candidates.isEmpty()) {
                        JSONObject firstCandidate = candidates.getJSONObject(0);
                        JSONObject contentObj = firstCandidate.getJSONObject("content");
                        JSONArray responseParts = contentObj.getJSONArray("parts");
                        if (!responseParts.isEmpty()) {
                            return responseParts.getJSONObject(0).getString("text");
                        }
                    }
                }
                return "Erreur API: Format de réponse inattendu (pas de texte trouvé).";
            } else {
                // Return explicitly defined error string
                return "Erreur API (Code " + response.statusCode() + "): " + response.body();
            }

        } catch (Exception e) {
            // Return explicitly defined Java error string
            return "Erreur Java: " + e.getMessage();
        }
    }

    public String identifyDestinationFromImage(java.io.File imageFile) {
        try {
            // 1. Read and encode the image to Base64
            byte[] fileContent = java.nio.file.Files.readAllBytes(imageFile.toPath());
            String base64Image = java.util.Base64.getEncoder().encodeToString(fileContent);

            // 2. Build the exact JSON structure for multimodal request
            JSONObject textPart = new JSONObject();
            textPart.put("text",
                    "Identify the tourist destination or monument in this image. Return ONLY the city and country name (e.g., 'Paris, France' or 'Rome, Italy'). Do not write any other text or punctuation, just the location name.");

            JSONObject inlineData = new JSONObject();
            inlineData.put("mime_type", "image/jpeg");
            inlineData.put("data", base64Image);

            JSONObject imagePart = new JSONObject();
            imagePart.put("inline_data", inlineData);

            JSONArray partsArray = new JSONArray();
            partsArray.put(textPart); // The text instruction
            partsArray.put(imagePart); // The base64 image

            JSONObject contentObject = new JSONObject();
            contentObject.put("parts", partsArray);

            JSONArray contentsArray = new JSONArray();
            contentsArray.put(contentObject);

            JSONObject requestBody = new JSONObject();
            requestBody.put("contents", contentsArray);

            // 3. Create the HTTP Request perfectly matching the Multimodal API needs
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("X-goog-api-key", API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString(), StandardCharsets.UTF_8))
                    .build();

            // 4. Send the Request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // 5. Safely handle the response
            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                if (jsonResponse.has("candidates")) {
                    JSONArray candidates = jsonResponse.getJSONArray("candidates");
                    if (!candidates.isEmpty()) {
                        JSONObject firstCandidate = candidates.getJSONObject(0);
                        JSONObject contentObj = firstCandidate.getJSONObject("content");
                        JSONArray responseParts = contentObj.getJSONArray("parts");
                        if (!responseParts.isEmpty()) {
                            return responseParts.getJSONObject(0).getString("text").trim();
                        }
                    }
                }
                return "Erreur API: Format de réponse inattendu (pas de texte trouvé).";
            } else {
                // Return explicitly defined error string and response body
                return "Erreur API (Code " + response.statusCode() + "): " + response.body();
            }

        } catch (Exception e) {
            // Return explicitly defined Java error string for IO/Parsing issues
            return "Erreur Java: " + e.getMessage();
        }
    }
}
