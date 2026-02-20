package tn.esprit.entities;

public class VoyageDestination {
    private int idVoyage;
    private int idDestination;

    public VoyageDestination() {
    }

    public VoyageDestination(int idVoyage, int idDestination) {
        this.idVoyage = idVoyage;
        this.idDestination = idDestination;
    }

    public int getIdVoyage() { return idVoyage; }
    public void setIdVoyage(int idVoyage) { this.idVoyage = idVoyage; }
    public int getIdDestination() { return idDestination; }
    public void setIdDestination(int idDestination) { this.idDestination = idDestination; }
}
