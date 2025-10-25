package com.example.couple_app.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Image {
    private String imageUrl;      // URL từ Firebase Storage
    private String uploadedBy;    // userId của người upload
    private Long timestamp;       // Thời gian upload (milliseconds)

    // Constructor mặc định cho Firebase
    public Image() {
    }

    public Image(String imageUrl, String uploadedBy) {
        this.imageUrl = imageUrl;
        this.uploadedBy = uploadedBy;
        this.timestamp = System.currentTimeMillis();
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    // Get formatted timestamp string
    public String getFormattedTime() {
        if (timestamp != null) {
            try {
                Date date = new Date(timestamp);
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                return sdf.format(date);
            } catch (Exception e) {
                return "";
            }
        }
        return "";
    }

}
