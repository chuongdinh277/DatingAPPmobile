package com.example.couple_app.data.model;

import com.google.firebase.Timestamp;

public class Story {
    private String text;
    private Timestamp timestamp;

    public Story() {}

    public Story(String text, Timestamp timestamp) {
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}
