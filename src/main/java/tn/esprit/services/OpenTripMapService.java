package tn.esprit.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Fetches nearby tourist attractions from OpenTripMap API.
 * Config: opentripmap.api.key in config.properties
 */
public class OpenTripMapService {

    private static final String RADIUS_URL = "https://api.opentripmap.com/0.1/en/places/radius";
    private final String apiKey;
    private final HttpClient httpClient = HttpClient.newBuilder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenTripMapService() throws IOException {
        Properties props = new Properties();
        try (var is = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) props.load(is);
        }
        this.apiKey = props.getProperty("opentripmap.api.key", "").trim();
    }

    /**
     * Fetches tourist attractions near the given coordinates.
     * @param lat latitude
     * @param lon longitude
     * @param radiusMeters radius in meters (e.g. 5000 for 5km)
     * @param limit max results
     */
    public List<TouristPlace> getPlacesNearby(double lat, double lon, int radiusMeters, int limit) {
        if (apiKey.isEmpty()) {
            System.err.println("OpenTripMap: API key not configured");
            return new ArrayList<>();
        }
        try {
            String url = RADIUS_URL + "?apikey=" + apiKey + "&lat=" + lat + "&lon=" + lon
                    + "&radius=" + radiusMeters + "&limit=" + limit + "&format=json&rate=1";
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("OpenTripMap error: " + response.statusCode());
                return new ArrayList<>();
            }
            return parsePlaces(response.body());
        } catch (Exception e) {
            System.err.println("OpenTripMap fetch error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<TouristPlace> parsePlaces(String json) {
        List<TouristPlace> list = new ArrayList<>();
        try {
            JsonNode arr = objectMapper.readTree(json);
            for (JsonNode n : arr) {
                String name = n.path("name").asText("");
                if (name.isBlank()) name = "Attraction";
                JsonNode pt = n.path("point");
                double lat = pt.path("lat").asDouble(0);
                double lon = pt.path("lon").asDouble(0);
                double dist = n.path("dist").asDouble(0);
                String kinds = n.path("kinds").asText("");
                list.add(new TouristPlace(name, lat, lon, dist, kinds));
            }
        } catch (Exception e) {
            System.err.println("OpenTripMap parse error: " + e.getMessage());
        }
        return list;
    }

    public static class TouristPlace {
        public final String name;
        public final double lat;
        public final double lon;
        public final double distMeters;
        public final String kinds;

        public TouristPlace(String name, double lat, double lon, double distMeters, String kinds) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
            this.distMeters = distMeters;
            this.kinds = kinds;
        }
    }
}
