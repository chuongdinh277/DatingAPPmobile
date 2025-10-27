package com.example.btl_mobileapp.viewmodels;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.firebase.Timestamp;

/**
 * Shared data holder for user profile information
 * This prevents reloading data when switching between fragments
 */
public class UserProfileData {
    private static UserProfileData instance;

    // Current user data
    private String currentUserId;
    private String currentUserName;
    private Bitmap currentUserAvatar;
    private int currentUserAge;
    private String currentUserZodiac;
    private String currentUserDateOfBirth;

    // Partner data
    private String partnerId;
    private String partnerName;
    private Bitmap partnerAvatar;
    private int partnerAge;
    private String partnerZodiac;
    private String partnerDateOfBirth;
    private String currentUserBackgroundImageUrl;
    private Timestamp currentUserStartLoveDate;

    private String currentUserProfilePicUrl; // <-- THÊM DÒNG NÀY
    private String partnerProfilePicUrl;
    // Trong lớp UserProfileData.java

    // ... (các biến cũ) ...
    private String currentUserGender; // <-- THÊM DÒNG NÀY
    private String partnerGender;     // <-- THÊM DÒNG NÀY
    // Loading state
    private boolean isLoaded = false;

    private UserProfileData() {}

    public static synchronized UserProfileData getInstance() {
        if (instance == null) {
            instance = new UserProfileData();
        }
        return instance;
    }

    public void clear() {
        currentUserId = null;
        currentUserName = null;
        currentUserAvatar = null;
        currentUserAge = 0;
        currentUserZodiac = null;
        currentUserDateOfBirth = null;

        partnerId = null;
        partnerName = null;
        partnerAvatar = null;
        partnerAge = 0;
        partnerZodiac = null;
        partnerDateOfBirth = null;

        isLoaded = false;
    }

    // Getters and Setters for current user
    public String getCurrentUserId() { return currentUserId; }
    public void setCurrentUserId(String currentUserId) { this.currentUserId = currentUserId; }

    public String getCurrentUserName() { return currentUserName; }
    public void setCurrentUserName(String currentUserName) { this.currentUserName = currentUserName; }

    public Bitmap getCurrentUserAvatar() { return currentUserAvatar; }
    public void setCurrentUserAvatar(Bitmap currentUserAvatar) { this.currentUserAvatar = currentUserAvatar; }

    public int getCurrentUserAge() { return currentUserAge; }
    public void setCurrentUserAge(int currentUserAge) { this.currentUserAge = currentUserAge; }

    public String getCurrentUserZodiac() { return currentUserZodiac; }
    public void setCurrentUserZodiac(String currentUserZodiac) { this.currentUserZodiac = currentUserZodiac; }

    public String getCurrentUserDateOfBirth() { return currentUserDateOfBirth; }
    public void setCurrentUserDateOfBirth(String currentUserDateOfBirth) { this.currentUserDateOfBirth = currentUserDateOfBirth; }

    // Getters and Setters for partner
    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }

    public String getPartnerName() { return partnerName; }
    public void setPartnerName(String partnerName) { this.partnerName = partnerName; }

    public Bitmap getPartnerAvatar() { return partnerAvatar; }
    public void setPartnerAvatar(Bitmap partnerAvatar) { this.partnerAvatar = partnerAvatar; }

    public int getPartnerAge() { return partnerAge; }
    public void setPartnerAge(int partnerAge) { this.partnerAge = partnerAge; }

    public String getPartnerZodiac() { return partnerZodiac; }
    public void setPartnerZodiac(String partnerZodiac) { this.partnerZodiac = partnerZodiac; }

    public String getPartnerDateOfBirth() { return partnerDateOfBirth; }
    public void setPartnerDateOfBirth(String partnerDateOfBirth) { this.partnerDateOfBirth = partnerDateOfBirth; }

    // Loading state
    public boolean isLoaded() { return isLoaded; }
    public void setLoaded(boolean loaded) { isLoaded = loaded; }

    public boolean hasCurrentUserData() {
        return currentUserName != null && !currentUserName.isEmpty();
    }

    public boolean hasPartnerData() {
        return partnerName != null && !partnerName.isEmpty();
    }

    public String getCurrentUserBackgroundImageUrl() {
        return currentUserBackgroundImageUrl;
    }

    public void setCurrentUserBackgroundImageUrl(String currentUserBackgroundImageUrl) {
        this.currentUserBackgroundImageUrl = currentUserBackgroundImageUrl;
    }
    public Timestamp getCurrentUserStartLoveDate() {
        return currentUserStartLoveDate;
    }

    public void setCurrentUserStartLoveDate(Timestamp currentUserStartLoveDate) {
        this.currentUserStartLoveDate = currentUserStartLoveDate;
    }

    // Vẫn trong lớp UserProfileData

    // --- THÊM CÁC HÀM NÀY ---
    public String getCurrentUserProfilePicUrl() {
        return currentUserProfilePicUrl;
    }

    public void setCurrentUserProfilePicUrl(String currentUserProfilePicUrl) {
        this.currentUserProfilePicUrl = currentUserProfilePicUrl;
    }

    public String getPartnerProfilePicUrl() {
        return partnerProfilePicUrl;
    }

    public void setPartnerProfilePicUrl(String partnerProfilePicUrl) {
        this.partnerProfilePicUrl = partnerProfilePicUrl;
    }
    // Vẫn trong lớp UserProfileData.java

// --- THÊM CÁC HÀM NÀY ---

    // Getter/Setter cho Giới tính User hiện tại
    public String getCurrentUserGender() {
        return currentUserGender;
    }

    public void setCurrentUserGender(String currentUserGender) {
        this.currentUserGender = currentUserGender;
    }

    // Getter/Setter cho Giới tính Partner
    public String getPartnerGender() {
        return partnerGender;
    }

    public void setPartnerGender(String partnerGender) {
        this.partnerGender = partnerGender;
    }


    public void clearPartnerData() {
        this.partnerId = null;
        this.partnerName = null;
        this.partnerAge = 0;
        this.partnerZodiac = null;
        this.partnerAvatar = null;
        this.partnerDateOfBirth = null;
        // Xóa các biến partner khác nếu có
        Log.d("UserProfileData", "Partner data cleared."); // Thêm log để dễ debug
    }
}