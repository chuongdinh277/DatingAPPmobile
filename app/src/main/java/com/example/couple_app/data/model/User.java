package com.example.couple_app.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;
import androidx.annotation.Keep;

@Keep
@IgnoreExtraProperties
public class User {
    private String userId;
    private String name;
    private String dateOfBirth; // Changed from LocalDate to String (format: "yyyy-MM-dd")
    private String email;
    private String phoneNumber; // New field for phone number
    private String profilePicUrl;
    private String pinCode;

    @PropertyName("partnerId")
    private String partnerId;


    private Timestamp startLoveDate;
    private String gender;

    private String backgroundImageUrl;
    private String fcmToken; // Firebase Cloud Messaging token for push notifications

    private boolean isOnline; // Online/Offline status
    private long lastSeen; // Timestamp of last activity

    // Default constructor required for Firebase
    public User() {}

    // Constructor for email registration
    public User(String userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phoneNumber = null;
        this.profilePicUrl = "";
        this.pinCode = null;
        this.partnerId = null;
        this.startLoveDate = null;
        this.dateOfBirth = null;
    }

    // Constructor for phone registration
    public User(String userId, String name, String email, String phoneNumber) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.profilePicUrl = "";
        this.pinCode = null;
        this.partnerId = null;
        this.startLoveDate = null;
        this.dateOfBirth = null;
    }

    public User(String userId, String name, String dateOfBirth, String email, String phoneNumber, String profilePicUrl, String pinCode, String partnerId, Timestamp startLoveDate) {
        this.userId = userId;
        this.name = name;
        this.dateOfBirth = dateOfBirth;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.profilePicUrl = profilePicUrl;
        this.pinCode = pinCode;
        this.partnerId = partnerId;
        this.startLoveDate = startLoveDate;
    }


    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getProfilePicUrl() { return profilePicUrl; }
    public void setProfilePicUrl(String profilePicUrl) { this.profilePicUrl = profilePicUrl; }

    public String getPinCode() { return pinCode; }
    public void setPinCode(String pinCode) { this.pinCode = pinCode; }

    @PropertyName("partnerId")
    public String getPartnerId() { return partnerId; }

    @PropertyName("partnerId")
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }


    public Timestamp getStartLoveDate() { return startLoveDate; }
    public void setStartLoveDate(Timestamp startLoveDate) { this.startLoveDate = startLoveDate; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public String getGender() { return gender;}
    public void setGender(String gender) { this.gender = gender;}

    public String getBackgroundImageUrl() {
        return backgroundImageUrl;
    }

    public void setBackgroundImageUrl(String backgroundImageUrl) {
        this.backgroundImageUrl = backgroundImageUrl;
    }

    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }

    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }
}