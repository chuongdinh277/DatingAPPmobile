package com.example.couple_app.models;

public class Notification {
    private String notificationId;

    private String senderId;

    private String receiverId;

    private String message;

    private boolean read; // Changed from isRead to read for Firebase consistency

    private long timestamp; // Changed from Timestamp to long for Realtime Database

    private String type; // "image", "plan", "other"

    // Default constructor required for Firebase
    public Notification() {
    }

    public Notification(String senderId, String receiverId, String message) {
        this.notificationId = null;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.read = false;
        this.timestamp = System.currentTimeMillis();
        this.type = "other";
    }

    public Notification(String senderId, String receiverId, String message, String type) {
        this.notificationId = null;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.read = false;
        this.timestamp = System.currentTimeMillis();
        this.type = type;
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


    public boolean getRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
