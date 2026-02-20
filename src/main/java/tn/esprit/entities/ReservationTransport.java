package tn.esprit.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationTransport {
    private int idReservationTransport;
    private LocalDate dateReservation;
    private StatutReservation statut;
    private BigDecimal prixTotal;
    private int idUser;

    public enum StatutReservation { EN_ATTENTE, CONFIRMEE, ANNULEE }

    public ReservationTransport() {
    }

    public ReservationTransport(int idReservationTransport, LocalDate dateReservation,
                                StatutReservation statut, BigDecimal prixTotal, int idUser) {
        this.idReservationTransport = idReservationTransport;
        this.dateReservation = dateReservation;
        this.statut = statut;
        this.prixTotal = prixTotal;
        this.idUser = idUser;
    }

    public int getIdReservationTransport() { return idReservationTransport; }
    public void setIdReservationTransport(int idReservationTransport) { this.idReservationTransport = idReservationTransport; }
    public LocalDate getDateReservation() { return dateReservation; }
    public void setDateReservation(LocalDate dateReservation) { this.dateReservation = dateReservation; }
    public StatutReservation getStatut() { return statut; }
    public void setStatut(StatutReservation statut) { this.statut = statut; }
    public BigDecimal getPrixTotal() { return prixTotal; }
    public void setPrixTotal(BigDecimal prixTotal) { this.prixTotal = prixTotal; }
    public int getIdUser() { return idUser; }
    public void setIdUser(int idUser) { this.idUser = idUser; }
}
