package com.example.couple_app.activities;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
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
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthSettings;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.functions.FirebaseFunctions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ForgotPasswordActivity extends AppCompatActivity {
    private EditText etPhoneNumber, etVerificationCode, etNewPassword, etConfirmPassword;
    private Button btnSendCode, btnVerifyAndReset, btnVerifyOtp;
    private TextView tvBackToLogin;
    private ProgressBar progressBar;
    private ImageButton btnBack;

    private FirebaseAuth mAuth;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private PhoneAuthCredential mPhoneCredential;

    private boolean tempSessionActive = false;
    private boolean resetCompleted = false;

    // HTTP function URL (region asia-southeast1, project couples-app-b83be)
    private static final String RESET_PASSWORD_HTTP_URL = "https://us-central1-couples-app-b83be.cloudfunctions.net/resetPasswordViaPhoneHttp";

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
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
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
        // In debug builds only, bypass app verification for TEST phone numbers
        try {
            boolean isDebuggable = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            if (isDebuggable) {
                FirebaseAuthSettings settings = mAuth.getFirebaseAuthSettings();
                // Enable disabling app verification for testing in debug builds
                settings.setAppVerificationDisabledForTesting(true);
            }
        } catch (Exception ignored) {}
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
                String message = e.getMessage() != null ? e.getMessage() : e.toString();
                Toast.makeText(ForgotPasswordActivity.this, "Xác thực thất bại: " + message,
                    Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                hideProgressBar();
                mVerificationId = verificationId;
                mResendToken = token;

                // Show OTP field and Verify OTP button only
                etVerificationCode.setVisibility(View.VISIBLE);
                btnVerifyOtp.setVisibility(View.VISIBLE);
                btnSendCode.setEnabled(false);

                Toast.makeText(ForgotPasswordActivity.this, "Mã xác nhận đã được gửi", Toast.LENGTH_SHORT).show();
            }
        };
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> leaveForgotPasswordAndFinish());

        btnSendCode.setOnClickListener(v -> handleSendCode());

        btnVerifyOtp.setOnClickListener(v -> handleVerifyOtpOnly());

        btnVerifyAndReset.setOnClickListener(v -> handleResetAfterOtpVerified());

        tvBackToLogin.setOnClickListener(v -> {
            signOutTempIfAny();
            Intent intent = new Intent(this, LoginByPhoneActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void leaveForgotPasswordAndFinish() {
        signOutTempIfAny();
        finish();
    }

    private void signOutTempIfAny() {
        try {
            if (tempSessionActive && mAuth.getCurrentUser() != null) {
                mAuth.signOut();
                tempSessionActive = false;
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // If activity is being destroyed (not due to config change) and reset hasn't completed,
        // ensure temporary session is cleared
        if (!isChangingConfigurations() && !resetCompleted) {
            signOutTempIfAny();
        }
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

        showProgressBar();
        // Directly send verification code without pre-checking registration
        sendVerificationCode(finalPhoneNumber);
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

    private void handleVerifyOtpOnly() {
        String code = etVerificationCode.getText().toString().trim();
        if (TextUtils.isEmpty(code)) {
            Toast.makeText(this, "Vui lòng nhập mã xác nhận", Toast.LENGTH_SHORT).show();
            return;
        }
        showProgressBar();
        PhoneAuthCredential credential = (mPhoneCredential != null)
                ? mPhoneCredential
                : PhoneAuthProvider.getCredential(mVerificationId, code);

        mAuth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                boolean isNewUser = false;
                try {
                    if (task.getResult() != null && task.getResult().getAdditionalUserInfo() != null) {
                        isNewUser = task.getResult().getAdditionalUserInfo().isNewUser();
                    }
                } catch (Exception ignored) {}

                if (isNewUser) {
                    mAuth.signOut();
                    hideProgressBar();
                    Toast.makeText(this, "Số điện thoại chưa đư���c liên kết với tài khoản.", Toast.LENGTH_LONG).show();
                    return;
                }

                // Ensure currentUser is present before marking temp session
                if (mAuth.getCurrentUser() == null) {
                    hideProgressBar();
                    Toast.makeText(this, "Không thể xác thực người dùng tạm thời. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
                    return;
                }

                // Mark temp session active only after verified and linked
                tempSessionActive = true;

                // Reveal password fields and reset button
                hideProgressBar();
                etNewPassword.setVisibility(View.VISIBLE);
                etConfirmPassword.setVisibility(View.VISIBLE);
                btnVerifyAndReset.setVisibility(View.VISIBLE);
                etVerificationCode.setEnabled(false);
                btnVerifyOtp.setEnabled(false);
                Toast.makeText(this, "Xác thực OTP thành công. Hãy nhập mật khẩu mới.", Toast.LENGTH_SHORT).show();
            } else {
                hideProgressBar();
                Toast.makeText(this, "Xác thực OTP thất bại.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleResetAfterOtpVerified() {
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
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
        showProgressBar();
        resetPasswordOnBackend(newPassword);
    }

    private void resetPasswordOnBackend(String newPassword) {
        // Ensure we have a signed-in user (temporary session) before calling backend
        if (mAuth.getCurrentUser() == null) {
            hideProgressBar();
            Toast.makeText(this, "Người dùng chưa được xác thực. Vui lòng xác thực OTP trước khi đổi mật khẩu.", Toast.LENGTH_LONG).show();
            return;
        }

        // Try to get a fresh ID token to provide to the backend for verification
        mAuth.getCurrentUser().getIdToken(true)
            .addOnSuccessListener(result -> {
                if (result == null) {
                    hideProgressBar();
                    Toast.makeText(ForgotPasswordActivity.this, "Không thể lấy token xác thực (kết quả null)", Toast.LENGTH_LONG).show();
                    return;
                }
                String idToken = result.getToken();
                if (idToken == null || idToken.isEmpty()) {
                    hideProgressBar();
                    Toast.makeText(ForgotPasswordActivity.this, "ID token rỗng, không thể tiến hành.", Toast.LENGTH_LONG).show();
                    return;
                }

                String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
                String phone = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getPhoneNumber() : null;

                // Use the correct region where functions are deployed
                FirebaseFunctions functions = FirebaseFunctions.getInstance("asia-southeast1");

                // Debug logging to help backend troubleshooting
                android.util.Log.d("ForgotPassword", "Calling resetPasswordViaPhone with uid=" + uid + " phone=" + phone);

                Map<String, Object> data = new HashMap<>();
                data.put("newPassword", newPassword);
                data.put("idToken", idToken); // server will verify as fallback if context.auth missing
                if (uid != null) data.put("uid", uid);
                if (phone != null) data.put("phoneNumber", phone);

                functions.getHttpsCallable("resetPasswordViaPhone")
                    .call(data)
                    .addOnSuccessListener(httpsCallableResult -> {
                        // success
                        resetCompleted = true;
                        mAuth.signOut();
                        tempSessionActive = false;
                        hideProgressBar();
                        Toast.makeText(ForgotPasswordActivity.this, "Đổi mật khẩu thành công!", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(ForgotPasswordActivity.this, LoginByPhoneActivity.class);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        hideProgressBar();
                        // Log full exception for debugging
                        android.util.Log.e("ForgotPassword", "resetPasswordViaPhone failed", e);

                        // If e has details (FirebaseFunctionsException), try to extract
                        String errMsg = e.getMessage();
                        try {
                            java.lang.reflect.Method m = e.getClass().getMethod("getDetails");
                            Object details = m.invoke(e);
                            android.util.Log.e("ForgotPassword", "Function details: " + String.valueOf(details));
                            if (details instanceof java.util.Map) {
                                Object desc = ((java.util.Map) details).get("message");
                                if (desc != null) errMsg = desc.toString();
                            }
                        } catch (Exception ex) {
                            // ignore reflection errors
                        }

                        // Try HTTP fallback using idToken
                        callResetPasswordHttp(newPassword, idToken);
                    });
            })
            .addOnFailureListener(e -> {
                hideProgressBar();
                String msg = e != null ? e.getMessage() : "Lỗi khi lấy token";
                android.util.Log.e("ForgotPassword", "getIdToken failed", e);
                Toast.makeText(ForgotPasswordActivity.this, "Không thể lấy token xác thực: " + msg, Toast.LENGTH_LONG).show();
            });
    }

    // HTTP fallback implementation: POST JSON { newPassword } with Authorization: Bearer <idToken>
    private void callResetPasswordHttp(String newPassword, String idToken) {
        if (idToken == null || idToken.isEmpty()) {
            android.util.Log.w("ForgotPassword", "No idToken available for HTTP fallback");
            return;
        }

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(RESET_PASSWORD_HTTP_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Authorization", "Bearer " + idToken);
                conn.setDoOutput(true);
                JSONObject body = new JSONObject();
                body.put("newPassword", newPassword);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes("UTF-8"));
                }

                int code = conn.getResponseCode();
                InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
                String responseText = readStream(is);
                android.util.Log.d("ForgotPassword", "HTTP fallback response code=" + code + " body=" + responseText);

                runOnUiThread(() -> {
                    if (code >= 200 && code < 300) {
                        resetCompleted = true;
                        try { mAuth.signOut(); } catch (Exception ignored) {}
                        tempSessionActive = false;
                        hideProgressBar();
                        Toast.makeText(ForgotPasswordActivity.this, "Đổi mật khẩu thành công! (via HTTP)", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(ForgotPasswordActivity.this, LoginByPhoneActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        hideProgressBar();
                        String msg = responseText != null ? responseText : "HTTP error code " + code;
                        Toast.makeText(ForgotPasswordActivity.this, "HTTP fallback lỗi: " + msg, Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception ex) {
                android.util.Log.e("ForgotPassword", "HTTP fallback exception", ex);
                runOnUiThread(() -> {
                    hideProgressBar();
                    Toast.makeText(ForgotPasswordActivity.this, "Lỗi khi gọi backend (HTTP): " + ex.getMessage(), Toast.LENGTH_LONG).show();
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private String readStream(InputStream is) {
        if (is == null) return null;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            android.util.Log.e("ForgotPassword", "readStream failed", e);
            return null;
        }
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
