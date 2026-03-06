package tn.esprit.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Google OAuth 2.0 service for JavaFX desktop app.
 * Uses local HTTP server to receive the authorization code callback.
 * <p>
 * IMPORTANT: Add these redirect URIs in Google Cloud Console (APIs & Services → Credentials):
 * - http://localhost:45678
 * - http://127.0.0.1:45678
 */
public class GoogleOAuthService {

    private static final int REDIRECT_PORT = 45678;
    private static final String REDIRECT_URI = "http://localhost:" + REDIRECT_PORT;
    private static final String SCOPE = "openid email profile";
    private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

    private final String clientId;
    private final String clientSecret;
    private final String authUri;
    private final String tokenUri;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder().build();

    public GoogleOAuthService() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/config/google-credentials.json")) {
            if (is == null) throw new IOException("google-credentials.json not found in resources/config/");
            JsonNode root = objectMapper.readTree(is);
            JsonNode installed = root.get("installed");
            if (installed == null) installed = root;
            this.clientId = installed.get("client_id").asText();
            this.clientSecret = installed.get("client_secret").asText();
            this.authUri = installed.get("auth_uri").asText();
            this.tokenUri = installed.get("token_uri").asText();
        }
    }

    /**
     * Initiates OAuth flow: starts local server, returns auth URL to open in browser.
     * Use {@link #waitForCode()} (or the future) to get the authorization code.
     */
    public String getAuthorizationUrl() {
        String state = Long.toHexString(System.currentTimeMillis());
        return authUri +
                "?client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                "&response_type=code" +
                "&scope=" + URLEncoder.encode(SCOPE, StandardCharsets.UTF_8) +
                "&state=" + state +
                "&access_type=offline" +
                "&prompt=consent";
    }

    /**
     * Exchanges authorization code for access token, then fetches user info.
     */
    public GoogleUserInfo exchangeCodeAndGetUserInfo(String code) throws IOException, InterruptedException {
        String tokenBody = "code=" + URLEncoder.encode(code, StandardCharsets.UTF_8) +
                "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                "&grant_type=authorization_code";

        HttpRequest tokenRequest = HttpRequest.newBuilder()
                .uri(URI.create(tokenUri))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(tokenBody))
                .build();

        HttpResponse<String> tokenResponse = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
        if (tokenResponse.statusCode() != 200) {
            throw new IOException("Token exchange failed: " + tokenResponse.body());
        }

        JsonNode tokenJson = objectMapper.readTree(tokenResponse.body());
        String accessToken = tokenJson.get("access_token").asText();

        HttpRequest userRequest = HttpRequest.newBuilder()
                .uri(URI.create(USERINFO_URL))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> userResponse = httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString());
        if (userResponse.statusCode() != 200) {
            throw new IOException("UserInfo failed: " + userResponse.body());
        }

        JsonNode userJson = objectMapper.readTree(userResponse.body());
        String email = userJson.has("email") ? userJson.get("email").asText() : null;
        String name = userJson.has("name") ? userJson.get("name").asText() : "";
        String picture = userJson.has("picture") ? userJson.get("picture").asText() : null;
        String givenName = userJson.has("given_name") ? userJson.get("given_name").asText() : "";
        String familyName = userJson.has("family_name") ? userJson.get("family_name").asText() : "";

        String nom = !familyName.isEmpty() ? familyName : (name.contains(" ") ? name.substring(name.lastIndexOf(' ') + 1) : name);
        String prenom = !givenName.isEmpty() ? givenName : (name.contains(" ") ? name.substring(0, name.indexOf(' ')) : name);

        return new GoogleUserInfo(email, nom, prenom, picture);
    }

    /**
     * Starts a local HTTP server and returns a CompletableFuture that completes with the authorization code
     * when the user finishes the OAuth flow in the browser. Open the auth URL in the browser before/after calling.
     */
    public CompletableFuture<String> startLocalServerAndWaitForCode() {
        CompletableFuture<String> future = new CompletableFuture<>();
        AtomicReference<com.sun.net.httpserver.HttpServer> serverRef = new AtomicReference<>();

        try {
            com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(
                    new java.net.InetSocketAddress(REDIRECT_PORT), 0);

            server.createContext("/", exchange -> {
                String query = exchange.getRequestURI().getQuery();
                String responseHtml;
                int status;

                if (query != null && query.contains("code=")) {
                    String code = extractParam(query, "code");
                    if (code != null) {
                        future.complete(code);
                        responseHtml = """
                            <html><body style="font-family:sans-serif;text-align:center;padding:40px;">
                            <h2 style="color:#27ae60;">Connexion réussie !</h2>
                            <p>Vous pouvez fermer cette fenêtre et revenir à l'application.</p>
                            </body></html>""";
                        status = 200;
                    } else {
                        future.completeExceptionally(new IOException("No code in callback"));
                        responseHtml = "<html><body><h2>Erreur: code manquant</h2></body></html>";
                        status = 400;
                    }
                } else if (query != null && query.contains("error=")) {
                    String error = extractParam(query, "error");
                    future.completeExceptionally(new IOException("OAuth error: " + error));
                    responseHtml = "<html><body><h2>Erreur d'authentification</h2><p>" + error + "</p></body></html>";
                    status = 400;
                } else {
                    responseHtml = "<html><body><h2>En attente...</h2></body></html>";
                    status = 200;
                }

                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(status, responseHtml.getBytes(StandardCharsets.UTF_8).length);
                exchange.getResponseBody().write(responseHtml.getBytes(StandardCharsets.UTF_8));
                exchange.close();

                com.sun.net.httpserver.HttpServer s = serverRef.get();
                if (s != null) {
                    s.stop(0);
                }
            });

            server.setExecutor(null);
            server.start();
            serverRef.set(server);

            future.whenComplete((code, ex) -> {
                com.sun.net.httpserver.HttpServer s = serverRef.get();
                if (s != null) {
                    s.stop(0);
                }
            });

        } catch (IOException e) {
            future.completeExceptionally(e);
        }

        return future;
    }

    private String extractParam(String query, String key) {
        for (String param : query.split("&")) {
            if (param.startsWith(key + "=")) {
                return param.substring(key.length() + 1);
            }
        }
        return null;
    }

    public static final class GoogleUserInfo {
        private final String email;
        private final String nom;
        private final String prenom;
        private final String photoUrl;

        public GoogleUserInfo(String email, String nom, String prenom, String photoUrl) {
            this.email = Objects.requireNonNull(email, "email");
            this.nom = nom != null ? nom : "";
            this.prenom = prenom != null ? prenom : "";
            this.photoUrl = photoUrl;
        }

        public String getEmail() { return email; }
        public String getNom() { return nom; }
        public String getPrenom() { return prenom; }
        public String getPhotoUrl() { return photoUrl; }
    }
}
