package com.example.couple_app.models;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Model representing a single message in a chat session
 * Stored as subcollection under chat_sessions/{sessionId}/messages
 */
public class ChatSessionMessage {

    @DocumentId
    private String messageId;

    @NonNull
    private String content;

    @NonNull
    private String sender; // "user" or "bot"

    @ServerTimestamp
    private Date timestamp;

    @NonNull
    private String sessionId; // Reference to parent session

    // For Firestore
    public ChatSessionMessage() {
    }

    public ChatSessionMessage(@NonNull String content, @NonNull String sender, @NonNull String sessionId) {
        this.content = content;
        this.sender = sender;
        this.sessionId = sessionId;
    }

    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    @NonNull
    public String getContent() {
        return content;
    }

    public void setContent(@NonNull String content) {
        this.content = content;
    }

    @NonNull
    public String getSender() {
        return sender;
    }

    public void setSender(@NonNull String sender) {
        this.sender = sender;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @NonNull
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(@NonNull String sessionId) {
        this.sessionId = sessionId;
    }

    public boolean isUser() {
        return "user".equals(sender);
    }

    public boolean isBot() {
        return "bot".equals(sender);
    }

    /**
     * Convert to ChatBotMessage for UI display
     */
    public ChatBotMessage toChatBotMessage() {
        int type = isUser() ? ChatBotMessage.TYPE_USER : ChatBotMessage.TYPE_BOT;
        long timestampMillis = timestamp != null ? timestamp.getTime() : System.currentTimeMillis();
        return new ChatBotMessage(content, type, timestampMillis);
    }
}

