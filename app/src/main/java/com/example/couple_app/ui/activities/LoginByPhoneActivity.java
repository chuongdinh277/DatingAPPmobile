package com.example.couple_app.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.couple_app.R;
import com.example.couple_app.managers.AuthManager;
import com.example.couple_app.utils.LoginPreferences;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginByPhoneActivity extends AppCompatActivity {
    private EditText etPhoneNumber, etPassword;
    private Button btnLogin;
    private TextView tvForgotPassword;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_by_phone);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Check if user is already logged in
        checkLoginState();

        // Initialize views
        ImageButton btnBack = findViewById(R.id.welcomeBack);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        progressBar = findViewById(R.id.progressBar);

        // Back button click
        btnBack.setOnClickListener(view -> {
            Intent intent = new Intent(LoginByPhoneActivity.this, WelcomeActivity.class);
            startActivity(intent);
            finish();
        });

        // Login button click
        btnLogin.setOnClickListener(view -> handleLogin());

        // Forgot password click
        tvForgotPassword.setOnClickListener(view -> {
            Intent intent = new Intent(LoginByPhoneActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
    }

    private void checkLoginState() {
        // Check SharedPreferences first
        if (LoginPreferences.isLoggedIn(this)) {
            // Also check Firebase Auth state
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                // User is still authenticated, check pairing status
                checkPairingStatus(currentUser);
            } else {
                // Firebase session expired, clear saved login state
                LoginPreferences.clearLoginState(this);
            }
        }
    }

    private void handleLogin() {
        String phoneNumber = etPhoneNumber.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(phoneNumber)) {
            Toast.makeText(this, "Vui lòng nhập số điện thoại", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Vui lòng nhập mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add country code if not present
        if (!phoneNumber.startsWith("+")) {
            phoneNumber = "+84" + phoneNumber;
        }

        String finalPhoneNumber = phoneNumber;
        showProgressBar();

        // Sign in with phone number and password
        AuthManager.getInstance().signInWithPhoneAndPassword(finalPhoneNumber, password, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                hideProgressBar();
                handleSuccessfulLogin(user);
            }

            @Override
            public void onError(String error) {
                hideProgressBar();
                Toast.makeText(LoginByPhoneActivity.this, "Đăng nhập thất bại: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleSuccessfulLogin(FirebaseUser user) {
        // Save login state to SharedPreferences
        LoginPreferences.saveLoginState(
            this,
            true,
            user.getEmail() != null ? user.getEmail() : "",
            user.getDisplayName() != null ? user.getDisplayName() : "",
            user.getUid()
        );

        Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();

        // Check pairing status before navigation
        checkPairingStatus(user);
    }

    private void checkPairingStatus(FirebaseUser user) {
        // Check if user is already paired before going to HomeMainActivity
        com.example.couple_app.data.local.DatabaseManager databaseManager =
            com.example.couple_app.data.local.DatabaseManager.getInstance();

        databaseManager.getCoupleByUserId(user.getUid(), new com.example.couple_app.data.local.DatabaseManager.DatabaseCallback<com.example.couple_app.data.model.Couple>() {
            @Override
            public void onSuccess(com.example.couple_app.data.model.Couple couple) {
                // User is already paired, go to HomeMainActivity
                Intent intent = new Intent(LoginByPhoneActivity.this, HomeMainActivity.class);
                intent.putExtra("user_name", user.getDisplayName() != null ? user.getDisplayName() : LoginPreferences.getUserName(LoginByPhoneActivity.this));
                intent.putExtra("user_email", user.getEmail());
                intent.putExtra("user_id", user.getUid());
                intent.putExtra("coupleId", couple.getCoupleId());
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String error) {
                // User is not paired yet, go to PairingActivity
                Intent intent = new Intent(LoginByPhoneActivity.this, PairingActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void showProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        if (btnLogin != null) {
            btnLogin.setEnabled(false);
        }
    }

    private void hideProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        if (btnLogin != null) {
            btnLogin.setEnabled(true);
        }
    }
}
