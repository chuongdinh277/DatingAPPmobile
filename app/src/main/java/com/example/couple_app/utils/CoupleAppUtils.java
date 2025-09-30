package com.example.couple_app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CoupleAppUtils {
    private static final String PREFS_NAME = "CoupleAppPrefs";
    private static final String KEY_COUPLE_ID = "couple_id";
    private static final String KEY_PARTNER_ID = "partner_id";
    private static final String KEY_USER_PIN = "user_pin";

    // SharedPreferences helper methods
    public static void saveCoupleId(Context context, String coupleId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_COUPLE_ID, coupleId).apply();
    }

    public static String getCoupleId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_COUPLE_ID, null);
    }

    public static void savePartnerId(Context context, String partnerId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_PARTNER_ID, partnerId).apply();
    }

    public static String getPartnerId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_PARTNER_ID, null);
    }

    public static void saveUserPin(Context context, String pin) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_USER_PIN, pin).apply();
    }

    public static String getUserPin(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USER_PIN, null);
    }

    public static void clearCoupleData(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .remove(KEY_COUPLE_ID)
                .remove(KEY_PARTNER_ID)
                .remove(KEY_USER_PIN)
                .apply();
    }

    // Date formatting helpers
    public static String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) return "";

        Date date = timestamp.toDate();
        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
        return formatter.format(date);
    }

    public static String formatChatTimestamp(Object timestamp) {
        if (timestamp == null) return "";

        long timeMillis;
        if (timestamp instanceof Long) {
            timeMillis = (Long) timestamp;
        } else {
            return "";
        }

        Date date = new Date(timeMillis);
        SimpleDateFormat formatter = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return formatter.format(date);
    }

    public static String formatLoveDays(long days) {
        if (days == 0) {
            return "Today is the beginning of your love story! ❤️";
        } else if (days == 1) {
            return "1 day of love! 💕";
        } else {
            return days + " days of love! 💖";
        }
    }

    // PIN validation
    public static boolean isValidPin(String pin) {
        return pin != null && pin.length() == 6 && pin.matches("\\d{6}");
    }

    // Input validation helpers
    public static boolean isValidEmail(String email) {
        return email != null && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    public static boolean isValidName(String name) {
        return name != null && name.trim().length() >= 2 && name.trim().length() <= 50;
    }

    // Error message helpers
    public static String getAuthErrorMessage(String errorCode) {
        switch (errorCode) {
            case "ERROR_INVALID_EMAIL":
                return "Invalid email address";
            case "ERROR_WRONG_PASSWORD":
                return "Incorrect password";
            case "ERROR_USER_NOT_FOUND":
                return "No account found with this email";
            case "ERROR_USER_DISABLED":
                return "This account has been disabled";
            case "ERROR_TOO_MANY_REQUESTS":
                return "Too many failed attempts. Try again later";
            case "ERROR_EMAIL_ALREADY_IN_USE":
                return "An account with this email already exists";
            case "ERROR_WEAK_PASSWORD":
                return "Password should be at least 6 characters";
            default:
                return "Authentication failed. Please try again";
        }
    }

    // Game type constants
    public static class GameTypes {
        public static final String QUIZ = "quiz";
        public static final String PUZZLE = "puzzle";
        public static final String MEMORY = "memory";
    }

    // AI suggestion types
    public static class SuggestionTypes {
        public static final String DATE_PLAN = "date_plan";
        public static final String GIFT = "gift";
    }
}
