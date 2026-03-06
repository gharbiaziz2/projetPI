package tn.esprit.entities;

public class ReservationTransportTransport {
    private int idReservationTransport;
    private int idTransport;

    public ReservationTransportTransport() {
    }

    public ReservationTransportTransport(int idReservationTransport, int idTransport) {
        this.idReservationTransport = idReservationTransport;
        this.idTransport = idTransport;
    }

    public int getIdReservationTransport() { return idReservationTransport; }
    public void setIdReservationTransport(int idReservationTransport) { this.idReservationTransport = idReservationTransport; }
    public int getIdTransport() { return idTransport; }
    public void setIdTransport(int idTransport) { this.idTransport = idTransport; }
}
