package tn.esprit.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Fetches real-time flight data from aviationstack.com API.
 * Config: aviationstack.api.key in config.properties
 */
public class AviationStackService {

    private static final String FLIGHTS_URL = "https://api.aviationstack.com/v1/flights";
    private static final String AIRPORTS_URL = "https://api.aviationstack.com/v1/airports";
    private final String apiKey;
    private final HttpClient httpClient = HttpClient.newBuilder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AviationStackService() {
        String key = System.getenv("AVIATIONSTACK_API_KEY");
        if (key == null) key = System.getProperty("aviationstack.api.key");
        if (key == null || key.isBlank()) {
            Properties props = new Properties();
            try (var is = getClass().getClassLoader().getResourceAsStream("config.properties")) {
                if (is != null) props.load(new InputStreamReader(is, StandardCharsets.UTF_8));
            } catch (Exception ignored) { }
            key = props.getProperty("aviationstack.api.key", "").trim();
        } else {
            key = key.trim();
        }
        this.apiKey = key;
    }

    public static class FlightInfo {
        public final String transport;
        public final String departure;
        public final String arrival;
        public final String status;
        public final String depIata;
        public final String arrIata;

        public FlightInfo(String transport, String departure, String arrival, String status, String depIata, String arrIata) {
            this.transport = transport != null ? transport : "—";
            this.departure = departure != null ? departure : "—";
            this.arrival = arrival != null ? arrival : "—";
            this.status = status != null ? status : "—";
            this.depIata = depIata != null ? depIata : "";
            this.arrIata = arrIata != null ? arrIata : "";
        }
    }

    /** Returns IATA code -> country name. Fetches from airports API. */
    public java.util.Map<String, String> getIataToCountry() {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        if (apiKey.isEmpty()) return map;
        try {
            String url = AIRPORTS_URL + "?access_key=" + apiKey + "&limit=2000";
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return map;
            JsonNode root = objectMapper.readTree(resp.body());
            if (root.has("error")) return map;
            JsonNode data = root.path("data");
            if (!data.isArray()) return map;
            for (JsonNode apt : data) {
                String iata = apt.path("iata_code").asText("").trim();
                String country = apt.path("country_name").asText("").trim();
                if (!iata.isEmpty() && !country.isEmpty()) map.put(iata, country);
            }
        } catch (Exception e) {
            System.err.println("AviationStack airports error: " + e.getMessage());
        }
        return map;
    }

    /** Returns sorted list of unique country names from airports. */
    public List<String> getCountries() {
        java.util.Set<String> set = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String c : getIataToCountry().values()) set.add(c);
        List<String> list = new ArrayList<>(set);
        java.util.Collections.sort(list);
        return list;
    }

    /**
     * Fetches real-time flights (up to limit).
     * @param limit max number of flights (1-100, default 10)
     * @return list of FlightInfo, or empty list on error
     */
    public List<FlightInfo> getFlights(int limit) {
        List<FlightInfo> result = new ArrayList<>();
        if (apiKey.isEmpty()) {
            System.err.println("AviationStack: API key not configured in config.properties");
            return result;
        }
        limit = Math.max(1, Math.min(100, limit));
        return getFlightsFiltered(null, null, limit);
    }

    /**
     * Fetches flights with optional country filters.
     * @param depCountry filter by departure country (null = all)
     * @param arrCountry filter by arrival country (null = all)
     */
    public List<FlightInfo> getFlightsFiltered(String depCountry, String arrCountry, int limit) {
        List<FlightInfo> result = new ArrayList<>();
        if (apiKey.isEmpty()) return result;
        limit = Math.max(1, Math.min(100, limit));
        try {
            String url = FLIGHTS_URL + "?access_key=" + apiKey + "&limit=" + limit;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            JsonNode root;
            try {
                root = objectMapper.readTree(body);
            } catch (Exception e) {
                System.err.println("AviationStack: Invalid JSON response");
                return result;
            }
            if (response.statusCode() != 200 || root.has("error")) {
                String errMsg = "HTTP " + response.statusCode();
                if (root.has("error")) {
                    JsonNode err = root.path("error");
                    String code = err.path("code").asText("");
                    String msg = err.path("message").asText("");
                    errMsg = code + ": " + msg;
                }
                System.err.println("AviationStack API error: " + errMsg);
                return result;
            }
            JsonNode data = root.path("data");
            if (!data.isArray()) return result;
            java.util.Map<String, String> iataToCountry = (depCountry != null || arrCountry != null) ? getIataToCountry() : java.util.Collections.emptyMap();
            for (JsonNode flight : data) {
                JsonNode airline = flight.path("airline");
                JsonNode fl = flight.path("flight");
                JsonNode dep = flight.path("departure");
                JsonNode arr = flight.path("arrival");
                String depIataVal = dep.path("iata").asText("");
                String arrIataVal = arr.path("iata").asText("");
                if (!iataToCountry.isEmpty()) {
                    String depC = iataToCountry.getOrDefault(depIataVal, "");
                    String arrC = iataToCountry.getOrDefault(arrIataVal, "");
                    if (depCountry != null && !depCountry.isEmpty() && !depCountry.equalsIgnoreCase(depC)) continue;
                    if (arrCountry != null && !arrCountry.isEmpty() && !arrCountry.equalsIgnoreCase(arrC)) continue;
                }
                String airlineName = airline.path("name").asText("");
                String flightNum = fl.path("iata").asText(fl.path("number").asText(""));
                String transport = airlineName.isEmpty() ? flightNum : airlineName + " " + flightNum;
                String depAirport = dep.path("airport").asText("");
                String departure = depAirport.isEmpty() ? depIataVal : depAirport + " (" + depIataVal + ")";
                if (departure.isEmpty() || departure.equals(" ()")) departure = "—";
                String arrAirport = arr.path("airport").asText("");
                String arrival = arrAirport.isEmpty() ? arrIataVal : arrAirport + " (" + arrIataVal + ")";
                if (arrival.isEmpty() || arrival.equals(" ()")) arrival = "—";
                String statusRaw = flight.path("flight_status").asText("");
                String status = translateStatus(statusRaw);
                result.add(new FlightInfo(transport, departure, arrival, status, depIataVal, arrIataVal));
            }
        } catch (Exception e) {
            System.err.println("AviationStack fetch error: " + e.getMessage());
        }
        return result;
    }

    private static String translateStatus(String status) {
        if (status == null || status.isEmpty()) return "—";
        return switch (status.toLowerCase()) {
            case "scheduled" -> "Programmé";
            case "active" -> "En vol";
            case "landed" -> "Atterri";
            case "cancelled" -> "Annulé";
            case "incident" -> "Incident";
            case "diverted" -> "Dérouté";
            default -> status;
        };
    }
}
