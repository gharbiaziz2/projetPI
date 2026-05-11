package tn.esprit.services;

import tn.esprit.config.DBConnection;
import tn.esprit.entities.ProgrammeVoyage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProgrammeVoyageServices {
    private Connection cnx;

    public ProgrammeVoyageServices() {
        cnx = DBConnection.getInstance().getCnx();
    }

    public void ajouter(ProgrammeVoyage p) throws SQLException {
        String sql = "INSERT INTO programme_voyage (id_voyage, jour, titre, description, lieu, heure_debut, heure_fin, image) VALUES (?,?,?,?,?,?,?,?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, p.getIdVoyage());
        ps.setInt(2, p.getJour());
        ps.setString(3, p.getTitre());
        ps.setString(4, p.getDescription());
        ps.setString(5, p.getLieu());
        ps.setTime(6, p.getHeureDebut() != null ? Time.valueOf(p.getHeureDebut()) : null);
        ps.setTime(7, p.getHeureFin() != null ? Time.valueOf(p.getHeureFin()) : null);
        ps.setString(8, p.getImage());
        ps.executeUpdate();
    }

    public void modifier(ProgrammeVoyage p) throws SQLException {
        String sql = "UPDATE programme_voyage SET id_voyage=?, jour=?, titre=?, description=?, lieu=?, heure_debut=?, heure_fin=?, image=? WHERE id_programme=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, p.getIdVoyage());
        ps.setInt(2, p.getJour());
        ps.setString(3, p.getTitre());
        ps.setString(4, p.getDescription());
        ps.setString(5, p.getLieu());
        ps.setTime(6, p.getHeureDebut() != null ? Time.valueOf(p.getHeureDebut()) : null);
        ps.setTime(7, p.getHeureFin() != null ? Time.valueOf(p.getHeureFin()) : null);
        ps.setString(8, p.getImage());
        ps.setInt(9, p.getIdProgramme());
        ps.executeUpdate();
    }

    public void supprimer(int idProgramme) throws SQLException {
        String sql = "DELETE FROM programme_voyage WHERE id_programme=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idProgramme);
        ps.executeUpdate();
    }

    public List<ProgrammeVoyage> afficher() throws SQLException {
        List<ProgrammeVoyage> list = new ArrayList<>();
        String sql = "SELECT * FROM programme_voyage ORDER BY id_voyage, jour, heure_debut";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    public List<ProgrammeVoyage> getByVoyage(int idVoyage) throws SQLException {
        List<ProgrammeVoyage> list = new ArrayList<>();
        String sql = "SELECT * FROM programme_voyage WHERE id_voyage=? ORDER BY jour, heure_debut";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idVoyage);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    private ProgrammeVoyage mapRow(ResultSet rs) throws SQLException {
        ProgrammeVoyage p = new ProgrammeVoyage();
        p.setIdProgramme(rs.getInt("id_programme"));
        p.setIdVoyage(rs.getInt("id_voyage"));
        p.setJour(rs.getInt("jour"));
        p.setTitre(rs.getString("titre"));
        p.setDescription(rs.getString("description"));
        p.setLieu(rs.getString("lieu"));
        Time hd = rs.getTime("heure_debut");
        if (hd != null) p.setHeureDebut(hd.toLocalTime());
        Time hf = rs.getTime("heure_fin");
        if (hf != null) p.setHeureFin(hf.toLocalTime());
        p.setImage(rs.getString("image"));
        return p;
    }
}
