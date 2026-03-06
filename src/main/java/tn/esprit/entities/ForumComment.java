package tn.esprit.entities;

import java.time.LocalDateTime;

public class ForumComment {
    private int idComment;
    private int idForum;
    private int idUser;
    private String contenu;
    private LocalDateTime dateCreation;

    public ForumComment() {
    }

    public ForumComment(int idComment, int idForum, int idUser, String contenu, LocalDateTime dateCreation) {
        this.idComment = idComment;
        this.idForum = idForum;
        this.idUser = idUser;
        this.contenu = contenu;
        this.dateCreation = dateCreation;
    }

    public int getIdComment() { return idComment; }
    public void setIdComment(int idComment) { this.idComment = idComment; }
    public int getIdForum() { return idForum; }
    public void setIdForum(int idForum) { this.idForum = idForum; }
    public int getIdUser() { return idUser; }
    public void setIdUser(int idUser) { this.idUser = idUser; }
    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}
