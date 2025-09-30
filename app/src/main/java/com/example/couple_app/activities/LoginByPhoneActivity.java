package com.example.couple_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.couple_app.R;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class LoginByPhoneActivity extends AppCompatActivity {
    private EditText etPhoneNumber, etVerificationCode;
    private Button btnSendCode, btnVerifyCode, btnLogin;
    private FirebaseAuth mAuth;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_by_phone);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        ImageButton btnBack = findViewById(R.id.welcomeBack);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etVerificationCode = findViewById(R.id.etVerificationCode);
        btnSendCode = findViewById(R.id.btnSendCode);
        btnVerifyCode = findViewById(R.id.btnVerifyCode);
        btnLogin = findViewById(R.id.btnLogin);

        // Initially hide verification code section
        etVerificationCode.setVisibility(View.GONE);
        btnVerifyCode.setVisibility(View.GONE);
        btnLogin.setVisibility(View.GONE);

        // Back button click
        btnBack.setOnClickListener(view -> {
            Intent intent = new Intent(LoginByPhoneActivity.this, WelcomeActivity.class);
            startActivity(intent);
            finish();
        });

        // Send verification code
        btnSendCode.setOnClickListener(view -> {
            String phoneNumber = etPhoneNumber.getText().toString().trim();
            if (TextUtils.isEmpty(phoneNumber)) {
                Toast.makeText(this, "Please enter phone number", Toast.LENGTH_SHORT).show();
                return;
            }

            // Add country code if not present
            if (!phoneNumber.startsWith("+")) {
                phoneNumber = "+84" + phoneNumber; // Vietnam country code
            }

            sendVerificationCode(phoneNumber);
        });

        // Verify code
        btnVerifyCode.setOnClickListener(view -> {
            String code = etVerificationCode.getText().toString().trim();
            if (TextUtils.isEmpty(code)) {
                Toast.makeText(this, "Please enter verification code", Toast.LENGTH_SHORT).show();
                return;
            }
            verifyCode(code);
        });

        // Login after verification
        btnLogin.setOnClickListener(view -> {
            Intent intent = new Intent(LoginByPhoneActivity.this, PairingActivity.class);
            startActivity(intent);
            finish();
        });

        // Initialize phone auth callbacks
        initializePhoneAuthCallbacks();
    }

    private void initializePhoneAuthCallbacks() {
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                // Auto-verification completed
                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                Toast.makeText(LoginByPhoneActivity.this, "Verification failed: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                mVerificationId = verificationId;
                mResendToken = token;

                // Show verification code input
                etVerificationCode.setVisibility(View.VISIBLE);
                btnVerifyCode.setVisibility(View.VISIBLE);
                btnSendCode.setEnabled(false);

                Toast.makeText(LoginByPhoneActivity.this, "Code sent to your phone", Toast.LENGTH_SHORT).show();
            }
        };
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

    private void verifyCode(String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success
                        Toast.makeText(this, "Phone verification successful!", Toast.LENGTH_SHORT).show();
                        btnLogin.setVisibility(View.VISIBLE);
                        btnVerifyCode.setEnabled(false);
                    } else {
                        // Sign in failed
                        Toast.makeText(this, "Verification failed. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
