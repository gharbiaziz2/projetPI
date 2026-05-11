package tn.esprit.entities;

import java.time.LocalDate;

public class ReservationVoyage {
    private int idReservationVoyage;
    private LocalDate dateReservation;
    private ReservationTransport.StatutReservation statut;
    private double montantTotal;
    private int idUser;
    private int idVoyage;
    private String numeroClient;
    private Double bagageWeight;

    public ReservationVoyage() {
    }

    public ReservationVoyage(int idReservationVoyage, LocalDate dateReservation,
                             ReservationTransport.StatutReservation statut, double montantTotal,
                             int idUser, int idVoyage) {
        this.idReservationVoyage = idReservationVoyage;
        this.dateReservation = dateReservation;
        this.statut = statut;
        this.montantTotal = montantTotal;
        this.idUser = idUser;
        this.idVoyage = idVoyage;
    }

    public ReservationVoyage(int idReservationVoyage, LocalDate dateReservation,
                             ReservationTransport.StatutReservation statut, double montantTotal,
                             int idUser, int idVoyage, String numeroClient, Double bagageWeight) {
        this.idReservationVoyage = idReservationVoyage;
        this.dateReservation = dateReservation;
        this.statut = statut;
        this.montantTotal = montantTotal;
        this.idUser = idUser;
        this.idVoyage = idVoyage;
        this.numeroClient = numeroClient;
        this.bagageWeight = bagageWeight;
    }

    public int getIdReservationVoyage() { return idReservationVoyage; }
    public void setIdReservationVoyage(int idReservationVoyage) { this.idReservationVoyage = idReservationVoyage; }
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
    public String getNumeroClient() { return numeroClient; }
    public void setNumeroClient(String numeroClient) { this.numeroClient = numeroClient; }
    public Double getBagageWeight() { return bagageWeight; }
    public void setBagageWeight(Double bagageWeight) { this.bagageWeight = bagageWeight; }
}
