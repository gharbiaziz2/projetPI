package tn.esprit.services;

import tn.esprit.config.DBConnection;
import tn.esprit.entities.User;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class UserServices {
    private Connection cnx;

    public UserServices() {
        cnx = DBConnection.getInstance().getCnx();
    }

    public void ajouter(User u) throws SQLException {
        String sql = "INSERT INTO `user` (nom, prenom, email, mot_de_passe, role, statut, date_creation, telephone, adresse, photo, bio) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, u.getNom());
        ps.setString(2, u.getPrenom());
        ps.setString(3, u.getEmail());
        ps.setString(4, u.getMotDePasse());
        ps.setString(5, u.getRole().name());
        ps.setString(6, u.getStatut().name());
        ps.setDate(7, Date.valueOf(u.getDateCreation()));
        ps.setString(8, u.getTelephone());
        ps.setString(9, u.getAdresse());
        ps.setString(10, u.getPhoto());
        ps.setString(11, u.getBio());
        ps.executeUpdate();
    }

    public void modifier(User u) throws SQLException {
        String sql = "UPDATE `user` SET nom=?, prenom=?, email=?, mot_de_passe=?, role=?, statut=?, date_creation=?, telephone=?, adresse=?, photo=?, bio=? WHERE id_user=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, u.getNom());
        ps.setString(2, u.getPrenom());
        ps.setString(3, u.getEmail());
        ps.setString(4, u.getMotDePasse());
        ps.setString(5, u.getRole().name());
        ps.setString(6, u.getStatut().name());
        ps.setDate(7, Date.valueOf(u.getDateCreation()));
        ps.setString(8, u.getTelephone());
        ps.setString(9, u.getAdresse());
        ps.setString(10, u.getPhoto());
        ps.setString(11, u.getBio());
        ps.setInt(12, u.getIdUser());
        ps.executeUpdate();
    }

    public void supprimer(int idUser) throws SQLException {
        String sql = "DELETE FROM `user` WHERE id_user=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idUser);
        ps.executeUpdate();
    }

    public List<User> afficher() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM `user`";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            User u = new User(
                    rs.getInt("id_user"),
                    rs.getString("nom"),
                    rs.getString("prenom"),
                    rs.getString("email"),
                    rs.getString("mot_de_passe"),
                    User.Role.valueOf(rs.getString("role")),
                    User.Statut.valueOf(rs.getString("statut")),
                    rs.getDate("date_creation").toLocalDate(),
                    rs.getString("telephone"),
                    rs.getString("adresse"),
                    rs.getString("photo"),
                    rs.getString("bio")
            );
            list.add(u);
        }
        return list;
    }

    /** Returns users with role GUIDE_TOURISTIQUE only (for voyage guide selection). */
    public List<User> getGuides() throws SQLException {
        List<User> all = afficher();
        List<User> guides = new ArrayList<>();
        for (User u : all) {
            if (u.getRole() == User.Role.GUIDE_TOURISTIQUE) guides.add(u);
        }
        return guides;
    }

    /** Returns the user with the given id, or null if not found. */
    public User getById(int idUser) throws SQLException {
        if (idUser <= 0) return null;
        String sql = "SELECT * FROM `user` WHERE id_user = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idUser);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) return null;
        return new User(
                rs.getInt("id_user"),
                rs.getString("nom"),
                rs.getString("prenom"),
                rs.getString("email"),
                rs.getString("mot_de_passe"),
                User.Role.valueOf(rs.getString("role")),
                User.Statut.valueOf(rs.getString("statut")),
                rs.getDate("date_creation").toLocalDate(),
                rs.getString("telephone"),
                rs.getString("adresse"),
                rs.getString("photo"),
                rs.getString("bio")
        );
    }

    /** Returns the user with the given email, or null if not found. */
    public User findByEmail(String email) throws SQLException {
        if (email == null || email.isBlank()) return null;
        String sql = "SELECT * FROM `user` WHERE email = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, email.trim());
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) return null;
        return new User(
                rs.getInt("id_user"),
                rs.getString("nom"),
                rs.getString("prenom"),
                rs.getString("email"),
                rs.getString("mot_de_passe"),
                User.Role.valueOf(rs.getString("role")),
                User.Statut.valueOf(rs.getString("statut")),
                rs.getDate("date_creation").toLocalDate(),
                rs.getString("telephone"),
                rs.getString("adresse"),
                rs.getString("photo"),
                rs.getString("bio")
        );
    }
}
