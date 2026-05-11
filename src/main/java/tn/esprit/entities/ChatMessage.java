package tn.esprit.entities;

import java.time.LocalDateTime;

public class ChatMessage {
    private int id;
    private int senderId;
    private int recipientId;
    private String content;
    private LocalDateTime createdAt;
    private boolean isRead;

    public ChatMessage() {
    }

    public ChatMessage(int id, int senderId, int recipientId, String content, LocalDateTime createdAt, boolean isRead) {
        this.id = id;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.content = content;
        this.createdAt = createdAt;
        this.isRead = isRead;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getSenderId() { return senderId; }
    public void setSenderId(int senderId) { this.senderId = senderId; }
    public int getRecipientId() { return recipientId; }
    public void setRecipientId(int recipientId) { this.recipientId = recipientId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}
