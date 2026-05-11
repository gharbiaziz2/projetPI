package tn.esprit.services;

import tn.esprit.config.DBConnection;
import tn.esprit.entities.VoyageSuggestion;

import java.sql.*;

import java.util.ArrayList;
import java.util.List;

public class VoyageSuggestionServices {
    private Connection cnx;

    public VoyageSuggestionServices() {
        cnx = DBConnection.getInstance().getCnx();
    }

    public List<VoyageSuggestion> afficher() throws SQLException {
        List<VoyageSuggestion> list = new ArrayList<>();
        String sql = "SELECT * FROM voyage_suggestion ORDER BY created_at DESC";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    public void updateStatus(int idSuggestion, String status) throws SQLException {
        String sql = "UPDATE voyage_suggestion SET status=? WHERE id_suggestion=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, status);
        ps.setInt(2, idSuggestion);
        ps.executeUpdate();
    }

    public void supprimer(int idSuggestion) throws SQLException {
        String sql = "DELETE FROM voyage_suggestion WHERE id_suggestion=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idSuggestion);
        ps.executeUpdate();
    }

    private VoyageSuggestion mapRow(ResultSet rs) throws SQLException {
        VoyageSuggestion s = new VoyageSuggestion();
        s.setIdSuggestion(rs.getInt("id_suggestion"));
        int idUser = rs.getInt("id_user");
        s.setIdUser(rs.wasNull() ? null : idUser);
        s.setFullName(rs.getString("full_name"));
        s.setEmail(rs.getString("email"));
        s.setDestination(rs.getString("destination"));
        s.setDepartureCity(rs.getString("departure_city"));
        Date dd = rs.getDate("desired_date");
        if (dd != null) s.setDesiredDate(dd.toLocalDate());
        double budget = rs.getDouble("budget");
        s.setBudget(rs.wasNull() ? null : budget);
        s.setMessage(rs.getString("message"));
        s.setStatus(rs.getString("status"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) s.setCreatedAt(ts.toLocalDateTime());
        return s;
    }
}
