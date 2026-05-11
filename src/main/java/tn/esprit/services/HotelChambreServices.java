package tn.esprit.services;

import tn.esprit.config.DBConnection;
import tn.esprit.entities.HotelChambre;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HotelChambreServices {
    private Connection cnx;

    public HotelChambreServices() {
        cnx = DBConnection.getInstance().getCnx();
    }

    public void ajouter(HotelChambre c) throws SQLException {
        String sql = "INSERT INTO hotel_chambre (id_hotel, numero_chambre, type_chambre, prix_chambre, description, image, disponible) VALUES (?,?,?,?,?,?,?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, c.getIdHotel());
        ps.setString(2, c.getNumeroChambre());
        ps.setString(3, c.getTypeChambre());
        ps.setDouble(4, c.getPrixChambre());
        ps.setString(5, c.getDescription());
        ps.setString(6, c.getImage());
        ps.setBoolean(7, c.isDisponible());
        ps.executeUpdate();
    }

    public void modifier(HotelChambre c) throws SQLException {
        String sql = "UPDATE hotel_chambre SET id_hotel=?, numero_chambre=?, type_chambre=?, prix_chambre=?, description=?, image=?, disponible=? WHERE id_chambre=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, c.getIdHotel());
        ps.setString(2, c.getNumeroChambre());
        ps.setString(3, c.getTypeChambre());
        ps.setDouble(4, c.getPrixChambre());
        ps.setString(5, c.getDescription());
        ps.setString(6, c.getImage());
        ps.setBoolean(7, c.isDisponible());
        ps.setInt(8, c.getIdChambre());
        ps.executeUpdate();
    }

    public void supprimer(int idChambre) throws SQLException {
        String sql = "DELETE FROM hotel_chambre WHERE id_chambre=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idChambre);
        ps.executeUpdate();
    }

    public List<HotelChambre> afficher() throws SQLException {
        List<HotelChambre> list = new ArrayList<>();
        String sql = "SELECT * FROM hotel_chambre";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    /** Returns rooms for a given hotel. */
    public List<HotelChambre> getByHotel(int idHotel) throws SQLException {
        List<HotelChambre> list = new ArrayList<>();
        String sql = "SELECT * FROM hotel_chambre WHERE id_hotel = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idHotel);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    /** Returns available rooms for a given hotel. */
    public List<HotelChambre> getAvailableByHotel(int idHotel) throws SQLException {
        List<HotelChambre> list = new ArrayList<>();
        String sql = "SELECT * FROM hotel_chambre WHERE id_hotel = ? AND disponible = 1";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idHotel);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    private HotelChambre mapRow(ResultSet rs) throws SQLException {
        return new HotelChambre(
                rs.getInt("id_chambre"),
                rs.getInt("id_hotel"),
                rs.getString("numero_chambre"),
                rs.getString("type_chambre"),
                rs.getDouble("prix_chambre"),
                rs.getString("description"),
                rs.getString("image"),
                rs.getBoolean("disponible")
        );
    }
}
