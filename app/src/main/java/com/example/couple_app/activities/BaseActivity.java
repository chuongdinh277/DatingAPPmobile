package com.example.couple_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.FrameLayout;
import android.view.View;
import android.os.Build;

import com.example.couple_app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public abstract class BaseActivity extends AppCompatActivity {

    private ImageButton btnHome;
    private ImageButton btnPlan;
    private ImageButton btnSettings;
    private ImageButton btnMessage;
    private ImageButton btnGame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Thiết lập system bars trước khi load layout
        setupSystemBars();

        // Load layout gốc chứa FrameLayout + bottom bar
        super.setContentView(R.layout.menu_button);
        setupBottomBar();
        setupInsetsHandling();

        // Ẩn thanh menu nếu activity con yêu cầu
        View bottomBar = findViewById(R.id.bottomBar);
        if (!shouldShowBottomBar() && bottomBar != null) {
            View parent = (View) bottomBar.getParent();
            if (parent != null) parent.setVisibility(View.GONE);
            bottomBar.setVisibility(View.GONE);
        }
    }

    // Cho phép activity con quyết định có hiển thị bottom bar hay không (mặc định: có)
    protected boolean shouldShowBottomBar() { return true; }

    // Cho phép activity con bật/tắt edge-to-edge. Mặc định: bật để đẹp, nhưng
    // với màn hình cần adjustResize (vd. Messenger) nên tắt để bàn phím đẩy nội dung lên.
    protected boolean shouldUseEdgeToEdge() { return true; }

    // Cho phép activity con yêu cầu "pin" bottom bar khi IME mở
    protected boolean pinBottomBarOverIme() { return false; }

    // Phương thức xử lý system bars để tránh phần màu đen và cấu hình edge-to-edge
    private void setupSystemBars() {
        boolean edgeToEdge = shouldUseEdgeToEdge();
        WindowCompat.setDecorFitsSystemWindows(getWindow(), !edgeToEdge);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Status bar: edgeToEdge -> transparent, ngược lại dùng màu nền app để tránh viền đen
            if (edgeToEdge) {
                getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
            } else {
                getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.couple_pink_bg));
            }
            // Navigation bar luôn dùng màu nền app để tránh đen
            getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.couple_pink_bg));

            // Thiết lập icon sáng/dark phù hợp
            WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
            controller.setAppearanceLightStatusBars(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                controller.setAppearanceLightNavigationBars(true);
            }
        }

        // Tránh navigation bar contrast làm đổi màu (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }
    }

    private void setupInsetsHandling() {
        final View root = findViewById(android.R.id.content);
        final View content = findViewById(R.id.baseContent);
        final View bottomBarContainer = findViewById(R.id.bottomBarContainer);
        if (root == null) return;

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            // System bars
            final WindowInsetsCompat sys = insets;
            final int statusTop = sys.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            final int navBottom = sys.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            final int left = sys.getInsets(WindowInsetsCompat.Type.systemBars()).left;
            final int right = sys.getInsets(WindowInsetsCompat.Type.systemBars()).right;
            // IME
            final int imeBottom = sys.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            final boolean imeVisible = sys.isVisible(WindowInsetsCompat.Type.ime());

            if (content != null) {
                int bottomPad = pinBottomBarOverIme() ? (imeVisible ? imeBottom : navBottom) : navBottom;
                content.setPadding(left, statusTop, right, bottomPad);
            }
            if (bottomBarContainer != null) {
                // Chỉ chèn padding theo nav bar, không bị đẩy bởi IME
                bottomBarContainer.setPadding(
                        bottomBarContainer.getPaddingLeft(),
                        bottomBarContainer.getPaddingTop(),
                        bottomBarContainer.getPaddingRight(),
                        navBottom
                );
            }
            return insets;
        });
    }

    // Hàm set layout con vào trong FrameLayout baseContent
    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        FrameLayout contentFrame = findViewById(R.id.baseContent);
        getLayoutInflater().inflate(layoutResID, contentFrame, true);
    }

    protected void setupBottomBar() {
        btnHome = findViewById(R.id.btnHome);
        btnPlan = findViewById(R.id.btnPlan);
        btnSettings = findViewById(R.id.btnSettings);
        btnMessage = findViewById(R.id.btnMessage);
        btnGame = findViewById(R.id.btnGame);

        btnHome.setOnClickListener(v -> {
            if (!(this instanceof HomeMainActivity)) {
                setActiveButton("home");
                navigateNoAnim(HomeMainActivity.class);
            }
        });

        // Open Plan screen when tapping the Plan button
        btnPlan.setOnClickListener(v -> {
            if (!(this instanceof PlanActivity)) {
                setActiveButton("plan");
                navigateNoAnim(PlanActivity.class);
            }
        });

        btnMessage.setOnClickListener(v -> {
            if (!(this instanceof MessengerActivity)) {
                openMessenger();
            }
        });

        btnSettings.setOnClickListener(v -> {
            if (!(this instanceof SettingActivity)) {
                setActiveButton("settings");
                navigateNoAnim(SettingActivity.class);
            }
        });
    }

    private void navigateNoAnim(Class<?> target) {
        startActivity(new Intent(this, target));
        overridePendingTransition(0, 0);
        finish();
        overridePendingTransition(0, 0);
    }

    protected void openMessenger() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(BaseActivity.this, MessengerActivity.class));
        overridePendingTransition(0, 0);
        // Không finish để back trở lại được
    }

    // Phương thức để highlight nút hiện tại
    protected void setActiveButton(String buttonName) {
        try {
            // Đảm bảo các button đã được khởi tạo
            if (btnHome == null || btnMessage == null || btnPlan == null || btnGame == null || btnSettings == null) {
                return;
            }

            // Reset tất cả nút về trạng thái bình thường
            resetAllButtons();

            // Highlight nút hiện tại - làm nổi bật hơn 1 chút: to hơn và có nền tròn mờ
            switch (buttonName.toLowerCase()) {
                case "home":
                    highlightButton(btnHome);
                    break;
                case "message":
                    highlightButton(btnMessage);
                    break;
                case "plan":
                    highlightButton(btnPlan);
                    break;
                case "game":
                    highlightButton(btnGame);
                    break;
                case "settings":
                    highlightButton(btnSettings);
                    break;
            }
        } catch (Exception e) {
            // Xử lý lỗi im lặng để không crash app
            Log.w("BaseActivity", "Error setting active button: " + e.getMessage());
        }
    }

    private void highlightButton(ImageButton btn) {
        if (btn == null) return;
        btn.setAlpha(1.0f);
        btn.setScaleX(1.12f);
        btn.setScaleY(1.12f);
        btn.setBackgroundResource(R.drawable.menu_active_bg);
    }

    private void resetAllButtons() {
        if (btnHome != null) { btnHome.setAlpha(0.6f); btnHome.setScaleX(1.0f); btnHome.setScaleY(1.0f); btnHome.setBackgroundResource(android.R.color.transparent); }
        if (btnMessage != null) { btnMessage.setAlpha(0.6f); btnMessage.setScaleX(1.0f); btnMessage.setScaleY(1.0f); btnMessage.setBackgroundResource(android.R.color.transparent); }
        if (btnPlan != null) { btnPlan.setAlpha(0.6f); btnPlan.setScaleX(1.0f); btnPlan.setScaleY(1.0f); btnPlan.setBackgroundResource(android.R.color.transparent); }
        if (btnGame != null) { btnGame.setAlpha(0.6f); btnGame.setScaleX(1.0f); btnGame.setScaleY(1.0f); btnGame.setBackgroundResource(android.R.color.transparent); }
        if (btnSettings != null) { btnSettings.setAlpha(0.6f); btnSettings.setScaleX(1.0f); btnSettings.setScaleY(1.0f); btnSettings.setBackgroundResource(android.R.color.transparent); }
    }
}
