package com.example.btl_mobileapp.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.IgnoreExtraProperties;
import androidx.annotation.Keep;

@Keep
@IgnoreExtraProperties
public class User {
    private String userId;
    private String name;
    private String email;
    private String phoneNumber; // New field for phone number
    private String profilePicUrl;
    private String pinCode;
    private String partnerId;
    private Timestamp startLoveDate;

    // Default constructor required for Firebase
    public User() {}

    // Constructor for email registration
    public User(String userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phoneNumber = null; // null when registering with email
        this.profilePicUrl = "";
        this.pinCode = null;
        this.partnerId = null;
        this.startLoveDate = null;
    }

    // Constructor for phone registration
    public User(String userId, String name, String phoneNumber, boolean isPhoneRegistration) {
        this.userId = userId;
        this.name = name;
        this.email = null; // null when registering with phone
        this.phoneNumber = phoneNumber;
        this.profilePicUrl = "";
        this.pinCode = null;
        this.partnerId = null;
        this.startLoveDate = null;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getProfilePicUrl() { return profilePicUrl; }
    public void setProfilePicUrl(String profilePicUrl) { this.profilePicUrl = profilePicUrl; }

    public String getPinCode() { return pinCode; }
    public void setPinCode(String pinCode) { this.pinCode = pinCode; }

    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }

    public Timestamp getStartLoveDate() { return startLoveDate; }
    public void setStartLoveDate(Timestamp startLoveDate) { this.startLoveDate = startLoveDate; }
}