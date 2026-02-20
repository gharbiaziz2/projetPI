package tn.esprit.services;

import tn.esprit.config.DBConnection;
import tn.esprit.entities.Destination;
import tn.esprit.entities.VoyageDestination;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VoyageDestinationServices {
    private Connection cnx;

    public VoyageDestinationServices() {
        cnx = DBConnection.getInstance().getCnx();
    }

    public void ajouter(VoyageDestination vd) throws SQLException {
        String sql = "INSERT INTO voyage_destination (id_voyage, id_destination) VALUES (?,?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, vd.getIdVoyage());
        ps.setInt(2, vd.getIdDestination());
        ps.executeUpdate();
    }

    public void supprimer(int idVoyage, int idDestination) throws SQLException {
        String sql = "DELETE FROM voyage_destination WHERE id_voyage=? AND id_destination=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idVoyage);
        ps.setInt(2, idDestination);
        ps.executeUpdate();
    }

    /** Remove all destination links for a voyage (e.g. before re-assigning on edit). */
    public void supprimerByVoyage(int idVoyage) throws SQLException {
        String sql = "DELETE FROM voyage_destination WHERE id_voyage=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idVoyage);
        ps.executeUpdate();
    }

    public List<VoyageDestination> afficher() throws SQLException {
        List<VoyageDestination> list = new ArrayList<>();
        String sql = "SELECT * FROM voyage_destination";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            VoyageDestination vd = new VoyageDestination(rs.getInt("id_voyage"), rs.getInt("id_destination"));
            list.add(vd);
        }
        return list;
    }

    /** Returns destinations linked to the given voyage. */
    public List<Destination> getDestinationsForVoyage(int idVoyage) throws SQLException {
        List<Destination> list = new ArrayList<>();
        String sql = "SELECT d.id_destination, d.pays_depart, d.pays_arrivee, d.description FROM destination d INNER JOIN voyage_destination vd ON d.id_destination = vd.id_destination WHERE vd.id_voyage = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idVoyage);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(new Destination(
                    rs.getInt("id_destination"),
                    rs.getString("pays_depart"),
                    rs.getString("pays_arrivee"),
                    rs.getString("description")
            ));
        }
        return list;
    }
}
