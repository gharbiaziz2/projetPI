package tn.esprit.services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class ItineraryGeneratorService {

    private static final String API_KEY = "AIzaSyCdCrbPjyBOISslG6EqZLFXtnjDL25jUL4";

    private final HttpClient httpClient;

    public ItineraryGeneratorService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

    /**
     * Calls Google Gemini API to generate an itinerary or answer travel-related
     * questions.
     * Enforces a strict Travel Agent persona using the system prompt.
     *
     * @param userInput The travel question or itinerary request from the user.
     * @return The AI-generated response text.
     * @throws Exception If the API call or parsing fails.
     */
    public String generateItinerary(String userInput) throws Exception {
        String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key="
                + API_KEY;

        // Build the JSON request body
        JSONObject requestBody = new JSONObject();

        // 1. Define the Strict Context using system_instruction
        JSONObject systemInstruction = new JSONObject();
        JSONObject systemPart = new JSONObject();
        systemPart.put("text",
                "You are a professional Travel Agent. You ONLY answer questions related to travel, tourism, and destinations. If the user asks about coding, math, cooking, or anything non-travel related, you must politely refuse and state clearly that you ONLY assist with travel.");

        JSONArray systemPartsArray = new JSONArray();
        systemPartsArray.put(systemPart);
        systemInstruction.put("parts", systemPartsArray);

        // Use camelCase systemInstruction for REST API
        requestBody.put("systemInstruction", systemInstruction);

        // 2. Add the User's input into the structure
        JSONObject contentObject = new JSONObject();
        JSONArray partsArray = new JSONArray();
        JSONObject textPart = new JSONObject();
        textPart.put("text", userInput);

        partsArray.put(textPart);
        contentObject.put("parts", partsArray);

        JSONArray contentsArray = new JSONArray();
        contentsArray.put(contentObject);
        requestBody.put("contents", contentsArray);

        // Create the HTTP Request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                // Make sure to use UTF-8 encoder for special characters (accents, emojis)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString(), StandardCharsets.UTF_8))
                .build();

        // Send the HTTP Request and receive response as a String
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            // Parse JSON response safely
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
            throw new Exception("Received success status but no content found in the Gemini response.");
        } else {
            // If API Error (400, 401, etc.), print explicitly to console and throw
            // exception
            String errorMsg = "Gemini API Error (" + response.statusCode() + "): " + response.body();
            System.err.println("--- GEMINI HTTP ERROR ---");
            System.err.println("Status Code: " + response.statusCode());
            System.err.println("Response Body: " + response.body());
            System.err.println("-------------------------");
            throw new Exception(errorMsg);
        }
    }
}
