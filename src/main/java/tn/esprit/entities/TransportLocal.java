package tn.esprit.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TransportLocal {
    private int idTransport;
    private String compagnie;
    private TypeTransport typeTransport;
    private String paysDepart;
    private String paysArrivee;
    private LocalDate dateDepart;
    private LocalDate dateRetour;
    private BigDecimal prix;
    private int idVoyage;
    private int nbrPlaces;

    public enum TypeTransport { VOL, VOITURE, TAXI, BATEAU, MOTO }

    public TransportLocal() {
        this.nbrPlaces = 10;
    }

    public TransportLocal(int idTransport, String compagnie, TypeTransport typeTransport,
                          String paysDepart, String paysArrivee, LocalDate dateDepart, LocalDate dateRetour,
                          BigDecimal prix, int idVoyage) {
        this(idTransport, compagnie, typeTransport, paysDepart, paysArrivee, dateDepart, dateRetour, prix, idVoyage, 10);
    }

    public TransportLocal(int idTransport, String compagnie, TypeTransport typeTransport,
                          String paysDepart, String paysArrivee, LocalDate dateDepart, LocalDate dateRetour,
                          BigDecimal prix, int idVoyage, int nbrPlaces) {
        this.idTransport = idTransport;
        this.compagnie = compagnie;
        this.typeTransport = typeTransport;
        this.paysDepart = paysDepart;
        this.paysArrivee = paysArrivee;
        this.dateDepart = dateDepart;
        this.dateRetour = dateRetour;
        this.prix = prix;
        this.idVoyage = idVoyage;
        this.nbrPlaces = nbrPlaces;
    }

    public int getIdTransport() { return idTransport; }
    public void setIdTransport(int idTransport) { this.idTransport = idTransport; }
    public String getCompagnie() { return compagnie; }
    public void setCompagnie(String compagnie) { this.compagnie = compagnie; }
    public TypeTransport getTypeTransport() { return typeTransport; }
    public void setTypeTransport(TypeTransport typeTransport) { this.typeTransport = typeTransport; }
    public String getPaysDepart() { return paysDepart; }
    public void setPaysDepart(String paysDepart) { this.paysDepart = paysDepart; }
    public String getPaysArrivee() { return paysArrivee; }
    public void setPaysArrivee(String paysArrivee) { this.paysArrivee = paysArrivee; }
    public LocalDate getDateDepart() { return dateDepart; }
    public void setDateDepart(LocalDate dateDepart) { this.dateDepart = dateDepart; }
    public LocalDate getDateRetour() { return dateRetour; }
    public void setDateRetour(LocalDate dateRetour) { this.dateRetour = dateRetour; }
    public BigDecimal getPrix() { return prix; }
    public void setPrix(BigDecimal prix) { this.prix = prix; }
    public int getIdVoyage() { return idVoyage; }
    public void setIdVoyage(int idVoyage) { this.idVoyage = idVoyage; }
    public int getNbrPlaces() { return nbrPlaces; }
    public void setNbrPlaces(int nbrPlaces) { this.nbrPlaces = nbrPlaces; }
}
