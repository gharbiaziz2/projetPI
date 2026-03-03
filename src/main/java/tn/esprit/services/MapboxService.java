package tn.esprit.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Mapbox Geocoding API - find nearby bus stations, airports.
 * Config: mapbox.api.key in config.properties
 */
public class MapboxService {
    private static final String GEOCODE_URL = "https://api.mapbox.com/geocoding/v5/mapbox.places";
    private final String apiKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MapboxService() {
        Properties props = new Properties();
        try (var is = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) props.load(is);
        } catch (Exception e) { }
        this.apiKey = props.getProperty("mapbox.api.key", "").trim();
    }

    public List<TransportPlace> searchNearby(double lat, double lon, String query, int limit) {
        if (apiKey.isEmpty()) return new ArrayList<>();
        try {
            String q = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = GEOCODE_URL + "/" + q + ".json?proximity=" + lon + "," + lat + "&limit=" + limit + "&access_token=" + apiKey;
            var request = java.net.http.HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            var response = java.net.http.HttpClient.newHttpClient().send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return new ArrayList<>();
            return parseGeocodeResponse(response.body());
        } catch (Exception e) {
            System.err.println("Mapbox geocode error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<TransportPlace> parseGeocodeResponse(String json) {
        List<TransportPlace> list = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            for (JsonNode f : root.path("features")) {
                JsonNode c = f.path("center");
                if (c.isArray() && c.size() >= 2) {
                    double lon = c.get(0).asDouble();
                    double lat = c.get(1).asDouble();
                    String name = f.path("place_name").asText(f.path("text").asText(""));
                    list.add(new TransportPlace(name, lat, lon));
                }
            }
        } catch (Exception e) { }
        return list;
    }

    public static class TransportPlace {
        public final String name;
        public final double lat;
        public final double lon;

        public TransportPlace(String name, double lat, double lon) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
        }
    }

    /**
     * Builds URL for Mapbox Static Images API.
     * JavaFX WebView does not support WebGL (used by Mapbox GL), so we use static PNG images.
     */
    public String buildStaticMapUrl(double hotelLat, double hotelLon,
                                    List<MapMarker> activities, List<MapMarker> transports) {
        if (apiKey.isEmpty()) return null;
        StringBuilder overlay = new StringBuilder();
        overlay.append("pin-l+e74c3c(").append(hotelLon).append(",").append(hotelLat).append(")");
        for (int i = 0; i < Math.min(activities.size(), 5); i++) {
            MapMarker m = activities.get(i);
            overlay.append(",pin-s+3498db(").append(m.lon).append(",").append(m.lat).append(")");
        }
        for (int i = 0; i < Math.min(transports.size(), 5); i++) {
            MapMarker m = transports.get(i);
            overlay.append(",pin-s+27ae60(").append(m.lon).append(",").append(m.lat).append(")");
        }
        return "https://api.mapbox.com/styles/v1/mapbox/streets-v12/static/"
            + overlay.toString() + "/"
            + hotelLon + "," + hotelLat + ",13,0,0/"
            + "800x500@2x?access_token=" + apiKey;
    }

    public static class MapMarker {
        public final String name;
        public final double lat;
        public final double lon;

        public MapMarker(String name, double lat, double lon) {
            this.name = name != null ? name : "";
            this.lat = lat;
            this.lon = lon;
        }
    }

    /** Builds interactive map HTML (Mapbox GL) - open in browser for pan/zoom. */
    public String buildInteractiveMapHtml(double hotelLat, double hotelLon, String hotelName,
                                          List<MapMarker> activities, List<MapMarker> transports,
                                          List<MapMarker> otherHotels) {
        if (apiKey.isEmpty()) return null;
        String token = apiKey.replace("'", "\\'");
        StringBuilder js = new StringBuilder();
        js.append("mapboxgl.accessToken='").append(token).append("';");
        js.append("var map=new mapboxgl.Map({container:'map',style:'mapbox://styles/mapbox/streets-v12',center:[").append(hotelLon).append(",").append(hotelLat).append("],zoom:13});");
        js.append("map.on('load',function(){");
        js.append("new mapboxgl.Marker({color:'#e74c3c'}).setLngLat([").append(hotelLon).append(",").append(hotelLat).append("]).setPopup(new mapboxgl.Popup().setHTML('<b>").append(escapeJs(hotelName)).append("</b><br>Votre hôtel')).addTo(map);");
        for (MapMarker m : activities) {
            js.append("new mapboxgl.Marker({color:'#3498db'}).setLngLat([").append(m.lon).append(",").append(m.lat).append("]).setPopup(new mapboxgl.Popup().setHTML('<b>").append(escapeJs(m.name)).append("</b><br>Activité')).addTo(map);");
        }
        for (MapMarker m : transports) {
            js.append("new mapboxgl.Marker({color:'#27ae60'}).setLngLat([").append(m.lon).append(",").append(m.lat).append("]).setPopup(new mapboxgl.Popup().setHTML('<b>").append(escapeJs(m.name)).append("</b><br>Transport')).addTo(map);");
        }
        for (MapMarker m : otherHotels) {
            js.append("new mapboxgl.Marker({color:'#9b59b6'}).setLngLat([").append(m.lon).append(",").append(m.lat).append("]).setPopup(new mapboxgl.Popup().setHTML('<b>").append(escapeJs(m.name)).append("</b><br>Autre hôtel')).addTo(map);");
        }
        js.append("});");
        return "<!DOCTYPE html><html><head><meta charset='utf-8'>" +
            "<link href='https://api.mapbox.com/mapbox-gl-js/v2.15.0/mapbox-gl.css' rel='stylesheet'/>" +
            "<script src='https://api.mapbox.com/mapbox-gl-js/v2.15.0/mapbox-gl.js'></script>" +
            "<style>body{margin:0}#map{width:100vw;height:100vh;}</style></head><body>" +
            "<div id='map'></div><script>" + js + "</script></body></html>";
    }

    private static String escapeJs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", " ");
    }
}
