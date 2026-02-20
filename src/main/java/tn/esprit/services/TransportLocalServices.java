package tn.esprit.services;

import tn.esprit.config.DBConnection;
import tn.esprit.entities.TransportLocal;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransportLocalServices {
    private Connection cnx;

    public TransportLocalServices() {
        cnx = DBConnection.getInstance().getCnx();
    }

    public void ajouter(TransportLocal t) throws SQLException {
        String sql = "INSERT INTO transportlocal (compagnie, type_transport, pays_depart, pays_arrivee, date_depart, date_retour, prix, id_voyage) VALUES (?,?,?,?,?,?,?,?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, t.getCompagnie());
        ps.setString(2, t.getTypeTransport().name());
        ps.setString(3, t.getPaysDepart());
        ps.setString(4, t.getPaysArrivee());
        ps.setDate(5, Date.valueOf(t.getDateDepart()));
        ps.setDate(6, Date.valueOf(t.getDateRetour()));
        ps.setBigDecimal(7, t.getPrix());
        ps.setInt(8, t.getIdVoyage());
        ps.executeUpdate();
    }

    public void modifier(TransportLocal t) throws SQLException {
        String sql = "UPDATE transportlocal SET compagnie=?, type_transport=?, pays_depart=?, pays_arrivee=?, date_depart=?, date_retour=?, prix=?, id_voyage=? WHERE id_transport=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, t.getCompagnie());
        ps.setString(2, t.getTypeTransport().name());
        ps.setString(3, t.getPaysDepart());
        ps.setString(4, t.getPaysArrivee());
        ps.setDate(5, Date.valueOf(t.getDateDepart()));
        ps.setDate(6, Date.valueOf(t.getDateRetour()));
        ps.setBigDecimal(7, t.getPrix());
        ps.setInt(8, t.getIdVoyage());
        ps.setInt(9, t.getIdTransport());
        ps.executeUpdate();
    }

    public void supprimer(int idTransport) throws SQLException {
        String sql = "DELETE FROM transportlocal WHERE id_transport=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idTransport);
        ps.executeUpdate();
    }

    public List<TransportLocal> afficher() throws SQLException {
        List<TransportLocal> list = new ArrayList<>();
        String sql = "SELECT * FROM transportlocal";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            TransportLocal t = new TransportLocal(
                    rs.getInt("id_transport"),
                    rs.getString("compagnie"),
                    TransportLocal.TypeTransport.valueOf(rs.getString("type_transport")),
                    rs.getString("pays_depart"),
                    rs.getString("pays_arrivee"),
                    rs.getDate("date_depart").toLocalDate(),
                    rs.getDate("date_retour").toLocalDate(),
                    rs.getBigDecimal("prix"),
                    rs.getInt("id_voyage")
            );
            list.add(t);
        }
        return list;
    }

    /** Returns transport options for the given voyage. */
    public List<TransportLocal> getByVoyage(int idVoyage) throws SQLException {
        List<TransportLocal> list = new ArrayList<>();
        String sql = "SELECT * FROM transportlocal WHERE id_voyage = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idVoyage);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    /** Returns transports linked to this voyage OR not linked to any voyage (id_voyage IS NULL or 0). Use this so users see all available transports when booking after a voyage. */
    public List<TransportLocal> getByVoyageOrUnassigned(int idVoyage) throws SQLException {
        List<TransportLocal> list = new ArrayList<>();
        String sql = "SELECT * FROM transportlocal WHERE id_voyage = ? OR id_voyage IS NULL OR id_voyage = 0";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idVoyage);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    private TransportLocal mapRow(ResultSet rs) throws SQLException {
        int idVoyageCol = rs.getInt("id_voyage");
        if (rs.wasNull()) idVoyageCol = 0;
        return new TransportLocal(
                rs.getInt("id_transport"),
                rs.getString("compagnie"),
                TransportLocal.TypeTransport.valueOf(rs.getString("type_transport")),
                rs.getString("pays_depart"),
                rs.getString("pays_arrivee"),
                rs.getDate("date_depart").toLocalDate(),
                rs.getDate("date_retour").toLocalDate(),
                rs.getBigDecimal("prix"),
                idVoyageCol
        );
    }
}
