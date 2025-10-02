package com.example.couple_app;

import android.app.Application;
import android.util.Log;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.couple_app.utils.AvatarCache;

public class CoupleApplication extends Application {
    private static final String TAG = "CoupleApplication";
    private FirebaseAuth.AuthStateListener authListener;

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(this);

            // Configure Firebase Realtime Database
            FirebaseDatabase database = FirebaseDatabase.getInstance();

            // Set database URL explicitly (using your project ID)
            String databaseUrl = "https://couples-app-b83be-default-rtdb.firebaseio.com/";
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
            }
        } catch (Exception e) {
            Log.w(TAG, "Prefetch on start failed", e);
        }

        // Listen for future sign-in events to prefetch avatar
        authListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                AvatarCache.prefetch(getApplicationContext());
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
}
