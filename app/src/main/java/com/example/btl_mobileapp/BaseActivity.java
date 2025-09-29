package com.example.btl_mobileapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.FrameLayout;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load layout gốc chứa FrameLayout + bottom bar
        super.setContentView(R.layout.menu_button);
        setupBottomBar();
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
        ImageButton btnMess = findViewById(R.id.btnGallery);
        btnHome.setOnClickListener(v -> {
            if (!(this instanceof HomeMainActivity)) {
                startActivity(new Intent(this, HomeMainActivity.class));
                finish();
            }
        });

        btnPlan.setOnClickListener(v -> {
            if (!(this instanceof PlanActivity)) {
                startActivity(new Intent(this, PlanActivity.class));
                finish();
            }
        });


        btnSettings.setOnClickListener(v -> {
            if (!(this instanceof SettingActivity)) {
                startActivity(new Intent(this, SettingActivity.class));
                finish();
            }
        });

        btnMess.setOnClickListener(v -> {
            if (!(this instanceof MessengerActivity)) {
                startActivity(new Intent(this, MessengerActivity.class));
                finish();
            }
        });

    }
}
