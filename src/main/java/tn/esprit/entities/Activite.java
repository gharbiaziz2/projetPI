package tn.esprit.entities;

import java.math.BigDecimal;

public class Activite {
    private int idActivite;
    private String nom;
    private String description;
    private BigDecimal prix;
    private int duree;

    public Activite() {
    }

    public Activite(int idActivite, String nom, String description, BigDecimal prix, int duree) {
        this.idActivite = idActivite;
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.duree = duree;
    }

    public int getIdActivite() { return idActivite; }
    public void setIdActivite(int idActivite) { this.idActivite = idActivite; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrix() { return prix; }
    public void setPrix(BigDecimal prix) { this.prix = prix; }
    public int getDuree() { return duree; }
    public void setDuree(int duree) { this.duree = duree; }
}
