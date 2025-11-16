package com.example.couple_app.utils;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.example.couple_app.ui.activities.WelcomeActivity;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Utility class to handle logout functionality
 * Can be used from Settings or any other part of the app
 */
public class LogoutHelper {

    /**
     * Perform complete logout from the app
     * This method should be called from Settings activity
     */
    public static void logout(Context context) {
        // Sign out from Firebase
        FirebaseAuth.getInstance().signOut();

        // Clear login state from SharedPreferences
        LoginPreferences.clearLoginState(context);

        // Clear background image data from AppSettings SharedPreferences
        android.content.SharedPreferences appSettingsPrefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        appSettingsPrefs.edit()
            .remove("saved_background_url")
            .remove("background_source_type")
            .commit();

        // Show logout message
        Toast.makeText(context, "Đã đăng xuất thành công", Toast.LENGTH_SHORT).show();

        // Redirect to welcome activity
        redirectToWelcome(context);
    }

    /**
     * Redirect user to welcome screen and clear all activities
     */
    private static void redirectToWelcome(Context context) {
        Intent intent = new Intent(context, WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);

        // If context is an activity, finish it
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).finish();
        }
    }

    /**
     * Check if user is currently logged in
     */
    public static boolean isUserLoggedIn(Context context) {
        return LoginPreferences.isLoggedIn(context) &&
               FirebaseAuth.getInstance().getCurrentUser() != null;
    }
}
