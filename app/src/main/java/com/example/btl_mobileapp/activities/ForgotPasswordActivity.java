package com.example.btl_mobileapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.btl_mobileapp.R;
import com.example.btl_mobileapp.managers.AuthManager;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.FirebaseAuthSettings;

import java.util.concurrent.TimeUnit;

public class ForgotPasswordActivity extends AppCompatActivity {
    private EditText etPhoneNumber, etVerificationCode, etNewPassword, etConfirmPassword;
    private Button btnSendCode, btnVerifyAndReset;
    private TextView tvBackToLogin;
    private ProgressBar progressBar;
    private ImageButton btnBack;

    private FirebaseAuth mAuth;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private PhoneAuthCredential mPhoneCredential;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        initViews();
        initializeFirebaseAuth();
        initializePhoneAuthCallbacks();
        setupClickListeners();
    }

    private void initViews() {
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etVerificationCode = findViewById(R.id.etVerificationCode);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSendCode = findViewById(R.id.btnSendCode);
        btnVerifyAndReset = findViewById(R.id.btnVerifyAndReset);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
        progressBar = findViewById(R.id.progressBar);
        btnBack = findViewById(R.id.btnBack);

        // Initially hide verification and password reset fields
        etVerificationCode.setVisibility(View.GONE);
        etNewPassword.setVisibility(View.GONE);
        etConfirmPassword.setVisibility(View.GONE);
        btnVerifyAndReset.setVisibility(View.GONE);
    }

    private void initializeFirebaseAuth() {
        mAuth = FirebaseAuth.getInstance();
        FirebaseAuthSettings firebaseAuthSettings = mAuth.getFirebaseAuthSettings();
        firebaseAuthSettings.setAppVerificationDisabledForTesting(true);
    }

    private void initializePhoneAuthCallbacks() {
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                hideProgressBar();
                mPhoneCredential = credential;
                Toast.makeText(ForgotPasswordActivity.this, "Xác thực tự động thành công!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                hideProgressBar();
                Toast.makeText(ForgotPasswordActivity.this, "Xác thực thất bại: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                hideProgressBar();
                mVerificationId = verificationId;
                mResendToken = token;

                // Show verification code and password fields
                etVerificationCode.setVisibility(View.VISIBLE);
                etNewPassword.setVisibility(View.VISIBLE);
                etConfirmPassword.setVisibility(View.VISIBLE);
                btnVerifyAndReset.setVisibility(View.VISIBLE);
                btnSendCode.setEnabled(false);

                Toast.makeText(ForgotPasswordActivity.this, "Mã xác nhận đã được gửi", Toast.LENGTH_SHORT).show();
            }
        };
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSendCode.setOnClickListener(v -> handleSendCode());

        btnVerifyAndReset.setOnClickListener(v -> handleVerifyAndReset());

        tvBackToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginByPhoneActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void handleSendCode() {
        String phoneNumber = etPhoneNumber.getText().toString().trim();

        if (TextUtils.isEmpty(phoneNumber)) {
            Toast.makeText(this, "Vui lòng nhập số điện thoại", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add country code if not present
        if (!phoneNumber.startsWith("+")) {
            phoneNumber = "+84" + phoneNumber;
        }

        String finalPhoneNumber = phoneNumber;

        // Check if phone is registered
        showProgressBar();
        AuthManager.getInstance().checkPhoneNumberRegistered(finalPhoneNumber, new AuthManager.DatabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean isRegistered) {
                if (isRegistered) {
                    sendVerificationCode(finalPhoneNumber);
                } else {
                    hideProgressBar();
                    Toast.makeText(ForgotPasswordActivity.this, "Số điện thoại chưa được đăng ký", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String error) {
                hideProgressBar();
                Toast.makeText(ForgotPasswordActivity.this, "Lỗi kiểm tra số điện thoại: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendVerificationCode(String phoneNumber) {
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(mCallbacks)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void handleVerifyAndReset() {
        String code = etVerificationCode.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String phoneNumber = etPhoneNumber.getText().toString().trim();

        if (TextUtils.isEmpty(code)) {
            Toast.makeText(this, "Vui lòng nhập mã xác nhận", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(newPassword)) {
            Toast.makeText(this, "Vui lòng nhập mật khẩu mới", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPassword.length() < 6) {
            Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add country code if not present
        if (!phoneNumber.startsWith("+")) {
            phoneNumber = "+84" + phoneNumber;
        }

        String finalPhoneNumber = phoneNumber;
        showProgressBar();

        // Get phone credential
        PhoneAuthCredential credential;
        if (mPhoneCredential != null) {
            credential = mPhoneCredential;
        } else {
            credential = PhoneAuthProvider.getCredential(mVerificationId, code);
        }

        // Reset password with phone verification
        AuthManager.getInstance().resetPasswordWithPhone(finalPhoneNumber, newPassword, credential, new AuthManager.AuthActionCallback() {
            @Override
            public void onSuccess() {
                hideProgressBar();
                Toast.makeText(ForgotPasswordActivity.this, "Đổi mật khẩu thành công! Vui lòng đăng nhập lại", Toast.LENGTH_LONG).show();

                // Navigate to login
                Intent intent = new Intent(ForgotPasswordActivity.this, LoginByPhoneActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String error) {
                hideProgressBar();
                Toast.makeText(ForgotPasswordActivity.this, "Đổi mật khẩu thất bại: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    private void hideProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }
}