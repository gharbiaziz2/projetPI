package tn.esprit.entities;

import java.time.LocalDateTime;

public class Forum {
    private int idForum;
    private String contenu;
    private LocalDateTime dateEnvoi;
    private int idUser;
    private int idVoyage;

    public Forum() {
    }

    public Forum(int idForum, String contenu, LocalDateTime dateEnvoi, int idUser, int idVoyage) {
        this.idForum = idForum;
        this.contenu = contenu;
        this.dateEnvoi = dateEnvoi;
        this.idUser = idUser;
        this.idVoyage = idVoyage;
    }

    public int getIdForum() { return idForum; }
    public void setIdForum(int idForum) { this.idForum = idForum; }
    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }
    public LocalDateTime getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(LocalDateTime dateEnvoi) { this.dateEnvoi = dateEnvoi; }
    public int getIdUser() { return idUser; }
    public void setIdUser(int idUser) { this.idUser = idUser; }
    public int getIdVoyage() { return idVoyage; }
    public void setIdVoyage(int idVoyage) { this.idVoyage = idVoyage; }
}
