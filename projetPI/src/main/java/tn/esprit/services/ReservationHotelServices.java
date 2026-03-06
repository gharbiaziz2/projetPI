package tn.esprit.services;

import tn.esprit.config.DBConnection;
import tn.esprit.entities.ReservationHotel;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationHotelServices {
    private Connection cnx;

    public ReservationHotelServices() {
        cnx = DBConnection.getInstance().getCnx();
    }

    public void ajouter(ReservationHotel r) throws SQLException {
        String sql = "INSERT INTO reservationhotel (date_checkin, date_checkout, prix_total, id_user, id_hotel) VALUES (?,?,?,?,?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setDate(1, Date.valueOf(r.getDateCheckin()));
        ps.setDate(2, Date.valueOf(r.getDateCheckout()));
        ps.setBigDecimal(3, r.getPrixTotal());
        ps.setInt(4, r.getIdUser());
        ps.setInt(5, r.getIdHotel());
        ps.executeUpdate();
    }

    public void modifier(ReservationHotel r) throws SQLException {
        String sql = "UPDATE reservationhotel SET date_checkin=?, date_checkout=?, prix_total=?, id_user=?, id_hotel=? WHERE id_reservation_hotel=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setDate(1, Date.valueOf(r.getDateCheckin()));
        ps.setDate(2, Date.valueOf(r.getDateCheckout()));
        ps.setBigDecimal(3, r.getPrixTotal());
        ps.setInt(4, r.getIdUser());
        ps.setInt(5, r.getIdHotel());
        ps.setInt(6, r.getIdReservationHotel());
        ps.executeUpdate();
    }

    public void supprimer(int idReservationHotel) throws SQLException {
        String sql = "DELETE FROM reservationhotel WHERE id_reservation_hotel=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idReservationHotel);
        ps.executeUpdate();
    }

    public List<ReservationHotel> afficher() throws SQLException {
        List<ReservationHotel> list = new ArrayList<>();
        String sql = "SELECT * FROM reservationhotel";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            ReservationHotel r = new ReservationHotel(
                    rs.getInt("id_reservation_hotel"),
                    rs.getDate("date_checkin").toLocalDate(),
                    rs.getDate("date_checkout").toLocalDate(),
                    rs.getBigDecimal("prix_total"),
                    rs.getInt("id_user"),
                    rs.getInt("id_hotel")
            );
            list.add(r);
        }
        return list;
    }
}
