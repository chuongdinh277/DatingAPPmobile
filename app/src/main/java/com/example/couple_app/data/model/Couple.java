package com.example.couple_app.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.IgnoreExtraProperties;
import androidx.annotation.Keep;

import java.util.List;

@Keep
@IgnoreExtraProperties
public class Couple {
    private String coupleId;
    private String user1Id;
    private String user2Id;
    private Timestamp startDate;
    private List<String> sharedStories;

    // Default constructor required for Firestore
    public Couple() {
    }

    // Constructor
    public Couple(String coupleId, String user1Id, String user2Id, Timestamp startDate) {
        this.coupleId = coupleId;
        this.user1Id = user1Id;
        this.user2Id = user2Id;
        this.startDate = startDate;
    }

    // Getters and Setters
    public String getCoupleId() {
        return coupleId;
    }

    public void setCoupleId(String coupleId) {
        this.coupleId = coupleId;
    }

    public String getUser1Id() {
        return user1Id;
    }

    public void setUser1Id(String user1Id) {
        this.user1Id = user1Id;
    }

    public String getUser2Id() {
        return user2Id;
    }

    public void setUser2Id(String user2Id) {
        this.user2Id = user2Id;
    }

    @Nullable
    public Timestamp getStartDate() {
        return startDate;
    }

    public void setStartDate(@Nullable Timestamp startDate) {
        this.startDate = startDate;
    }

    @Nullable
    public List<String> getSharedStories() {
        return sharedStories;
    }

    public void setSharedStories(@Nullable List<String> sharedStories) {
        this.sharedStories = sharedStories;
    }

    @NonNull
    @Override
    public String toString() {
        return "Couple{" +
                "coupleId='" + coupleId + '\'' +
                ", user1Id='" + user1Id + '\'' +
                ", user2Id='" + user2Id + '\'' +
                ", startDate=" + startDate +
                '}';
    }
}

