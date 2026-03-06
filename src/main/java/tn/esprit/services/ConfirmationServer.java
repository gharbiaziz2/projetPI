package tn.esprit.services;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import tn.esprit.entities.ReservationTransport;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.concurrent.Executors;

/**
 * Lightweight HTTP server that handles reservation confirmation links from
 * emails.
 * Starts on port 8765 when the JavaFX app launches.
 * URL uses the machine's local IP address so phones on the same Wi-Fi can
 * access it.
 */
public class ConfirmationServer {

    private static final int PORT = 8765;
    private static HttpServer server;
    private static final ReservationVoyageServices rvService = new ReservationVoyageServices();
    private static String hostIp = "localhost";

    public static void start() {
        if (server != null)
            return; // already running

        // Determine the local IP reliably by opening a dummy UDP socket
        try (java.net.DatagramSocket socket = new java.net.DatagramSocket()) {
            socket.connect(java.net.InetAddress.getByName("8.8.8.8"), 10002);
            hostIp = socket.getLocalAddress().getHostAddress();
        } catch (Exception e) {
            System.err.println("[ConfirmationServer] Impossible de detecter l'IP: " + e.getMessage());
            hostIp = "192.168.190.11"; // Fallback to current known IP from ipconfig
        }

        try {
            // Bind to 0.0.0.0 to accept connections from other devices on the LAN
            server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);
            server.createContext("/confirm", ConfirmationServer::handleConfirm);
            server.setExecutor(Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "ConfirmationServer");
                t.setDaemon(true);
                return t;
            }));
            server.start();
            System.out.println("[ConfirmationServer] Démarré, accessible via http://" + hostIp + ":" + PORT);
        } catch (IOException e) {
            System.err.println("[ConfirmationServer] Erreur démarrage: " + e.getMessage());
        }
    }

    public static void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    /** Returns the confirmation URL for a given reservation ID. */
    public static String buildConfirmUrl(int idReservation) {
        return "http://" + hostIp + ":" + PORT + "/confirm?id=" + idReservation;
    }

    private static void handleConfirm(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery(); // e.g. "id=5"
        String html;
        int statusCode;

        if (query != null && query.startsWith("id=")) {
            try {
                int id = Integer.parseInt(query.substring(3));
                rvService.updateStatut(id, ReservationTransport.StatutReservation.CONFIRMEE);
                html = successPage();
                statusCode = 200;
                System.out.println("[ConfirmationServer] Réservation #" + id + " confirmée.");

                // Tell the UI to refresh if MyReservationsController is active
                javafx.application.Platform.runLater(() -> {
                    tn.esprit.gui.MyReservationsController.refreshIfActive();
                });

            } catch (NumberFormatException e) {
                html = errorPage("Identifiant de réservation invalide.");
                statusCode = 400;
            } catch (SQLException e) {
                html = errorPage("Erreur base de données: " + e.getMessage());
                statusCode = 500;
            }
        } else {
            html = errorPage("Paramètre manquant.");
            statusCode = 400;
        }

        byte[] bytes = html.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String successPage() {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
                "<title>Reservation confirmee</title>" +
                "<style>body{font-family:Arial,sans-serif;background:#f0f4ff;" +
                "display:flex;justify-content:center;align-items:center;min-height:100vh;margin:0;}" +
                ".box{background:#fff;border-radius:16px;padding:48px 56px;text-align:center;" +
                "box-shadow:0 8px 32px rgba(26,35,126,0.12);max-width:420px;}" +
                ".check{font-size:64px;margin-bottom:16px;}" +
                "h1{color:#1a237e;margin:0 0 12px;font-size:24px;}" +
                "p{color:#555;font-size:15px;line-height:1.6;margin:0;}" +
                "</style></head><body>" +
                "<div class='box'>" +
                "<div class='check'>&#10004;</div>" +
                "<h1>Reservation confirmee !</h1>" +
                "<p>Votre reservation de voyage a ete confirmee avec succes.<br>" +
                "Vous pouvez fermer cette page.</p>" +
                "</div></body></html>";
    }

    private static String errorPage(String msg) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
                "<title>Erreur</title>" +
                "<style>body{font-family:Arial,sans-serif;background:#fff5f5;" +
                "display:flex;justify-content:center;align-items:center;min-height:100vh;margin:0;}" +
                ".box{background:#fff;border-radius:16px;padding:48px 56px;text-align:center;" +
                "box-shadow:0 8px 32px rgba(200,0,0,0.08);max-width:420px;}" +
                "h1{color:#c62828;margin:0 0 12px;font-size:22px;}" +
                "p{color:#555;font-size:14px;}</style></head><body>" +
                "<div class='box'><h1>Erreur</h1><p>" + msg + "</p></div></body></html>";
    }
}
