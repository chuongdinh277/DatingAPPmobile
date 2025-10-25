package com.example.couple_app.models;

import com.google.firebase.database.ServerValue;
import java.util.Map;

public class ChatMessage {
    private String messageId;
    private String senderId;
    private String message;
    private Object timestamp; // Use Object to handle ServerValue.TIMESTAMP
    private boolean read; // ✅ Thêm trường read
    private Object readAt; // ✅ Thêm trường readAt

    public ChatMessage() {}

    public ChatMessage(String senderId, String message) {
        this.senderId = senderId;
        this.message = message;
        this.timestamp = ServerValue.TIMESTAMP;
        this.read = false; // Mặc định chưa đọc
        this.readAt = null;
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

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public Object getReadAt() { return readAt; }
    public void setReadAt(Object readAt) { this.readAt = readAt; }
}
