package tn.esprit.services;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

/**
 * Sends an HTML confirmation email containing a "Confirm Reservation" button.
 *
 * SETUP:
 * 1. Replace EMAIL_FROM with your Gmail address.
 * 2. Replace EMAIL_PASSWORD with a Gmail App Password (NOT your regular
 * password).
 * To generate one: Google Account → Security → 2-Step Verification → App
 * Passwords.
 */
public class EmailConfirmationService {

  // ─── CONFIGURE THESE TWO CONSTANTS ───────────────────────────────────────
  private static final String EMAIL_FROM = "mohamedamine.seddiki@esprit.tn";
  private static final String EMAIL_PASSWORD = "aual yzgw erur fiaj";
  // ─────────────────────────────────────────────────────────────────────────

  private static String getConfirmUrl(int reservationId) {
    // Uses the server's actual LAN IP+port — works from phones on the same WiFi
    return ConfirmationHttpServer.getBaseUrl() + "/confirm?id=" + reservationId;
  }

  /**
   * Sends a styled HTML confirmation email to the user.
   *
   * @param toEmail       Recipient's email address (e.g. from User.getEmail())
   * @param toName        Recipient's display name (e.g. "Youssef Eleuch")
   * @param reservationId The ID returned by ajouterAndReturnId()
   */
  public static void sendConfirmationEmail(String toEmail,
      String toName,
      int reservationId) {
    // Run on a background thread so the JavaFX UI never freezes
    new Thread(() -> {
      try {
        Session session = buildSession();
        Message message = buildMessage(session, toEmail, toName, reservationId);
        Transport.send(message);
        System.out.println("[EmailConfirmationService] Confirmation email sent to " + toEmail);
      } catch (MessagingException | UnsupportedEncodingException e) {
        System.err.println("[EmailConfirmationService] Failed to send email: " + e.getMessage());
        e.printStackTrace();
      }
    }, "EmailSender-Thread").start();
  }

  // ─── Private helpers ──────────────────────────────────────────────────────

  private static Session buildSession() {
    Properties props = new Properties();
    props.put("mail.smtp.host", "smtp.gmail.com");
    props.put("mail.smtp.port", "587");
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable", "true");

    return Session.getInstance(props, new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD);
      }
    });
  }

  private static Message buildMessage(Session session,
      String toEmail,
      String toName,
      int reservationId) throws MessagingException, UnsupportedEncodingException {
    String confirmUrl = getConfirmUrl(reservationId);

    MimeMessage msg = new MimeMessage(session);
    msg.setFrom(new InternetAddress(EMAIL_FROM, "CarthageVoyage", "UTF-8"));
    msg.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail, toName, "UTF-8"));
    msg.setSubject("Confirmez votre réservation — CarthageVoyage", "UTF-8");
    msg.setContent(buildHtmlBody(toName, reservationId, confirmUrl), "text/html; charset=UTF-8");
    return msg;
  }

  private static String buildHtmlBody(String name, int resId, String confirmUrl) {
    return """
        <!DOCTYPE html>
        <html lang="fr">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
        </head>
        <body style="margin:0;padding:0;background-color:#f4f7fb;font-family:'Segoe UI',Arial,sans-serif;">
          <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f7fb;padding:40px 0;">
            <tr><td align="center">
              <table width="600" cellpadding="0" cellspacing="0"
                     style="background:#ffffff;border-radius:16px;overflow:hidden;
                            box-shadow:0 4px 24px rgba(0,0,0,0.08);">
                <!-- Header -->
                <tr>
                  <td style="background:linear-gradient(135deg,#1a3a5c,#2e6da4);
                             padding:36px 40px;text-align:center;">
                    <h1 style="color:#ffffff;margin:0;font-size:26px;letter-spacing:1px;">
                      ✈️ CarthageVoyage
                    </h1>
                    <p style="color:#a8c8f0;margin:8px 0 0;font-size:14px;">
                      Votre agence de voyage de confiance
                    </p>
                  </td>
                </tr>
                <!-- Body -->
                <tr>
                  <td style="padding:40px;">
                    <h2 style="color:#1a3a5c;margin:0 0 16px;font-size:20px;">
                      Bonjour %s,
                    </h2>
                    <p style="color:#4a5568;font-size:15px;line-height:1.7;margin:0 0 12px;">
                      Votre réservation <strong>#%d</strong> a bien été enregistrée avec
                      le statut <span style="color:#e67e22;font-weight:bold;">EN ATTENTE</span>.
                    </p>
                    <p style="color:#4a5568;font-size:15px;line-height:1.7;margin:0 0 32px;">
                      Pour finaliser votre réservation, veuillez cliquer sur le bouton ci-dessous :
                    </p>
                    <!-- CTA Button -->
                    <div style="text-align:center;margin:0 0 36px;">
                      <a href="%s"
                         style="display:inline-block;background:linear-gradient(135deg,#2e6da4,#1a3a5c);
                                color:#ffffff;text-decoration:none;font-size:16px;font-weight:bold;
                                padding:16px 40px;border-radius:50px;
                                box-shadow:0 4px 15px rgba(46,109,164,0.4);
                                letter-spacing:0.5px;">
                        ✅ Confirmer ma réservation
                      </a>
                    </div>
                    <p style="color:#a0aec0;font-size:13px;text-align:center;margin:0;">
                      Ce lien est valable uniquement si votre application CarthageVoyage est ouverte.
                    </p>
                  </td>
                </tr>
                <!-- Footer -->
                <tr>
                  <td style="background:#f4f7fb;padding:20px 40px;text-align:center;
                             border-top:1px solid #e2e8f0;">
                    <p style="color:#a0aec0;font-size:12px;margin:0;">
                      © 2026 CarthageVoyage. Tous droits réservés.
                    </p>
                  </td>
                </tr>
              </table>
            </td></tr>
          </table>
        </body>
        </html>
        """.formatted(name, resId, confirmUrl);
  }
}
