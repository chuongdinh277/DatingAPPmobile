package com.example.couple_app.activities;

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

import com.example.couple_app.R;
import com.example.couple_app.managers.DatabaseManager;
import com.example.couple_app.managers.AuthManager;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class SignUpActivity extends AppCompatActivity {
    private EditText etName, etPhoneNumber, etVerificationCode;
    private Button btnSendCode, btnVerifyCode, btnSignUp;
    private TextView tvLogin;
    private ImageButton btnBack;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        initViews();
        initializeFirebaseAuth();
        initializePhoneAuthCallbacks();
        setupClickListeners();
    }

    private void initViews() {
        etName = findViewById(R.id.etName);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etVerificationCode = findViewById(R.id.etVerificationCode);
        btnSendCode = findViewById(R.id.btnSendCode);
        btnVerifyCode = findViewById(R.id.btnVerifyCode);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvLogin = findViewById(R.id.tvLogin);
        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);

        // Initially hide verification steps
        etVerificationCode.setVisibility(View.GONE);
        btnVerifyCode.setVisibility(View.GONE);
        btnSignUp.setVisibility(View.GONE);

        // Hide password field and switch options as we only support phone signup
        EditText etPassword = findViewById(R.id.etPassword);
        if (etPassword != null) etPassword.setVisibility(View.GONE);

        View llSwitchOptions = findViewById(R.id.llSwitchOptions);
        if (llSwitchOptions != null) llSwitchOptions.setVisibility(View.GONE);
    }

    private void initializeFirebaseAuth() {
        mAuth = FirebaseAuth.getInstance();
    }

    private void initializePhoneAuthCallbacks() {
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                hideProgressBar();
                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                hideProgressBar();
                Toast.makeText(SignUpActivity.this, "Verification failed: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                hideProgressBar();
                mVerificationId = verificationId;
                mResendToken = token;

                // Show verification code input
                etVerificationCode.setVisibility(View.VISIBLE);
                btnVerifyCode.setVisibility(View.VISIBLE);
                btnSendCode.setEnabled(false);

                Toast.makeText(SignUpActivity.this, "Mã xác nhận đã được gửi", Toast.LENGTH_SHORT).show();
            }
        };
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
        });

        btnSendCode.setOnClickListener(v -> handlePhoneSignup());

        btnVerifyCode.setOnClickListener(v -> {
            String code = etVerificationCode.getText().toString().trim();
            if (TextUtils.isEmpty(code)) {
                Toast.makeText(this, "Vui lòng nhập mã xác nhận", Toast.LENGTH_SHORT).show();
                return;
            }
            verifyCode(code);
        });

        btnSignUp.setOnClickListener(v -> completeSignup());

        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
        });
    }

    private void handlePhoneSignup() {
        String name = etName.getText().toString().trim();
        String phoneNumber = etPhoneNumber.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Vui lòng nhập họ tên", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(phoneNumber)) {
            Toast.makeText(this, "Vui lòng nhập số điện thoại", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add country code if not present
        if (!phoneNumber.startsWith("+")) {
            phoneNumber = "+84" + phoneNumber;
        }

        sendVerificationCode(phoneNumber);
    }

    private void sendVerificationCode(String phoneNumber) {
        showProgressBar();
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(mCallbacks)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void verifyCode(String code) {
        showProgressBar();
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    hideProgressBar();
                    if (task.isSuccessful()) {
                        // Sign up success - save user to database with phone number
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String name = etName.getText().toString().trim();
                            String phoneNumber = etPhoneNumber.getText().toString().trim();

                            // Add country code if not present
                            if (!phoneNumber.startsWith("+")) {
                                phoneNumber = "+84" + phoneNumber;
                            }

                            // Save user to database with phone number
                            DatabaseManager.getInstance().createUserDocumentWithPhone(
                                user,
                                name,
                                phoneNumber,
                                new AuthManager.AuthCallback() {
                                    @Override
                                    public void onSuccess(FirebaseUser firebaseUser) {
                                        Toast.makeText(SignUpActivity.this, "Tạo tài khoản thành công!", Toast.LENGTH_SHORT).show();
                                        btnSignUp.setVisibility(View.VISIBLE);
                                        btnVerifyCode.setEnabled(false);
                                    }

                                    @Override
                                    public void onError(String error) {
                                        Toast.makeText(SignUpActivity.this, "Lỗi lưu thông tin: " + error, Toast.LENGTH_SHORT).show();
                                        btnSignUp.setVisibility(View.VISIBLE);
                                        btnVerifyCode.setEnabled(false);
                                    }
                                }
                            );
                        }
                    } else {
                        Toast.makeText(this, "Xác nhận thất bại. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void completeSignup() {
        Intent intent = new Intent(SignUpActivity.this, PairingActivity.class);
        intent.putExtra("user_name", etName.getText().toString().trim());
        startActivity(intent);
        finish();
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
