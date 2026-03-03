package tn.esprit.entities;

import java.time.LocalDateTime;

public class Notification {
    private int idNotification;
    private int idUser;
    private String message;
    private LocalDateTime dateCreation;
    private boolean lu;

    public Notification() {
    }

    public Notification(int idNotification, int idUser, String message, LocalDateTime dateCreation, boolean lu) {
        this.idNotification = idNotification;
        this.idUser = idUser;
        this.message = message;
        this.dateCreation = dateCreation;
        this.lu = lu;
    }

    public int getIdNotification() { return idNotification; }
    public void setIdNotification(int idNotification) { this.idNotification = idNotification; }
    public int getIdUser() { return idUser; }
    public void setIdUser(int idUser) { this.idUser = idUser; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
    public boolean isLu() { return lu; }
    public void setLu(boolean lu) { this.lu = lu; }
}
