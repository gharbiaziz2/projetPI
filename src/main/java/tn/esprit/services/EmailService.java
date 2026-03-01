package tn.esprit.services;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.util.Properties;

/**
 * Sends HTML emails via Gmail SMTP on a background daemon thread.
 */
public class EmailService {

    private static final String FROM_EMAIL = "mohamedamine.seddiki@esprit.tn";
    private static final String APP_PASSWORD = "aualyzgwerurfiaj"; // spaces removed

    /**
     * Sends a reservation confirmation email with a Confirmer button.
     *
     * @param toEmail       recipient email address
     * @param userName      client full name
     * @param voyageType    type of the voyage
     * @param dateDepart    departure date string
     * @param dateRetour    return date string
     * @param montant       total price string
     * @param idReservation reservation ID used to build the confirm URL
     */
    public static void sendReservationConfirmation(String toEmail,
            String userName,
            String voyageType,
            String dateDepart,
            String dateRetour,
            String montant) {
        Thread t = new Thread(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
                    }
                });

                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(FROM_EMAIL, "Carthage Voyage"));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
                message.setSubject("Confirmation de votre reservation - Carthage Voyage");

                String html = buildHtml(userName, voyageType, dateDepart, dateRetour, montant);
                message.setContent(html, "text/html; charset=UTF-8");

                Transport.send(message);
                System.out.println("[EmailService] Email envoye a " + toEmail);

            } catch (Exception e) {
                System.err.println("[EmailService] Echec envoi email: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    Alert err = new Alert(Alert.AlertType.ERROR);
                    err.setTitle("Email non envoye");
                    err.setHeaderText("Impossible d'envoyer l'email de confirmation");
                    err.setContentText("Erreur SMTP: " + e.getMessage());
                    err.showAndWait();
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private static String buildHtml(String userName, String voyageType,
            String dateDepart, String dateRetour,
            String montant) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'/></head>" +
                "<body style='margin:0;padding:0;background:#f4f6fb;font-family:Arial,sans-serif;'>" +
                "<table width='100%' cellpadding='0' cellspacing='0'>" +
                "<tr><td align='center' style='padding:40px 0;'>" +
                "<table width='580' cellpadding='0' cellspacing='0' style='" +
                "background:#ffffff;border-radius:12px;overflow:hidden;" +
                "box-shadow:0 4px 20px rgba(0,0,0,0.08);'>" +
                // Header
                "<tr><td style='background:linear-gradient(135deg,#1a237e,#283593);" +
                "padding:36px 40px;text-align:center;'>" +
                "<h1 style='color:#ffffff;margin:0;font-size:26px;letter-spacing:1px;'>" +
                "Carthage Voyage</h1>" +
                "<p style='color:#e8eaf6;margin:8px 0 0;font-size:14px;'>" +
                "Reservation recue avec succes</p></td></tr>" +
                // Body
                "<tr><td style='padding:36px 40px;'>" +
                "<p style='color:#333;font-size:16px;margin:0 0 20px;'>Bonjour <strong>" +
                userName + "</strong>,</p>" +
                "<p style='color:#555;font-size:14px;line-height:1.7;margin:0 0 24px;'>" +
                "Nous avons bien recu votre reservation. Elle est actuellement en attente de confirmation." +
                "<br>Vous pourrez la confirmer depuis <strong>Mes Reservations</strong>.</p>" +
                // Details box
                "<table width='100%' cellpadding='0' cellspacing='0' style='" +
                "background:#f8f9ff;border-radius:8px;border:1px solid #e3e7ff;margin-bottom:28px;'>" +
                "<tr><td style='padding:20px 24px;'>" +
                "<table width='100%' cellpadding='6' cellspacing='0'>" +
                row("Voyage", voyageType) +
                row("Depart", dateDepart) +
                row("Retour", dateRetour) +
                row("Montant", montant + " DT") +
                row("Statut", "EN ATTENTE") +
                "</table></td></tr></table>" +
                "<p style='color:#777;font-size:13px;line-height:1.6;'>" +
                "Merci de nous faire confiance.<br>" +
                "<strong style='color:#1a237e;'>L'equipe Carthage Voyage</strong></p>" +
                "</td></tr>" +
                // Footer
                "<tr><td style='background:#f0f2ff;padding:18px 40px;text-align:center;'>" +
                "<p style='color:#aaa;font-size:12px;margin:0;'>" +
                "2026 Carthage Voyage - Tous droits reserves</p>" +
                "</td></tr>" +
                "</table></td></tr></table></body></html>";
    }

    private static String row(String label, String value) {
        return "<tr>" +
                "<td style='color:#555;font-size:13px;font-weight:bold;white-space:nowrap;" +
                "padding-right:16px;'>" + label + "</td>" +
                "<td style='color:#1a1a2e;font-size:13px;'>" +
                (value != null ? value : "-") + "</td>" +
                "</tr>";
    }
}
