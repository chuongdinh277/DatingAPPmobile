package com.example.btl_mobileapp.models;

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
    private List<Story> sharedStories;

    public Couple() {}

    public Couple(String coupleId, String user1Id, String user2Id, Timestamp startDate) {
        this.coupleId = coupleId;
        this.user1Id = user1Id;
        this.user2Id = user2Id;
        this.startDate = startDate;
    }

    // Getters and Setters
    public String getCoupleId() { return coupleId; }
    public void setCoupleId(String coupleId) { this.coupleId = coupleId; }

    public String getUser1Id() { return user1Id; }
    public void setUser1Id(String user1Id) { this.user1Id = user1Id; }

    public String getUser2Id() { return user2Id; }
    public void setUser2Id(String user2Id) { this.user2Id = user2Id; }

    public Timestamp getStartDate() { return startDate; }
    public void setStartDate(Timestamp startDate) { this.startDate = startDate; }

    public List<Story> getSharedStories() { return sharedStories; }
    public void setSharedStories(List<Story> sharedStories) { this.sharedStories = sharedStories; }
}