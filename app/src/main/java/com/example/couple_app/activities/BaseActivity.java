package com.example.couple_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import android.widget.FrameLayout;
import android.view.View;
import android.os.Build;

import com.example.couple_app.R;
import com.example.couple_app.managers.DatabaseManager;
import com.example.couple_app.models.Couple;
import com.example.couple_app.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Thiết lập system bars trước khi load layout
        setupSystemBars();

        // Load layout gốc chứa FrameLayout + bottom bar
        super.setContentView(R.layout.menu_button);
        setupBottomBar();
    }

    // Phương thức xử lý system bars để tránh phần màu đen
    private void setupSystemBars() {
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // Thiết lập màu navigation bar và status bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
            // Sử dụng màu hồng giống với nền app
            getWindow().setNavigationBarColor(getResources().getColor(R.color.couple_pink_bg));

            // Thiết lập màu icon trên system bars
            WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
            controller.setAppearanceLightStatusBars(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                controller.setAppearanceLightNavigationBars(true);
            }
        }

        // Tránh navigation bar contrast trên Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }
    }

    // Hàm set layout con vào trong FrameLayout baseContent
    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        FrameLayout contentFrame = findViewById(R.id.baseContent);
        getLayoutInflater().inflate(layoutResID, contentFrame, true);
    }

    protected void setupBottomBar() {
        ImageButton btnHome = findViewById(R.id.btnHome);
        ImageButton btnPlan = findViewById(R.id.btnPlan);
        ImageButton btnSettings = findViewById(R.id.btnSettings);
        ImageButton btnMessage = findViewById(R.id.btnMessage);
        ImageButton btnGame = findViewById(R.id.btnGame);

        btnHome.setOnClickListener(v -> {
            if (!(this instanceof HomeMainActivity)) {
                startActivity(new Intent(this, HomeMainActivity.class));
                finish();
            }
        });

        btnMessage.setOnClickListener(v -> {
            openMessenger();
        });

        btnSettings.setOnClickListener(v -> {
            if (!(this instanceof SettingActivity)) {
                startActivity(new Intent(this, SettingActivity.class));
            }
        });

        // Add other button listeners as needed
    }

    protected void openMessenger() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseManager databaseManager = DatabaseManager.getInstance();

        // Get couple information first
        databaseManager.getCoupleByUserId(currentUser.getUid(), new DatabaseManager.DatabaseCallback<Couple>() {
            @Override
            public void onSuccess(Couple couple) {
                // Get partner ID
                String partnerId = couple.getUser1Id().equals(currentUser.getUid()) ?
                        couple.getUser2Id() : couple.getUser1Id();

                // Get partner name and open messenger
                databaseManager.getUser(partnerId, new DatabaseManager.DatabaseCallback<User>() {
                    @Override
                    public void onSuccess(User partner) {
                        Intent intent = new Intent(BaseActivity.this, MessengerActivity.class);
                        intent.putExtra("coupleId", couple.getCoupleId());
                        intent.putExtra("partnerId", partnerId);
                        intent.putExtra("partnerName", partner.getName() != null ? partner.getName() : "Partner");
                        startActivity(intent);
                    }

                    @Override
                    public void onError(String error) {
                        Intent intent = new Intent(BaseActivity.this, MessengerActivity.class);
                        intent.putExtra("coupleId", couple.getCoupleId());
                        intent.putExtra("partnerId", partnerId);
                        intent.putExtra("partnerName", "Partner");
                        startActivity(intent);
                    }
                });
            }

            @Override
            public void onError(String error) {
                Toast.makeText(BaseActivity.this, "You need to pair with someone first", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
