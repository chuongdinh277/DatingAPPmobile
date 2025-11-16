package com.example.couple_app.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/**
 * Enhanced chat response model matching backend API
 * Includes user context information
 */
public class ChatResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("answer")
    @NonNull
    private String answer;

    @SerializedName("session_id")
    @NonNull
    private String sessionId;

    @SerializedName("user_id")
    @Nullable
    private String userId;

    @SerializedName("question")
    @Nullable
    private String question;

    @SerializedName("timestamp")
    @Nullable
    private String timestamp;

    @SerializedName("has_personal_context")
    private boolean hasPersonalContext;

    @SerializedName("error")
    @Nullable
    private String error;

    // Default constructor
    public ChatResponse() {
    }

    // Getters
    public boolean isSuccess() {
        return success;
    }

    @NonNull
    public String getAnswer() {
        return answer != null ? answer : "";
    }

    @NonNull
    public String getSessionId() {
        return sessionId != null ? sessionId : "";
    }

    @Nullable
    public String getUserId() {
        return userId;
    }

    @Nullable
    public String getQuestion() {
        return question;
    }

    @Nullable
    public String getTimestamp() {
        return timestamp;
    }

    public boolean hasPersonalContext() {
        return hasPersonalContext;
    }

    @Nullable
    public String getError() {
        return error;
    }

    // Setters
    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setAnswer(@NonNull String answer) {
        this.answer = answer;
    }

    public void setSessionId(@NonNull String sessionId) {
        this.sessionId = sessionId;
    }

    public void setUserId(@Nullable String userId) {
        this.userId = userId;
    }

    public void setQuestion(@Nullable String question) {
        this.question = question;
    }

    public void setTimestamp(@Nullable String timestamp) {
        this.timestamp = timestamp;
    }

    public void setHasPersonalContext(boolean hasPersonalContext) {
        this.hasPersonalContext = hasPersonalContext;
    }

    public void setError(@Nullable String error) {
        this.error = error;
    }

    @NonNull
    @Override
    public String toString() {
        return "ChatResponse{" +
                "success=" + success +
                ", answer='" + answer + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", userId='" + userId + '\'' +
                ", hasPersonalContext=" + hasPersonalContext +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}

