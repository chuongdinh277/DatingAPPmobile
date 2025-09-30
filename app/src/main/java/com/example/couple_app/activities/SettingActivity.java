package com.example.couple_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.button.MaterialButton;
import com.example.couple_app.R;
import com.google.firebase.auth.FirebaseAuth;

public class SettingActivity extends BaseActivity {
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Ánh xạ các button
        MaterialButton btProfile = findViewById(R.id.bt_profile);
        MaterialButton btNotification = findViewById(R.id.bt_notification);
        MaterialButton btPrivacy = findViewById(R.id.bt_privacy);
        MaterialButton btAbout = findViewById(R.id.bt_about);
        MaterialButton btLogout = findViewById(R.id.bt_logout);

        // Profile
        btProfile.setOnClickListener(v -> {
            // TODO: Create SettingProfileActivity later
            Toast.makeText(this, "Profile settings coming soon", Toast.LENGTH_SHORT).show();
        });

        // Notification
        btNotification.setOnClickListener(v -> {
            // TODO: Create SettingNotificationActivity later
            Toast.makeText(this, "Notification settings coming soon", Toast.LENGTH_SHORT).show();
        });

        // Privacy
        btPrivacy.setOnClickListener(v -> {
            // TODO: Create SettingPrivacyActivity later
            Toast.makeText(this, "Privacy settings coming soon", Toast.LENGTH_SHORT).show();
        });

        // About
        btAbout.setOnClickListener(v -> {
            showAboutDialog();
        });

        // Logout with confirmation dialog and Firebase logout
        btLogout.setOnClickListener(v -> {
            showLogoutDialog();
        });
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("About ForLove")
                .setMessage("ForLove - Ứng dụng dành cho các cặp đôi yêu thương\n\nPhiên bản: 1.0.0\nPhát triển với ❤️")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất không?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> performLogout())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void performLogout() {
        // Sign out from Firebase Authentication
        mAuth.signOut();

        // Navigate to welcome screen and clear all previous activities
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();

        Toast.makeText(this, "Đã đăng xuất thành công", Toast.LENGTH_SHORT).show();
    }
}
