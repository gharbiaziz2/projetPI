package tn.esprit.services;

import tn.esprit.config.DBConnection;
import tn.esprit.entities.ReservationTransport;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationTransportServices {
    private Connection cnx;

    public ReservationTransportServices() {
        cnx = DBConnection.getInstance().getCnx();
    }

    public void ajouter(ReservationTransport r) throws SQLException {
        String sql = "INSERT INTO reservationtransport (date_reservation, statut, prix_total, id_user) VALUES (?,?,?,?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setDate(1, Date.valueOf(r.getDateReservation()));
        ps.setString(2, r.getStatut().name());
        ps.setBigDecimal(3, r.getPrixTotal());
        ps.setInt(4, r.getIdUser());
        ps.executeUpdate();
    }

    /** Inserts and returns the generated id_reservation_transport. */
    public int ajouterAndReturnId(ReservationTransport r) throws SQLException {
        String sql = "INSERT INTO reservationtransport (date_reservation, statut, prix_total, id_user) VALUES (?,?,?,?)";
        PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setDate(1, Date.valueOf(r.getDateReservation()));
        ps.setString(2, r.getStatut().name());
        ps.setBigDecimal(3, r.getPrixTotal());
        ps.setInt(4, r.getIdUser());
        ps.executeUpdate();
        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            int id = rs.getInt(1);
            rs.close();
            ps.close();
            return id;
        }
        rs.close();
        ps.close();
        throw new SQLException("Could not get generated id for reservation transport");
    }

    public void modifier(ReservationTransport r) throws SQLException {
        String sql = "UPDATE reservationtransport SET date_reservation=?, statut=?, prix_total=?, id_user=? WHERE id_reservation_transport=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setDate(1, Date.valueOf(r.getDateReservation()));
        ps.setString(2, r.getStatut().name());
        ps.setBigDecimal(3, r.getPrixTotal());
        ps.setInt(4, r.getIdUser());
        ps.setInt(5, r.getIdReservationTransport());
        ps.executeUpdate();
    }

    public void supprimer(int idReservationTransport) throws SQLException {
        String sql = "DELETE FROM reservationtransport WHERE id_reservation_transport=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idReservationTransport);
        ps.executeUpdate();
    }

    public List<ReservationTransport> afficher() throws SQLException {
        List<ReservationTransport> list = new ArrayList<>();
        String sql = "SELECT * FROM reservationtransport";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            ReservationTransport r = new ReservationTransport(
                    rs.getInt("id_reservation_transport"),
                    rs.getDate("date_reservation").toLocalDate(),
                    ReservationTransport.StatutReservation.valueOf(rs.getString("statut")),
                    rs.getBigDecimal("prix_total"),
                    rs.getInt("id_user")
            );
            list.add(r);
        }
        return list;
    }
}
