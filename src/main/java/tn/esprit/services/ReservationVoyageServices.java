package tn.esprit.services;

import tn.esprit.config.DBConnection;
import tn.esprit.entities.ReservationVoyage;
import tn.esprit.entities.ReservationTransport;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationVoyageServices {
    private Connection cnx;

    public ReservationVoyageServices() {
        cnx = DBConnection.getInstance().getCnx();
    }

    /** @return true if this user already has a reservation for this voyage */
    public boolean existsByUserAndVoyage(int idUser, int idVoyage) throws SQLException {
        String sql = "SELECT 1 FROM reservation_voyage WHERE id_user=? AND id_voyage=? LIMIT 1";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idUser);
        ps.setInt(2, idVoyage);
        ResultSet rs = ps.executeQuery();
        boolean exists = rs.next();
        rs.close();
        ps.close();
        return exists;
    }

    public void ajouter(ReservationVoyage r) throws SQLException {
        ajouterAndReturnId(r);
    }

    /**
     * Inserts a new reservation and returns its generated id_reservation_voyage.
     */
    public int ajouterAndReturnId(ReservationVoyage r) throws SQLException {
        if (existsByUserAndVoyage(r.getIdUser(), r.getIdVoyage())) {
            throw new SQLException("DUPLICATE_VOYAGE");
        }
        String sql = "INSERT INTO reservation_voyage (date_reservation, statut, montant_total, id_user, id_voyage) VALUES (?,?,?,?,?)";
        PreparedStatement ps = cnx.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
        ps.setDate(1, Date.valueOf(r.getDateReservation()));
        ps.setString(2, r.getStatut().name());
        ps.setBigDecimal(3, r.getMontantTotal());
        ps.setInt(4, r.getIdUser());
        ps.setInt(5, r.getIdVoyage());
        ps.executeUpdate();
        ResultSet keys = ps.getGeneratedKeys();
        if (keys.next()) {
            return keys.getInt(1);
        }
        throw new SQLException("Impossible de récupérer l'ID de la réservation.");
    }

    public void modifier(ReservationVoyage r) throws SQLException {
        String sql = "UPDATE reservation_voyage SET date_reservation=?, statut=?, montant_total=?, id_user=?, id_voyage=? WHERE id_reservation_voyage=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setDate(1, Date.valueOf(r.getDateReservation()));
        ps.setString(2, r.getStatut().name());
        ps.setBigDecimal(3, r.getMontantTotal());
        ps.setInt(4, r.getIdUser());
        ps.setInt(5, r.getIdVoyage());
        ps.setInt(6, r.getIdReservationVoyage());
        ps.executeUpdate();
    }

    public void updateStatut(int idReservationVoyage, ReservationTransport.StatutReservation statut)
            throws SQLException {
        String sql = "UPDATE reservation_voyage SET statut=? WHERE id_reservation_voyage=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, statut.name());
        ps.setInt(2, idReservationVoyage);
        ps.executeUpdate();
        ps.close();
    }

    /**
     * Confirms a reservation by setting its status to CONFIRMEE.
     * Called by ConfirmationHttpServer when the user clicks the email link.
     *
     * @param idReservationVoyage The reservation ID to confirm.
     */
    public void confirmerReservation(int idReservationVoyage) throws SQLException {
        updateStatut(idReservationVoyage, ReservationTransport.StatutReservation.CONFIRMEE);
    }

    public void supprimer(int idReservationVoyage) throws SQLException {
        String sql = "DELETE FROM reservation_voyage WHERE id_reservation_voyage=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idReservationVoyage);
        ps.executeUpdate();
    }

    public List<ReservationVoyage> afficher() throws SQLException {
        List<ReservationVoyage> list = new ArrayList<>();
        String sql = "SELECT * FROM reservation_voyage";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            ReservationVoyage r = new ReservationVoyage(
                    rs.getInt("id_reservation_voyage"),
                    rs.getDate("date_reservation").toLocalDate(),
                    ReservationTransport.StatutReservation.valueOf(rs.getString("statut")),
                    rs.getBigDecimal("montant_total"),
                    rs.getInt("id_user"),
                    rs.getInt("id_voyage"));
            list.add(r);
        }
        return list;
    }
}
