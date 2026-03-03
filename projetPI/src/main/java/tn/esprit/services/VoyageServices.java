package tn.esprit.services;

import tn.esprit.config.DBConnection;
import tn.esprit.entities.User;
import tn.esprit.entities.Voyage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VoyageServices {
    private Connection cnx;
    private final UserServices userServices = new UserServices();

    public VoyageServices() {
        cnx = DBConnection.getInstance().getCnx();
    }

    /** Throws IllegalArgumentException if idGuide is not a user with role GUIDE_TOURISTIQUE. */
    private void validateGuide(int idGuide) throws SQLException {
        User u = userServices.getById(idGuide);
        if (u == null || u.getRole() != User.Role.GUIDE_TOURISTIQUE) {
            throw new IllegalArgumentException("Le guide doit être un utilisateur avec le rôle Guide touristique.");
        }
    }

    public void ajouter(Voyage v) throws SQLException {
        validateGuide(v.getIdGuide());
        String sql = "INSERT INTO voyage (type_voyage, date_depart, date_retour, prix, places_disponibles, statut, id_guide) VALUES (?,?,?,?,?,?,?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, v.getTypeVoyage());
        ps.setDate(2, Date.valueOf(v.getDateDepart()));
        ps.setDate(3, Date.valueOf(v.getDateRetour()));
        ps.setBigDecimal(4, v.getPrix());
        ps.setInt(5, v.getPlacesDisponibles());
        ps.setString(6, v.getStatut());
        ps.setInt(7, v.getIdGuide());
        ps.executeUpdate();
    }

    /** Inserts voyage and returns the generated id_voyage. */
    public int ajouterAndReturnId(Voyage v) throws SQLException {
        validateGuide(v.getIdGuide());
        String sql = "INSERT INTO voyage (type_voyage, date_depart, date_retour, prix, places_disponibles, statut, id_guide) VALUES (?,?,?,?,?,?,?)";
        PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, v.getTypeVoyage());
        ps.setDate(2, Date.valueOf(v.getDateDepart()));
        ps.setDate(3, Date.valueOf(v.getDateRetour()));
        ps.setBigDecimal(4, v.getPrix());
        ps.setInt(5, v.getPlacesDisponibles());
        ps.setString(6, v.getStatut());
        ps.setInt(7, v.getIdGuide());
        ps.executeUpdate();
        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            int id = rs.getInt(1);
            rs.close();
            ps.close();
            return id;
        }
        rs.close();
        ps.close();
        throw new SQLException("Could not get generated id for voyage");
    }

    public void modifier(Voyage v) throws SQLException {
        validateGuide(v.getIdGuide());
        String sql = "UPDATE voyage SET type_voyage=?, date_depart=?, date_retour=?, prix=?, places_disponibles=?, statut=?, id_guide=? WHERE id_voyage=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, v.getTypeVoyage());
        ps.setDate(2, Date.valueOf(v.getDateDepart()));
        ps.setDate(3, Date.valueOf(v.getDateRetour()));
        ps.setBigDecimal(4, v.getPrix());
        ps.setInt(5, v.getPlacesDisponibles());
        ps.setString(6, v.getStatut());
        ps.setInt(7, v.getIdGuide());
        ps.setInt(8, v.getIdVoyage());
        ps.executeUpdate();
    }

    public void supprimer(int idVoyage) throws SQLException {
        String sql = "DELETE FROM voyage WHERE id_voyage=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idVoyage);
        ps.executeUpdate();
    }

    public List<Voyage> afficher() throws SQLException {
        List<Voyage> list = new ArrayList<>();
        String sql = "SELECT * FROM voyage";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            Voyage v = new Voyage(
                    rs.getInt("id_voyage"),
                    rs.getString("type_voyage"),
                    rs.getDate("date_depart").toLocalDate(),
                    rs.getDate("date_retour").toLocalDate(),
                    rs.getBigDecimal("prix"),
                    rs.getInt("places_disponibles"),
                    rs.getString("statut"),
                    rs.getInt("id_guide")
            );
            list.add(v);
        }
        return list;
    }
}
