package tn.esprit.services;

import tn.esprit.config.DBConnection;
import tn.esprit.entities.Notification;
import tn.esprit.entities.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotificationServices {
    private Connection cnx;
    private final UserServices userService = new UserServices();

    public NotificationServices() {
        cnx = DBConnection.getInstance().getCnx();
    }

    /**
     * Sends a notification to all CLIENT users.
     */
    public void notifyAllClients(String message) throws SQLException {
        List<User> clients = new ArrayList<>();
        for (User u : userService.afficher()) {
            if (u.getRole() == User.Role.CLIENT && u.getStatut() == User.Statut.ACTIVE) {
                clients.add(u);
            }
        }
        for (User u : clients) {
            ajouter(new Notification(0, u.getIdUser(), message, LocalDateTime.now(), false));
        }
    }

    public void ajouter(Notification n) throws SQLException {
        String sql = "INSERT INTO notification (id_user, message, date_creation, lu) VALUES (?,?,?,?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, n.getIdUser());
        ps.setString(2, n.getMessage());
        ps.setTimestamp(3, Timestamp.valueOf(n.getDateCreation()));
        ps.setBoolean(4, n.isLu());
        ps.executeUpdate();
    }

    public List<Notification> getByUserId(int idUser) throws SQLException {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM notification WHERE id_user = ? ORDER BY date_creation DESC";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idUser);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(new Notification(
                    rs.getInt("id_notification"),
                    rs.getInt("id_user"),
                    rs.getString("message"),
                    rs.getTimestamp("date_creation").toLocalDateTime(),
                    rs.getBoolean("lu")
            ));
        }
        return list;
    }

    public void marquerCommeLu(int idNotification) throws SQLException {
        String sql = "UPDATE notification SET lu = 1 WHERE id_notification = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idNotification);
        ps.executeUpdate();
    }

    /** Returns the count of unread notifications for a user. */
    public int countUnreadByUserId(int idUser) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notification WHERE id_user = ? AND lu = 0";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idUser);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getInt(1) : 0;
    }

    /** Marks all notifications as read for a user. */
    public void marquerToutesCommeLu(int idUser) throws SQLException {
        String sql = "UPDATE notification SET lu = 1 WHERE id_user = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idUser);
        ps.executeUpdate();
    }
}
