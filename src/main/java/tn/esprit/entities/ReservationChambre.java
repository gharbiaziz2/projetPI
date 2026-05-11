package tn.esprit.entities;

import java.time.LocalDate;

public class ReservationChambre {
    private int idReservationChambre;
    private int idChambre;
    private int idUser;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private double montantTotal;
    private String statut;

    public ReservationChambre() {
    }

    public ReservationChambre(int idReservationChambre, int idChambre, int idUser,
                              LocalDate dateDebut, LocalDate dateFin, double montantTotal, String statut) {
        this.idReservationChambre = idReservationChambre;
        this.idChambre = idChambre;
        this.idUser = idUser;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.montantTotal = montantTotal;
        this.statut = statut;
    }

    public int getIdReservationChambre() { return idReservationChambre; }
    public void setIdReservationChambre(int idReservationChambre) { this.idReservationChambre = idReservationChambre; }
    public int getIdChambre() { return idChambre; }
    public void setIdChambre(int idChambre) { this.idChambre = idChambre; }
    public int getIdUser() { return idUser; }
    public void setIdUser(int idUser) { this.idUser = idUser; }
    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }
    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }
    public double getMontantTotal() { return montantTotal; }
    public void setMontantTotal(double montantTotal) { this.montantTotal = montantTotal; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
}
