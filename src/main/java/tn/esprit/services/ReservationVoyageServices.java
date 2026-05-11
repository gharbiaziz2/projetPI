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
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            ps.setInt(2, idVoyage);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification doubloon: " + e.getMessage());
            throw e;
        }
    }

    public void ajouter(ReservationVoyage r) throws SQLException {
        ajouterAndReturnId(r);
    }

    public int ajouterAndReturnId(ReservationVoyage r) throws SQLException {
        if (existsByUserAndVoyage(r.getIdUser(), r.getIdVoyage())) {
            throw new SQLException("DUPLICATE_VOYAGE");
        }
        String sql = "INSERT INTO reservation_voyage (date_reservation, statut, montant_total, id_user, id_voyage, numero_client, bagage_weight) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setDate(1, Date.valueOf(r.getDateReservation()));
            ps.setString(2, r.getStatut() != null ? r.getStatut().name() : "EN_ATTENTE");
            ps.setDouble(3, r.getMontantTotal());
            ps.setInt(4, r.getIdUser());
            ps.setInt(5, r.getIdVoyage());
            ps.setString(6, r.getNumeroClient());
            if (r.getBagageWeight() != null) {
                ps.setDouble(7, r.getBagageWeight());
            } else {
                ps.setNull(7, java.sql.Types.DECIMAL);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'insertion voyage: " + e.getMessage());
            throw e;
        }
        throw new SQLException("Impossible de récupérer l'ID de la réservation.");
    }

    public void modifier(ReservationVoyage r) throws SQLException {
        String sql = "UPDATE reservation_voyage SET date_reservation=?, statut=?, montant_total=?, id_user=?, id_voyage=?, numero_client=?, bagage_weight=? WHERE id_reservation_voyage=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(r.getDateReservation()));
            ps.setString(2, r.getStatut() != null ? r.getStatut().name() : "EN_ATTENTE");
            ps.setDouble(3, r.getMontantTotal());
            ps.setInt(4, r.getIdUser());
            ps.setInt(5, r.getIdVoyage());
            ps.setString(6, r.getNumeroClient());
            if (r.getBagageWeight() != null) {
                ps.setDouble(7, r.getBagageWeight());
            } else {
                ps.setNull(7, java.sql.Types.DECIMAL);
            }
            ps.setInt(8, r.getIdReservationVoyage());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la modification voyage: " + e.getMessage());
            throw e;
        }
    }

    public void updateStatut(int idReservationVoyage, ReservationTransport.StatutReservation statut)
            throws SQLException {
        String sql = "UPDATE reservation_voyage SET statut=? WHERE id_reservation_voyage=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, statut.name());
            ps.setInt(2, idReservationVoyage);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour du statut: " + e.getMessage());
            throw e;
        }
    }

    public void confirmerReservation(int idReservationVoyage) throws SQLException {
        updateStatut(idReservationVoyage, ReservationTransport.StatutReservation.CONFIRMEE);
    }

    public void supprimer(int idReservationVoyage) throws SQLException {
        String sql = "DELETE FROM reservation_voyage WHERE id_reservation_voyage=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idReservationVoyage);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression voyage: " + e.getMessage());
            throw e;
        }
    }

    public List<ReservationVoyage> afficher() throws SQLException {
        List<ReservationVoyage> list = new ArrayList<>();
        String sql = "SELECT * FROM reservation_voyage";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                double bw = rs.getDouble("bagage_weight");
                Double bagageWeight = rs.wasNull() ? null : bw;
                ReservationVoyage r = new ReservationVoyage(
                        rs.getInt("id_reservation_voyage"),
                        rs.getDate("date_reservation").toLocalDate(),
                        ReservationTransport.StatutReservation.valueOf(rs.getString("statut")),
                        rs.getDouble("montant_total"),
                        rs.getInt("id_user"),
                        rs.getInt("id_voyage"),
                        rs.getString("numero_client"),
                        bagageWeight);
                list.add(r);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la lecture voyages: " + e.getMessage());
            throw e;
        }
        return list;
    }

    public List<ReservationVoyage> getByUser(int idUser) throws SQLException {
        List<ReservationVoyage> list = new ArrayList<>();
        String sql = "SELECT * FROM reservation_voyage WHERE id_user = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double bw = rs.getDouble("bagage_weight");
                    Double bagageWeight = rs.wasNull() ? null : bw;
                    ReservationVoyage r = new ReservationVoyage(
                            rs.getInt("id_reservation_voyage"),
                            rs.getDate("date_reservation").toLocalDate(),
                            ReservationTransport.StatutReservation.valueOf(rs.getString("statut")),
                            rs.getDouble("montant_total"),
                            rs.getInt("id_user"),
                            rs.getInt("id_voyage"),
                            rs.getString("numero_client"),
                            bagageWeight);
                    list.add(r);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la lecture par utilisateur: " + e.getMessage());
            throw e;
        }
        return list;
    }

    public ReservationVoyage getById(int idReservationVoyage) throws SQLException {
        String sql = "SELECT * FROM reservation_voyage WHERE id_reservation_voyage = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idReservationVoyage);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double bw = rs.getDouble("bagage_weight");
                    Double bagageWeight = rs.wasNull() ? null : bw;
                    return new ReservationVoyage(
                            rs.getInt("id_reservation_voyage"),
                            rs.getDate("date_reservation").toLocalDate(),
                            ReservationTransport.StatutReservation.valueOf(rs.getString("statut")),
                            rs.getDouble("montant_total"),
                            rs.getInt("id_user"),
                            rs.getInt("id_voyage"),
                            rs.getString("numero_client"),
                            bagageWeight);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la lecture par ID: " + e.getMessage());
            throw e;
        }
        return null;
    }
}
