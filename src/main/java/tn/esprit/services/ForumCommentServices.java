package tn.esprit.services;

import tn.esprit.config.DBConnection;
import tn.esprit.entities.Forum;
import tn.esprit.entities.ForumComment;
import tn.esprit.entities.Notification;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ForumCommentServices {
    private Connection cnx;
    private final ForumServices forumService = new ForumServices();
    private final NotificationServices notificationService = new NotificationServices();
    private final UserServices userService = new UserServices();

    public ForumCommentServices() {
        cnx = DBConnection.getInstance().getCnx();
    }

    public void ajouter(ForumComment c) throws SQLException {
        String sql = "INSERT INTO forum_comment (id_forum, id_user, contenu, date_creation) VALUES (?,?,?,?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, c.getIdForum());
        ps.setInt(2, c.getIdUser());
        ps.setString(3, c.getContenu());
        ps.setTimestamp(4, Timestamp.valueOf(c.getDateCreation() != null ? c.getDateCreation() : LocalDateTime.now()));
        ps.executeUpdate();
        notifyForumAuthorOnReply(c);
    }

    private void notifyForumAuthorOnReply(ForumComment c) {
        try {
            Forum f = forumService.getById(c.getIdForum());
            if (f == null || f.getIdUser() == c.getIdUser()) return;
            var actor = userService.getById(c.getIdUser());
            String actorName = actor != null ? (actor.getNom() + " " + actor.getPrenom()).trim() : "Un utilisateur";
            String msg = actorName + " a repondu a votre message sur le forum.";
            Notification n = new Notification(0, f.getIdUser(), msg, LocalDateTime.now(), false);
            notificationService.ajouter(n);
        } catch (SQLException ignored) { /* best-effort notification */ }
    }

    public void modifier(ForumComment c) throws SQLException {
        String sql = "UPDATE forum_comment SET contenu=? WHERE id_comment=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, c.getContenu());
        ps.setInt(2, c.getIdComment());
        ps.executeUpdate();
    }

    public void supprimer(int idComment) throws SQLException {
        String sql = "DELETE FROM forum_reaction_comment WHERE id_comment=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idComment);
        ps.executeUpdate();
        sql = "DELETE FROM forum_comment WHERE id_comment=?";
        ps = cnx.prepareStatement(sql);
        ps.setInt(1, idComment);
        ps.executeUpdate();
    }

    public List<ForumComment> getByForumId(int idForum) throws SQLException {
        List<ForumComment> list = new ArrayList<>();
        String sql = "SELECT * FROM forum_comment WHERE id_forum = ? ORDER BY date_creation ASC";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idForum);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Timestamp ts = rs.getTimestamp("date_creation");
            list.add(new ForumComment(
                    rs.getInt("id_comment"),
                    rs.getInt("id_forum"),
                    rs.getInt("id_user"),
                    rs.getString("contenu"),
                    ts != null ? ts.toLocalDateTime() : null
            ));
        }
        return list;
    }

    public ForumComment getById(int idComment) throws SQLException {
        String sql = "SELECT * FROM forum_comment WHERE id_comment = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idComment);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            Timestamp ts = rs.getTimestamp("date_creation");
            return new ForumComment(
                    rs.getInt("id_comment"),
                    rs.getInt("id_forum"),
                    rs.getInt("id_user"),
                    rs.getString("contenu"),
                    ts != null ? ts.toLocalDateTime() : null
            );
        }
        return null;
    }
}
