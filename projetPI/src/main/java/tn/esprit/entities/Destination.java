package tn.esprit.entities;

public class Destination {
    private int idDestination;
    private String paysDepart;
    private String paysArrivee;
    private String description;

    public Destination() {
    }

    public Destination(int idDestination, String paysDepart, String paysArrivee, String description) {
        this.idDestination = idDestination;
        this.paysDepart = paysDepart;
        this.paysArrivee = paysArrivee;
        this.description = description;
    }

    public int getIdDestination() { return idDestination; }
    public void setIdDestination(int idDestination) { this.idDestination = idDestination; }
    public String getPaysDepart() { return paysDepart; }
    public void setPaysDepart(String paysDepart) { this.paysDepart = paysDepart; }
    public String getPaysArrivee() { return paysArrivee; }
    public void setPaysArrivee(String paysArrivee) { this.paysArrivee = paysArrivee; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
