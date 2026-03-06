package tn.esprit.services;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.sql.SQLException;
import java.util.Collections;
import java.util.concurrent.Executors;

/**
 * Embedded HTTP server that handles email confirmation link clicks.
 *
 * KEY CHANGE vs previous version:
 * - Binds to 0.0.0.0 (ALL network interfaces), not just localhost.
 * This means phones / other devices on the same WiFi can reach it.
 * - Port changed to 8081 to avoid conflicts with other services on 8080.
 * - getLocalIp() detects the PC's LAN IP automatically so the email
 * contains a reachable address (e.g. http://192.168.1.12:8081/confirm?id=25).
 *
 * Usage: ConfirmationHttpServer.start() — call once at app startup.
 * ConfirmationHttpServer.getBaseUrl() — use in EmailConfirmationService.
 */
public class ConfirmationHttpServer {

  private static final int[] PORTS = {8081, 8082, 8083, 8084, 8085};
  private static HttpServer server;
  private static String baseUrl; // e.g. http://192.168.1.12:8081
  private static int boundPort;

  // ─── Public API ──────────────────────────────────────────────────────────

  /** Starts the server bound to ALL network interfaces (0.0.0.0). Tries multiple ports if one is in use. */
  public static synchronized void start() {
    if (server != null) {
      System.out.println("[ConfirmationHttpServer] Already running at " + baseUrl);
      return;
    }
    IOException lastError = null;
    for (int port : PORTS) {
      try {
        server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 10);
        server.createContext("/confirm", ConfirmationHttpServer::handleConfirm);
        server.createContext("/stripe-success", ConfirmationHttpServer::handleStripeSuccess);
        server.createContext("/stripe-cancel", ConfirmationHttpServer::handleStripeCancel);
        server.setExecutor(Executors.newFixedThreadPool(4, r -> {
          Thread t = new Thread(r, "ConfirmHttpServer-Worker");
          t.setDaemon(true);
          return t;
        }));

        Thread serverThread = new Thread(server::start, "ConfirmHttpServer-Main");
        serverThread.setDaemon(true);
        serverThread.start();

        boundPort = port;
        baseUrl = "http://" + getLocalIp() + ":" + boundPort;
        System.out.println("[ConfirmationHttpServer] Listening on " + baseUrl + "/confirm?id=<ID>");
        System.out.println("[ConfirmationHttpServer] Also reachable as http://localhost:" + boundPort + "/confirm?id=<ID>");
        return;
      } catch (IOException e) {
        lastError = e;
        if (port == PORTS[PORTS.length - 1]) break;
        System.out.println("[ConfirmationHttpServer] Port " + port + " in use, trying next...");
      }
    }
    System.err.println("[ConfirmationHttpServer] Failed to start on any port: " + (lastError != null ? lastError.getMessage() : ""));
    if (lastError != null) lastError.printStackTrace();
  }

  /**
   * Returns the base URL to embed in confirmation emails.
   * Uses the PC's LAN IP, so phones on the same WiFi can reach the app.
   * Falls back to localhost if the server hasn't started yet.
   */
  public static String getBaseUrl() {
    return (baseUrl != null) ? baseUrl : ("http://localhost:" + PORTS[0]);
  }

  /** Stripe Checkout success redirect URL (after payment). */
  public static String getStripeSuccessUrl() {
    return getBaseUrl() + "/stripe-success";
  }

  /** Stripe Checkout cancel redirect URL (user cancelled). */
  public static String getStripeCancelUrl() {
    return getBaseUrl() + "/stripe-cancel";
  }

  /** Stops the server (daemon threads also stop on JVM exit automatically). */
  public static synchronized void stop() {
    if (server != null) {
      server.stop(0);
      server = null;
      System.out.println("[ConfirmationHttpServer] Stopped.");
    }
  }

  // ─── Request handler ─────────────────────────────────────────────────────

  private static void handleConfirm(HttpExchange exchange) throws IOException {
    // CORS header so mobile browsers don't block the response
    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

    if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
      sendResponse(exchange, 405, buildErrorPage("Méthode non autorisée."));
      return;
    }

    String query = exchange.getRequestURI().getQuery(); // "id=25"
    Integer reservationId = parseIdParam(query);

    if (reservationId == null) {
      sendResponse(exchange, 400, buildErrorPage("Paramètre 'id' manquant ou invalide."));
      return;
    }

    try {
      ReservationVoyageServices svc = new ReservationVoyageServices();
      svc.confirmerReservation(reservationId);
      System.out.println("[ConfirmationHttpServer] ✅ Reservation #" + reservationId + " CONFIRMED.");
      sendResponse(exchange, 200, buildSuccessPage(reservationId));
    } catch (SQLException e) {
      System.err.println("[ConfirmationHttpServer] DB error: " + e.getMessage());
      sendResponse(exchange, 500, buildErrorPage("Erreur base de données : " + e.getMessage()));
    }
  }

  private static void handleStripeSuccess(HttpExchange exchange) throws IOException {
    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
    sendResponse(exchange, 200, buildStripeSuccessPage());
  }

  private static void handleStripeCancel(HttpExchange exchange) throws IOException {
    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
    sendResponse(exchange, 200, buildStripeCancelPage());
  }

  private static String buildStripeSuccessPage() {
    return """
        <!DOCTYPE html>
        <html lang="fr">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width,initial-scale=1">
          <title>Paiement réussi</title>
          <style>
            * { box-sizing: border-box; margin: 0; padding: 0; }
            body {
              min-height: 100vh;
              display: flex; align-items: center; justify-content: center;
              background: linear-gradient(135deg,#1a3a5c,#2e6da4);
              font-family: 'Segoe UI', Arial, sans-serif;
              padding: 20px;
            }
            .card {
              background: #fff; border-radius: 20px;
              padding: 44px 48px; text-align: center;
              box-shadow: 0 20px 60px rgba(0,0,0,0.25);
              max-width: 460px; width: 100%%;
            }
            .icon  { font-size: 60px; margin-bottom: 18px; }
            h1    { color: #1a3a5c; font-size: 24px; margin-bottom: 10px; }
            p     { color: #4a5568; font-size: 15px; line-height: 1.6; }
            .sub  { margin-top: 20px; font-size: 12px; color: #a0aec0; }
          </style>
        </head>
        <body>
          <div class="card">
            <div class="icon">✅</div>
            <h1>Paiement réussi !</h1>
            <p>Votre paiement a été effectué avec succès. Votre réservation est en cours de traitement.</p>
            <p class="sub">Vous pouvez fermer cet onglet et retourner à l'application CarthageVoyage.</p>
          </div>
        </body>
        </html>
        """;
  }

  private static String buildStripeCancelPage() {
    return """
        <!DOCTYPE html>
        <html lang="fr">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width,initial-scale=1">
          <title>Paiement annulé</title>
          <style>
            * { box-sizing: border-box; margin: 0; padding: 0; }
            body {
              min-height: 100vh;
              display: flex; align-items: center; justify-content: center;
              background: linear-gradient(135deg,#4a5568,#2d3748);
              font-family: 'Segoe UI', Arial, sans-serif;
              padding: 20px;
            }
            .card {
              background: #fff; border-radius: 20px;
              padding: 44px 48px; text-align: center;
              box-shadow: 0 20px 60px rgba(0,0,0,0.25);
              max-width: 460px; width: 100%%;
            }
            .icon  { font-size: 60px; margin-bottom: 18px; }
            h1    { color: #4a5568; font-size: 24px; margin-bottom: 10px; }
            p     { color: #4a5568; font-size: 15px; line-height: 1.6; }
            .sub  { margin-top: 20px; font-size: 12px; color: #a0aec0; }
          </style>
        </head>
        <body>
          <div class="card">
            <div class="icon">ℹ️</div>
            <h1>Paiement annulé</h1>
            <p>Vous avez annulé le paiement. Aucun prélèvement n'a été effectué.</p>
            <p class="sub">Vous pouvez fermer cet onglet et retourner à l'application CarthageVoyage.</p>
          </div>
        </body>
        </html>
        """;
  }

  // ─── Network helper ──────────────────────────────────────────────────────

  /**
   * Finds the PC's LAN IPv4 address (e.g. 192.168.1.12).
   * Prefers non-loopback, non-virtual, WiFi/Ethernet addresses.
   * Falls back to "localhost" if nothing is found.
   */
  private static String getLocalIp() {
    try {
      for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
        if (ni.isLoopback() || !ni.isUp() || ni.isVirtual())
          continue;
        for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
          String ip = addr.getHostAddress();
          // Skip IPv6 and loopback
          if (ip.contains(":") || ip.startsWith("127."))
            continue;
          return ip;
        }
      }
    } catch (Exception ignored) {
    }
    return "localhost";
  }

  // ─── HTTP helpers ─────────────────────────────────────────────────────────

  private static Integer parseIdParam(String query) {
    if (query == null || query.isBlank())
      return null;
    for (String param : query.split("&")) {
      String[] kv = param.split("=", 2);
      if (kv.length == 2 && "id".equals(kv[0])) {
        try {
          return Integer.parseInt(kv[1]);
        } catch (NumberFormatException e) {
          return null;
        }
      }
    }
    return null;
  }

  private static void sendResponse(HttpExchange exchange, int status, String html) throws IOException {
    byte[] bytes = html.getBytes("UTF-8");
    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
    exchange.sendResponseHeaders(status, bytes.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(bytes);
    }
  }

  // ─── HTML pages ──────────────────────────────────────────────────────────

  private static String buildSuccessPage(int id) {
    return """
        <!DOCTYPE html>
        <html lang="fr">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width,initial-scale=1">
          <title>Réservation confirmée</title>
          <style>
            * { box-sizing: border-box; margin: 0; padding: 0; }
            body {
              min-height: 100vh;
              display: flex; align-items: center; justify-content: center;
              background: linear-gradient(135deg,#1a3a5c,#2e6da4);
              font-family: 'Segoe UI', Arial, sans-serif;
              padding: 20px;
            }
            .card {
              background: #fff; border-radius: 20px;
              padding: 44px 48px; text-align: center;
              box-shadow: 0 20px 60px rgba(0,0,0,0.25);
              max-width: 460px; width: 100%%;
            }
            .icon  { font-size: 60px; margin-bottom: 18px; }
            h1    { color: #1a3a5c; font-size: 24px; margin-bottom: 10px; }
            p     { color: #4a5568; font-size: 15px; line-height: 1.6; }
            .badge {
              display: inline-block;
              background: #e8f5e9; color: #2e7d32;
              border-radius: 50px; padding: 6px 20px;
              font-weight: bold; font-size: 14px; margin-top: 16px;
              letter-spacing: 1px;
            }
            .sub { margin-top: 20px; font-size: 12px; color: #a0aec0; }
          </style>
        </head>
        <body>
          <div class="card">
            <div class="icon">✅</div>
            <h1>Réservation confirmée !</h1>
            <p>Votre réservation <strong>#%d</strong> a été confirmée avec succès.</p>
            <div class="badge">CONFIRMÉE</div>
            <p class="sub">Vous pouvez fermer cet onglet.</p>
          </div>
        </body>
        </html>
        """.formatted(id);
  }

  private static String buildErrorPage(String message) {
    return """
        <!DOCTYPE html>
        <html lang="fr">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width,initial-scale=1">
          <title>Erreur</title>
          <style>
            * { box-sizing: border-box; margin: 0; padding: 0; }
            body {
              min-height: 100vh;
              display: flex; align-items: center; justify-content: center;
              background: linear-gradient(135deg,#5c1a1a,#a42e2e);
              font-family: 'Segoe UI', Arial, sans-serif;
              padding: 20px;
            }
            .card {
              background: #fff; border-radius: 20px;
              padding: 44px 48px; text-align: center;
              box-shadow: 0 20px 60px rgba(0,0,0,0.25);
              max-width: 460px; width: 100%%;
            }
            .icon { font-size: 60px; margin-bottom: 18px; }
            h1   { color: #5c1a1a; font-size: 22px; margin-bottom: 10px; }
            p    { color: #4a5568; font-size: 14px; }
          </style>
        </head>
        <body>
          <div class="card">
            <div class="icon">❌</div>
            <h1>Une erreur est survenue</h1>
            <p>%s</p>
          </div>
        </body>
        </html>
        """.formatted(message);
  }
}
