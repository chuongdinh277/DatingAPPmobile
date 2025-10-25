package com.example.couple_app.models;

import com.google.firebase.Timestamp;

public class Notification {
    private String notificationId;

    private String senderId;

    private String receiverId;

    private String message;

    private boolean isRead;

    private Timestamp timestamp;

    public Notification(String senderId, String receiverId, String message) {
        this.notificationId = null;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.isRead = false;
        this.timestamp = Timestamp.now();
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public String getTimestamp() {
        return timestamp.toString();
    }

}
