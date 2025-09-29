package com.example.btl_mobileapp; // đổi thành package app của bạn

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;


public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome); // file XML bạn vừa gửi

        // Bắt các view theo ID
        LinearLayout btnLoginPhone = findViewById(R.id.btnLoginPhone);
        LinearLayout btnLoginMail = findViewById(R.id.btnLoginMail);
        TextView tvSignUp = findViewById(R.id.tv_signup);


        // Xử lý sự kiện click Login with Phone
        btnLoginPhone.setOnClickListener(v -> {
            // Ví dụ: mở màn hình LoginPhoneActivity
            Intent intent = new Intent(WelcomeActivity.this, LoginByPhoneActivity.class);
            startActivity(intent);
        });

        // Xử lý sự kiện click Login with Mail
        btnLoginMail.setOnClickListener(v -> {
            // Ví dụ: mở màn hình LoginMailActivity
            Intent intent = new Intent(WelcomeActivity.this, LoginByEmailActivity.class);
            startActivity(intent);
        });

        // Xử lý sự kiện click Sign Up

    }
}
