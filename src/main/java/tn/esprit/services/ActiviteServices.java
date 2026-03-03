package tn.esprit.services;

import tn.esprit.config.DBConnection;
import tn.esprit.entities.Activite;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ActiviteServices {
    private Connection cnx;

    public ActiviteServices() {
        cnx = DBConnection.getInstance().getCnx();
    }

    public void ajouter(Activite a) throws SQLException {
        String sql = "INSERT INTO activite (nom, description, prix, duree, latitude, longitude, date_activite, photo) VALUES (?,?,?,?,?,?,?,?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, a.getNom());
        ps.setString(2, a.getDescription());
        ps.setBigDecimal(3, a.getPrix());
        ps.setInt(4, a.getDuree());
        ps.setObject(5, a.getLatitude());
        ps.setObject(6, a.getLongitude());
        ps.setDate(7, a.getDate() != null ? Date.valueOf(a.getDate()) : null);
        ps.setString(8, a.getPhoto());
        ps.executeUpdate();
    }

    public void modifier(Activite a) throws SQLException {
        String sql = "UPDATE activite SET nom=?, description=?, prix=?, duree=?, latitude=?, longitude=?, date_activite=?, photo=? WHERE id_activite=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, a.getNom());
        ps.setString(2, a.getDescription());
        ps.setBigDecimal(3, a.getPrix());
        ps.setInt(4, a.getDuree());
        ps.setObject(5, a.getLatitude());
        ps.setObject(6, a.getLongitude());
        ps.setDate(7, a.getDate() != null ? Date.valueOf(a.getDate()) : null);
        ps.setString(8, a.getPhoto());
        ps.setInt(9, a.getIdActivite());
        ps.executeUpdate();
    }

    public void supprimer(int idActivite) throws SQLException {
        String sql = "DELETE FROM activite WHERE id_activite=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idActivite);
        ps.executeUpdate();
    }

    public List<Activite> afficher() throws SQLException {
        List<Activite> list = new ArrayList<>();
        String sql = "SELECT * FROM activite";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            Date dateVal = rs.getDate("date_activite");
            LocalDate localDate = dateVal != null ? dateVal.toLocalDate() : null;
            double latVal = rs.getDouble("latitude");
            Double lat = rs.wasNull() ? null : latVal;
            double lonVal = rs.getDouble("longitude");
            Double lon = rs.wasNull() ? null : lonVal;
            Activite a = new Activite(
                    rs.getInt("id_activite"),
                    rs.getString("nom"),
                    rs.getString("description"),
                    rs.getBigDecimal("prix"),
                    rs.getInt("duree"),
                    lat,
                    lon,
                    localDate,
                    rs.getString("photo")
            );
            list.add(a);
        }
        return list;
    }

    /** Returns activities with coordinates, sorted by distance from (lat, lon), within radiusKm. */
    public List<Activite> getNearby(double lat, double lon, double radiusKm) throws SQLException {
        List<Activite> all = afficher();
        List<Activite> nearby = new ArrayList<>();
        for (Activite a : all) {
            if (a.getLatitude() == null || a.getLongitude() == null) continue;
            double d = haversineKm(lat, lon, a.getLatitude(), a.getLongitude());
            if (d <= radiusKm) nearby.add(a);
        }
        nearby.sort((a, b) -> {
            double da = haversineKm(lat, lon, a.getLatitude(), a.getLongitude());
            double db = haversineKm(lat, lon, b.getLatitude(), b.getLongitude());
            return Double.compare(da, db);
        });
        return nearby.subList(0, Math.min(10, nearby.size()));
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2) + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.sin(dLon/2)*Math.sin(dLon/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }
}
