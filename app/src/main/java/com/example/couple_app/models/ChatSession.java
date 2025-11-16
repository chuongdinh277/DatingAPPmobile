package com.example.couple_app.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Model representing a chat session with the chatbot
 * Stores conversation history in Firestore
 */
public class ChatSession {

    @DocumentId
    private String sessionId;

    @NonNull
    private String userId;

    @Nullable
    private String title; // Auto-generated from first message or custom

    @Nullable
    private String lastMessage;

    @ServerTimestamp
    private Date createdAt;

    @ServerTimestamp
    private Date updatedAt;

    private int messageCount;

    // For Firestore
    public ChatSession() {
    }

    public ChatSession(@NonNull String sessionId, @NonNull String userId) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.messageCount = 0;
    }

    public ChatSession(@NonNull String sessionId, @NonNull String userId, @Nullable String title) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.title = title;
        this.messageCount = 0;
    }

    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @NonNull
    public String getUserId() {
        return userId;
    }

    public void setUserId(@NonNull String userId) {
        this.userId = userId;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    public void setTitle(@Nullable String title) {
        this.title = title;
    }

    @Nullable
    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(@Nullable String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    /**
     * Get display title - uses title if available, otherwise truncated last message
     */
    @NonNull
    public String getDisplayTitle() {
        if (title != null && !title.isEmpty()) {
            return title;
        }
        if (lastMessage != null && !lastMessage.isEmpty()) {
            return lastMessage.length() > 30
                ? lastMessage.substring(0, 30) + "..."
                : lastMessage;
        }
        return "Chat mới";
    }

    /**
     * Get preview text for the session
     */
    @NonNull
    public String getPreviewText() {
        if (lastMessage != null && !lastMessage.isEmpty()) {
            return lastMessage.length() > 50
                ? lastMessage.substring(0, 50) + "..."
                : lastMessage;
        }
        return "Chưa có tin nhắn";
    }
}

