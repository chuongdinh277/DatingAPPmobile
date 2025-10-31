package com.example.btl_mobileapp;

import android.app.Application;
import android.content.SharedPreferences;         // <-- THÊM IMPORT NÀY
import androidx.appcompat.app.AppCompatDelegate; // <-- THÊM IMPORT NÀY
import android.util.Log;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.btl_mobileapp.utils.AvatarCache;

public class CoupleApplication extends Application {
    private static final String TAG = "CoupleApplication";

    // THÊM 2 HẰNG SỐ NÀY VÀO
    public static final String PREFS_NAME = "theme_prefs";
    public static final String KEY_NIGHT_MODE = "night_mode";

    private FirebaseAuth.AuthStateListener authListener;

    @Override
    public void onCreate() {
        super.onCreate();

        // --- THÊM KHỐI CODE NÀY VÀO ĐẦU HÀM ---
        // Đọc cài đặt theme đã lưu
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Lấy chế độ đã lưu, mặc định là "theo hệ thống"
        int savedMode = prefs.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        // Áp dụng ngay lập tức
        AppCompatDelegate.setDefaultNightMode(savedMode);
        // --- KẾT THÚC KHỐI CODE THÊM VÀO ---


        // Code Firebase cũ của bạn giữ nguyên bên dưới
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