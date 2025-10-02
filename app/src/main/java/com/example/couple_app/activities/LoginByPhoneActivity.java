package com.example.couple_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.example.couple_app.R;
import com.example.couple_app.utils.LoginPreferences;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.FirebaseAuthSettings;
import com.google.firebase.FirebaseTooManyRequestsException;

import java.util.concurrent.TimeUnit;

public class LoginByPhoneActivity extends AppCompatActivity {
    private EditText etPhoneNumber, etVerificationCode;
    private Button btnSendCode, btnVerifyCode, btnLogin;
    private TextView tvQuotaError, tvAlternativeLogin;
    private FirebaseAuth mAuth;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private boolean quotaExceeded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_by_phone);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        FirebaseAuthSettings authSettings = mAuth.getFirebaseAuthSettings();
        // Only disable for testing - remove in production
        authSettings.setAppVerificationDisabledForTesting(true);

        // Check if user is already logged in
        checkLoginState();

        // Initialize views
        ImageButton btnBack = findViewById(R.id.welcomeBack);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etVerificationCode = findViewById(R.id.etVerificationCode);
        btnSendCode = findViewById(R.id.btnSendCode);
        btnVerifyCode = findViewById(R.id.btnVerifyCode);
        btnLogin = findViewById(R.id.btnLogin);

        // Add quota error text view (you'll need to add this to your layout)
        tvQuotaError = findViewById(R.id.tvQuotaError);
        tvAlternativeLogin = findViewById(R.id.tvAlternativeLogin);

        // Initially hide verification code section and error messages
        etVerificationCode.setVisibility(View.GONE);
        btnVerifyCode.setVisibility(View.GONE);
        btnLogin.setVisibility(View.GONE);
        if (tvQuotaError != null) tvQuotaError.setVisibility(View.GONE);
        if (tvAlternativeLogin != null) tvAlternativeLogin.setVisibility(View.GONE);

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
            com.example.couple_app.managers.DatabaseManager databaseManager =
                com.example.couple_app.managers.DatabaseManager.getInstance();

            databaseManager.checkPhoneNumberExists(finalPhoneNumber, new com.example.couple_app.managers.DatabaseManager.DatabaseCallback<Boolean>() {
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
        com.example.couple_app.managers.DatabaseManager databaseManager =
            com.example.couple_app.managers.DatabaseManager.getInstance();

        databaseManager.getCoupleByUserId(user.getUid(), new com.example.couple_app.managers.DatabaseManager.DatabaseCallback<com.example.couple_app.models.Couple>() {
            @Override
            public void onSuccess(com.example.couple_app.models.Couple couple) {
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
                handleVerificationError(e);
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

    private void handleVerificationError(FirebaseException e) {
        if (e instanceof FirebaseTooManyRequestsException) {
            // SMS quota exceeded
            quotaExceeded = true;
            showQuotaExceededDialog();
        } else if (e.getMessage() != null && (e.getMessage().contains("17052") || e.getMessage().contains("Exceeded quota"))) {
            // Handle the specific error code 17052
            quotaExceeded = true;
            showQuotaExceededDialog();
        } else {
            Toast.makeText(LoginByPhoneActivity.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showQuotaExceededDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("SMS Verification Unavailable")
                .setMessage("SMS verification is temporarily unavailable due to quota limits. Please try one of these alternatives:\n\n" +
                        "1. Try again later (quota resets daily)\n" +
                        "2. Use email login if available\n" +
                        "3. Contact support for assistance")
                .setPositiveButton("Try Email Login", (dialog, which) -> {
                    // Navigate to email login or show email login option
                    showEmailLoginOption();
                })
                .setNegativeButton("Try Later", (dialog, which) -> {
                    dialog.dismiss();
                    // Show quota error message
                    if (tvQuotaError != null) {
                        tvQuotaError.setText("SMS verification quota exceeded. Please try again later or use email login.");
                        tvQuotaError.setVisibility(View.VISIBLE);
                    }
                })
                .setNeutralButton("Back", (dialog, which) -> {
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void showEmailLoginOption() {
        // For now, navigate back to welcome screen to show other login options
        // You can implement email login here if you have it
        Toast.makeText(this, "Please use the Sign Up option to create an account with email", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(LoginByPhoneActivity.this, WelcomeActivity.class);
        startActivity(intent);
        finish();
    }

    private void sendVerificationCode(String phoneNumber) {
        if (quotaExceeded) {
            showQuotaExceededDialog();
            return;
        }

        try {
            PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                    .setPhoneNumber(phoneNumber)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(this)
                    .setCallbacks(mCallbacks)
                    .build();
            PhoneAuthProvider.verifyPhoneNumber(options);
        } catch (Exception e) {
            handleVerificationError(new FirebaseException("Failed to send verification code: " + e.getMessage()));
        }
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
