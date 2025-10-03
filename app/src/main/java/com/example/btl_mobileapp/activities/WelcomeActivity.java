package com.example.btl_mobileapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import android.os.Build;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.google.firebase.auth.FirebaseAuthSettings;

import com.example.btl_mobileapp.R;
import com.example.btl_mobileapp.utils.LoginPreferences;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class WelcomeActivity extends AppCompatActivity {
    @SuppressWarnings("deprecation")
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // Thiết lập navigation bar trong suốt trước khi load layout
            setupTransparentSystemBars();

            setContentView(R.layout.welcome);

            // Initialize Firebase Auth
            mAuth = FirebaseAuth.getInstance();
            FirebaseAuthSettings firebaseAuthSettings = mAuth.getFirebaseAuthSettings();
            firebaseAuthSettings.setAppVerificationDisabledForTesting(true);


            // Configure Google Sign In
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();

            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

            // Initialize Google Sign-In launcher
            googleSignInLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                            try {
                                GoogleSignInAccount account = task.getResult(ApiException.class);
                                firebaseAuthWithGoogle(account.getIdToken());
                            } catch (ApiException e) {
                                Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

            setupClickListeners();
        } catch (Exception e) {
            // Log error và không crash app
            android.util.Log.e("WelcomeActivity", "Error in onCreate", e);
        }
    }

    // Phương thức thiết lập navigation bar và status bar trong suốt
    private void setupTransparentSystemBars() {
        try {
            // Enable edge-to-edge display
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // Thiết lập status bar và navigation bar trong suốt
                getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);

                // Sử dụng màu an toàn cho navigation bar
                int navBarColor = androidx.core.content.ContextCompat.getColor(this, R.color.couple_pink_bg);
                getWindow().setNavigationBarColor(navBarColor);

                // Thiết lập màu icon trên system bars
                WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
                controller.setAppearanceLightStatusBars(true);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    controller.setAppearanceLightNavigationBars(true);
                }
            }

            // Tắt navigation bar contrast trên Android 10+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                getWindow().setNavigationBarContrastEnforced(false);
            }

            // Sử dụng WindowInsetsController thay vì deprecated system UI flags
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ - Sử dụng WindowInsetsController
                WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
                controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            } else {
                // Android 10 và thấp hơn - Sử dụng deprecated flags (nhưng vẫn cần thiết cho compatibility)
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                );
            }
        } catch (Exception e) {
            android.util.Log.e("WelcomeActivity", "Error setting up transparent system bars", e);
            // Fallback: sử dụng màu mặc định
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setStatusBarColor(getResources().getColor(android.R.color.black, getTheme()));
                    getWindow().setNavigationBarColor(getResources().getColor(android.R.color.black, getTheme()));
                }
            } catch (Exception fallbackException) {
                android.util.Log.e("WelcomeActivity", "Fallback also failed", fallbackException);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Chỉ check login state khi activity đã được resume hoàn toàn
      //  checkLoginState();
    }

    /*private void checkLoginState() {
        // Đảm bảo activity không bị destroyed
        if (isDestroyed() || isFinishing()) {
            return;
        }

        try {
            // Check SharedPreferences first
            if (LoginPreferences.isLoggedIn(this)) {
                // Also check Firebase Auth state
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null) {
                    // Kiểm tra xem user đã paired chưa
                    android.content.SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
                    boolean isPaired = prefs.getBoolean("isPaired", false);
                    String coupleId = prefs.getString("coupleId", "");
                    String partnerName = prefs.getString("partnerName", "");

                    if (isPaired && !coupleId.isEmpty()) {
                        // User đã paired, chuyển thẳng vào HomeMainActivity
                        android.util.Log.d("WelcomeActivity", "User already paired, navigating to HomeMainActivity");
                        navigateToHome(coupleId, partnerName);
                    } else {
                        // User đã đăng nhập nhưng chưa paired, chuyển vào PairingActivity
                        android.util.Log.d("WelcomeActivity", "User logged in but not paired, navigating to PairingActivity");
                        navigateToPairing();
                    }
                } else {
                    // Firebase user is null, clear login state
                    LoginPreferences.saveLoginState(this, false, "", "", "");
                    android.util.Log.d("WelcomeActivity", "Firebase user is null, staying on welcome screen");
                }
            } else {
                android.util.Log.d("WelcomeActivity", "User not logged in, staying on welcome screen");
            }
        } catch (Exception e) {
            android.util.Log.e("WelcomeActivity", "Error checking login state", e);
        }
    }
*/
    private void navigateToHome(String coupleId, String partnerName) {
        Intent intent = new Intent(this, HomeMainActivity.class);
        intent.putExtra("coupleId", coupleId);
        intent.putExtra("partnerName", partnerName);
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

    private void navigateToHome(FirebaseUser user) {
        // Đảm bảo activity không bị destroyed trước khi navigate
        if (isDestroyed() || isFinishing()) {
            return;
        }

        // First check if user needs pairing
        checkPairingStatus(user);
    }

    private void checkPairingStatus(FirebaseUser user) {
        // Đảm bảo activity không bị destroyed
        if (isDestroyed() || isFinishing()) {
            return;
        }

        try {
            // Check if user is already paired before going to HomeMainActivity
            com.example.btl_mobileapp.managers.DatabaseManager databaseManager =
                    com.example.btl_mobileapp.managers.DatabaseManager.getInstance();

            databaseManager.getCoupleByUserId(user.getUid(), new com.example.btl_mobileapp.managers.DatabaseManager.DatabaseCallback<com.example.btl_mobileapp.models.Couple>() {
                @Override
                public void onSuccess(com.example.btl_mobileapp.models.Couple couple) {
                    // Kiểm tra lại activity state trước khi navigate
                    if (isDestroyed() || isFinishing()) {
                        return;
                    }

                    // User is already paired, go to HomeMainActivity
                    Intent intent = new Intent(WelcomeActivity.this, HomeMainActivity.class);
                    if (user != null) {
                        intent.putExtra("user_name", user.getDisplayName() != null ? user.getDisplayName() : LoginPreferences.getUserName(WelcomeActivity.this));
                        intent.putExtra("user_email", user.getEmail());
                        intent.putExtra("user_id", user.getUid());
                        intent.putExtra("coupleId", couple.getCoupleId());
                    }

                    try {
                        startActivity(intent);
                        finish();
                    } catch (Exception e) {
                        android.util.Log.e("WelcomeActivity", "Error starting HomeMainActivity", e);
                    }
                }

                @Override
                public void onError(String error) {
                    // Kiểm tra lại activity state trước khi navigate
                    if (isDestroyed() || isFinishing()) {
                        return;
                    }

                    // User is not paired yet, go to PairingActivity
                    Intent intent = new Intent(WelcomeActivity.this, PairingActivity.class);
                    try {
                        startActivity(intent);
                        finish();
                    } catch (Exception e) {
                        android.util.Log.e("WelcomeActivity", "Error starting PairingActivity", e);
                    }
                }
            });
        } catch (Exception e) {
            android.util.Log.e("WelcomeActivity", "Error in checkPairingStatus", e);
        }
    }

    private void setupClickListeners() {
        // Setup click listeners using actual IDs from welcome.xml layout

        // Phone login button -> redirect to LoginByPhoneActivity
        setupClickListener(R.id.btnLoginPhone, () -> {
            Intent intent = new Intent(WelcomeActivity.this, LoginByPhoneActivity.class);
            startActivity(intent);
        });

        // Email login button -> redirect to Google login
        setupClickListener(R.id.btnLoginMail, this::signInWithGoogle);

        // Sign up text -> redirect to SignUpActivity
        setupClickListener(R.id.tv_signup, () -> {
            Intent intent = new Intent(WelcomeActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
    }

    private void setupClickListener(int viewId, Runnable action) {
        try {
            android.view.View view = findViewById(viewId);
            if (view != null) {
                view.setOnClickListener(v -> action.run());
            }
        } catch (Exception e) {
            // Ignore if view ID doesn't exist
        }
    }

    private void signInWithGoogle() {
        // Clear any existing Google sign-in to force account selection
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            // After signing out, start the sign-in process which will show account picker
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        // First, get the Google account info to check email
        GoogleSignInAccount lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(this);
        if (lastSignedInAccount != null && lastSignedInAccount.getEmail() != null) {
            String googleEmail = lastSignedInAccount.getEmail();

            // Check if this email exists in Firestore database
            com.example.btl_mobileapp.managers.DatabaseManager.getInstance().checkEmailExists(
                    googleEmail,
                    new com.example.btl_mobileapp.managers.DatabaseManager.DatabaseCallback<com.example.btl_mobileapp.models.User>() {
                        @Override
                        public void onSuccess(com.example.btl_mobileapp.models.User existingUser) {
                            if (existingUser != null) {
                                // Email exists in database, proceed with Firebase Auth
                                proceedWithGoogleLogin(credential);
                            } else {
                                // Email not found in database
                                showAccountNotFoundDialog(googleEmail);
                            }
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(WelcomeActivity.this,
                                    "Lỗi kiểm tra tài khoản: " + error, Toast.LENGTH_LONG).show();
                        }
                    }
            );
        } else {
            // Fallback: proceed with Firebase Auth if can't get email
            proceedWithGoogleLogin(credential);
        }
    }

    private void proceedWithGoogleLogin(AuthCredential credential) {

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Save login state to SharedPreferences
                            LoginPreferences.saveLoginState(
                                    this,
                                    true,
                                    user.getEmail(),
                                    user.getDisplayName() != null ? user.getDisplayName() : "",
                                    user.getUid()
                            );

                            Toast.makeText(this, "Đăng nhập Google thành công!", Toast.LENGTH_SHORT).show();
                            navigateToHome(user);
                        }
                    } else {
                        // Sign in failed
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showAccountNotFoundDialog(String email) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Tài khoản chưa tồn tại")
                .setMessage("Email " + email + " chưa được đăng ký trong hệ thống.\n\n" +
                        "Bạn cần tạo tài khoản trước khi đăng nhập bằng Google.")
                .setPositiveButton("Đăng ký ngay", (dialog, which) -> {
                    // Navigate to sign up screen
                    Intent intent = new Intent(WelcomeActivity.this, SignUpActivity.class);
                    intent.putExtra("google_email", email); // Pass Google email to pre-fill
                    startActivity(intent);
                })
                .setNegativeButton("Đóng", (dialog, which) -> {
                    // Sign out from Google to clear selection
                    mGoogleSignInClient.signOut();
                })
                .setCancelable(false)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check login state every time the activity starts
        //checkLoginState();
    }
}