package com.example.btl_mobileapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

import com.example.btl_mobileapp.R;

public class LoginByEmailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_by_email);

        // Lấy tham chiếu đến nút back
        ImageButton btnBack = findViewById(R.id.welcomeBack);

        // Gắn sự kiện click cho nút
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Chuyển về WelcomeActivity
                Intent intent = new Intent(LoginByEmailActivity.this, WelcomeActivity.class);
                startActivity(intent);
                finish(); // đóng luôn LoginByPhoneActivity
            }
        });

        Button btnLogin = findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Chuyển sang HomeMainActivity (chứa ViewPager2)
                Intent intent = new Intent(LoginByEmailActivity.this, HomeMainActivity.class);
                startActivity(intent);
                finish(); // đóng luôn LoginByPhoneActivity
            }
        });

    }
}
