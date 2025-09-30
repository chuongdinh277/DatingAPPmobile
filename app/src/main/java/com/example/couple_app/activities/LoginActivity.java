package com.example.couple_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.example.couple_app.R;
import com.example.couple_app.MainActivity;
import com.example.couple_app.managers.AuthManager;
import com.example.couple_app.managers.DatabaseManager;
import com.example.couple_app.utils.CoupleAppUtils;
import java.util.List;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    private EditText etEmail, etPassword;
    private Button btnLogin, btnGoogleSignIn;
    private TextView tvSignUp, tvForgotPassword;
    private ProgressBar progressBar;

    private AuthManager authManager;
    private GoogleSignInClient googleSignInClient;

    // Replace deprecated startActivityForResult
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        setupGoogleSignInLauncher(); // Add this
        setupGoogleSignIn();
        setupClickListeners();

        authManager = AuthManager.getInstance();

        // Check if user is already signed in
        if (authManager.isSignedIn()) {
            navigateToPairing();
        }

        // Check if email was passed from SignUpActivity
        handleIntentExtras();
    }

    private void handleIntentExtras() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("email")) {
            String email = intent.getStringExtra("email");
            if (email != null && !email.isEmpty()) {
                etEmail.setText(email);
                etPassword.requestFocus(); // Focus on password field since email is pre-filled
                Log.d(TAG, "Email pre-filled from signup: " + email);
            }
        }
    }

    private void initViews() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnGoogleSignIn = findViewById(R.id.btn_google_signin);
        tvSignUp = findViewById(R.id.tv_signup);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupGoogleSignInLauncher() {
        googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d(TAG, "Google Sign-In result code: " + result.getResultCode());

                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        Log.d(TAG, "Google sign in successful: " + account.getId());
                        firebaseAuthWithGoogle(account.getIdToken());
                    } catch (ApiException e) {
                        Log.w(TAG, "Google sign in failed", e);
                        showLoading(false);
                        handleGoogleSignInError(e);
                    }
                } else if (result.getResultCode() == RESULT_CANCELED) {
                    showLoading(false);
                    Log.d(TAG, "Google Sign-In was cancelled by user");
                    Toast.makeText(this, "Đăng nhập Google đã bị hủy", Toast.LENGTH_SHORT).show();
                } else {
                    showLoading(false);
                    Log.e(TAG, "Google Sign-In failed with result code: " + result.getResultCode());
                    Toast.makeText(this, "Đăng nhập Google thất bại", Toast.LENGTH_SHORT).show();
                }
            }
        );
    }

    private void handleGoogleSignInError(ApiException e) {
        String errorMsg = "Google đăng nhập thất bại";

        switch (e.getStatusCode()) {
            case 12501: // CommonStatusCodes.CANCELED
                errorMsg = "Đăng nhập Google đã bị hủy";
                Log.d(TAG, "Google Sign-In cancelled by user");
                break;
            case 10: // GoogleSignInStatusCodes.SIGN_IN_FAILED
                errorMsg = "Cấu hình Google Sign-In không đúng. Kiểm tra SHA-1 fingerprint và Web Client ID";
                Log.e(TAG, "Google Sign-In configuration error");
                break;
            case 7: // CommonStatusCodes.NETWORK_ERROR
                errorMsg = "Lỗi kết nối mạng. Vui lòng kiểm tra internet";
                break;
            case 8: // CommonStatusCodes.INTERNAL_ERROR
                errorMsg = "Lỗi hệ thống Google. Vui lòng thử lại";
                break;
            case 12500: // GoogleSignInStatusCodes.SIGN_IN_CANCELLED
                errorMsg = "Đăng nhập đã bị hủy";
                break;
            default:
                errorMsg += ": " + e.getMessage() + " (Code: " + e.getStatusCode() + ")";
                Log.e(TAG, "Unknown Google Sign-In error: " + e.getStatusCode());
                break;
        }

        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
    }

    private void setupGoogleSignIn() {
        try {
            // Check if Web Client ID is configured
            String webClientId = getString(R.string.default_web_client_id);
            Log.d(TAG, "Web Client ID: " + webClientId);

            if (webClientId.equals("your_web_client_id_here") || webClientId.isEmpty()) {
                Log.e(TAG, "❌ CRITICAL ERROR: Google Web Client ID not configured!");
                Toast.makeText(this, "❌ Google Sign-In chưa được cấu hình. Vui lòng thêm Web Client ID vào strings.xml", Toast.LENGTH_LONG).show();
                // Disable Google Sign-In button
                btnGoogleSignIn.setEnabled(false);
                btnGoogleSignIn.setText("Google Sign-In chưa được cấu hình");
                return;
            }

            // Sign out any previously signed in account to avoid conflicts
            GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut();

            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .requestProfile()
                    .build();

            googleSignInClient = GoogleSignIn.getClient(this, gso);

            Log.d(TAG, "✅ Google Sign-In configured successfully");

            // Verify Google Play Services availability
            verifyGooglePlayServices();

        } catch (Exception e) {
            Log.e(TAG, "Error setting up Google Sign-In", e);
            Toast.makeText(this, "Lỗi thiết lập Google Sign-In: " + e.getMessage(), Toast.LENGTH_LONG).show();
            btnGoogleSignIn.setEnabled(false);
        }
    }

    private void verifyGooglePlayServices() {
        try {
            // Check if Google Play Services is available
            int result = com.google.android.gms.common.GoogleApiAvailability.getInstance()
                    .isGooglePlayServicesAvailable(this);

            if (result != com.google.android.gms.common.ConnectionResult.SUCCESS) {
                Log.w(TAG, "Google Play Services not available: " + result);
                Toast.makeText(this, "Google Play Services không khả dụng. Vui lòng cập nhật Google Play Services",
                    Toast.LENGTH_LONG).show();
                btnGoogleSignIn.setEnabled(false);
            } else {
                Log.d(TAG, "✅ Google Play Services is available");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking Google Play Services", e);
        }
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> signInWithEmail());
        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
        tvSignUp.setOnClickListener(v -> navigateToSignUp());
        tvForgotPassword.setOnClickListener(v -> showForgotPassword());
    }

    private void signInWithEmail() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!validateInput(email, password)) {
            return;
        }

        showLoading(true);

        authManager.signIn(email, password, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                Log.d(TAG, "Email sign in successful: " + user.getEmail());
                showLoading(false);
                Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                navigateToPairing();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Email sign in failed: " + error);
                showLoading(false);
                Toast.makeText(LoginActivity.this, "Đăng nhập thất bại: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void signInWithGoogle() {
        if (googleSignInClient == null) {
            Toast.makeText(this, "Google Sign-In chưa được cấu hình", Toast.LENGTH_SHORT).show();
            return;
        }

        // Log debug information
        logDebugInfo();

        showLoading(true);
        Intent signInIntent = googleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        // Lấy email từ Google account
        GoogleSignInAccount googleAccount = GoogleSignIn.getLastSignedInAccount(this);
        String email = googleAccount != null ? googleAccount.getEmail() : "";

        if (email.isEmpty()) {
            showLoading(false);
            Toast.makeText(this, "Không thể lấy email từ Google account", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Starting Google authentication for email: " + email);

        // Store the Google ID token in AuthManager for potential re-authentication
        authManager.setStoredGoogleIdToken(idToken);

        // Sử dụng phương thức mới để đăng nhập và kiểm tra liên kết
        authManager.signInWithGoogleAndCheckLinking(credential, email, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                Log.d(TAG, "Google authentication completed successfully");
                showLoading(false);

                Toast.makeText(LoginActivity.this, "Đăng nhập Google thành công!", Toast.LENGTH_SHORT).show();

                navigateToPairing();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Google authentication failed: " + error);

                // Kiểm tra xem có phải lỗi cần liên kết không
                if (error.startsWith("NEEDS_LINKING:")) {
                    String emailToLink = error.substring("NEEDS_LINKING:".length());
                    showLoading(false);
                    showLinkingRequiredDialog(emailToLink, credential);
                } else {
                    showLoading(false);
                    Toast.makeText(LoginActivity.this, "Đăng nhập Google thất bại: " + error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void showLinkingRequiredDialog(String email, AuthCredential googleCredential) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Cần liên kết tài khoản")
            .setMessage("Email " + email + " đã tồn tại với mật khẩu.\n\n" +
                "Để sử dụng đăng nhập Google, bạn cần liên kết với tài khoản hiện có.\n\n" +
                "Vui lòng nhập mật khẩu hiện tại để liên kết:")
            .setPositiveButton("Liên kết", (dialog, which) -> {
                dialog.dismiss();
                showPasswordInputForLinking(email, googleCredential);
            })
            .setNegativeButton("Hủy", (dialog, which) -> {
                dialog.dismiss();
                Toast.makeText(this, "Đã hủy đăng nhập Google", Toast.LENGTH_SHORT).show();
            })
            .setCancelable(false)
            .show();
    }

    private void showPasswordInputForLinking(String email, AuthCredential googleCredential) {
        // Tạo dialog nhập mật khẩu
        android.widget.EditText passwordInput = new android.widget.EditText(this);
        passwordInput.setHint("Nhập mật khẩu hiện tại");
        passwordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setPadding(50, 40, 50, 40);

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Liên kết tài khoản")
            .setMessage("Email: " + email + "\n\nMật khẩu:")
            .setView(passwordInput)
            .setPositiveButton("Xác nhận", (dialog, which) -> {
                String password = passwordInput.getText().toString().trim();

                if (password.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập mật khẩu", Toast.LENGTH_SHORT).show();
                    // Hiển thị lại dialog
                    showPasswordInputForLinking(email, googleCredential);
                    return;
                }

                // Thực hiện liên kết tài khoản
                performAccountLinking(email, password, googleCredential);
            })
            .setNegativeButton("Hủy", (dialog, which) -> {
                Toast.makeText(this, "Đã hủy liên kết tài khoản", Toast.LENGTH_SHORT).show();
            })
            .setCancelable(false)
            .show();

        // Focus vào input và hiện bàn phím
        passwordInput.requestFocus();
    }

    private void performAccountLinking(String email, String password, AuthCredential googleCredential) {
        showLoading(true);

        // Sử dụng phương thức mới từ AuthManager
        authManager.linkGoogleAccountWithPassword(email, password, googleCredential, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                showLoading(false);

                // Hiển thị thông báo thành công
                new androidx.appcompat.app.AlertDialog.Builder(LoginActivity.this)
                    .setTitle("Liên kết thành công!")
                    .setMessage("Tài khoản của bạn đã được liên kết thành công.\n\n" +
                        "Từ giờ bạn có thể đăng nhập bằng:\n" +
                        "• Email và mật khẩu\n" +
                        "• Tài khoản Google\n\n" +
                        "Cả hai phương thức đều truy cập vào cùng một tài khoản.")
                    .setPositiveButton("Tiếp tục", (dialog, which) -> {
                        Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                        navigateToPairing();
                    })
                    .setCancelable(false)
                    .show();
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                Log.e(TAG, "Failed to link account: " + error);

                new androidx.appcompat.app.AlertDialog.Builder(LoginActivity.this)
                    .setTitle("Lỗi liên kết")
                    .setMessage(error)
                    .setPositiveButton("Thử lại", (dialog, which) -> {
                        showPasswordInputForLinking(email, googleCredential);
                    })
                    .setNegativeButton("Hủy", (dialog, which) -> {
                        Toast.makeText(LoginActivity.this, "Đã hủy liên kết tài khoản", Toast.LENGTH_SHORT).show();
                    })
                    .show();
            }
        });
    }

    private boolean validateInput(String email, String password) {
        if (!CoupleAppUtils.isValidEmail(email)) {
            etEmail.setError("Email không hợp lệ");
            etEmail.requestFocus();
            return false;
        }

        if (!CoupleAppUtils.isValidPassword(password)) {
            etPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            etPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void showForgotPassword() {
        String email = etEmail.getText().toString().trim();

        if (!CoupleAppUtils.isValidEmail(email)) {
            etEmail.setError("Vui lòng nhập email hợp lệ để reset mật khẩu");
            etEmail.requestFocus();
            return;
        }

        showLoading(true);

        authManager.sendPasswordResetEmail(email, new AuthManager.AuthActionCallback() {
            @Override
            public void onSuccess() {
                showLoading(false);
                Toast.makeText(LoginActivity.this, "Email reset mật khẩu đã được gửi!", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                Toast.makeText(LoginActivity.this, "Gửi email reset thất bại: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void navigateToSignUp() {
        Intent intent = new Intent(this, SignUpActivity.class);
        startActivity(intent);
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToPairing() {
        Intent intent = new Intent(this, PairingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
        btnGoogleSignIn.setEnabled(!show);
    }

    private void logDebugInfo() {
        try {
            Log.d(TAG, "=== Google Sign-In Debug Info ===");
            Log.d(TAG, "Web Client ID configured: " + getString(R.string.default_web_client_id));
            Log.d(TAG, "Google Play Services available: " +
                (com.google.android.gms.common.GoogleApiAvailability.getInstance()
                    .isGooglePlayServicesAvailable(this) == com.google.android.gms.common.ConnectionResult.SUCCESS));
            Log.d(TAG, "GoogleSignInClient initialized: " + (googleSignInClient != null));
            Log.d(TAG, "================================");
        } catch (Exception e) {
            Log.e(TAG, "Error logging debug info", e);
        }
    }
}
