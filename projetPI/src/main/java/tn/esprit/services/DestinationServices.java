package tn.esprit.services;

import tn.esprit.config.DBConnection;
import tn.esprit.entities.Destination;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DestinationServices {
    private Connection cnx;

    public DestinationServices() {
        cnx = DBConnection.getInstance().getCnx();
    }

    public void ajouter(Destination d) throws SQLException {
        String sql = "INSERT INTO destination (pays_depart, pays_arrivee, description) VALUES (?,?,?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, d.getPaysDepart());
        ps.setString(2, d.getPaysArrivee());
        ps.setString(3, d.getDescription());
        ps.executeUpdate();
    }

    public void modifier(Destination d) throws SQLException {
        String sql = "UPDATE destination SET pays_depart=?, pays_arrivee=?, description=? WHERE id_destination=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, d.getPaysDepart());
        ps.setString(2, d.getPaysArrivee());
        ps.setString(3, d.getDescription());
        ps.setInt(4, d.getIdDestination());
        ps.executeUpdate();
    }

    public void supprimer(int idDestination) throws SQLException {
        String sql = "DELETE FROM destination WHERE id_destination=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idDestination);
        ps.executeUpdate();
    }

    public List<Destination> afficher() throws SQLException {
        List<Destination> list = new ArrayList<>();
        String sql = "SELECT * FROM destination";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            Destination d = new Destination(
                    rs.getInt("id_destination"),
                    rs.getString("pays_depart"),
                    rs.getString("pays_arrivee"),
                    rs.getString("description")
            );
            list.add(d);
        }
        return list;
    }
}
