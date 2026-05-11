package tn.esprit.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class VoyageSuggestion {
    private int idSuggestion;
    private Integer idUser;
    private String fullName;
    private String email;
    private String destination;
    private String departureCity;
    private LocalDate desiredDate;
    private Double budget;
    private String message;
    private String status;
    private LocalDateTime createdAt;

    public VoyageSuggestion() {
    }

    public VoyageSuggestion(int idSuggestion, Integer idUser, String fullName, String email,
                            String destination, String departureCity, LocalDate desiredDate,
                            Double budget, String message, String status, LocalDateTime createdAt) {
        this.idSuggestion = idSuggestion;
        this.idUser = idUser;
        this.fullName = fullName;
        this.email = email;
        this.destination = destination;
        this.departureCity = departureCity;
        this.desiredDate = desiredDate;
        this.budget = budget;
        this.message = message;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getIdSuggestion() { return idSuggestion; }
    public void setIdSuggestion(int idSuggestion) { this.idSuggestion = idSuggestion; }
    public Integer getIdUser() { return idUser; }
    public void setIdUser(Integer idUser) { this.idUser = idUser; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public String getDepartureCity() { return departureCity; }
    public void setDepartureCity(String departureCity) { this.departureCity = departureCity; }
    public LocalDate getDesiredDate() { return desiredDate; }
    public void setDesiredDate(LocalDate desiredDate) { this.desiredDate = desiredDate; }
    public Double getBudget() { return budget; }
    public void setBudget(Double budget) { this.budget = budget; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
