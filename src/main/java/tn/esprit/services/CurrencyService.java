package tn.esprit.services;

import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class CurrencyService {

    private static final String API_KEY = "31cafad1134b2327bff5c30d";
    private static final String BASE_URL = "https://v6.exchangerate-api.com/v6/" + API_KEY + "/latest/";

    private final HttpClient httpClient;

    public CurrencyService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

    /**
     * Converts an amount from one currency to another using ExchangeRate-API.
     *
     * @param fromCurrency The base currency code (e.g., "TND")
     * @param toCurrency   The target currency code (e.g., "EUR")
     * @param amount       The amount to convert
     * @return The converted amount
     * @throws Exception If the API call fails or JSON parsing errors occur
     */
    public double convertCurrency(String fromCurrency, String toCurrency, double amount) throws Exception {
        // Build the API URL with the base currency
        String urlString = BASE_URL + fromCurrency.toUpperCase();

        // Create the HTTP Request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlString))
                .GET()
                .build();

        // Send the Request and get the response as a String
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            // Parse the JSON response
            JSONObject jsonObject = new JSONObject(response.body());

            // Check if API result was successful
            if ("success".equals(jsonObject.optString("result"))) {
                // Get the conversion rates object
                JSONObject conversionRates = jsonObject.getJSONObject("conversion_rates");

                // Extract the specific rate for our target currency
                if (!conversionRates.has(toCurrency.toUpperCase())) {
                    throw new Exception("Currency " + toCurrency + " is not supported by the API.");
                }

                double rate = conversionRates.getDouble(toCurrency.toUpperCase());

                // Calculate and return the final converted amount
                return amount * rate;
            } else {
                String errorType = jsonObject.optString("error-type", "Unknown Error");
                throw new Exception("ExchangeRate-API failed with error type: " + errorType);
            }
        } else {
            throw new Exception("HTTP GET failed. Status code: " + response.statusCode());
        }
    }
}
