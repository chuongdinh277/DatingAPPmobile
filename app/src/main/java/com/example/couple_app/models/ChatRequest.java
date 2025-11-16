package com.example.couple_app.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/**
 * Request model for sending chat messages to backend
 * Includes user_id for personalized responses
 */
public class ChatRequest {

    @SerializedName("message")
    @NonNull
    private final String message;

    @SerializedName("session_id")
    @NonNull
    private final String sessionId;

    @SerializedName("user_id")
    @Nullable
    private final String userId;

    /**
     * Constructor with userId (recommended for personalized responses)
     */
    public ChatRequest(@NonNull String message, @NonNull String sessionId, @NonNull String userId) {
        this.message = message;
        this.sessionId = sessionId;
        this.userId = userId;
    }

    /**
     * Constructor without userId (legacy support)
     */
    public ChatRequest(@NonNull String message, @NonNull String sessionId) {
        this.message = message;
        this.sessionId = sessionId;
        this.userId = null;
    }

    @NonNull
    public String getMessage() {
        return message;
    }

    @NonNull
    public String getSessionId() {
        return sessionId;
    }

    @Nullable
    public String getUserId() {
        return userId;
    }
}

