package tn.esprit.services;

import tn.esprit.config.DBConnection;
import tn.esprit.entities.Activite;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActiviteServices {
    private Connection cnx;

    public ActiviteServices() {
        cnx = DBConnection.getInstance().getCnx();
    }

    public void ajouter(Activite a) throws SQLException {
        String sql = "INSERT INTO activite (nom, description, prix, duree) VALUES (?,?,?,?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, a.getNom());
        ps.setString(2, a.getDescription());
        ps.setBigDecimal(3, a.getPrix());
        ps.setInt(4, a.getDuree());
        ps.executeUpdate();
    }

    public void modifier(Activite a) throws SQLException {
        String sql = "UPDATE activite SET nom=?, description=?, prix=?, duree=? WHERE id_activite=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, a.getNom());
        ps.setString(2, a.getDescription());
        ps.setBigDecimal(3, a.getPrix());
        ps.setInt(4, a.getDuree());
        ps.setInt(5, a.getIdActivite());
        ps.executeUpdate();
    }

    public void supprimer(int idActivite) throws SQLException {
        String sql = "DELETE FROM activite WHERE id_activite=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idActivite);
        ps.executeUpdate();
    }

    public List<Activite> afficher() throws SQLException {
        List<Activite> list = new ArrayList<>();
        String sql = "SELECT * FROM activite";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            Activite a = new Activite(
                    rs.getInt("id_activite"),
                    rs.getString("nom"),
                    rs.getString("description"),
                    rs.getBigDecimal("prix"),
                    rs.getInt("duree")
            );
            list.add(a);
        }
        return list;
    }
}
