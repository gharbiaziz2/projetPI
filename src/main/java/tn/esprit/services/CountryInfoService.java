package tn.esprit.services;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import tn.esprit.dto.CountryInfoDto;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service that calls the free RestCountries API to retrieve country
 * information.
 * Endpoint: GET https://restcountries.com/v3.1/name/{countryName}
 *
 * Uses java.net.http.HttpClient (Java 11+) and Jackson for JSON parsing.
 * No Spring dependency required.
 */
public class CountryInfoService {

    private static final String BASE_URL = "https://restcountries.com/v3.1/name/";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /** Constructor injection — easy to unit-test. */
    public CountryInfoService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Fetches country info from the RestCountries API.
     *
     * @param countryName the country name (in English, e.g. "France", "Tunisia")
     * @return a {@link CountryInfoDto} with the simplified country data
     * @throws CountryNotFoundException if the API returns 404 or an empty list
     * @throws RuntimeException         for network or parsing errors
     */
    public CountryInfoDto getCountryInfo(String countryName) {
        if (countryName == null || countryName.isBlank()) {
            throw new IllegalArgumentException("Le nom du pays ne peut pas être vide.");
        }

        String encoded = URLEncoder.encode(countryName.trim(), StandardCharsets.UTF_8);
        String url = BASE_URL + encoded + "?fields=name,capital,currencies,languages,flags,timezones,maps";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                throw new CountryNotFoundException(countryName);
            }
            if (response.statusCode() != 200) {
                throw new RuntimeException("Erreur API RestCountries (HTTP " + response.statusCode() + ")");
            }

            // Parse the JSON array
            List<RawCountry> countries = objectMapper.readValue(
                    response.body(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, RawCountry.class));

            if (countries == null || countries.isEmpty()) {
                throw new CountryNotFoundException(countryName);
            }

            return mapToDto(countries.get(0));

        } catch (CountryNotFoundException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Impossible de contacter l'API RestCountries : " + e.getMessage(), e);
        }
    }

    // ─── Mapping ──────────────────────────────────────────────────────────────

    private CountryInfoDto mapToDto(RawCountry raw) {
        // Common name
        String commonName = (raw.name != null && raw.name.common != null)
                ? raw.name.common
                : "—";

        // Capital (first element if exists)
        String capital = (raw.capital != null && !raw.capital.isEmpty())
                ? raw.capital.get(0)
                : "—";

        // Currency (take the first currency in the map)
        String currencyName = "—";
        String currencySymbol = "—";
        if (raw.currencies != null && !raw.currencies.isEmpty()) {
            Map.Entry<String, RawCurrency> entry = raw.currencies.entrySet().iterator().next();
            RawCurrency cur = entry.getValue();
            if (cur.name != null)
                currencyName = cur.name;
            if (cur.symbol != null)
                currencySymbol = cur.symbol;
        }

        // Languages (values of the map)
        List<String> languages = new ArrayList<>();
        if (raw.languages != null) {
            languages.addAll(raw.languages.values());
        }

        // Flag URL: prefer PNG (JavaFX does NOT support SVG), fall back to SVG
        String flagUrl = "—";
        if (raw.flags != null) {
            flagUrl = raw.flags.png != null ? raw.flags.png
                    : (raw.flags.svg != null ? raw.flags.svg : "—");
        }

        // Timezones (first one shown, or all joined)
        List<String> timezones = new ArrayList<>();
        if (raw.timezones != null) {
            timezones.addAll(raw.timezones);
        }

        // Google Maps URL
        String googleMapsUrl = (raw.maps != null && raw.maps.googleMaps != null)
                ? raw.maps.googleMaps
                : null;

        return new CountryInfoDto(commonName, capital, currencyName, currencySymbol,
                languages, flagUrl, timezones, googleMapsUrl);
    }

    // ─── Internal JSON mapping classes (ignored outside this file) ────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class RawCountry {
        public RawName name;
        public List<String> capital;
        public Map<String, RawCurrency> currencies;
        public Map<String, String> languages;
        public RawFlags flags;
        public List<String> timezones;
        public RawMaps maps;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class RawMaps {
        public String googleMaps;
        public String openStreetMaps;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class RawName {
        public String common;
        public String official;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class RawCurrency {
        public String name;
        public String symbol;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class RawFlags {
        public String png;
        public String svg;
    }
}
