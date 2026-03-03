package tn.esprit.entities;

import java.time.LocalDate;

public class User {
    private int idUser;
    private String nom;
    private String prenom;
    private String email;
    private String motDePasse;
    private Role role;
    private Statut statut;
    private LocalDate dateCreation;
    private String telephone;
    private String adresse;
    private String photo;
    private String bio;

    public enum Role { ADMIN, CLIENT, GUIDE_TOURISTIQUE }
    public enum Statut { ACTIVE, DESACTIVE }

    public User() {
    }

    public User(int idUser, String nom, String prenom, String email, String motDePasse,
                Role role, Statut statut, LocalDate dateCreation, String telephone,
                String adresse, String photo, String bio) {
        this.idUser = idUser;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.role = role;
        this.statut = statut;
        this.dateCreation = dateCreation;
        this.telephone = telephone;
        this.adresse = adresse;
        this.photo = photo;
        this.bio = bio;
    }

    public int getIdUser() { return idUser; }
    public void setIdUser(int idUser) { this.idUser = idUser; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }
    public LocalDate getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDate dateCreation) { this.dateCreation = dateCreation; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
