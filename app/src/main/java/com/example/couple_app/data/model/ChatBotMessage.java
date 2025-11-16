package com.example.couple_app.data.model;

import androidx.annotation.NonNull;

/**
 * Model for individual chatbot messages in UI
 * Used for AI Chatbot feature
 */
public class ChatBotMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_BOT = 1;

    private final String message;
    private final int type;
    private final long timestamp;

    public ChatBotMessage(@NonNull String message, int type) {
        this.message = message;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    public ChatBotMessage(@NonNull String message, int type, long timestamp) {
        this.message = message;
        this.type = type;
        this.timestamp = timestamp;
    }

    @NonNull
    public String getMessage() {
        return message;
    }

    public int getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isUser() {
        return type == TYPE_USER;
    }

    public boolean isBot() {
        return type == TYPE_BOT;
    }
}

