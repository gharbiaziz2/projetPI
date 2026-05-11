package tn.esprit.services;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import java.util.Properties;

/**
 * Stripe Checkout for voyage bundle payments.
 * Config: stripe.api.key.sk (secret) in config.properties
 */
public class StripeService {
    private final String secretKey;
    private final String successUrl;
    private final String cancelUrl;
    private final double tndToEurRate;

    public StripeService() {
        Properties props = new Properties();
        try (var is = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) props.load(is);
        } catch (Exception e) { }
        this.secretKey = props.getProperty("stripe.api.key.sk", "").trim();
        this.successUrl = props.getProperty("stripe.success.url", "").trim();
        this.cancelUrl = props.getProperty("stripe.cancel.url", "").trim();
        String rateStr = props.getProperty("stripe.tnd.to.eur.rate", "0.30").trim();
        double rate;
        try {
            rate = Double.parseDouble(rateStr);
        } catch (NumberFormatException e) {
            rate = 0.30;
        }
        this.tndToEurRate = rate;
    }

    /**
     * Creates a Stripe Checkout Session for the given amount in TND.
     * Converts TND to EUR (Stripe does not support TND by default).
     * @param amountTnd Total amount in Tunisian Dinars
     * @param description Description for the payment (e.g. "Réservation voyage CarthageVoyage")
     * @return Checkout URL to open in browser, or null if config/API error
     */
    public String createCheckoutSession(double amountTnd, String description) {
        if (secretKey.isEmpty()) return null;
        if (amountTnd <= 0) return null;
        try {
            Stripe.apiKey = secretKey;
            double amountEur = amountTnd * tndToEurRate;
            long amountCents = Math.round(amountEur * 100);
            if (amountCents < 50) amountCents = 50;

            SessionCreateParams.LineItem.PriceData.ProductData productData =
                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                            .setName(description != null && !description.isBlank() ? description : "Réservation CarthageVoyage")
                            .build();

            SessionCreateParams.LineItem.PriceData priceData =
                    SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency("eur")
                            .setUnitAmount(amountCents)
                            .setProductData(productData)
                            .build();

            SessionCreateParams.LineItem lineItem =
                    SessionCreateParams.LineItem.builder()
                            .setPriceData(priceData)
                            .setQuantity(1L)
                            .build();

            String success = successUrl.isEmpty() ? ConfirmationHttpServer.getStripeSuccessUrl() : successUrl;
            String cancel = cancelUrl.isEmpty() ? ConfirmationHttpServer.getStripeCancelUrl() : cancelUrl;
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .addLineItem(lineItem)
                    .setSuccessUrl(success)
                    .setCancelUrl(cancel)
                    .build();

            Session session = Session.create(params);
            return session.getUrl();
        } catch (Exception e) {
            System.err.println("Stripe error: " + e.getMessage());
            return null;
        }
    }
}
