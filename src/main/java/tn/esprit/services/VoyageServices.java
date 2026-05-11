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

    /**
     * Throws IllegalArgumentException if idGuide is not a user with role
     * GUIDE_TOURISTIQUE.
     */
    private void validateGuide(int idGuide) throws SQLException {
        User u = userServices.getById(idGuide);
        if (u == null || u.getRole() != User.Role.GUIDE_TOURISTIQUE) {
            throw new IllegalArgumentException("Le guide doit être un utilisateur avec le rôle Guide touristique.");
        }
    }

    public void ajouter(Voyage v) throws SQLException {
        validateGuide(v.getIdGuide());
        String sql = "INSERT INTO voyage (type_voyage, date_depart, date_retour, prix, places_disponibles, statut, id_guide, image) VALUES (?,?,?,?,?,?,?,?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, v.getTypeVoyage());
        ps.setDate(2, Date.valueOf(v.getDateDepart()));
        ps.setDate(3, Date.valueOf(v.getDateRetour()));
        ps.setDouble(4, v.getPrix());
        ps.setInt(5, v.getPlacesDisponibles());
        ps.setString(6, v.getStatut());
        ps.setInt(7, v.getIdGuide());
        ps.setString(8, v.getImage());
        ps.executeUpdate();
    }

    /** Inserts voyage and returns the generated id_voyage. */
    public int ajouterAndReturnId(Voyage v) throws SQLException {
        validateGuide(v.getIdGuide());
        String sql = "INSERT INTO voyage (type_voyage, date_depart, date_retour, prix, places_disponibles, statut, id_guide, image) VALUES (?,?,?,?,?,?,?,?)";
        PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, v.getTypeVoyage());
        ps.setDate(2, Date.valueOf(v.getDateDepart()));
        ps.setDate(3, Date.valueOf(v.getDateRetour()));
        ps.setDouble(4, v.getPrix());
        ps.setInt(5, v.getPlacesDisponibles());
        ps.setString(6, v.getStatut());
        ps.setInt(7, v.getIdGuide());
        ps.setString(8, v.getImage());
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
        String sql = "UPDATE voyage SET type_voyage=?, date_depart=?, date_retour=?, prix=?, places_disponibles=?, statut=?, id_guide=?, image=? WHERE id_voyage=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, v.getTypeVoyage());
        ps.setDate(2, Date.valueOf(v.getDateDepart()));
        ps.setDate(3, Date.valueOf(v.getDateRetour()));
        ps.setDouble(4, v.getPrix());
        ps.setInt(5, v.getPlacesDisponibles());
        ps.setString(6, v.getStatut());
        ps.setInt(7, v.getIdGuide());
        ps.setString(8, v.getImage());
        ps.setInt(9, v.getIdVoyage());
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
                    rs.getDouble("prix"),
                    rs.getInt("places_disponibles"),
                    rs.getString("statut"),
                    rs.getInt("id_guide"),
                    rs.getString("image"));
            list.add(v);
        }
        return list;
    }

    public List<Voyage> getVoyagesCritiques() throws SQLException {
        List<Voyage> voyagesCritiques = new ArrayList<>();
        // SELECT où DATEDIFF(date_depart, CURDATE()) <= 15 et >= 0, et places_disponibles >= 5
        String query = "SELECT * FROM voyage WHERE DATEDIFF(date_depart, CURDATE()) <= 15 " +
                       "AND DATEDIFF(date_depart, CURDATE()) >= 0 " +
                       "AND places_disponibles >= 5";
        
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                Voyage v = new Voyage();
                v.setIdVoyage(rs.getInt("id_voyage"));
                v.setTypeVoyage(rs.getString("type_voyage"));
                // Conversion de java.sql.Date en LocalDate
                if (rs.getDate("date_depart") != null) {
                    v.setDateDepart(rs.getDate("date_depart").toLocalDate());
                }
                if (rs.getDate("date_retour") != null) {
                    v.setDateRetour(rs.getDate("date_retour").toLocalDate());
                }
                v.setPrix(rs.getDouble("prix"));
                v.setPlacesDisponibles(rs.getInt("places_disponibles"));
                v.setStatut(rs.getString("statut"));
                v.setIdGuide(rs.getInt("id_guide"));
                v.setImage(rs.getString("image"));
                voyagesCritiques.add(v);
            }
        }
        return voyagesCritiques;
    }

    public void appliquerPromo(int idVoyage) throws SQLException {
        // UPDATE avec prix = prix * 0.8
        String query = "UPDATE voyage SET prix = prix * 0.8 WHERE id_voyage = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, idVoyage);
            pst.executeUpdate();
        }
    }

    public List<String> getAllClientEmails() throws SQLException {
        List<String> emails = new ArrayList<>();
        // SELECT pour les users avec rôle 'CLIENT'
        String query = "SELECT email FROM user WHERE role = 'CLIENT'";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                emails.add(rs.getString("email"));
            }
        }
        return emails;
    }

    /**
     * Permet aux clients de proposer un voyage (en attente d'approbation admin)
     */
    public int ajouterPropositionVoyage(Voyage v, int idUserClient) throws SQLException {
        String sql = "INSERT INTO voyage (type_voyage, date_depart, date_retour, prix, places_disponibles, statut, id_guide, image, id_user_createur, est_proposition_client) " +
                     "VALUES (?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, v.getTypeVoyage());
        ps.setDate(2, Date.valueOf(v.getDateDepart()));
        ps.setDate(3, Date.valueOf(v.getDateRetour()));
        ps.setDouble(4, v.getPrix());
        ps.setInt(5, v.getPlacesDisponibles());
        ps.setString(6, "EN ATTENTE"); // Statut initial: EN ATTENTE
        ps.setInt(7, 0); // No guide initially
        ps.setString(8, v.getImage() != null ? v.getImage() : "");
        ps.setInt(9, idUserClient);
        ps.setBoolean(10, true);
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
        throw new SQLException("Impossible de créer la proposition de voyage");
    }

    /**
     * Récupère toutes les propositions de voyage en attente d'approbation
     */
    public List<Voyage> obtenirPropositionsEnAttente() throws SQLException {
        List<Voyage> list = new ArrayList<>();
        String sql = "SELECT * FROM voyage WHERE est_proposition_client = true AND statut = 'EN ATTENTE'";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Voyage v = new Voyage(
                        rs.getInt("id_voyage"),
                        rs.getString("type_voyage"),
                        rs.getDate("date_depart").toLocalDate(),
                        rs.getDate("date_retour").toLocalDate(),
                        rs.getDouble("prix"),
                        rs.getInt("places_disponibles"),
                        rs.getString("statut"),
                        rs.getInt("id_guide"),
                        rs.getString("image"),
                        rs.getInt("id_user_createur"),
                        rs.getBoolean("est_proposition_client")
                );
                list.add(v);
            }
        }
        return list;
    }

    /**
     * Accepte une proposition de voyage (change le statut à ACCEPTEE et assigna un guide)
     */
    public void accepterPropositionVoyage(int idVoyage, int idGuide) throws SQLException {
        validateGuide(idGuide);
        String sql = "UPDATE voyage SET statut = 'ACCEPTEE', id_guide = ? WHERE id_voyage = ? AND est_proposition_client = true";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idGuide);
        ps.setInt(2, idVoyage);
        ps.executeUpdate();
        ps.close();
    }

    /**
     * Refuse une proposition de voyage
     */
    public void refuserPropositionVoyage(int idVoyage) throws SQLException {
        String sql = "UPDATE voyage SET statut = 'REFUSEE' WHERE id_voyage = ? AND est_proposition_client = true";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idVoyage);
        ps.executeUpdate();
        ps.close();
    }
}
