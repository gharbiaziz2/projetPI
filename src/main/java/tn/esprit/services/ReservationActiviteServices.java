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
        String sql = "INSERT INTO reservation_activite (date_reservation, statut, montant_total, id_user, id_voyage, id_activite, phone_number) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(r.getDateReservation()));
            ps.setString(2, r.getStatut() != null ? r.getStatut().name() : "EN_ATTENTE");
            ps.setDouble(3, r.getMontantTotal());
            ps.setInt(4, r.getIdUser());
            ps.setInt(5, r.getIdVoyage());
            ps.setInt(6, r.getIdActivite());
            ps.setString(7, r.getPhoneNumber());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'insertion activité: " + e.getMessage());
            throw e;
        }
    }

    public void modifier(ReservationActivite r) throws SQLException {
        String sql = "UPDATE reservation_activite SET date_reservation=?, statut=?, montant_total=?, id_user=?, id_voyage=?, id_activite=?, phone_number=? WHERE id_reservation_activite=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(r.getDateReservation()));
            ps.setString(2, r.getStatut() != null ? r.getStatut().name() : "EN_ATTENTE");
            ps.setDouble(3, r.getMontantTotal());
            ps.setInt(4, r.getIdUser());
            ps.setInt(5, r.getIdVoyage());
            ps.setInt(6, r.getIdActivite());
            ps.setString(7, r.getPhoneNumber());
            ps.setInt(8, r.getIdReservationActivite());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la modification activité: " + e.getMessage());
            throw e;
        }
    }

    public void supprimer(int idReservationActivite) throws SQLException {
        String sql = "DELETE FROM reservation_activite WHERE id_reservation_activite=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idReservationActivite);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression activité: " + e.getMessage());
            throw e;
        }
    }

    public List<ReservationActivite> afficher() throws SQLException {
        List<ReservationActivite> list = new ArrayList<>();
        String sql = "SELECT * FROM reservation_activite";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                ReservationActivite r = new ReservationActivite(
                        rs.getInt("id_reservation_activite"),
                        rs.getDate("date_reservation").toLocalDate(),
                        ReservationTransport.StatutReservation.valueOf(rs.getString("statut")),
                        rs.getDouble("montant_total"),
                        rs.getInt("id_user"),
                        rs.getInt("id_voyage"),
                        rs.getInt("id_activite"),
                        rs.getString("phone_number")
                );
                list.add(r);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la lecture activités: " + e.getMessage());
            throw e;
        }
        return list;
    }

    public List<ReservationActivite> getByUser(int idUser) throws SQLException {
        List<ReservationActivite> list = new ArrayList<>();
        String sql = "SELECT * FROM reservation_activite WHERE id_user = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ReservationActivite r = new ReservationActivite(
                            rs.getInt("id_reservation_activite"),
                            rs.getDate("date_reservation").toLocalDate(),
                            ReservationTransport.StatutReservation.valueOf(rs.getString("statut")),
                            rs.getDouble("montant_total"),
                            rs.getInt("id_user"),
                            rs.getInt("id_voyage"),
                            rs.getInt("id_activite"),
                            rs.getString("phone_number")
                    );
                    list.add(r);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la lecture par utilisateur activité: " + e.getMessage());
            throw e;
        }
        return list;
    }

    public List<ReservationActivite> getByVoyage(int idVoyage) throws SQLException {
        List<ReservationActivite> list = new ArrayList<>();
        String sql = "SELECT * FROM reservation_activite WHERE id_voyage = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idVoyage);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ReservationActivite r = new ReservationActivite(
                            rs.getInt("id_reservation_activite"),
                            rs.getDate("date_reservation").toLocalDate(),
                            ReservationTransport.StatutReservation.valueOf(rs.getString("statut")),
                            rs.getDouble("montant_total"),
                            rs.getInt("id_user"),
                            rs.getInt("id_voyage"),
                            rs.getInt("id_activite"),
                            rs.getString("phone_number")
                    );
                    list.add(r);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la lecture par voyage activité: " + e.getMessage());
            throw e;
        }
        return list;
    }

    public ReservationActivite getById(int idReservationActivite) throws SQLException {
        String sql = "SELECT * FROM reservation_activite WHERE id_reservation_activite = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idReservationActivite);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ReservationActivite(
                            rs.getInt("id_reservation_activite"),
                            rs.getDate("date_reservation").toLocalDate(),
                            ReservationTransport.StatutReservation.valueOf(rs.getString("statut")),
                            rs.getDouble("montant_total"),
                            rs.getInt("id_user"),
                            rs.getInt("id_voyage"),
                            rs.getInt("id_activite"),
                            rs.getString("phone_number")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la lecture par ID activité: " + e.getMessage());
            throw e;
        }
        return null;
    }

    public int ajouterAndReturnId(ReservationActivite r) throws SQLException {
        String sql = "INSERT INTO reservation_activite (date_reservation, statut, montant_total, id_user, id_voyage, id_activite, phone_number) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setDate(1, Date.valueOf(r.getDateReservation()));
            ps.setString(2, r.getStatut() != null ? r.getStatut().name() : "EN_ATTENTE");
            ps.setDouble(3, r.getMontantTotal());
            ps.setInt(4, r.getIdUser());
            ps.setInt(5, r.getIdVoyage());
            ps.setInt(6, r.getIdActivite());
            ps.setString(7, r.getPhoneNumber());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'insertion et retour d'ID activité: " + e.getMessage());
            throw e;
        }
        throw new SQLException("Impossible de créer la réservation activité");
    }
}
