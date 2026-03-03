package tn.esprit.entities;

public class VoyageActivite {
    private int idVoyage;
    private int idActivite;

    public VoyageActivite() {
    }

    public VoyageActivite(int idVoyage, int idActivite) {
        this.idVoyage = idVoyage;
        this.idActivite = idActivite;
    }

    public int getIdVoyage() { return idVoyage; }
    public void setIdVoyage(int idVoyage) { this.idVoyage = idVoyage; }
    public int getIdActivite() { return idActivite; }
    public void setIdActivite(int idActivite) { this.idActivite = idActivite; }
}
