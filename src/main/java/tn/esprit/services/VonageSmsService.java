package tn.esprit.services;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Sends SMS via Vonage (Nexmo) API.
 * Config: vonage.api.key, vonage.api.secret, vonage.from (sender ID, optional, default CarthageVoyage)
 */
public class VonageSmsService {

    private static final String VONAGE_SMS_URL = "https://rest.nexmo.com/sms/json";

    private final String apiKey;
    private final String apiSecret;
    private final String from;
    private final HttpClient httpClient = HttpClient.newBuilder().build();

    public VonageSmsService() throws IOException {
        Properties props = new Properties();
        try (var is = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) props.load(is);
        }
        this.apiKey = props.getProperty("vonage.api.key", "").trim();
        this.apiSecret = props.getProperty("vonage.api.secret", "").trim();
        this.from = props.getProperty("vonage.from", "CarthageVoyage").trim();
    }

    /**
     * Sends an SMS to the given phone number. Phone must be in E.164 format (e.g. +21612345678).
     */
    public boolean sendSms(String to, String text) {
        if (apiKey.isEmpty() || apiSecret.isEmpty()) {
            System.err.println("Vonage: API key/secret not configured in config.properties");
            return false;
        }
        if (to == null || to.isBlank() || text == null || text.isBlank()) {
            return false;
        }
        String normalizedTo = normalizePhone(to);
        if (normalizedTo == null) return false;

        String body = "api_key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8) +
                "&api_secret=" + URLEncoder.encode(apiSecret, StandardCharsets.UTF_8) +
                "&from=" + URLEncoder.encode(from, StandardCharsets.UTF_8) +
                "&to=" + URLEncoder.encode(normalizedTo, StandardCharsets.UTF_8) +
                "&text=" + URLEncoder.encode(text, StandardCharsets.UTF_8);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(VONAGE_SMS_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Vonage SMS failed: HTTP " + response.statusCode() + " " + response.body());
                return false;
            }
            String bodyResp = response.body();
            if (bodyResp != null && bodyResp.contains("\"status\":\"0\"")) {
                return true;
            }
            if (bodyResp != null && bodyResp.contains("Bad Credentials")) {
                System.err.println("Vonage: Bad Credentials. Vérifiez vonage.api.key et vonage.api.secret dans config.properties. Récupérez le secret sur https://dashboard.nexmo.com");
            } else {
                System.err.println("Vonage SMS response: " + bodyResp);
            }
            return false;
        } catch (IOException | InterruptedException e) {
            System.err.println("Vonage SMS error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Normalizes phone to E.164. Handles +216..., 216..., 12345678 (assume Tunisia).
     */
    private String normalizePhone(String phone) {
        if (phone == null) return null;
        String p = phone.trim().replaceAll("\\s+", "").replaceAll("-", "");
        if (p.startsWith("+")) {
            return p;
        }
        if (p.startsWith("216") && p.length() >= 11) {
            return "+" + p;
        }
        if (p.matches("\\d{8}")) {
            return "+216" + p;
        }
        if (p.matches("\\d{10,15}")) {
            return "+" + p;
        }
        return p.matches("\\d+") ? "+" + p : null;
    }
}
