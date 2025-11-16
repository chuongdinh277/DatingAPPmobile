package com.example.couple_app.data.model;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.Exclude;

/**
 * Model for chat messages in Messenger (Firebase Realtime Database)
 * Used for couple messaging feature
 */
@Keep
public class ChatMessage {
    private String messageId;
    private String senderId;
    private String message;
    private Object timestamp; // Can be ServerValue.TIMESTAMP or Long
    private boolean isRead;
    private Object readAt;

    // Default constructor required for Firebase
    public ChatMessage() {
    }

    public ChatMessage(@NonNull String senderId, @NonNull String message) {
        this.senderId = senderId;
        this.message = message;
        this.timestamp = com.google.firebase.database.ServerValue.TIMESTAMP;
        this.isRead = false;
        this.readAt = null;
    }

    // Getters and setters
    @Nullable
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(@Nullable String messageId) {
        this.messageId = messageId;
    }

    @NonNull
    public String getSenderId() {
        return senderId != null ? senderId : "";
    }

    public void setSenderId(@NonNull String senderId) {
        this.senderId = senderId;
    }

    @NonNull
    public String getMessage() {
        return message != null ? message : "";
    }

    public void setMessage(@NonNull String message) {
        this.message = message;
    }

    @NonNull
    public Object getTimestamp() {
        return timestamp != null ? timestamp : 0L;
    }

    public void setTimestamp(@NonNull Object timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    @Nullable
    public Object getReadAt() {
        return readAt;
    }

    public void setReadAt(@Nullable Object readAt) {
        this.readAt = readAt;
    }

    /**
     * Get timestamp as long value (for sorting)
     */
    @Exclude
    public long getTimestampLong() {
        if (timestamp instanceof Long) {
            return (Long) timestamp;
        } else if (timestamp instanceof Double) {
            return ((Double) timestamp).longValue();
        }
        return 0L;
    }
}

