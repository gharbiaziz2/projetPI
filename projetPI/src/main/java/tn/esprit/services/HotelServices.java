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
        String sql = "INSERT INTO hotel (nom, pays, ville, prix_nuit, description) VALUES (?,?,?,?,?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, h.getNom());
        ps.setString(2, h.getPays());
        ps.setString(3, h.getVille());
        ps.setBigDecimal(4, h.getPrixNuit());
        ps.setString(5, h.getDescription());
        ps.executeUpdate();
    }

    public void modifier(Hotel h) throws SQLException {
        String sql = "UPDATE hotel SET nom=?, pays=?, ville=?, prix_nuit=?, description=? WHERE id_hotel=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, h.getNom());
        ps.setString(2, h.getPays());
        ps.setString(3, h.getVille());
        ps.setBigDecimal(4, h.getPrixNuit());
        ps.setString(5, h.getDescription());
        ps.setInt(6, h.getIdHotel());
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
                    rs.getBigDecimal("prix_nuit"),
                    rs.getString("description")
            );
            list.add(h);
        }
        return list;
    }
}
