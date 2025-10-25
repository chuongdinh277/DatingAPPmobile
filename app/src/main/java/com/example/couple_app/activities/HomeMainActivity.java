package com.example.couple_app.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.example.couple_app.R;
import com.example.couple_app.utils.AvatarCache;
import com.example.couple_app.utils.LoginPreferences;
import com.example.couple_app.viewmodels.AvatarViewModel;
import com.example.couple_app.viewmodels.ImageViewModel;
import com.example.couple_app.viewmodels.UserProfileData;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class HomeMainActivity extends BaseActivity {
    private static final String TAG = "HomeMainActivity";
    private static final String PREF_NAME = "UserSession";
    private static final String KEY_LAST_USER_ID = "last_user_id";

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homemain);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Check if user is logged in (use FirebaseAuth as source of truth)
        if (mAuth.getCurrentUser() == null) {
            // User not logged in, redirect to welcome
            redirectToWelcome();
            return;
        }

        // Clear cache if user changed (different account logged in)
        clearCacheIfUserChanged();

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

    /**
     * Clear all caches if the logged-in user has changed
     */
    private void clearCacheIfUserChanged() {
        if (mAuth.getCurrentUser() == null) {
            return;
        }

        String currentUserId = mAuth.getCurrentUser().getUid();
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String lastUserId = prefs.getString(KEY_LAST_USER_ID, null);

        // ⭐ CRITICAL FIX: Always clear cache if user changed OR if lastUserId is null (first login after logout)
        boolean shouldClearCache = false;

        if (lastUserId == null) {
            // First login after app install or after logout cleared the userId
            Log.d(TAG, "First login detected (lastUserId is null) - clearing all caches");
            shouldClearCache = true;
        } else if (!lastUserId.equals(currentUserId)) {
            // Different user logged in
            Log.d(TAG, "User changed from " + lastUserId + " to " + currentUserId + " - clearing all caches");
            shouldClearCache = true;
        }

        if (shouldClearCache) {
            // Clear AvatarViewModel cache
            try {
                AvatarViewModel avatarViewModel = new ViewModelProvider(this).get(AvatarViewModel.class);
                avatarViewModel.clearCache();
                Log.d(TAG, "AvatarViewModel cache cleared");
            } catch (Exception e) {
                Log.e(TAG, "Error clearing AvatarViewModel: " + e.getMessage());
            }

            // Clear ImageViewModel cache (will be recreated)
            try {
                ImageViewModel imageViewModel = new ViewModelProvider(this).get(ImageViewModel.class);
                // ImageViewModel will reload data automatically
                Log.d(TAG, "ImageViewModel will reload data");
            } catch (Exception e) {
                Log.e(TAG, "Error accessing ImageViewModel: " + e.getMessage());
            }

            // Clear UserProfileData singleton
            UserProfileData.getInstance().clearAll();
            Log.d(TAG, "UserProfileData cache cleared");

            // Clear file-based avatar cache
            AvatarCache.clearAllCache(this);
            Log.d(TAG, "AvatarCache cleared");
        } else {
            Log.d(TAG, "Same user logged in (" + currentUserId + ") - keeping cache");
        }

        // Save current user ID for next time
        prefs.edit().putString(KEY_LAST_USER_ID, currentUserId).apply();
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
        // Check login state when activity starts (Firebase only)
        if (mAuth.getCurrentUser() == null) {
            redirectToWelcome();
        }
    }
}
