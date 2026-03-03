package tn.esprit.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Fetches weather forecast from WeatherAPI.com (weatherapi.com).
 * Config: weather.api.key in config.properties
 */
public class WeatherService {

    private static final String BASE_URL = "https://api.weatherapi.com/v1/forecast.json";
    private final String apiKey;
    private final HttpClient httpClient = HttpClient.newBuilder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WeatherService() throws IOException {
        Properties props = new Properties();
        try (var is = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) props.load(is);
        }
        this.apiKey = props.getProperty("weather.api.key", "").trim();
    }

    /**
     * Fetches forecast for a location (city name, coordinates, etc.).
     * @param location e.g. "Tunis", "Carthage", "Paris"
     * @param days number of forecast days (1-14)
     * @return WeatherForecast or null on error
     */
    public WeatherForecast getForecast(String location, int days) {
        if (apiKey.isEmpty()) {
            System.err.println("Weather: API key not configured in config.properties");
            return null;
        }
        if (location == null || location.isBlank()) location = "Tunis";
        days = Math.max(1, Math.min(14, days));
        try {
            String q = URLEncoder.encode(location, StandardCharsets.UTF_8);
            String url = BASE_URL + "?key=" + apiKey + "&q=" + q + "&days=" + days + "&lang=fr";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("Weather API error: " + response.statusCode() + " " + response.body());
                return null;
            }
            return parseForecast(response.body());
        } catch (Exception e) {
            System.err.println("Weather fetch error: " + e.getMessage());
            return null;
        }
    }

    private WeatherForecast parseForecast(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode loc = root.path("location");
            JsonNode cur = root.path("current");
            JsonNode cond = cur.path("condition");

            String city = loc.path("name").asText("");
            String country = loc.path("country").asText("");
            double tempC = cur.path("temp_c").asDouble(0);
            String conditionText = cond.path("text").asText("");
            String iconUrl = cond.path("icon").asText("");
            if (iconUrl.startsWith("//")) iconUrl = "https:" + iconUrl;

            List<DayForecast> days = new ArrayList<>();
            JsonNode forecastDays = root.path("forecast").path("forecastday");
            for (JsonNode fd : forecastDays) {
                JsonNode day = fd.path("day");
                days.add(new DayForecast(
                        fd.path("date").asText(""),
                        day.path("maxtemp_c").asDouble(0),
                        day.path("mintemp_c").asDouble(0),
                        day.path("condition").path("text").asText("")
                ));
            }
            return new WeatherForecast(city, country, tempC, conditionText, iconUrl, days);
        } catch (Exception e) {
            System.err.println("Weather parse error: " + e.getMessage());
            return null;
        }
    }

    public static class WeatherForecast {
        public final String city;
        public final String country;
        public final double tempC;
        public final String conditionText;
        public final String iconUrl;
        public final List<DayForecast> days;

        public WeatherForecast(String city, String country, double tempC, String conditionText, String iconUrl, List<DayForecast> days) {
            this.city = city;
            this.country = country;
            this.tempC = tempC;
            this.conditionText = conditionText;
            this.iconUrl = iconUrl;
            this.days = days;
        }
    }

    public static class DayForecast {
        public final String date;
        public final double maxTempC;
        public final double minTempC;
        public final String conditionText;

        public DayForecast(String date, double maxTempC, double minTempC, String conditionText) {
            this.date = date;
            this.maxTempC = maxTempC;
            this.minTempC = minTempC;
            this.conditionText = conditionText;
        }
    }
}
