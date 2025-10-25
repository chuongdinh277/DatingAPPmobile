package com.example.couple_app.models;

/**
 * Model đại diện cho một kế hoạch của couple
 */
public class Plan {
    private String id;
    private String coupleId;
    private String date; // Format: yyyy-MM-dd
    private String content;
    private long timestamp;

    // Constructor mặc định cho Firestore
    public Plan() {
    }

    // Constructor đầy đủ
    public Plan(String id, String coupleId, String date, String content) {
        this.id = id;
        this.coupleId = coupleId;
        this.date = date;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    // Constructor không có id (dùng khi tạo mới)
    public Plan(String coupleId, String date, String content) {
        this.coupleId = coupleId;
        this.date = date;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCoupleId() {
        return coupleId;
    }

    public void setCoupleId(String coupleId) {
        this.coupleId = coupleId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}

