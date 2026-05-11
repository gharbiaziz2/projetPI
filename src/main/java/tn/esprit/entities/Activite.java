package tn.esprit.entities;

import java.time.LocalDate;

public class Activite {
    private int idActivite;
    private String nom;
    private String description;
    private double prix;
    private int duree;
    private Double latitude;
    private Double longitude;
    private LocalDate dateActivite;
    private String photo;

    public Activite() {
    }

    public Activite(int idActivite, String nom, String description, double prix, int duree, Double latitude, Double longitude, LocalDate dateActivite, String photo) {
        this.idActivite = idActivite;
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.duree = duree;
        this.latitude = latitude;
        this.longitude = longitude;
        this.dateActivite = dateActivite;
        this.photo = photo;
    }

    public int getIdActivite() { return idActivite; }
    public void setIdActivite(int idActivite) { this.idActivite = idActivite; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getPrix() { return prix; }
    public void setPrix(double prix) { this.prix = prix; }
    public int getDuree() { return duree; }
    public void setDuree(int duree) { this.duree = duree; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public LocalDate getDateActivite() { return dateActivite; }
    public void setDateActivite(LocalDate dateActivite) { this.dateActivite = dateActivite; }
    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    /** @deprecated use getDateActivite() */
    @Deprecated
    public LocalDate getDate() { return dateActivite; }
    /** @deprecated use setDateActivite() */
    @Deprecated
    public void setDate(LocalDate date) { this.dateActivite = date; }
}
