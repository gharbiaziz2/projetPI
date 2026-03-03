package tn.esprit.entities;

import java.math.BigDecimal;

public class Hotel {
    private int idHotel;
    private String nom;
    private String pays;
    private String ville;
    private BigDecimal prixNuit;
    private String description;
    private Double longitude;
    private Double latitude;
    private String image;

    public Hotel() {
    }

    public Hotel(int idHotel, String nom, String pays, String ville, BigDecimal prixNuit, String description, Double longitude, Double latitude, String image) {
        this.idHotel = idHotel;
        this.nom = nom;
        this.pays = pays;
        this.ville = ville;
        this.prixNuit = prixNuit;
        this.description = description;
        this.longitude = longitude;
        this.latitude = latitude;
        this.image = image;
    }

    public int getIdHotel() { return idHotel; }
    public void setIdHotel(int idHotel) { this.idHotel = idHotel; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPays() { return pays; }
    public void setPays(String pays) { this.pays = pays; }
    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }
    public BigDecimal getPrixNuit() { return prixNuit; }
    public void setPrixNuit(BigDecimal prixNuit) { this.prixNuit = prixNuit; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
}
