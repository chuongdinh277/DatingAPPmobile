package com.example.couple_app.models;

import com.google.firebase.database.ServerValue;
import java.util.Map;

public class ChatMessage {
    private String messageId;
    private String senderId;
    private String message;
    private Object timestamp; // Use Object to handle ServerValue.TIMESTAMP

    public ChatMessage() {}

    public ChatMessage(String senderId, String message) {
        this.senderId = senderId;
        this.message = message;
        this.timestamp = ServerValue.TIMESTAMP;
    }

    // Getters and Setters
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Object getTimestamp() { return timestamp; }
    public void setTimestamp(Object timestamp) { this.timestamp = timestamp; }
}
