package com.example.couple_app.models;

import com.google.firebase.database.ServerValue;
import androidx.annotation.Keep;

@Keep
public class Message extends ChatMessage {
    private String coupleId;
    private String senderName;
    private String messageType; // "text", "image", "emoji"
    private boolean isRead;
    private Object readAt; // Timestamp when message was read

    // Default constructor required for Firebase
    public Message() {
        super();
    }

    public Message(String coupleId, String senderId, String senderName, String messageText) {
        super(senderId, messageText);
        this.coupleId = coupleId;
        this.senderName = senderName;
        this.messageType = "text";
        this.isRead = false;
        this.readAt = null;
    }

    // Additional getters and setters for new fields
    public String getCoupleId() { return coupleId; }
    public void setCoupleId(String coupleId) { this.coupleId = coupleId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getMessageText() { return getMessage(); } // Delegate to parent class
    public void setMessageText(String messageText) { setMessage(messageText); } // Delegate to parent class

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public Object getReadAt() { return readAt; }
    public void setReadAt(Object readAt) { this.readAt = readAt; }
}
