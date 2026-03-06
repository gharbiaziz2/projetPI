package tn.esprit.services;

import tn.esprit.config.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FavoriteHotelService {
    private Connection cnx;

    public FavoriteHotelService() {
        cnx = DBConnection.getInstance().getCnx();
    }

    public boolean isFavorite(int idUser, int idHotel) throws SQLException {
        String sql = "SELECT 1 FROM favorite_hotel WHERE id_user = ? AND id_hotel = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idUser);
        ps.setInt(2, idHotel);
        ResultSet rs = ps.executeQuery();
        return rs.next();
    }

    public void addFavorite(int idUser, int idHotel) throws SQLException {
        String sql = "INSERT IGNORE INTO favorite_hotel (id_user, id_hotel) VALUES (?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idUser);
        ps.setInt(2, idHotel);
        ps.executeUpdate();
    }

    public void removeFavorite(int idUser, int idHotel) throws SQLException {
        String sql = "DELETE FROM favorite_hotel WHERE id_user = ? AND id_hotel = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idUser);
        ps.setInt(2, idHotel);
        ps.executeUpdate();
    }

    public void toggleFavorite(int idUser, int idHotel) throws SQLException {
        if (isFavorite(idUser, idHotel)) {
            removeFavorite(idUser, idHotel);
        } else {
            addFavorite(idUser, idHotel);
        }
    }
}
