package tn.esprit.utils;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.List;
import java.util.Properties;
import java.math.BigDecimal;

public class EmailUtil {

    // Renseignez vos identifiants d'envoi. Pour Gmail, utilisez un "Mot de passe d'application".
    private static final String SENDER_EMAIL = "mohamedamine.seddiki@esprit.tn"; 
    private static final String SENDER_PASSWORD = "aual yzgw erur fiaj"; 

    /**
     * Envoie la promotion de manière asynchrone (nouveau Thread).
     */
    public static void envoyerPromoEmail(List<String> emailsClients, String destination, BigDecimal nouveauPrix) {
        if (emailsClients == null || emailsClients.isEmpty()) {
            return;
        }

        // Création du Thread classique
        Thread emailThread = new Thread(() -> {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
                }
            });

            try {
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SENDER_EMAIL));

                // Ajout des destinataires en BCC (Copie cachée) pour éviter le partage d'emails entre clients
                InternetAddress[] bccAddresses = new InternetAddress[emailsClients.size()];
                for (int i = 0; i < emailsClients.size(); i++) {
                    bccAddresses[i] = new InternetAddress(emailsClients.get(i));
                }
                message.setRecipients(Message.RecipientType.BCC, bccAddresses);

                message.setSubject("✈️ VENTE FLASH ! Promotion immanquable vers " + destination);
                
                String htmlContent = "<div style='font-family: \"Helvetica Neue\", Helvetica, Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 8px rgba(0,0,0,0.05);'>"
                        + "<div style='background-color: #0f172a; padding: 25px; text-align: center;'>" // Couleur sombre style CarthageVoyage (votre dashboard)
                        + "<h1 style='color: #ffffff; margin: 0; font-size: 24px; font-weight: 600; letter-spacing: 1px;'>CARTHAGE VOYAGE</h1>"
                        + "</div>"
                        + "<div style='padding: 30px; text-align: center; background-color: #ffffff;'>"
                        + "<h2 style='color: #e11d48; font-size: 26px; margin-top: 0;'>🔥 Dernière minute vers " + destination + " !</h2>"
                        + "<p style='color: #475569; font-size: 16px; line-height: 1.6; margin-bottom: 25px;'>Ne ratez pas cette occasion unique. Profitez d'une réduction exclusive de <strong>20%</strong> sur notre voyage vers <strong>" + destination + "</strong>.</p>"
                        + "<div style='background-color: #f8fafc; border-left: 4px solid #10b981; padding: 20px; margin: 25px 0; border-radius: 4px;'>"
                        + "<p style='margin: 0; color: #64748b; font-size: 14px; text-transform: uppercase; font-weight: bold;'>Votre NOUVEAU Tarif</p>"
                        + "<p style='margin: 10px 0 0 0; color: #10b981; font-size: 32px; font-weight: bold;'>" + String.format("%.2f", nouveauPrix) + " DNT</p>"
                        + "</div>"
                        + "<p style='color: #ef4444; font-weight: bold; font-size: 15px;'>⏳ Faites vite, très peu de places sont encore disponibles à ce tarif !</p>"
                        + "<a href='#' style='display: inline-block; background-color: #2563eb; color: #ffffff; text-decoration: none; padding: 14px 32px; border-radius: 30px; font-weight: bold; font-size: 16px; margin-top: 15px; box-shadow: 0 4px 6px rgba(37, 99, 235, 0.2);'>Réserver maintenant</a>"
                        + "</div>"
                        + "<div style='background-color: #f1f5f9; padding: 15px; text-align: center; color: #94a3b8; font-size: 12px;'>"
                        + "<p style='margin: 0;'>Cet email a été généré automatiquement par le système d'alerte Carthage Voyage.</p>"
                        + "</div>"
                        + "</div>";
                
                message.setContent(htmlContent, "text/html; charset=utf-8");

                Transport.send(message);
                System.out.println("Succès : Les emails de promotion pour " + destination + " ont été envoyés en arrière-plan.");

            } catch (MessagingException e) {
                System.err.println("Erreur (MessagingException) lors de l'envoi de l'email : " + e.getMessage());
                e.printStackTrace();
            }
        });

        // Démarrage du thread (non-bloquant pour JavaFX)
        emailThread.setDaemon(true); 
        emailThread.start();
    }
}
