package com.example.couple_app.data.model;

/**
 * Model đại diện cho một kế hoạch của couple
 * Firestore fields: coupleId, title, date, time, details
 */
public class Plan {
    private String id;
    private String coupleId;
    private String title;
    private String date; // Format: yyyy-MM-dd
    private String time; // Format: HH:mm
    private String details;
    private long timestamp;

    // Constructor mặc định cho Firestore
    public Plan() {
    }

    // Constructor đầy đủ
    public Plan(String id, String coupleId, String title, String date, String time, String details) {
        this.id = id;
        this.coupleId = coupleId;
        this.title = title;
        this.date = date;
        this.time = time;
        this.details = details;
        this.timestamp = System.currentTimeMillis();
    }

    // Constructor không có id (dùng khi tạo mới)
    public Plan(String coupleId, String title, String date, String time, String details) {
        this.coupleId = coupleId;
        this.title = title;
        this.date = date;
        this.time = time;
        this.details = details;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}

