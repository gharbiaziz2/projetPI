package tn.esprit.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Voyage {
    private int idVoyage;
    private String nomVoyage;
    private LocalDate dateDepart;
    private LocalDate dateRetour;
    private BigDecimal prix;
    private int placesDisponibles;
    private String statut;
    private int idGuide;
    private String image;

    public Voyage() {
    }

    public Voyage(int idVoyage, String nomVoyage, LocalDate dateDepart, LocalDate dateRetour,
            BigDecimal prix, int placesDisponibles, String statut, int idGuide, String image) {
        this.idVoyage = idVoyage;
        this.nomVoyage = nomVoyage;
        this.dateDepart = dateDepart;
        this.dateRetour = dateRetour;
        this.prix = prix;
        this.placesDisponibles = placesDisponibles;
        this.statut = statut;
        this.idGuide = idGuide;
        this.image = image;
    }

    public int getIdVoyage() {
        return idVoyage;
    }

    public void setIdVoyage(int idVoyage) {
        this.idVoyage = idVoyage;
    }

    public String getNomVoyage() {
        return nomVoyage;
    }

    public void setNomVoyage(String nomVoyage) {
        this.nomVoyage = nomVoyage;
    }

    /** @deprecated use getNomVoyage() */
    public String getTypeVoyage() {
        return nomVoyage;
    }

    /** @deprecated use setNomVoyage() */
    public void setTypeVoyage(String v) {
        this.nomVoyage = v;
    }

    public LocalDate getDateDepart() {
        return dateDepart;
    }

    public void setDateDepart(LocalDate dateDepart) {
        this.dateDepart = dateDepart;
    }

    public LocalDate getDateRetour() {
        return dateRetour;
    }

    public void setDateRetour(LocalDate dateRetour) {
        this.dateRetour = dateRetour;
    }

    public BigDecimal getPrix() {
        return prix;
    }

    public void setPrix(BigDecimal prix) {
        this.prix = prix;
    }

    public int getPlacesDisponibles() {
        return placesDisponibles;
    }

    public void setPlacesDisponibles(int placesDisponibles) {
        this.placesDisponibles = placesDisponibles;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public int getIdGuide() {
        return idGuide;
    }

    public void setIdGuide(int idGuide) {
        this.idGuide = idGuide;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
