package tn.esprit.services;

import tn.esprit.config.DBConnection;
import tn.esprit.entities.ReservationChambre;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationHotelServices {
    private Connection cnx;

    public ReservationHotelServices() {
        cnx = DBConnection.getInstance().getCnx();
    }

    public void ajouter(ReservationChambre r) throws SQLException {
        String sql = "INSERT INTO reservation_chambre (id_chambre, id_user, date_debut, date_fin, montant_total, statut) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, r.getIdChambre());
            ps.setInt(2, r.getIdUser());
            ps.setDate(3, Date.valueOf(r.getDateDebut()));
            ps.setDate(4, Date.valueOf(r.getDateFin()));
            ps.setDouble(5, r.getMontantTotal());
            ps.setString(6, r.getStatut() != null ? r.getStatut() : "EN_ATTENTE");
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'insertion: " + e.getMessage());
            throw e;
        }
    }

    public void modifier(ReservationChambre r) throws SQLException {
        String sql = "UPDATE reservation_chambre SET id_chambre=?, id_user=?, date_debut=?, date_fin=?, montant_total=?, statut=? WHERE id_reservation_chambre=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, r.getIdChambre());
            ps.setInt(2, r.getIdUser());
            ps.setDate(3, Date.valueOf(r.getDateDebut()));
            ps.setDate(4, Date.valueOf(r.getDateFin()));
            ps.setDouble(5, r.getMontantTotal());
            ps.setString(6, r.getStatut());
            ps.setInt(7, r.getIdReservationChambre());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la modification: " + e.getMessage());
            throw e;
        }
    }

    public void supprimer(int idReservationChambre) throws SQLException {
        String sql = "DELETE FROM reservation_chambre WHERE id_reservation_chambre=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idReservationChambre);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression: " + e.getMessage());
            throw e;
        }
    }

    public List<ReservationChambre> afficher() throws SQLException {
        List<ReservationChambre> list = new ArrayList<>();
        String sql = "SELECT * FROM reservation_chambre";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                ReservationChambre r = new ReservationChambre(
                        rs.getInt("id_reservation_chambre"),
                        rs.getInt("id_chambre"),
                        rs.getInt("id_user"),
                        rs.getDate("date_debut").toLocalDate(),
                        rs.getDate("date_fin").toLocalDate(),
                        rs.getDouble("montant_total"),
                        rs.getString("statut")
                );
                list.add(r);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la lecture: " + e.getMessage());
            throw e;
        }
        return list;
    }

    public List<ReservationChambre> getByUser(int idUser) throws SQLException {
        List<ReservationChambre> list = new ArrayList<>();
        String sql = "SELECT * FROM reservation_chambre WHERE id_user = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ReservationChambre r = new ReservationChambre(
                            rs.getInt("id_reservation_chambre"),
                            rs.getInt("id_chambre"),
                            rs.getInt("id_user"),
                            rs.getDate("date_debut").toLocalDate(),
                            rs.getDate("date_fin").toLocalDate(),
                            rs.getDouble("montant_total"),
                            rs.getString("statut")
                    );
                    list.add(r);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la lecture par utilisateur: " + e.getMessage());
            throw e;
        }
        return list;
    }

    public ReservationChambre getById(int idReservationChambre) throws SQLException {
        String sql = "SELECT * FROM reservation_chambre WHERE id_reservation_chambre = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idReservationChambre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ReservationChambre(
                            rs.getInt("id_reservation_chambre"),
                            rs.getInt("id_chambre"),
                            rs.getInt("id_user"),
                            rs.getDate("date_debut").toLocalDate(),
                            rs.getDate("date_fin").toLocalDate(),
                            rs.getDouble("montant_total"),
                            rs.getString("statut")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la lecture par ID: " + e.getMessage());
            throw e;
        }
        return null;
    }

    public int ajouterAndReturnId(ReservationChambre r) throws SQLException {
        String sql = "INSERT INTO reservation_chambre (id_chambre, id_user, date_debut, date_fin, montant_total, statut) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getIdChambre());
            ps.setInt(2, r.getIdUser());
            ps.setDate(3, Date.valueOf(r.getDateDebut()));
            ps.setDate(4, Date.valueOf(r.getDateFin()));
            ps.setDouble(5, r.getMontantTotal());
            ps.setString(6, r.getStatut() != null ? r.getStatut() : "EN_ATTENTE");
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'insertion et retour d'ID: " + e.getMessage());
            throw e;
        }
        throw new SQLException("Impossible de créer la réservation chambre");
    }
}
