package tn.esprit.services;

import tn.esprit.config.DBConnection;
import tn.esprit.entities.ReservationActivite;
import tn.esprit.entities.ReservationTransport;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationActiviteServices {
    private Connection cnx;

    public ReservationActiviteServices() {
        cnx = DBConnection.getInstance().getCnx();
    }

    public void ajouter(ReservationActivite r) throws SQLException {
        String sql = "INSERT INTO reservation_activite (date_reservation, statut, montant_total, id_user, id_voyage, id_activite) VALUES (?,?,?,?,?,?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setDate(1, Date.valueOf(r.getDateReservation()));
        ps.setString(2, r.getStatut().name());
        ps.setBigDecimal(3, r.getMontantTotal());
        ps.setInt(4, r.getIdUser());
        ps.setInt(5, r.getIdVoyage());
        ps.setInt(6, r.getIdActivite());
        ps.executeUpdate();
    }

    public void modifier(ReservationActivite r) throws SQLException {
        String sql = "UPDATE reservation_activite SET date_reservation=?, statut=?, montant_total=?, id_user=?, id_voyage=?, id_activite=? WHERE id_reservation_activite=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setDate(1, Date.valueOf(r.getDateReservation()));
        ps.setString(2, r.getStatut().name());
        ps.setBigDecimal(3, r.getMontantTotal());
        ps.setInt(4, r.getIdUser());
        ps.setInt(5, r.getIdVoyage());
        ps.setInt(6, r.getIdActivite());
        ps.setInt(7, r.getIdReservationActivite());
        ps.executeUpdate();
    }

    public void supprimer(int idReservationActivite) throws SQLException {
        String sql = "DELETE FROM reservation_activite WHERE id_reservation_activite=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idReservationActivite);
        ps.executeUpdate();
    }

    public List<ReservationActivite> afficher() throws SQLException {
        List<ReservationActivite> list = new ArrayList<>();
        String sql = "SELECT * FROM reservation_activite";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            ReservationActivite r = new ReservationActivite(
                    rs.getInt("id_reservation_activite"),
                    rs.getDate("date_reservation").toLocalDate(),
                    ReservationTransport.StatutReservation.valueOf(rs.getString("statut")),
                    rs.getBigDecimal("montant_total"),
                    rs.getInt("id_user"),
                    rs.getInt("id_voyage"),
                    rs.getInt("id_activite")
            );
            list.add(r);
        }
        return list;
    }
}
