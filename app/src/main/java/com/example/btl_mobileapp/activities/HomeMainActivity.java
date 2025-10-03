package com.example.btl_mobileapp.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.btl_mobileapp.R;
import com.example.btl_mobileapp.utils.LoginPreferences;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class HomeMainActivity extends BaseActivity {
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homemain);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Check if user is logged in
        if (!LoginPreferences.isLoggedIn(this) || mAuth.getCurrentUser() == null) {
            // User not logged in, redirect to welcome
            redirectToWelcome();
            return;
        }

        // Setup back button callback
        setupBackButtonHandler();

        // Setup ViewPager with fragments
        setupViewPager();

        // Get user info from intent
        Intent intent = getIntent();
        if (intent != null) {
            String userName = intent.getStringExtra("user_name");

            // Display user name in title
            if (userName != null && !userName.isEmpty()) {
                setTitle("Welcome, " + userName);
            }
        }
    }

    private void setupBackButtonHandler() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Show exit confirmation
                new AlertDialog.Builder(HomeMainActivity.this)
                        .setTitle("Thoát ứng dụng")
                        .setMessage("Bạn có chắc chắn muốn thoát ứng dụng?")
                        .setPositiveButton("Thoát", (dialog, which) -> finishAffinity())
                        .setNegativeButton("Hủy", null)
                        .show();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void setupViewPager() {
        ViewPager2 viewPager = findViewById(R.id.viewPager);

        if (viewPager != null) {
            // Create list of fragments
            List<Fragment> fragments = new ArrayList<>();
            fragments.add(new HomeMain1Fragment());
            fragments.add(new HomeMain2Fragment());

            // Setup adapter
            HomePagerAdapter adapter = new HomePagerAdapter(this, fragments);
            viewPager.setAdapter(adapter);

            // Set default page to first fragment
            viewPager.setCurrentItem(0);
        }
    }

    private void redirectToWelcome() {
        Intent intent = new Intent(HomeMainActivity.this, WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check login state when activity starts
        if (!LoginPreferences.isLoggedIn(this) || mAuth.getCurrentUser() == null) {
            redirectToWelcome();
        }
    }
}