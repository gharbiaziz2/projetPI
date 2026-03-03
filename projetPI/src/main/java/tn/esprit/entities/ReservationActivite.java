package tn.esprit.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationActivite {
    private int idReservationActivite;
    private LocalDate dateReservation;
    private ReservationTransport.StatutReservation statut;
    private BigDecimal montantTotal;
    private int idUser;
    private int idVoyage;
    private int idActivite;

    public ReservationActivite() {
    }

    public ReservationActivite(int idReservationActivite, LocalDate dateReservation,
                               ReservationTransport.StatutReservation statut, BigDecimal montantTotal,
                               int idUser, int idVoyage, int idActivite) {
        this.idReservationActivite = idReservationActivite;
        this.dateReservation = dateReservation;
        this.statut = statut;
        this.montantTotal = montantTotal;
        this.idUser = idUser;
        this.idVoyage = idVoyage;
        this.idActivite = idActivite;
    }

    public int getIdReservationActivite() { return idReservationActivite; }
    public void setIdReservationActivite(int idReservationActivite) { this.idReservationActivite = idReservationActivite; }
    public LocalDate getDateReservation() { return dateReservation; }
    public void setDateReservation(LocalDate dateReservation) { this.dateReservation = dateReservation; }
    public ReservationTransport.StatutReservation getStatut() { return statut; }
    public void setStatut(ReservationTransport.StatutReservation statut) { this.statut = statut; }
    public BigDecimal getMontantTotal() { return montantTotal; }
    public void setMontantTotal(BigDecimal montantTotal) { this.montantTotal = montantTotal; }
    public int getIdUser() { return idUser; }
    public void setIdUser(int idUser) { this.idUser = idUser; }
    public int getIdVoyage() { return idVoyage; }
    public void setIdVoyage(int idVoyage) { this.idVoyage = idVoyage; }
    public int getIdActivite() { return idActivite; }
    public void setIdActivite(int idActivite) { this.idActivite = idActivite; }
}
