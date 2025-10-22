package com.example.couple_app.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
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
import com.google.firebase.auth.FirebaseAuthSettings;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class SignUpActivity extends AppCompatActivity {
    private EditText etName, etDateOfBirth, etPhoneNumber, etVerificationCode;
    private Button btnSendCode, btnVerifyCode, btnSignUp;
    private TextView tvLogin;
    private ImageButton btnBack;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    private String selectedDateOfBirthString; // Changed from LocalDate to String

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
        etDateOfBirth = findViewById(R.id.etDateOfBirth);
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
        FirebaseAuthSettings firebaseAuthSettings = mAuth.getFirebaseAuthSettings();
        firebaseAuthSettings.setAppVerificationDisabledForTesting(true);

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

        // Setup DatePicker for date of birth
        etDateOfBirth.setOnClickListener(v -> showDatePickerDialog());

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

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR) - 18; // Default to 18 years ago
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, selectedYear, selectedMonth, selectedDay) -> {
                // Save selected date as String in format "yyyy-MM-dd"
                selectedDateOfBirthString = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);

                // Format and display date as dd/MM/yyyy
                String displayDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                etDateOfBirth.setText(displayDate);
            },
            year, month, day
        );

        // Set max date to today (can't select future dates)
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

        // Set min date to 100 years ago
        calendar.add(Calendar.YEAR, -100);
        datePickerDialog.getDatePicker().setMinDate(calendar.getTimeInMillis());

        datePickerDialog.show();
    }

    private void handlePhoneSignup() {
        String name = etName.getText().toString().trim();
        String dateOfBirthStr = etDateOfBirth.getText().toString().trim();
        String phoneNumber = etPhoneNumber.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Vui lòng nhập họ tên", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(dateOfBirthStr)) {
            Toast.makeText(this, "Vui lòng chọn ngày sinh", Toast.LENGTH_SHORT).show();
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
                        // Sign up success - save user to database with phone number and date of birth
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String name = etName.getText().toString().trim();
                            String phoneNumber = etPhoneNumber.getText().toString().trim();

                            // Add country code if not present
                            if (!phoneNumber.startsWith("+")) {
                                phoneNumber = "+84" + phoneNumber;
                            }

                            // Convert String to LocalDate for backward compatibility with DatabaseManager
                            java.time.LocalDate dateOfBirth = null;
                            if (selectedDateOfBirthString != null) {
                                try {
                                    dateOfBirth = java.time.LocalDate.parse(selectedDateOfBirthString);
                                } catch (Exception e) {
                                    // If parsing fails, just log it
                                }
                            }

                            // Save user to database with phone number and date of birth
                            DatabaseManager.getInstance().createUserDocumentWithPhoneAndDOB(
                                user,
                                name,
                                phoneNumber,
                                dateOfBirth,
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
                        Toast.makeText(SignUpActivity.this, "Xác thực thất bại: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
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
