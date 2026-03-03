package tn.esprit.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationHotel {
    private int idReservationHotel;
    private LocalDate dateCheckin;
    private LocalDate dateCheckout;
    private BigDecimal prixTotal;
    private int idUser;
    private int idHotel;

    public ReservationHotel() {
    }

    public ReservationHotel(int idReservationHotel, LocalDate dateCheckin, LocalDate dateCheckout,
                            BigDecimal prixTotal, int idUser, int idHotel) {
        this.idReservationHotel = idReservationHotel;
        this.dateCheckin = dateCheckin;
        this.dateCheckout = dateCheckout;
        this.prixTotal = prixTotal;
        this.idUser = idUser;
        this.idHotel = idHotel;
    }

    public int getIdReservationHotel() { return idReservationHotel; }
    public void setIdReservationHotel(int idReservationHotel) { this.idReservationHotel = idReservationHotel; }
    public LocalDate getDateCheckin() { return dateCheckin; }
    public void setDateCheckin(LocalDate dateCheckin) { this.dateCheckin = dateCheckin; }
    public LocalDate getDateCheckout() { return dateCheckout; }
    public void setDateCheckout(LocalDate dateCheckout) { this.dateCheckout = dateCheckout; }
    public BigDecimal getPrixTotal() { return prixTotal; }
    public void setPrixTotal(BigDecimal prixTotal) { this.prixTotal = prixTotal; }
    public int getIdUser() { return idUser; }
    public void setIdUser(int idUser) { this.idUser = idUser; }
    public int getIdHotel() { return idHotel; }
    public void setIdHotel(int idHotel) { this.idHotel = idHotel; }
}
