package com.example.btl_mobileapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.widget.FrameLayout;
import android.view.View;
import com.example.btl_mobileapp.R;

// IMPORT CÁC MODEL VÀ MANAGER CẦN THIẾT
import com.example.btl_mobileapp.managers.DatabaseManager;
import com.example.btl_mobileapp.models.Couple; // <-- Thêm import
import com.example.btl_mobileapp.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.widget.Toast;
import android.graphics.drawable.GradientDrawable;
import android.graphics.PorterDuff;

public abstract class BaseActivity extends AppCompatActivity {

    // Khai báo các biến của bạn
    private LinearLayout llPlan, llGallery, llHome, llGame, llSettings;
    private ImageView btnPlan, btnGallery, btnHome, btnGame, btnSettings;

    private FirebaseAuth mAuth;
    private DatabaseManager databaseManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.menu_button);

        mAuth = FirebaseAuth.getInstance();
        databaseManager = DatabaseManager.getInstance();

        initViews();
        setupBottomBar();

        updateUIVisibility();
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
    }

    protected void setupBottomBar() {
        llPlan.setOnClickListener(v -> navigate(v, PlanActivity.class));
        llGallery.setOnClickListener(v -> showMessengerSheet()); // <-- Dòng này đã đúng
        llHome.setOnClickListener(v -> navigate(v, HomeMainActivity.class));
        llGame.setOnClickListener(v -> navigate(v, GameListActivity.class));
        llSettings.setOnClickListener(v -> navigate(v, SettingActivity.class));
    }

    private void navigate(View clickedView, Class<?> targetActivity) {
        if (!this.getClass().equals(targetActivity)) {
            startActivity(new Intent(this, targetActivity));
            overridePendingTransition(0, 0); // Tắt hiệu ứng chuyển tab
            finish();
            overridePendingTransition(0, 0); // Tắt hiệu ứng khi finish
        }
    }

    // ĐÃ DỌN DẸP: Xóa 'MessengerActivity'
    private void updateButtonState() {
        resetButton(llPlan, btnPlan);
        resetButton(llGallery, btnGallery);
        resetButton(llHome, btnHome);
        resetButton(llGame, btnGame);
        resetButton(llSettings, btnSettings);

        if (this instanceof PlanActivity) {
            setActiveButton(llPlan, btnPlan);
        } else if (this instanceof HomeMainActivity) { // Đã xóa MessengerActivity
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
        layout.setBackgroundResource(R.drawable.rounded_button);
        icon.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_IN);
    }

    // ... (Các hàm setHeaderColor, shouldShowBottomBar, ... giữ nguyên) ...

    protected void setHeaderColor(int colorResId) {
        LinearLayout header = findViewById(R.id.headerLayout);
        if (header != null) {
            header.setBackground(getRoundedDrawable(colorResId, 0));
        }
    }
    protected boolean shouldShowBottomBar() { return true; }
    protected boolean shouldShowHeader() { return true; }
    protected boolean shouldUseEdgeToEdge() { return true; }
    protected void setBottomBarColor(int colorResId) {
        LinearLayout bottomBar = findViewById(R.id.bottomBar);
        if (bottomBar != null) {
            float radius = 0;
            bottomBar.setBackground(getRoundedDrawable(colorResId, radius));
        }
    }
    private GradientDrawable getRoundedDrawable(int colorResId, float radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(ContextCompat.getColor(this, colorResId));
        drawable.setCornerRadius(radius);
        return drawable;
    }
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


    // *** HÀM QUAN TRỌNG NHẤT - ĐÃ SỬA LẠI LOGIC ***
    private void showMessengerSheet() {
        FirebaseUser currentUserAuth = mAuth.getCurrentUser();
        if (currentUserAuth == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = currentUserAuth.getUid();

        // BƯỚC 1: Lấy thông tin user hiện tại (để tìm partnerId)
        databaseManager.getUser(currentUserId, new DatabaseManager.DatabaseCallback<User>() {
            @Override
            public void onSuccess(User currentUserData) {
                String partnerId = currentUserData.getPartnerId(); // Lấy partnerId từ User

                if (partnerId == null || partnerId.isEmpty()) {
                    Toast.makeText(BaseActivity.this, "Lỗi: Bạn chưa kết đôi", Toast.LENGTH_SHORT).show();
                    return;
                }

                // BƯỚC 2: Dùng hàm CÓ SẴN (getCoupleByUserId) để tìm "Couple"
                databaseManager.getCoupleByUserId(currentUserId, new DatabaseManager.DatabaseCallback<Couple>() {
                    @Override
                    public void onSuccess(Couple couple) {
                        String coupleId = couple.getCoupleId(); // Lấy coupleId từ Couple

                        // BƯỚC 3: Lấy thông tin partner (để lấy tên, avatar,...)
                        databaseManager.getUser(partnerId, new DatabaseManager.DatabaseCallback<User>() {
                            @Override
                            public void onSuccess(User partnerData) {
                                String partnerName = partnerData.getName();

                                // ĐÃ CÓ TẤT CẢ DỮ LIỆU!
                                ChatBottomSheetFragment chatSheet = ChatBottomSheetFragment.newInstance(
                                        coupleId,
                                        partnerId,
                                        partnerName,
                                        partnerData // Truyền cả đối tượng User của partner
                                );

                                chatSheet.show(getSupportFragmentManager(), "ChatBottomSheetFragment");
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(BaseActivity.this, "Lỗi: Không tải được dữ liệu partner", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        // Lỗi này xảy ra nếu hàm getCoupleByUserId của bạn không tìm thấy
                        Toast.makeText(BaseActivity.this, "Lỗi: Không tìm thấy couple", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                Toast.makeText(BaseActivity.this, "Lỗi: Không tải được dữ liệu người dùng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateButtonState();
    }
}