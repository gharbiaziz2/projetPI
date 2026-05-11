package tn.esprit.entities;

public class HotelChambre {
    private int idChambre;
    private int idHotel;
    private String numeroChambre;
    private String typeChambre;
    private double prixChambre;
    private String description;
    private String image;
    private boolean disponible;

    public HotelChambre() {
        this.disponible = true;
    }

    public HotelChambre(int idChambre, int idHotel, String numeroChambre, String typeChambre,
                        double prixChambre, String description, String image, boolean disponible) {
        this.idChambre = idChambre;
        this.idHotel = idHotel;
        this.numeroChambre = numeroChambre;
        this.typeChambre = typeChambre;
        this.prixChambre = prixChambre;
        this.description = description;
        this.image = image;
        this.disponible = disponible;
    }

    public int getIdChambre() { return idChambre; }
    public void setIdChambre(int idChambre) { this.idChambre = idChambre; }
    public int getIdHotel() { return idHotel; }
    public void setIdHotel(int idHotel) { this.idHotel = idHotel; }
    public String getNumeroChambre() { return numeroChambre; }
    public void setNumeroChambre(String numeroChambre) { this.numeroChambre = numeroChambre; }
    public String getTypeChambre() { return typeChambre; }
    public void setTypeChambre(String typeChambre) { this.typeChambre = typeChambre; }
    public double getPrixChambre() { return prixChambre; }
    public void setPrixChambre(double prixChambre) { this.prixChambre = prixChambre; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }
}
