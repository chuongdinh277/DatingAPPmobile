package com.example.couple_app.ui.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.example.couple_app.R;
import com.example.couple_app.ui.fragments.ChatbotBottomSheetFragment;
import com.example.couple_app.ui.fragments.ChatbotHost;
import com.example.couple_app.utils.AvatarCache;
import com.example.couple_app.ui.views.DraggableImageView;
import com.example.couple_app.ui.viewmodels.AvatarViewModel;
import com.example.couple_app.ui.viewmodels.ImageViewModel;
import com.example.couple_app.ui.viewmodels.UserProfileData;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class HomeMainActivity extends BaseActivity implements ChatbotHost {
    private static final String TAG = "HomeMainActivity";
    private static final String PREF_NAME = "UserSession";
    private static final String KEY_LAST_USER_ID = "last_user_id";

    private FirebaseAuth mAuth;
    private String lastActiveChatSessionId = null;
    private boolean hasAutoOpenedChatbot = false; // Flag to prevent multiple auto-opens

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homemain);

        // Restore flag from savedInstanceState to prevent reopening chatbot after recreate
        if (savedInstanceState != null) {
            hasAutoOpenedChatbot = savedInstanceState.getBoolean("hasAutoOpenedChatbot", false);
        }

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Check if user is logged in (use FirebaseAuth as source of truth)
        if (mAuth.getCurrentUser() == null) {
            // User not logged in, redirect to welcome
            redirectToWelcome();
            return;
        }

        // Set user as online
        String currentUserId = mAuth.getCurrentUser().getUid();
        Log.d(TAG, "HomeMainActivity.onCreate() - Setting user online: " + currentUserId);
        com.example.couple_app.managers.UserPresenceManager.getInstance().setUserOnline(currentUserId);

        // ✅ Ensure FCM token is registered for push notifications
        ensureFCMTokenRegistered(currentUserId);

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

        // --- Chatbot FAB: click to open ChatbotActivity and draggable ---
        DraggableImageView chatbotFab = findViewById(R.id.ic_chatbot_fab);
        if (chatbotFab != null) {
            chatbotFab.setOnTouchListener(new View.OnTouchListener() {
                private float dX, dY;
                private float initialX, initialY;
                private boolean isDragging = false;
                private static final float DRAG_THRESHOLD = 10f; // Threshold in pixels

                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            dX = view.getX() - event.getRawX();
                            dY = view.getY() - event.getRawY();
                            initialX = event.getRawX();
                            initialY = event.getRawY();
                            isDragging = false;
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            float newX = event.getRawX() + dX;
                            float newY = event.getRawY() + dY;

                            // Check if movement exceeds threshold
                            float deltaX = Math.abs(event.getRawX() - initialX);
                            float deltaY = Math.abs(event.getRawY() - initialY);

                            if (deltaX > DRAG_THRESHOLD || deltaY > DRAG_THRESHOLD) {
                                isDragging = true;
                                view.setX(newX);
                                view.setY(newY);
                            }
                            return true;

                        case MotionEvent.ACTION_UP:
                            // Only trigger click if not dragging
                            if (!isDragging) {
                                view.performClick();
                                // Open ChatbotBottomSheetFragment
                                openChatbotBottomSheet();
                            }
                            return true;

                        default:
                            return false;
                    }
                }
            });
        }

        // Auto-open chatbot if we have a session to resume (ONLY ONCE in onCreate)
        if (intent != null && intent.hasExtra("resume_session_id") && !hasAutoOpenedChatbot) {
            hasAutoOpenedChatbot = true; // Set flag to prevent reopening
            // Delay slightly to ensure UI is ready
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                openChatbotBottomSheet();
            }, 300);
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
                new ViewModelProvider(this).get(ImageViewModel.class);
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

    @Override
    public void onChatbotDismiss(String sessionId) {
        lastActiveChatSessionId = sessionId;
    }

    /**
     * Open chatbot bottom sheet dialog
     */
    private void openChatbotBottomSheet() {
        try {
            // Check if chatbot is already open to prevent duplicate instances
            ChatbotBottomSheetFragment existingFragment = (ChatbotBottomSheetFragment)
                getSupportFragmentManager().findFragmentByTag("ChatbotBottomSheet");
            if (existingFragment != null && existingFragment.isVisible()) {
                Log.d(TAG, "Chatbot already open, ignoring request");
                return;
            }

            ChatbotBottomSheetFragment bottomSheet;
            Intent intent = getIntent();
            String resumeSessionId = intent != null ? intent.getStringExtra("resume_session_id") : null;
            String resumeSessionTitle = intent != null ? intent.getStringExtra("resume_session_title") : null;

            if (resumeSessionId != null && !resumeSessionId.isEmpty()) {
                bottomSheet = ChatbotBottomSheetFragment.newInstance(resumeSessionId, resumeSessionTitle);
            } else if (lastActiveChatSessionId != null && !lastActiveChatSessionId.isEmpty()) {
                // Resume the last active session in this Activity lifecycle
                bottomSheet = ChatbotBottomSheetFragment.newInstance(lastActiveChatSessionId, null);
            } else {
                bottomSheet = new ChatbotBottomSheetFragment();
            }

            bottomSheet.show(getSupportFragmentManager(), "ChatbotBottomSheet");
        } catch (Exception e) {
            Log.e(TAG, "Error opening chatbot bottom sheet: " + e.getMessage());
        }
    }

    // ❌ REMOVED: openChatBottomSheet() method
    // HomeMainActivity no longer supports opening ChatBottomSheetFragment
    // Use BaseActivity's bottom bar gallery icon to open chat instead

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
            return;
        }

        // Set user online when app comes to foreground
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            Log.d(TAG, "HomeMainActivity.onStart() - Setting user online: " + userId);
            com.example.couple_app.managers.UserPresenceManager.getInstance()
                .setUserOnline(userId);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Update last seen when app goes to background
        // Note: Don't set offline here, let Firebase handle it on disconnect
        com.example.couple_app.managers.UserPresenceManager.getInstance().updateLastSeen();
    }

    /**
     * ✅ Ensure FCM token is registered for the current user
     * This is called when the app starts to make sure the token is always up-to-date
     */
    private void ensureFCMTokenRegistered(String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "Cannot register FCM token: userId is null");
            return;
        }

        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "❌ Failed to get FCM token", task.getException());
                    return;
                }

                String token = task.getResult();
                if (token != null && !token.isEmpty()) {
                    Log.d(TAG, "✅ FCM Token retrieved: " + token.substring(0, Math.min(20, token.length())) + "...");

                    // Save token to Firestore
                    com.example.couple_app.data.local.DatabaseManager.getInstance()
                        .updateUserFcmToken(userId, token, new com.example.couple_app.data.local.DatabaseManager.DatabaseActionCallback() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "✅ FCM token registered successfully in Firestore");
                            }

                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "❌ Failed to register FCM token: " + error);
                            }
                        });
                } else {
                    Log.w(TAG, "⚠️ FCM token is null or empty");
                }
            });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save flag to prevent reopening chatbot after recreate
        outState.putBoolean("hasAutoOpenedChatbot", hasAutoOpenedChatbot);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up presence when activity is destroyed
        if (isFinishing() && mAuth.getCurrentUser() != null) {
            // Only set offline if app is truly finishing, not just rotating
            com.example.couple_app.managers.UserPresenceManager.getInstance()
                .setUserOffline(mAuth.getCurrentUser().getUid());
        }
    }
}
