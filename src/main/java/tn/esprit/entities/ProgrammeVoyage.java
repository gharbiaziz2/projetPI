package tn.esprit.entities;

import java.time.LocalTime;

public class ProgrammeVoyage {
    private int idProgramme;
    private int idVoyage;
    private int jour;
    private String titre;
    private String description;
    private String lieu;
    private LocalTime heureDebut;
    private LocalTime heureFin;
    private String image;
    private String statut;
    private int idUserCreateur;
    private boolean estPropositionClient;

    public ProgrammeVoyage() {
    }

    public ProgrammeVoyage(int idProgramme, int idVoyage, int jour, String titre, String description,
                           String lieu, LocalTime heureDebut, LocalTime heureFin, String image) {
        this.idProgramme = idProgramme;
        this.idVoyage = idVoyage;
        this.jour = jour;
        this.titre = titre;
        this.description = description;
        this.lieu = lieu;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.image = image;
        this.statut = "EN ATTENTE";
        this.idUserCreateur = 0;
        this.estPropositionClient = false;
    }

    public int getIdProgramme() { return idProgramme; }
    public void setIdProgramme(int idProgramme) { this.idProgramme = idProgramme; }
    public int getIdVoyage() { return idVoyage; }
    public void setIdVoyage(int idVoyage) { this.idVoyage = idVoyage; }
    public int getJour() { return jour; }
    public void setJour(int jour) { this.jour = jour; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }
    public LocalTime getHeureDebut() { return heureDebut; }
    public void setHeureDebut(LocalTime heureDebut) { this.heureDebut = heureDebut; }
    public LocalTime getHeureFin() { return heureFin; }
    public void setHeureFin(LocalTime heureFin) { this.heureFin = heureFin; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public int getIdUserCreateur() { return idUserCreateur; }
    public void setIdUserCreateur(int idUserCreateur) { this.idUserCreateur = idUserCreateur; }
    public boolean isEstPropositionClient() { return estPropositionClient; }
    public void setEstPropositionClient(boolean estPropositionClient) { this.estPropositionClient = estPropositionClient; }
}
