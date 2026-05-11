package tn.esprit.entities;

import java.time.LocalDate;

public class ReservationActivite {
    private int idReservationActivite;
    private LocalDate dateReservation;
    private ReservationTransport.StatutReservation statut;
    private double montantTotal;
    private int idUser;
    private int idVoyage;
    private int idActivite;
    private String phoneNumber;

    public ReservationActivite() {
    }

    public ReservationActivite(int idReservationActivite, LocalDate dateReservation,
                               ReservationTransport.StatutReservation statut, double montantTotal,
                               int idUser, int idVoyage, int idActivite) {
        this.idReservationActivite = idReservationActivite;
        this.dateReservation = dateReservation;
        this.statut = statut;
        this.montantTotal = montantTotal;
        this.idUser = idUser;
        this.idVoyage = idVoyage;
        this.idActivite = idActivite;
    }

    public ReservationActivite(int idReservationActivite, LocalDate dateReservation,
                               ReservationTransport.StatutReservation statut, double montantTotal,
                               int idUser, int idVoyage, int idActivite, String phoneNumber) {
        this.idReservationActivite = idReservationActivite;
        this.dateReservation = dateReservation;
        this.statut = statut;
        this.montantTotal = montantTotal;
        this.idUser = idUser;
        this.idVoyage = idVoyage;
        this.idActivite = idActivite;
        this.phoneNumber = phoneNumber;
    }

    public int getIdReservationActivite() { return idReservationActivite; }
    public void setIdReservationActivite(int idReservationActivite) { this.idReservationActivite = idReservationActivite; }
    public LocalDate getDateReservation() { return dateReservation; }
    public void setDateReservation(LocalDate dateReservation) { this.dateReservation = dateReservation; }
    public ReservationTransport.StatutReservation getStatut() { return statut; }
    public void setStatut(ReservationTransport.StatutReservation statut) { this.statut = statut; }
    public double getMontantTotal() { return montantTotal; }
    public void setMontantTotal(double montantTotal) { this.montantTotal = montantTotal; }
    public int getIdUser() { return idUser; }
    public void setIdUser(int idUser) { this.idUser = idUser; }
    public int getIdVoyage() { return idVoyage; }
    public void setIdVoyage(int idVoyage) { this.idVoyage = idVoyage; }
    public int getIdActivite() { return idActivite; }
    public void setIdActivite(int idActivite) { this.idActivite = idActivite; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}
