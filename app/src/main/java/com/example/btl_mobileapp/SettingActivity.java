package com.example.btl_mobileapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;

public class SettingActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting);

        // Ánh xạ các button
        MaterialButton btProfile = findViewById(R.id.bt_profile);
        MaterialButton btNotification = findViewById(R.id.bt_notification);
        MaterialButton btPrivacy = findViewById(R.id.bt_privacy);
        MaterialButton btAbout = findViewById(R.id.bt_about);
        MaterialButton btLogout = findViewById(R.id.bt_logout);

        // Profile
        btProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingProfileActivity.class);
            startActivity(intent);
        });

        // Notification
        btNotification.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingNotificationActivity.class);
            startActivity(intent);
        });

        // Privacy
        btPrivacy.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingPrivacyActivity.class);
            startActivity(intent);
        });

        // About
        btAbout.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingAboutActivity.class);
            startActivity(intent);
        });

        // Logout
        btLogout.setOnClickListener(v -> {
            Toast.makeText(this, "Đăng xuất thành công", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, WelcomeActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
