package tn.esprit.services;

import tn.esprit.config.DBConnection;
import tn.esprit.entities.ReservationTransportTransport;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationTransportTransportServices {
    private Connection cnx;

    public ReservationTransportTransportServices() {
        cnx = DBConnection.getInstance().getCnx();
    }

    public void ajouter(ReservationTransportTransport rtt) throws SQLException {
        String sql = "INSERT INTO reservationtransport_transport (id_reservation_transport, id_transport) VALUES (?,?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, rtt.getIdReservationTransport());
        ps.setInt(2, rtt.getIdTransport());
        ps.executeUpdate();
    }

    public void supprimer(int idReservationTransport, int idTransport) throws SQLException {
        String sql = "DELETE FROM reservationtransport_transport WHERE id_reservation_transport=? AND id_transport=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idReservationTransport);
        ps.setInt(2, idTransport);
        ps.executeUpdate();
    }

    public List<ReservationTransportTransport> afficher() throws SQLException {
        List<ReservationTransportTransport> list = new ArrayList<>();
        String sql = "SELECT * FROM reservationtransport_transport";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            ReservationTransportTransport rtt = new ReservationTransportTransport(
                    rs.getInt("id_reservation_transport"),
                    rs.getInt("id_transport")
            );
            list.add(rtt);
        }
        return list;
    }

    /** Returns id_transport values that are already booked on the given date. */
    public List<Integer> getTransportIdsBookedForDate(LocalDate date) throws SQLException {
        List<Integer> list = new ArrayList<>();
        String sql = "SELECT rtt.id_transport FROM reservationtransport_transport rtt INNER JOIN reservationtransport rt ON rtt.id_reservation_transport = rt.id_reservation_transport WHERE rt.date_reservation = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setDate(1, Date.valueOf(date));
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(rs.getInt("id_transport"));
        }
        rs.close();
        ps.close();
        return list;
    }
}
