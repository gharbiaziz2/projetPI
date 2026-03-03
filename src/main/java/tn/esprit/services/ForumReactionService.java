package tn.esprit.services;

import tn.esprit.config.DBConnection;
import tn.esprit.entities.Notification;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles like/dislike reactions for forum messages and comments.
 * reaction: 1 = like, -1 = dislike
 */
public class ForumReactionService {
    private Connection cnx;
    private final ForumServices forumService = new ForumServices();
    private final ForumCommentServices commentService = new ForumCommentServices();
    private final NotificationServices notificationService = new NotificationServices();
    private final UserServices userService = new UserServices();

    public ForumReactionService() {
        cnx = DBConnection.getInstance().getCnx();
    }

    // --- Forum message reactions ---
    public int getUserForumReaction(int idUser, int idForum) throws SQLException {
        String sql = "SELECT reaction FROM forum_reaction WHERE id_user = ? AND id_forum = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idUser);
        ps.setInt(2, idForum);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getInt("reaction") : 0;
    }

    public void setForumReaction(int idUser, int idForum, int reaction) throws SQLException {
        if (reaction == 0) {
            String sql = "DELETE FROM forum_reaction WHERE id_user = ? AND id_forum = ?";
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, idUser);
            ps.setInt(2, idForum);
            ps.executeUpdate();
        } else {
            String sql = "INSERT INTO forum_reaction (id_user, id_forum, reaction) VALUES (?,?,?) ON DUPLICATE KEY UPDATE reaction = ?";
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, idUser);
            ps.setInt(2, idForum);
            ps.setInt(3, reaction);
            ps.setInt(4, reaction);
            ps.executeUpdate();
            notifyForumAuthorOnReaction(idUser, idForum, reaction);
        }
    }

    private void notifyForumAuthorOnReaction(int reactorId, int idForum, int reaction) {
        try {
            var f = forumService.getById(idForum);
            if (f == null || f.getIdUser() == reactorId) return;
            var actor = userService.getById(reactorId);
            String actorName = actor != null ? (actor.getNom() + " " + actor.getPrenom()).trim() : "Un utilisateur";
            String action = reaction == 1 ? "aime" : "n'aime pas";
            String msg = actorName + " " + action + " votre message sur le forum.";
            Notification n = new Notification(0, f.getIdUser(), msg, LocalDateTime.now(), false);
            notificationService.ajouter(n);
        } catch (SQLException ignored) { /* best-effort */ }
    }

    public Map<String, Integer> getForumReactionCounts(int idForum) throws SQLException {
        Map<String, Integer> m = new HashMap<>();
        m.put("likes", 0);
        m.put("dislikes", 0);
        String sql = "SELECT reaction, COUNT(*) as c FROM forum_reaction WHERE id_forum = ? GROUP BY reaction";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idForum);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            int r = rs.getInt("reaction");
            int c = rs.getInt("c");
            if (r == 1) m.put("likes", c);
            else if (r == -1) m.put("dislikes", c);
        }
        return m;
    }

    // --- Comment reactions ---
    public int getUserCommentReaction(int idUser, int idComment) throws SQLException {
        String sql = "SELECT reaction FROM forum_reaction_comment WHERE id_user = ? AND id_comment = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idUser);
        ps.setInt(2, idComment);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getInt("reaction") : 0;
    }

    public void setCommentReaction(int idUser, int idComment, int reaction) throws SQLException {
        if (reaction == 0) {
            String sql = "DELETE FROM forum_reaction_comment WHERE id_user = ? AND id_comment = ?";
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, idUser);
            ps.setInt(2, idComment);
            ps.executeUpdate();
        } else {
            String sql = "INSERT INTO forum_reaction_comment (id_user, id_comment, reaction) VALUES (?,?,?) ON DUPLICATE KEY UPDATE reaction = ?";
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, idUser);
            ps.setInt(2, idComment);
            ps.setInt(3, reaction);
            ps.setInt(4, reaction);
            ps.executeUpdate();
            notifyCommentAuthorOnReaction(idUser, idComment, reaction);
        }
    }

    private void notifyCommentAuthorOnReaction(int reactorId, int idComment, int reaction) {
        try {
            var c = commentService.getById(idComment);
            if (c == null || c.getIdUser() == reactorId) return;
            var actor = userService.getById(reactorId);
            String actorName = actor != null ? (actor.getNom() + " " + actor.getPrenom()).trim() : "Un utilisateur";
            String action = reaction == 1 ? "aime" : "n'aime pas";
            String msg = actorName + " " + action + " votre commentaire.";
            Notification n = new Notification(0, c.getIdUser(), msg, LocalDateTime.now(), false);
            notificationService.ajouter(n);
        } catch (SQLException ignored) { /* best-effort */ }
    }

    public Map<String, Integer> getCommentReactionCounts(int idComment) throws SQLException {
        Map<String, Integer> m = new HashMap<>();
        m.put("likes", 0);
        m.put("dislikes", 0);
        String sql = "SELECT reaction, COUNT(*) as c FROM forum_reaction_comment WHERE id_comment = ? GROUP BY reaction";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idComment);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            int r = rs.getInt("reaction");
            int c = rs.getInt("c");
            if (r == 1) m.put("likes", c);
            else if (r == -1) m.put("dislikes", c);
        }
        return m;
    }
}
