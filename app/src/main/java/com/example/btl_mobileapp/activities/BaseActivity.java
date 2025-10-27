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
import android.graphics.drawable.GradientDrawable;
import android.graphics.PorterDuff;

public abstract class BaseActivity extends AppCompatActivity {

    // Khai báo các biến của bạn
    private LinearLayout llPlan, llGallery, llHome, llGame, llSettings;
    private ImageView btnPlan, btnGallery, btnHome, btnGame, btnSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.menu_button);
        initViews();
        setupBottomBar();

        // Ẩn header và bottom bar nếu activity yêu cầu
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
        llGallery.setOnClickListener(v -> navigate(v, MessengerActivity.class));
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

    private void updateButtonState() {
        resetButton(llPlan, btnPlan);
        resetButton(llGallery, btnGallery);
        resetButton(llHome, btnHome);
        resetButton(llGame, btnGame);
        resetButton(llSettings, btnSettings);

        if (this instanceof PlanActivity) {
            setActiveButton(llPlan, btnPlan);
        } else if (this instanceof MessengerActivity) {
            setActiveButton(llGallery, btnGallery);
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

    @Override
    protected void onResume() {
        super.onResume();
        updateButtonState();
    }
}