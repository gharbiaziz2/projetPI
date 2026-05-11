package tn.esprit.entities;

import java.time.LocalDate;

public class Voyage {
    private int idVoyage;
    private String typeVoyage;
    private LocalDate dateDepart;
    private LocalDate dateRetour;
    private double prix;
    private int placesDisponibles;
    private String statut;
    private int idGuide;
    private String image;
    private int idUserCreateur; // ID du client qui a proposé le voyage
    private boolean estPropositionClient; // true si c'est une proposition de client

    public Voyage() {
    }

    public Voyage(int idVoyage, String typeVoyage, LocalDate dateDepart, LocalDate dateRetour,
            double prix, int placesDisponibles, String statut, int idGuide, String image) {
        this.idVoyage = idVoyage;
        this.typeVoyage = typeVoyage;
        this.dateDepart = dateDepart;
        this.dateRetour = dateRetour;
        this.prix = prix;
        this.placesDisponibles = placesDisponibles;
        this.statut = statut;
        this.idGuide = idGuide;
        this.image = image;
        this.estPropositionClient = false;
    }

    public Voyage(int idVoyage, String typeVoyage, LocalDate dateDepart, LocalDate dateRetour,
            double prix, int placesDisponibles, String statut, int idGuide, String image,
            int idUserCreateur, boolean estPropositionClient) {
        this.idVoyage = idVoyage;
        this.typeVoyage = typeVoyage;
        this.dateDepart = dateDepart;
        this.dateRetour = dateRetour;
        this.prix = prix;
        this.placesDisponibles = placesDisponibles;
        this.statut = statut;
        this.idGuide = idGuide;
        this.image = image;
        this.idUserCreateur = idUserCreateur;
        this.estPropositionClient = estPropositionClient;
    }

    public int getIdVoyage() {
        return idVoyage;
    }

    public void setIdVoyage(int idVoyage) {
        this.idVoyage = idVoyage;
    }

    public String getTypeVoyage() {
        return typeVoyage;
    }

    public void setTypeVoyage(String typeVoyage) {
        this.typeVoyage = typeVoyage;
    }

    /** @deprecated use getTypeVoyage() */
    @Deprecated
    public String getNomVoyage() {
        return typeVoyage;
    }

    /** @deprecated use setTypeVoyage() */
    @Deprecated
    public void setNomVoyage(String v) {
        this.typeVoyage = v;
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

    public double getPrix() {
        return prix;
    }

    public void setPrix(double prix) {
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

    public int getIdUserCreateur() {
        return idUserCreateur;
    }

    public void setIdUserCreateur(int idUserCreateur) {
        this.idUserCreateur = idUserCreateur;
    }

    public boolean isEstPropositionClient() {
        return estPropositionClient;
    }

    public void setEstPropositionClient(boolean estPropositionClient) {
        this.estPropositionClient = estPropositionClient;
    }
}
