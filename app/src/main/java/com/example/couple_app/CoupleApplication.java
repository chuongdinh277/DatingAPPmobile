package com.example.couple_app;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.couple_app.utils.AvatarCache;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.messaging.FirebaseMessaging;
import com.example.couple_app.data.local.DatabaseManager;

public class CoupleApplication extends Application {
    private static final String TAG = "CoupleApplication";
    private FirebaseAuth.AuthStateListener authListener;

    @Override
    public void onCreate() {
        super.onCreate();

        // Apply saved theme preference
        applySavedTheme();

        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(this);

            // Initialize Firebase App Check with Play Integrity (FIX for error 17093)
            FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            );
            Log.d(TAG, "Firebase App Check initialized with Play Integrity");

            // Configure Firebase Realtime Database
            FirebaseDatabase database = FirebaseDatabase.getInstance();

            // Set database URL explicitly (using your project ID + region)
            String databaseUrl = "https://couples-app-b83be-default-rtdb.asia-southeast1.firebasedatabase.app";
            database = FirebaseDatabase.getInstance(databaseUrl);

            // Enable offline persistence
            database.setPersistenceEnabled(true);

            // Keep sync for better performance
            database.getReference().keepSynced(true);

            Log.d(TAG, "Firebase initialized successfully with database URL: " + databaseUrl);

        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase", e);
        }

        // Prefetch avatar if already signed in
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                AvatarCache.prefetch(this);
                // ✅ Register FCM token when app starts
                registerFCMToken(user.getUid());
            }
        } catch (Exception e) {
            Log.w(TAG, "Prefetch on start failed", e);
        }

        // Listen for future sign-in events to prefetch avatar and register FCM token
        authListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                AvatarCache.prefetch(getApplicationContext());
                // ✅ Register FCM token when user signs in
                registerFCMToken(user.getUid());
            } else {
                // ✅ Clear FCM token when user signs out
                Log.d(TAG, "User signed out, FCM token will be cleared on next login");
            }
        };
        FirebaseAuth.getInstance().addAuthStateListener(authListener);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (authListener != null) {
            FirebaseAuth.getInstance().removeAuthStateListener(authListener);
        }
    }

    private void applySavedTheme() {
        SharedPreferences prefs = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        Log.d(TAG, "Theme applied: " + (isDarkMode ? "Dark" : "Light"));
    }

    /**
     * ✅ Register FCM token for push notifications
     * This is called when the app starts and when user signs in
     */
    private void registerFCMToken(String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "Cannot register FCM token: userId is null");
            return;
        }

        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                    return;
                }

                // Get new FCM registration token
                String token = task.getResult();
                Log.d(TAG, "FCM Token retrieved: " + token);

                // Save token to Firestore
                DatabaseManager.getInstance().updateUserFcmToken(userId, token, new DatabaseManager.DatabaseActionCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "✅ FCM token registered successfully in Firestore");
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "❌ Failed to register FCM token: " + error);
                    }
                });
            });
    }
}
