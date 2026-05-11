package tn.esprit.services;

import tn.esprit.config.DBConnection;
import tn.esprit.entities.Hotel;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HotelServices {
    private Connection cnx;

    public HotelServices() {
        cnx = DBConnection.getInstance().getCnx();
    }

    public void ajouter(Hotel h) throws SQLException {
        String sql = "INSERT INTO hotel (nom, pays, ville, prix_nuit, description, longitude, latitude, image) VALUES (?,?,?,?,?,?,?,?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, h.getNom());
        ps.setString(2, h.getPays());
        ps.setString(3, h.getVille());
        ps.setDouble(4, h.getPrixNuit());
        ps.setString(5, h.getDescription());
        ps.setObject(6, h.getLongitude());
        ps.setObject(7, h.getLatitude());
        ps.setString(8, h.getImage());
        ps.executeUpdate();
    }

    public void modifier(Hotel h) throws SQLException {
        String sql = "UPDATE hotel SET nom=?, pays=?, ville=?, prix_nuit=?, description=?, longitude=?, latitude=?, image=? WHERE id_hotel=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, h.getNom());
        ps.setString(2, h.getPays());
        ps.setString(3, h.getVille());
        ps.setDouble(4, h.getPrixNuit());
        ps.setString(5, h.getDescription());
        ps.setObject(6, h.getLongitude());
        ps.setObject(7, h.getLatitude());
        ps.setString(8, h.getImage());
        ps.setInt(9, h.getIdHotel());
        ps.executeUpdate();
    }

    public void supprimer(int idHotel) throws SQLException {
        String sql = "DELETE FROM hotel WHERE id_hotel=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idHotel);
        ps.executeUpdate();
    }

    public List<Hotel> afficher() throws SQLException {
        List<Hotel> list = new ArrayList<>();
        String sql = "SELECT * FROM hotel";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            Hotel h = new Hotel(
                    rs.getInt("id_hotel"),
                    rs.getString("nom"),
                    rs.getString("pays"),
                    rs.getString("ville"),
                    rs.getDouble("prix_nuit"),
                    rs.getString("description"),
                    getDoubleOrNull(rs, "longitude"),
                    getDoubleOrNull(rs, "latitude"),
                    rs.getString("image")
            );
            list.add(h);
        }
        return list;
    }

    private static Double getDoubleOrNull(ResultSet rs, String col) throws SQLException {
        double v = rs.getDouble(col);
        return rs.wasNull() ? null : v;
    }

    /** Returns nearby hotels (with coords) excluding the given idHotel, sorted by distance, max 10. */
    public List<Hotel> getNearby(double lat, double lon, int excludeIdHotel, double radiusKm) throws SQLException {
        List<Hotel> all = afficher();
        List<Hotel> nearby = new ArrayList<>();
        for (Hotel h : all) {
            if (h.getLatitude() == null || h.getLongitude() == null) continue;
            if (h.getIdHotel() == excludeIdHotel) continue;
            double d = haversineKm(lat, lon, h.getLatitude(), h.getLongitude());
            if (d <= radiusKm) nearby.add(h);
        }
        nearby.sort((a, b) -> Double.compare(
            haversineKm(lat, lon, a.getLatitude(), a.getLongitude()),
            haversineKm(lat, lon, b.getLatitude(), b.getLongitude())));
        int take = Math.min(10, nearby.size());
        return take == 0 ? nearby : nearby.subList(0, take);
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2) + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.sin(dLon/2)*Math.sin(dLon/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }
}
