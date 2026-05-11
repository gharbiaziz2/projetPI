package tn.esprit.services;

import tn.esprit.config.DBConnection;
import tn.esprit.entities.Activite;
import tn.esprit.entities.VoyageActivite;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class VoyageActiviteServices {
    private Connection cnx;

    public VoyageActiviteServices() {
        cnx = DBConnection.getInstance().getCnx();
    }

    public void ajouter(VoyageActivite va) throws SQLException {
        String sql = "INSERT INTO voyage_activite (id_voyage, id_activite) VALUES (?,?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, va.getIdVoyage());
        ps.setInt(2, va.getIdActivite());
        ps.executeUpdate();
    }

    public void supprimer(int idVoyage, int idActivite) throws SQLException {
        String sql = "DELETE FROM voyage_activite WHERE id_voyage=? AND id_activite=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idVoyage);
        ps.setInt(2, idActivite);
        ps.executeUpdate();
    }

    /** Remove all activity links for a voyage (e.g. before re-assigning on edit). */
    public void supprimerByVoyage(int idVoyage) throws SQLException {
        String sql = "DELETE FROM voyage_activite WHERE id_voyage=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idVoyage);
        ps.executeUpdate();
    }

    public List<VoyageActivite> afficher() throws SQLException {
        List<VoyageActivite> list = new ArrayList<>();
        String sql = "SELECT * FROM voyage_activite";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            VoyageActivite va = new VoyageActivite(rs.getInt("id_voyage"), rs.getInt("id_activite"));
            list.add(va);
        }
        return list;
    }

    /** Returns activities linked to the given voyage. */
    public List<Activite> getActivitesForVoyage(int idVoyage) throws SQLException {
        List<Activite> list = new ArrayList<>();
        String sql = "SELECT a.id_activite, a.nom, a.description, a.prix, a.duree, a.latitude, a.longitude, a.date_activite, a.photo FROM activite a INNER JOIN voyage_activite va ON a.id_activite = va.id_activite WHERE va.id_voyage = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idVoyage);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Date dateVal = rs.getDate("date_activite");
            LocalDate localDate = dateVal != null ? dateVal.toLocalDate() : null;
            double latVal = rs.getDouble("latitude");
            Double lat = rs.wasNull() ? null : latVal;
            double lonVal = rs.getDouble("longitude");
            Double lon = rs.wasNull() ? null : lonVal;
            list.add(new Activite(
                    rs.getInt("id_activite"),
                    rs.getString("nom"),
                    rs.getString("description"),
                    rs.getDouble("prix"),
                    rs.getInt("duree"),
                    lat,
                    lon,
                    localDate,
                    rs.getString("photo")
            ));
        }
        return list;
    }
}
