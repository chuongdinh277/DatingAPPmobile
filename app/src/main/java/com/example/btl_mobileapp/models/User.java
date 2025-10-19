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
    private String phoneNumber;
    private String profilePicUrl;
    private String pinCode;
    private String partnerId;
    private Timestamp startLoveDate;
    private String dateOfBirth;
    private String gender; // Thêm trường giới tính

    // Constructors
    public User() {}

    public User(String userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
    }

    public User(String userId, String name, String phoneNumber, boolean isPhoneRegistration) {
        this.userId = userId;
        this.name = name;
        this.phoneNumber = phoneNumber;
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

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
}
