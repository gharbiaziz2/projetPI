package tn.esprit.services;

import tn.esprit.config.DBConnection;
import tn.esprit.entities.Forum;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ForumServices {
    private Connection cnx;

    public ForumServices() {
        cnx = DBConnection.getInstance().getCnx();
    }

    public void ajouter(Forum f) throws SQLException {
        String sql = "INSERT INTO forum (contenu, date_envoi, id_user, id_voyage) VALUES (?,?,?,?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, f.getContenu());
        ps.setTimestamp(2, Timestamp.valueOf(f.getDateEnvoi() != null ? f.getDateEnvoi() : LocalDateTime.now()));
        ps.setInt(3, f.getIdUser());
        ps.setInt(4, f.getIdVoyage());
        ps.executeUpdate();
    }

    public void modifier(Forum f) throws SQLException {
        String sql = "UPDATE forum SET contenu=?, date_envoi=?, id_user=?, id_voyage=? WHERE id_forum=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, f.getContenu());
        ps.setTimestamp(2, f.getDateEnvoi() != null ? Timestamp.valueOf(f.getDateEnvoi()) : null);
        ps.setInt(3, f.getIdUser());
        ps.setInt(4, f.getIdVoyage());
        ps.setInt(5, f.getIdForum());
        ps.executeUpdate();
    }

    public void supprimer(int idForum) throws SQLException {
        String sql = "DELETE FROM forum WHERE id_forum=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idForum);
        ps.executeUpdate();
    }

    public List<Forum> afficher() throws SQLException {
        List<Forum> list = new ArrayList<>();
        String sql = "SELECT * FROM forum";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            Timestamp ts = rs.getTimestamp("date_envoi");
            Forum f = new Forum(
                    rs.getInt("id_forum"),
                    rs.getString("contenu"),
                    ts != null ? ts.toLocalDateTime() : null,
                    rs.getInt("id_user"),
                    rs.getInt("id_voyage")
            );
            list.add(f);
        }
        return list;
    }
}
