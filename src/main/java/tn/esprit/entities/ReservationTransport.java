package tn.esprit.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ReservationTransport {
    private int idReservationTransport;
    private LocalDate dateReservation;
    private StatutReservation statut;
    private double prixTotal;
    private int idUser;
    private String phoneNumber;
    private int reservedPlaces;
    private LocalDateTime holdExpiresAt;

    public enum StatutReservation { EN_ATTENTE, CONFIRMEE, ANNULEE }

    public ReservationTransport() {
        this.reservedPlaces = 1;
    }

    public ReservationTransport(int idReservationTransport, LocalDate dateReservation,
                                StatutReservation statut, double prixTotal, int idUser) {
        this.idReservationTransport = idReservationTransport;
        this.dateReservation = dateReservation;
        this.statut = statut;
        this.prixTotal = prixTotal;
        this.idUser = idUser;
        this.reservedPlaces = 1;
    }

    public ReservationTransport(int idReservationTransport, LocalDate dateReservation,
                                StatutReservation statut, double prixTotal, int idUser,
                                String phoneNumber, int reservedPlaces, LocalDateTime holdExpiresAt) {
        this.idReservationTransport = idReservationTransport;
        this.dateReservation = dateReservation;
        this.statut = statut;
        this.prixTotal = prixTotal;
        this.idUser = idUser;
        this.phoneNumber = phoneNumber;
        this.reservedPlaces = reservedPlaces;
        this.holdExpiresAt = holdExpiresAt;
    }

    public int getIdReservationTransport() { return idReservationTransport; }
    public void setIdReservationTransport(int idReservationTransport) { this.idReservationTransport = idReservationTransport; }
    public LocalDate getDateReservation() { return dateReservation; }
    public void setDateReservation(LocalDate dateReservation) { this.dateReservation = dateReservation; }
    public StatutReservation getStatut() { return statut; }
    public void setStatut(StatutReservation statut) { this.statut = statut; }
    public double getPrixTotal() { return prixTotal; }
    public void setPrixTotal(double prixTotal) { this.prixTotal = prixTotal; }
    public int getIdUser() { return idUser; }
    public void setIdUser(int idUser) { this.idUser = idUser; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public int getReservedPlaces() { return reservedPlaces; }
    public void setReservedPlaces(int reservedPlaces) { this.reservedPlaces = reservedPlaces; }
    public LocalDateTime getHoldExpiresAt() { return holdExpiresAt; }
    public void setHoldExpiresAt(LocalDateTime holdExpiresAt) { this.holdExpiresAt = holdExpiresAt; }
}
