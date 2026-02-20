package tn.esprit.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationVoyage {
    private int idReservationVoyage;
    private LocalDate dateReservation;
    private ReservationTransport.StatutReservation statut;
    private BigDecimal montantTotal;
    private int idUser;
    private int idVoyage;

    public ReservationVoyage() {
    }

    public ReservationVoyage(int idReservationVoyage, LocalDate dateReservation,
                             ReservationTransport.StatutReservation statut, BigDecimal montantTotal,
                             int idUser, int idVoyage) {
        this.idReservationVoyage = idReservationVoyage;
        this.dateReservation = dateReservation;
        this.statut = statut;
        this.montantTotal = montantTotal;
        this.idUser = idUser;
        this.idVoyage = idVoyage;
    }

    public int getIdReservationVoyage() { return idReservationVoyage; }
    public void setIdReservationVoyage(int idReservationVoyage) { this.idReservationVoyage = idReservationVoyage; }
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
}
