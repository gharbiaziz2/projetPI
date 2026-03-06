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
        String sql = "INSERT INTO transportlocal (compagnie, type_transport, pays_depart, pays_arrivee, date_depart, date_retour, prix, id_voyage, nbr_places) VALUES (?,?,?,?,?,?,?,?,?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, t.getCompagnie());
        ps.setString(2, t.getTypeTransport().name());
        ps.setString(3, t.getPaysDepart());
        ps.setString(4, t.getPaysArrivee());
        ps.setDate(5, Date.valueOf(t.getDateDepart()));
        ps.setDate(6, Date.valueOf(t.getDateRetour()));
        ps.setBigDecimal(7, t.getPrix());
        ps.setInt(8, t.getIdVoyage());
        ps.setInt(9, t.getNbrPlaces() > 0 ? t.getNbrPlaces() : 10);
        ps.executeUpdate();
    }

    public void modifier(TransportLocal t) throws SQLException {
        String sql = "UPDATE transportlocal SET compagnie=?, type_transport=?, pays_depart=?, pays_arrivee=?, date_depart=?, date_retour=?, prix=?, id_voyage=?, nbr_places=? WHERE id_transport=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, t.getCompagnie());
        ps.setString(2, t.getTypeTransport().name());
        ps.setString(3, t.getPaysDepart());
        ps.setString(4, t.getPaysArrivee());
        ps.setDate(5, Date.valueOf(t.getDateDepart()));
        ps.setDate(6, Date.valueOf(t.getDateRetour()));
        ps.setBigDecimal(7, t.getPrix());
        ps.setInt(8, t.getIdVoyage());
        ps.setInt(9, t.getNbrPlaces());
        ps.setInt(10, t.getIdTransport());
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
            list.add(mapRow(rs));
        }
        return list;
    }

    /** Returns transport options for the given voyage (excludes transports with 0 places). */
    public List<TransportLocal> getByVoyage(int idVoyage) throws SQLException {
        List<TransportLocal> list = new ArrayList<>();
        String sql = "SELECT * FROM transportlocal WHERE id_voyage = ? AND (nbr_places IS NULL OR nbr_places > 0)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idVoyage);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    /** Returns transports linked to this voyage OR not linked to any voyage (id_voyage IS NULL or 0). Excludes transports with 0 places. */
    public List<TransportLocal> getByVoyageOrUnassigned(int idVoyage) throws SQLException {
        List<TransportLocal> list = new ArrayList<>();
        String sql = "SELECT * FROM transportlocal WHERE (id_voyage = ? OR id_voyage IS NULL OR id_voyage = 0) AND (nbr_places IS NULL OR nbr_places > 0)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idVoyage);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    /** Returns all transports that have at least 1 place (for reservation fallback when voyage has no linked transports). */
    public List<TransportLocal> getAvailableTransports() throws SQLException {
        List<TransportLocal> list = new ArrayList<>();
        String sql = "SELECT * FROM transportlocal WHERE nbr_places IS NULL OR nbr_places > 0";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    /** Decreases nbr_places by 1 when a user reserves this transport. Does nothing if already 0. */
    public void decrementerPlaces(int idTransport) throws SQLException {
        String sql = "UPDATE transportlocal SET nbr_places = GREATEST(0, COALESCE(nbr_places, 10) - 1) WHERE id_transport = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idTransport);
        ps.executeUpdate();
    }

    private TransportLocal mapRow(ResultSet rs) throws SQLException {
        int idVoyageCol = rs.getInt("id_voyage");
        if (rs.wasNull()) idVoyageCol = 0;
        int nbrPlacesCol = 10;
        try {
            nbrPlacesCol = rs.getInt("nbr_places");
            if (rs.wasNull()) nbrPlacesCol = 10;
        } catch (SQLException e) {
            nbrPlacesCol = 10;
        }
        return new TransportLocal(
                rs.getInt("id_transport"),
                rs.getString("compagnie"),
                TransportLocal.TypeTransport.valueOf(rs.getString("type_transport")),
                rs.getString("pays_depart"),
                rs.getString("pays_arrivee"),
                rs.getDate("date_depart").toLocalDate(),
                rs.getDate("date_retour").toLocalDate(),
                rs.getBigDecimal("prix"),
                idVoyageCol,
                nbrPlacesCol
        );
    }
}
