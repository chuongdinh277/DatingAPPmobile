package com.example.couple_app.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class LoginPreferences {
    private static final String PREF_NAME = "CoupleAppLoginPrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_USER_PHONE = "userPhone";
    private static final String KEY_LOGIN_TYPE = "loginType"; // "phone", "google"

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Context context;

    public LoginPreferences(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    // Save login state
    public void saveLoginState(String userId, String userName, String email, String phone, String loginType) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_NAME, userName);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_PHONE, phone);
        editor.putString(KEY_LOGIN_TYPE, loginType);
        editor.apply();
    }

    // Check if user is logged in
    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    // Get saved user data
    public String getUserId() {
        return sharedPreferences.getString(KEY_USER_ID, null);
    }

    public String getUserName() {
        return sharedPreferences.getString(KEY_USER_NAME, null);
    }

    public String getUserEmail() {
        return sharedPreferences.getString(KEY_USER_EMAIL, null);
    }

    public String getUserPhone() {
        return sharedPreferences.getString(KEY_USER_PHONE, null);
    }

    public String getLoginType() {
        return sharedPreferences.getString(KEY_LOGIN_TYPE, null);
    }

    // Clear login state (logout)
    public void clearLoginState() {
        editor.clear();
        editor.apply();
    }

    // Check if auto-login is enabled
    public boolean isAutoLoginEnabled() {
        return sharedPreferences.getBoolean("autoLogin", true);
    }

    // Set auto-login preference
    public void setAutoLoginEnabled(boolean enabled) {
        editor.putBoolean("autoLogin", enabled);
        editor.apply();
    }
}
