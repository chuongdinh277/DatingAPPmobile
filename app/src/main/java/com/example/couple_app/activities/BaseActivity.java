package com.example.couple_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.widget.FrameLayout;
import android.view.View;
import com.example.couple_app.R;
import android.graphics.drawable.GradientDrawable;
import android.graphics.PorterDuff;

public abstract class BaseActivity extends AppCompatActivity {

    // Khai báo các biến của bạn
    private LinearLayout llPlan, llGallery, llHome, llGame, llSettings;
    private ImageView btnPlan, btnGallery, btnHome, btnGame, btnSettings;
    private ImageView btnHeaderGallery; // Icon gallery trong header
    private ImageView btnNotification; // Icon notification trong header
    private android.widget.TextView tvNotificationBadge; // Badge hiển thị số lượng thông báo chưa đọc

    // Flag to prevent multiple chat bottom sheets from opening
    private boolean isChatBottomSheetOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.menu_button);

        // ✅ Ensure no auto-restored ChatBottomSheet is shown after process death/app relaunch
        removeRestoredChatBottomSheetIfAny(savedInstanceState);

        initViews();
        setupBottomBar();

        // Ẩn header và bottom bar nếu activity yêu cầu
        updateUIVisibility();

        // Setup notification badge listener
        setupNotificationBadge();
    }

    /**
     * Proactively remove any ChatBottomSheetFragment that FragmentManager might auto-restore
     * when Activity is recreated by the system (e.g., process death). This prevents the
     * chat sheet from auto-appearing on app relaunch.
     */
    private void removeRestoredChatBottomSheetIfAny(Bundle savedInstanceState) {
        if (savedInstanceState == null) return; // Only relevant on system recreation
        try {
            androidx.fragment.app.Fragment existing = getSupportFragmentManager().findFragmentByTag("ChatBottomSheet");
            if (existing != null) {
                // Dismiss if it's our dialog fragment
                if (existing instanceof com.example.couple_app.fragments.ChatBottomSheetFragment) {
                    try {
                        ((com.example.couple_app.fragments.ChatBottomSheetFragment) existing).dismissAllowingStateLoss();
                    } catch (Exception ignore) { }
                }
                // Ensure removal from FragmentManager immediately
                try {
                    getSupportFragmentManager().beginTransaction()
                        .remove(existing)
                        .commitNowAllowingStateLoss();
                    isChatBottomSheetOpen = false;
                    android.util.Log.d("BaseActivity", "✅ Removed auto-restored ChatBottomSheet on Activity recreate");
                } catch (Exception e) {
                    android.util.Log.e("BaseActivity", "Error removing restored ChatBottomSheet: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            android.util.Log.e("BaseActivity", "Error checking restored fragments: " + e.getMessage());
        }
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        FrameLayout contentFrame = findViewById(R.id.baseContent);
        getLayoutInflater().inflate(layoutResID, contentFrame, true);
        updateButtonState();
        updateUIVisibility();
    }

    private void initViews() {
        llPlan = findViewById(R.id.llPlan);
        llGallery = findViewById(R.id.llGallery);
        llHome = findViewById(R.id.llHome);
        llGame = findViewById(R.id.llGame);
        llSettings = findViewById(R.id.llSettings);

        btnPlan = findViewById(R.id.btnPlan);
        btnGallery = findViewById(R.id.btnGallery);
        btnHome = findViewById(R.id.btnHome);
        btnGame = findViewById(R.id.btnGame);
        btnSettings = findViewById(R.id.btnSettings);

        // Khởi tạo header gallery button với ID riêng biệt
        btnHeaderGallery = findViewById(R.id.btnHeaderGallery);
        if (btnHeaderGallery != null) {
            btnHeaderGallery.setOnClickListener(v -> openImageActivity());
        }

        // btnGallery listener will be set in setupBottomBar() to open chat bottom sheet

        // Khởi tạo header notification button
        btnNotification = findViewById(R.id.btnNotification);
        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> openNotificationActivity());
        }

        // Khởi tạo notification badge
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
    }

    protected void setupBottomBar() {
        llPlan.setOnClickListener(v -> navigate(v, PlanActivity.class));
        llGallery.setOnClickListener(v -> openChatBottomSheet());
        llHome.setOnClickListener(v -> navigate(v, HomeMainActivity.class));
        llGame.setOnClickListener(v -> navigate(v, GameListActivity.class));
        llSettings.setOnClickListener(v -> navigate(v, SettingActivity.class));
    }

    /**
     * Open chat bottom sheet - must be implemented by activities that support chat
     */
    private void openChatBottomSheet() {
        // Prevent opening multiple chat bottom sheets
        if (isChatBottomSheetOpen) {
            return;
        }
        isChatBottomSheetOpen = true;

        // Get partner info and open chat
        loadPartnerInfoAndOpenChat();
    }

    /**
     * Mở ImageActivity khi click icon gallery trong header
     */
    private void openImageActivity() {
        if (!this.getClass().equals(ImageActivity.class)) {
            startActivity(new Intent(this, ImageActivity.class));
            overridePendingTransition(0, 0);
        }
    }

    /**
     * Mở NotificationActivity khi click icon notification trong header
     */
    private void openNotificationActivity() {
        if (!this.getClass().equals(NotificationActivity.class)) {
            startActivity(new Intent(this, NotificationActivity.class));
            overridePendingTransition(0, 0);
        }
    }

    private void navigate(View clickedView, Class<?> targetActivity) {
        if (!this.getClass().equals(targetActivity)) {
            startActivity(new Intent(this, targetActivity));
            overridePendingTransition(0, 0); // Tắt hiệu ứng chuyển tab
            finish();
            overridePendingTransition(0, 0); // Tắt hiệu ứng khi finish
        }
    }

    private void updateButtonState() {
        resetButton(llPlan, btnPlan);
        resetButton(llGallery, btnGallery);
        resetButton(llHome, btnHome);
        resetButton(llGame, btnGame);
        resetButton(llSettings, btnSettings);

        if (this instanceof PlanActivity) {
            setActiveButton(llPlan, btnPlan);
        } else if (this instanceof HomeMainActivity) {
            setActiveButton(llHome, btnHome);
        } else if (this instanceof GameListActivity) {
            setActiveButton(llGame, btnGame);
        } else if (this instanceof SettingActivity) {
            setActiveButton(llSettings, btnSettings);
        }
    }

    private void resetButton(LinearLayout layout, ImageView icon) {
        layout.setBackground(null);
        icon.setColorFilter(ContextCompat.getColor(this, R.color.default_button_color), PorterDuff.Mode.SRC_IN);
    }

    private void setActiveButton(LinearLayout layout, ImageView icon) {
        // Sử dụng Drawable để giữ hiệu ứng bo góc
        layout.setBackgroundResource(R.drawable.rounded_button);
        icon.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_IN);
    }

    // Phương thức mới để thay đổi màu header và giữ bo góc
    protected void setHeaderColor(int colorResId) {
        LinearLayout header = findViewById(R.id.headerLayout);
        if (header != null) {
            header.setBackground(getRoundedDrawable(colorResId, 0)); // Tạo drawable với bán kính bo góc 0
        }
    }

    // Cho phép activity con quyết định có hiển thị bottom bar hay không (mặc định: có)
    protected boolean shouldShowBottomBar() { return true; }

    // Cho phép activity con quyết định có hiển thị header hay không (mặc định: có)
    protected boolean shouldShowHeader() { return true; }

    // Cho phép activity con bật/tắt edge-to-edge. Mặc định: bật để đẹp, nhưng
    // với màn hình cần adjustResize (vd. Messenger) nên tắt để bàn phím đẩy nội dung lên.
    protected boolean shouldUseEdgeToEdge() { return true; }

    // Phương thức mới để thay đổi màu thanh bottom bar và giữ bo góc
    protected void setBottomBarColor(int colorResId) {
        LinearLayout bottomBar = findViewById(R.id.bottomBar);
        if (bottomBar != null) {
            // Lấy bán kính bo góc từ tài nguyên hoặc đặt một giá trị cố định
            float radius = 0;
            bottomBar.setBackground(getRoundedDrawable(colorResId, radius));
        }
    }


    // Phương thức giúp tạo một GradientDrawable với màu và bán kính bo góc
    private GradientDrawable getRoundedDrawable(int colorResId, float radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(ContextCompat.getColor(this, colorResId));
        drawable.setCornerRadius(radius);
        return drawable;
    }

    // Phương thức cập nhật hiển thị header và bottom bar
    private void updateUIVisibility() {
        View header = findViewById(R.id.header);
        LinearLayout bottomBar = findViewById(R.id.bottomBar);

        if (header != null) {
            header.setVisibility(shouldShowHeader() ? View.VISIBLE : View.GONE);
        }

        if (bottomBar != null) {
            bottomBar.setVisibility(shouldShowBottomBar() ? View.VISIBLE : View.GONE);
        }
    }


    /**
     * Load partner information and open chat bottom sheet
     * Uses DatabaseManager (Realtime Database) like MessengerActivity
     */
    private void loadPartnerInfoAndOpenChat() {
        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            isChatBottomSheetOpen = false; // Reset flag
            android.widget.Toast.makeText(this, "Vui lòng đăng nhập", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = currentUser.getUid();

        // Use DatabaseManager to get couple info (same as MessengerActivity)
        com.example.couple_app.managers.DatabaseManager databaseManager =
            com.example.couple_app.managers.DatabaseManager.getInstance();

        databaseManager.getCoupleByUserId(currentUserId, new com.example.couple_app.managers.DatabaseManager.DatabaseCallback<com.example.couple_app.models.Couple>() {
            @Override
            public void onSuccess(com.example.couple_app.models.Couple couple) {
                if (couple == null) {
                    isChatBottomSheetOpen = false; // Reset flag
                    android.widget.Toast.makeText(BaseActivity.this, "Bạn chưa ghép đôi với ai", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }

                String coupleId = couple.getCoupleId();
                String partnerId = couple.getUser1Id().equals(currentUserId) ?
                                   couple.getUser2Id() : couple.getUser1Id();

                // Get partner details
                databaseManager.getUser(partnerId, new com.example.couple_app.managers.DatabaseManager.DatabaseCallback<com.example.couple_app.models.User>() {
                    @Override
                    public void onSuccess(com.example.couple_app.models.User partner) {
                        String partnerName = partner != null && partner.getName() != null ?
                                           partner.getName() : "Partner";
                        String partnerAvatar = partner != null ? partner.getProfilePicUrl() : null;

                        // Open chat bottom sheet directly on current activity
                        openChatBottomSheetDirectly(partnerId, partnerName, partnerAvatar, coupleId);
                    }

                    @Override
                    public void onError(String error) {
                        isChatBottomSheetOpen = false; // Reset flag
                        android.util.Log.e("BaseActivity", "Error loading partner: " + error);
                        android.widget.Toast.makeText(BaseActivity.this,
                            "Lỗi tải thông tin người dùng", android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                isChatBottomSheetOpen = false; // Reset flag
                android.util.Log.e("BaseActivity", "Error loading couple: " + error);
                android.widget.Toast.makeText(BaseActivity.this,
                    "Bạn chưa ghép đôi với ai", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Open chat bottom sheet directly on current activity
     */
    private void openChatBottomSheetDirectly(String partnerId, String partnerName, String partnerAvatar, String coupleId) {
        try {
            // ✅ IMPROVED: Check if a chat bottom sheet already exists
            androidx.fragment.app.Fragment existingFragment = getSupportFragmentManager().findFragmentByTag("ChatBottomSheet");

            if (existingFragment != null) {
                boolean isVisible = existingFragment.isVisible();
                boolean isAdded = existingFragment.isAdded();
                boolean isRemoving = existingFragment.isRemoving();

                android.util.Log.d("BaseActivity", "Found existing fragment - visible: " + isVisible +
                    ", added: " + isAdded + ", removing: " + isRemoving);

                // ✅ Only skip if fragment is actually visible and active
                if (isVisible && isAdded && !isRemoving) {
                    android.util.Log.d("BaseActivity", "Fragment is active and visible, skipping");
                    isChatBottomSheetOpen = false;
                    return;
                }

                // ✅ Fragment exists but not visible - remove it and create new
                android.util.Log.d("BaseActivity", "Fragment exists but not visible, removing old and creating new");
                try {
                    getSupportFragmentManager().beginTransaction()
                        .remove(existingFragment)
                        .commitNowAllowingStateLoss(); // Force immediate removal
                    android.util.Log.d("BaseActivity", "Old fragment removed, will create new one");
                } catch (Exception e) {
                    android.util.Log.e("BaseActivity", "Error removing fragment: " + e.getMessage());
                    // Wait a bit and retry
                    isChatBottomSheetOpen = false;
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        openChatBottomSheetDirectly(partnerId, partnerName, partnerAvatar, coupleId);
                    }, 200);
                    return;
                }
            }

            android.util.Log.d("BaseActivity", "Creating NEW chat fragment instance");
            com.example.couple_app.fragments.ChatBottomSheetFragment chatFragment =
                com.example.couple_app.fragments.ChatBottomSheetFragment.newInstance(
                    partnerId, partnerName, partnerAvatar, coupleId);

            chatFragment.show(getSupportFragmentManager(), "ChatBottomSheet");

            // Use postDelayed to wait for dialog to be created, then attach dismiss listener
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    androidx.fragment.app.Fragment fragment = getSupportFragmentManager().findFragmentByTag("ChatBottomSheet");
                    if (fragment instanceof com.example.couple_app.fragments.ChatBottomSheetFragment) {
                        android.app.Dialog dialog = ((com.example.couple_app.fragments.ChatBottomSheetFragment) fragment).getDialog();
                        if (dialog != null) {
                            dialog.setOnDismissListener(dialogInterface -> {
                                android.util.Log.d("BaseActivity", "Chat fragment dismissed - resetting flag");
                                isChatBottomSheetOpen = false;
                            });
                            android.util.Log.d("BaseActivity", "Dismiss listener attached to chat fragment");
                        }
                    }
                }
            }, 100);

        } catch (Exception e) {
            isChatBottomSheetOpen = false; // Reset flag on error
            android.util.Log.e("BaseActivity", "Error opening chat bottom sheet", e);
            android.widget.Toast.makeText(this, "Lỗi mở chat", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Setup notification badge to show unread notification count
     */
    private void setupNotificationBadge() {
        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || tvNotificationBadge == null) {
            return;
        }

        String userId = currentUser.getUid();
        com.example.couple_app.managers.NotificationManager.getInstance().listenForUnreadCount(userId,
            new com.example.couple_app.managers.NotificationManager.UnreadCountCallback() {
                @Override
                public void onCount(int count) {
                    runOnUiThread(() -> {
                        if (count > 0) {
                            tvNotificationBadge.setVisibility(View.VISIBLE);
                            tvNotificationBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                        } else {
                            tvNotificationBadge.setVisibility(View.GONE);
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    android.util.Log.e("BaseActivity", "Error getting unread count: " + error);
                }
            });
    }

    /**
     * Refresh notification badge count (call this after marking notifications as read)
     */
    protected void refreshNotificationBadge() {
        setupNotificationBadge();
    }

    // ❌ REMOVED: onSaveInstanceState for isChatBottomSheetOpen
    // No longer needed - we check fragment existence in onResume() instead
}
