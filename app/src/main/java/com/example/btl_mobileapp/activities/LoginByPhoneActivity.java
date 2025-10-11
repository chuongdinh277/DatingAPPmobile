package com.example.btl_mobileapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.btl_mobileapp.R;
import com.example.btl_mobileapp.utils.LoginPreferences;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

        // Check if user is already logged in
        checkLoginState();

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

            // Check if phone number exists in database before sending verification code
            final String finalPhoneNumber = phoneNumber;
            com.example.btl_mobileapp.managers.DatabaseManager databaseManager =
                    com.example.btl_mobileapp.managers.DatabaseManager.getInstance();

            databaseManager.checkPhoneNumberExists(finalPhoneNumber, new com.example.btl_mobileapp.managers.DatabaseManager.DatabaseCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean exists) {
                    if (exists) {
                        // Phone number exists in database, proceed with verification
                        sendVerificationCode(finalPhoneNumber);
                    } else {
                        // Phone number not found in database
                        Toast.makeText(LoginByPhoneActivity.this, "Phone number not registered. Please sign up first.", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(LoginByPhoneActivity.this, "Error checking phone number: " + error, Toast.LENGTH_LONG).show();
                }
            });
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

        // Remove the direct navigation to PairingActivity - will be handled in signInWithPhoneAuthCredential
        btnLogin.setOnClickListener(view -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                handleSuccessfulLogin(user);
            }
        });

        // Initialize phone auth callbacks
        initializePhoneAuthCallbacks();
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

    private void handleSuccessfulLogin(FirebaseUser user) {
        // Save login state to SharedPreferences
        LoginPreferences.saveLoginState(
                this,
                true,
                user.getEmail() != null ? user.getEmail() : "",
                user.getDisplayName() != null ? user.getDisplayName() : "",
                user.getUid()
        );

        Toast.makeText(this, "Phone login successful!", Toast.LENGTH_SHORT).show();

        // Check pairing status before navigation
        checkPairingStatus(user);
    }

    private void checkPairingStatus(FirebaseUser user) {
        // Check if user is already paired before going to HomeMainActivity
        com.example.btl_mobileapp.managers.DatabaseManager databaseManager =
                com.example.btl_mobileapp.managers.DatabaseManager.getInstance();

        databaseManager.getCoupleByUserId(user.getUid(), new com.example.btl_mobileapp.managers.DatabaseManager.DatabaseCallback<com.example.btl_mobileapp.models.Couple>() {
            @Override
            public void onSuccess(com.example.btl_mobileapp.models.Couple couple) {
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

    private void initializePhoneAuthCallbacks() {
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                // Auto-verification completed
                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                Toast.makeText(LoginByPhoneActivity.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
                        FirebaseUser user = task.getResult().getUser();
                        if (user != null) {
                            // Handle successful login with pairing check
                            handleSuccessfulLogin(user);
                        }
                    } else {
                        Toast.makeText(this, "Sign in failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}