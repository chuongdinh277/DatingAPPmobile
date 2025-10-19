package com.example.btl_mobileapp.models;

import com.google.firebase.Timestamp;


public class Image {
    private String imageId;

    private String imageUrl;

    private String coupleId;

    private Timestamp timeStamp;

    public Image(String imageUrl, String coupleId) {
        this.imageId = null;
        this.imageUrl = imageUrl;
        this.coupleId = coupleId;
        this.timeStamp = Timestamp.now();
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCoupleId() {
        return coupleId;
    }

    public void setCoupleId(String coupleId) {
        this.coupleId = coupleId;
    }

    public String getTimeStamp() {
        return timeStamp.toString();
    }

}
