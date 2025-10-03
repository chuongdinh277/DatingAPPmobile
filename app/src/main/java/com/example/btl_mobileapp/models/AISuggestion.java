package com.example.btl_mobileapp.models;

import com.google.firebase.Timestamp;

public class AISuggestion {
    private String suggestionId;
    private String coupleId;
    private String type; // "date_plan" or "gift"
    private String suggestionText;
    private Timestamp timestamp;

    public AISuggestion() {}

    public AISuggestion(String suggestionId, String coupleId, String type, String suggestionText, Timestamp timestamp) {
        this.suggestionId = suggestionId;
        this.coupleId = coupleId;
        this.type = type;
        this.suggestionText = suggestionText;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getSuggestionId() { return suggestionId; }
    public void setSuggestionId(String suggestionId) { this.suggestionId = suggestionId; }

    public String getCoupleId() { return coupleId; }
    public void setCoupleId(String coupleId) { this.coupleId = coupleId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSuggestionText() { return suggestionText; }
    public void setSuggestionText(String suggestionText) { this.suggestionText = suggestionText; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}