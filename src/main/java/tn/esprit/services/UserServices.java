package tn.esprit.services;

import tn.esprit.config.DBConnection;
import tn.esprit.entities.User;
import tn.esprit.util.PasswordUtil;

import java.sql.*;
import java.time.LocalDate;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class UserServices {
    private Connection cnx;

    public UserServices() {
        cnx = DBConnection.getInstance().getCnx();
    }

    public void ajouter(User u) throws SQLException {
        String pwd = hashPasswordIfNeeded(u.getMotDePasse());
        String sql = "INSERT INTO `user` (nom, prenom, email, mot_de_passe, role, statut, date_creation, telephone, adresse, photo, bio, bad_word_count, google_id, google_avatar, reset_password_token, reset_password_expires_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, u.getNom());
        ps.setString(2, u.getPrenom());
        ps.setString(3, u.getEmail());
        ps.setString(4, pwd);
        ps.setString(5, u.getRole().name());
        ps.setString(6, u.getStatut().name());
        ps.setDate(7, Date.valueOf(u.getDateCreation()));
        ps.setString(8, u.getTelephone());
        ps.setString(9, u.getAdresse());
        ps.setString(10, u.getPhoto());
        ps.setString(11, u.getBio());
        ps.setInt(12, u.getBadWordCount());
        ps.setString(13, u.getGoogleId());
        ps.setString(14, u.getGoogleAvatar());
        ps.setString(15, u.getResetPasswordToken());
        ps.setTimestamp(16, u.getResetPasswordExpiresAt() != null ? Timestamp.valueOf(u.getResetPasswordExpiresAt()) : null);
        ps.executeUpdate();
    }

    public void modifier(User u) throws SQLException {
        String pwd = hashPasswordIfNeeded(u.getMotDePasse());
        String sql = "UPDATE `user` SET nom=?, prenom=?, email=?, mot_de_passe=?, role=?, statut=?, date_creation=?, telephone=?, adresse=?, photo=?, bio=?, bad_word_count=?, google_id=?, google_avatar=?, reset_password_token=?, reset_password_expires_at=? WHERE id_user=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, u.getNom());
        ps.setString(2, u.getPrenom());
        ps.setString(3, u.getEmail());
        ps.setString(4, pwd);
        ps.setString(5, u.getRole().name());
        ps.setString(6, u.getStatut().name());
        ps.setDate(7, Date.valueOf(u.getDateCreation()));
        ps.setString(8, u.getTelephone());
        ps.setString(9, u.getAdresse());
        ps.setString(10, u.getPhoto());
        ps.setString(11, u.getBio());
        ps.setInt(12, u.getBadWordCount());
        ps.setString(13, u.getGoogleId());
        ps.setString(14, u.getGoogleAvatar());
        ps.setString(15, u.getResetPasswordToken());
        ps.setTimestamp(16, u.getResetPasswordExpiresAt() != null ? Timestamp.valueOf(u.getResetPasswordExpiresAt()) : null);
        ps.setInt(17, u.getIdUser());
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
            list.add(mapUser(rs));
        }
        return list;
    }

    private User mapUser(ResultSet rs) throws SQLException {
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
        u.setBadWordCount(rs.getInt("bad_word_count"));
        u.setGoogleId(rs.getString("google_id"));
        u.setGoogleAvatar(rs.getString("google_avatar"));
        u.setResetPasswordToken(rs.getString("reset_password_token"));
        Timestamp rpExpires = rs.getTimestamp("reset_password_expires_at");
        u.setResetPasswordExpiresAt(rpExpires != null ? rpExpires.toLocalDateTime() : null);
        return u;
    }

    /**
     * Hashes the password if it is plain text. Keeps [GOOGLE_OAUTH] and existing BCrypt hashes unchanged.
     */
    private String hashPasswordIfNeeded(String value) {
        if (value == null || value.isBlank()) return value;
        if (PasswordUtil.isGoogleOAuthSentinel(value)) return value;
        if (PasswordUtil.isHashed(value)) return value;
        return PasswordUtil.hash(value);
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
        return mapUser(rs);
    }

    /**
     * Finds user by email, or creates a new CLIENT user if not found (for Google OAuth sign-in).
     * Uses a placeholder password for OAuth users since they won't log in with password.
     */
    public User findOrCreateFromGoogle(String email, String nom, String prenom, String photoUrl) throws SQLException {
        User existing = findByEmail(email);
        if (existing != null) return existing;
        User nu = new User(0, nom, prenom, email, "[GOOGLE_OAUTH]", User.Role.CLIENT, User.Statut.ACTIVE,
                LocalDate.now(), null, null, photoUrl, null);
        ajouter(nu);
        return findByEmail(email);
    }

    /**
     * Resets user password to a temporary one. Returns the plain temp password if success, null otherwise.
     * Caller should send it via SMS. Does nothing for Google OAuth users.
     */
    public String resetPasswordToTemp(String email) throws SQLException {
        User u = findByEmail(email);
        if (u == null) return null;
        if (PasswordUtil.isGoogleOAuthSentinel(u.getMotDePasse())) return null;
        String temp = generateTempPassword();
        String sql = "UPDATE `user` SET mot_de_passe = ? WHERE id_user = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, PasswordUtil.hash(temp));
        ps.setInt(2, u.getIdUser());
        ps.executeUpdate();
        return temp;
    }

    private static String generateTempPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        Random r = new Random();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) sb.append(chars.charAt(r.nextInt(chars.length())));
        return sb.toString();
    }

    /** Returns the user with the given email, or null if not found. */
    public User findByEmail(String email) throws SQLException {
        if (email == null || email.isBlank()) return null;
        String sql = "SELECT * FROM `user` WHERE email = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, email.trim());
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) return null;
        return mapUser(rs);
    }

    /** Returns all users with the given role. */
    public List<User> getUsersByRole(User.Role role) throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM `user` WHERE role = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, role.name());
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapUser(rs));
        }
        return list;
    }
}
