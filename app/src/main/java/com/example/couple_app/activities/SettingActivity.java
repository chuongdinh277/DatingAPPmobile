package com.example.couple_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.button.MaterialButton;
import com.example.couple_app.R;
import com.example.couple_app.managers.AuthManager;
import com.example.couple_app.managers.DatabaseManager;
import com.example.couple_app.utils.AvatarCache;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;
import java.util.List;
import com.google.firebase.auth.FirebaseAuthSettings;

public class SettingActivity extends BaseActivity {
    private static final String TAG = "SettingActivity";
    private static final int RC_GOOGLE_SIGN_IN = 9001;

    private FirebaseAuth mAuth;
    private AuthManager authManager;
    private GoogleSignInClient googleSignInClient;
    private MaterialButton btLinkGoogle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting);

        // Highlight tab "settings"
        setActiveButton("settings");

        // Initialize Firebase Auth and AuthManager
        mAuth = FirebaseAuth.getInstance();
        FirebaseAuthSettings firebaseAuthSettings = mAuth.getFirebaseAuthSettings();
        firebaseAuthSettings.setAppVerificationDisabledForTesting(true);
        authManager = AuthManager.getInstance();

        // Setup Google Sign In
        setupGoogleSignIn();

        // Ánh xạ các button
        MaterialButton btProfile = findViewById(R.id.bt_profile);
        MaterialButton btNotification = findViewById(R.id.bt_notification);
        MaterialButton btPrivacy = findViewById(R.id.bt_privacy);
        MaterialButton btAbout = findViewById(R.id.bt_about);
        btLinkGoogle = findViewById(R.id.bt_link_google);
        MaterialButton btLogout = findViewById(R.id.bt_logout);

        // Update UI based on current linked providers
        updateLinkGoogleButton();

        // Profile
        btProfile.setOnClickListener(v -> {
            Intent intent = new Intent(SettingActivity.this, SettingProfileActivity.class);
            startActivity(intent);
        });

        // Notification
        btNotification.setOnClickListener(v -> {
            // TODO: Create SettingNotificationActivity later
            Toast.makeText(this, "Notification settings coming soon", Toast.LENGTH_SHORT).show();
        });

        // Privacy
        btPrivacy.setOnClickListener(v -> {
            // TODO: Create SettingPrivacyActivity later
            Toast.makeText(this, "Privacy settings coming soon", Toast.LENGTH_SHORT).show();
        });

        // About
        btAbout.setOnClickListener(v -> {
            showAboutDialog();
        });

        // Link/Unlink Google
        btLinkGoogle.setOnClickListener(v -> {
            handleGoogleLinkAction();
        });

        // Logout with confirmation dialog and Firebase logout
        btLogout.setOnClickListener(v -> {
            showLogoutDialog();
        });
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void updateLinkGoogleButton() {
        List<String> providers = authManager.getLinkedProviders();
        boolean hasGoogle = providers.contains("google.com");
        boolean canUnlink = providers.size() > 1; // Must keep at least one provider

        if (hasGoogle) {
            btLinkGoogle.setText("Hủy liên kết Google");
            btLinkGoogle.setEnabled(canUnlink);
            if (!canUnlink) {
                btLinkGoogle.setText("Google (Phương thức duy nhất)");
            }
        } else {
            btLinkGoogle.setText("Liên kết Google");
            btLinkGoogle.setEnabled(true);
        }
    }

    private void handleGoogleLinkAction() {
        List<String> providers = authManager.getLinkedProviders();
        boolean hasGoogle = providers.contains("google.com");

        if (hasGoogle) {
            // Unlink Google account
            showUnlinkGoogleDialog();
        } else {
            // Link Google account
            linkGoogleAccount();
        }
    }

    private void linkGoogleAccount() {
        // Sign out from Google first to ensure account picker shows up
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Log.d(TAG, "Google sign out completed, now showing account picker");

            // Create new GoogleSignInOptions that forces account selection
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();

            GoogleSignInClient newGoogleSignInClient = GoogleSignIn.getClient(this, gso);

            // Get sign in intent - this will show account picker since we signed out
            Intent signInIntent = newGoogleSignInClient.getSignInIntent();

            // Update the client reference for future use
            googleSignInClient = newGoogleSignInClient;

            startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
        });
    }

    private void showUnlinkGoogleDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Hủy liên kết Google")
                .setMessage("Bạn có chắc chắn muốn hủy liên kết tài khoản Google?\n\nLưu ý: Bạn phải có ít nhất một phương thức đăng nhập.")
                .setPositiveButton("Hủy liên kết", (dialog, which) -> {
                    authManager.unlinkGoogleAccount(new AuthManager.AuthActionCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(SettingActivity.this,
                                    "Đã hủy liên kết tài khoản Google thành công", Toast.LENGTH_SHORT).show();
                            updateLinkGoogleButton();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(SettingActivity.this,
                                    "Lỗi hủy liên kết: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);

                authManager.linkGoogleAccount(credential, new AuthManager.AuthActionCallback() {
                    @Override
                    public void onSuccess() {
                        // After successful Google linking, check if email already exists in database
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null && account.getEmail() != null) {
                            // First check if email already exists in database
                            DatabaseManager.getInstance().checkEmailExists(
                                account.getEmail(),
                                new DatabaseManager.DatabaseCallback<com.example.couple_app.models.User>() {
                                    @Override
                                    public void onSuccess(com.example.couple_app.models.User existingUser) {
                                        if (existingUser != null && !existingUser.getUserId().equals(user.getUid())) {
                                            // Email already exists with different user
                                            Log.w(TAG, "Email already linked to different user: " + existingUser.getUserId());

                                            // Show detailed error with existing user info
                                            String errorMessage = "Email " + account.getEmail() +
                                                " đã được liên kết với tài khoản khác";

                                            if (existingUser.getName() != null) {
                                                errorMessage += " (Tên: " + existingUser.getName() + ")";
                                            }
                                            if (existingUser.getPhoneNumber() != null) {
                                                errorMessage += "\nSố điện thoại: " + existingUser.getPhoneNumber();
                                            }

                                            showEmailAlreadyLinkedDialog(account.getEmail(), existingUser);

                                            // Unlink the Google account since we can't use this email
                                            authManager.unlinkGoogleAccount(new AuthManager.AuthActionCallback() {
                                                @Override
                                                public void onSuccess() {
                                                    Log.d(TAG, "Google account unlinked due to email conflict");
                                                    updateLinkGoogleButton();
                                                }

                                                @Override
                                                public void onError(String error) {
                                                    Log.e(TAG, "Failed to unlink Google account: " + error);
                                                }
                                            });

                                        } else {
                                            // Email not exists or belongs to current user, proceed with email update
                                            DatabaseManager.getInstance().updateUserEmail(
                                                user.getUid(),
                                                account.getEmail(),
                                                new AuthManager.AuthActionCallback() {
                                                    @Override
                                                    public void onSuccess() {
                                                        Log.d(TAG, "Email updated in database: " + account.getEmail());
                                                        Toast.makeText(SettingActivity.this,
                                                                "Đã liên kết và cập nhật email Google thành công!", Toast.LENGTH_SHORT).show();
                                                        updateLinkGoogleButton();

                                                        // Show current user info
                                                        showAccountLinkSuccessDialog(user);
                                                    }

                                                    @Override
                                                    public void onError(String error) {
                                                        Log.w(TAG, "Failed to update email in database: " + error);
                                                        // Still show success for linking, but note email update failed
                                                        Toast.makeText(SettingActivity.this,
                                                                "Liên kết Google thành công, nhưng không cập nhật được email vào database",
                                                                Toast.LENGTH_LONG).show();
                                                        updateLinkGoogleButton();
                                                        showAccountLinkSuccessDialog(user);
                                                    }
                                                }
                                            );
                                        }
                                    }

                                    @Override
                                    public void onError(String error) {
                                        Log.e(TAG, "Failed to check email existence: " + error);
                                        Toast.makeText(SettingActivity.this,
                                                "Lỗi kiểm tra email: " + error, Toast.LENGTH_LONG).show();
                                    }
                                }
                            );
                        } else {
                            // No email available or user is null
                            Toast.makeText(SettingActivity.this,
                                    "Đã liên kết tài khoản Google thành công!", Toast.LENGTH_SHORT).show();
                            updateLinkGoogleButton();

                            if (user != null) {
                                showAccountLinkSuccessDialog(user);
                            }
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Google linking error: " + error);
                        if (error.contains("already linked")) {
                            Toast.makeText(SettingActivity.this,
                                    "Tài khoản Google này đã được liên kết với tài khoản khác", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(SettingActivity.this,
                                    "Lỗi liên kết Google: " + error, Toast.LENGTH_LONG).show();
                        }
                    }
                });

            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(this, "Đăng nhập Google thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showAccountLinkSuccessDialog(FirebaseUser user) {
        StringBuilder message = new StringBuilder("Tài khoản đã được liên kết thành công!\n\n");
        message.append("Thông tin tài khoản:\n");

        if (user.getDisplayName() != null) {
            message.append("Tên: ").append(user.getDisplayName()).append("\n");
        }
        if (user.getEmail() != null) {
            message.append("Email: ").append(user.getEmail()).append("\n");
        }
        if (user.getPhoneNumber() != null) {
            message.append("SĐT: ").append(user.getPhoneNumber()).append("\n");
        }

        List<String> providers = authManager.getLinkedProviders();
        message.append("\nPhương thức đăng nhập:\n");
        for (String provider : providers) {
            switch (provider) {
                case "google.com":
                    message.append("✓ Google\n");
                    break;
                case "phone":
                    message.append("✓ Số điện thoại\n");
                    break;
                case "password":
                    message.append("✓ Email/Mật khẩu\n");
                    break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Liên kết thành công!")
                .setMessage(message.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void showEmailAlreadyLinkedDialog(String email, com.example.couple_app.models.User existingUser) {
        StringBuilder message = new StringBuilder();
        message.append("❌ Không thể liên kết tài khoản Google\n\n");
        message.append("Email: ").append(email).append("\n");
        message.append("đã được liên kết với tài khoản khác:\n\n");

        if (existingUser.getName() != null) {
            message.append("👤 Tên: ").append(existingUser.getName()).append("\n");
        }
        if (existingUser.getPhoneNumber() != null) {
            message.append("📱 SĐT: ").append(existingUser.getPhoneNumber()).append("\n");
        }
        if (existingUser.getUserId() != null) {
            message.append("🆔 ID: ").append(existingUser.getUserId().substring(0, 8)).append("...\n");
        }

        message.append("\n💡 Gợi ý:\n");
        message.append("• Sử dụng email Google khác\n");
        message.append("• Hoặc đăng nhập vào tài khoản đã liên kết email này");

        new AlertDialog.Builder(this)
                .setTitle("Email đã được sử dụng")
                .setMessage(message.toString())
                .setPositiveButton("Thử email khác", (dialog, which) -> {
                    // User can try linking with different Google account
                    linkGoogleAccount();
                })
                .setNegativeButton("Đóng", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLinkGoogleButton();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("About ForLove")
                .setMessage("ForLove - Ứng dụng dành cho các cặp đôi yêu thương\n\nPhiên bản: 1.0.0\nPhát triển với ❤️")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất không?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> performLogout())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void performLogout() {
        // Sign out from Firebase Authentication
        mAuth.signOut();

        // Clear cached avatar
        try {
            java.io.File f = AvatarCache.getCachedFile(this);
            if (f.exists()) f.delete();
        } catch (Exception ignored) {}

        // Navigate to welcome screen and clear all previous activities
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();

        Toast.makeText(this, "Đã đăng xuất thành công", Toast.LENGTH_SHORT).show();
    }
}
