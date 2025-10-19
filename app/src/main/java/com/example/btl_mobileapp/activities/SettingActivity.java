package com.example.btl_mobileapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;
import com.google.android.material.button.MaterialButton;
import com.example.btl_mobileapp.R;
import com.example.btl_mobileapp.managers.AuthManager;
import com.example.btl_mobileapp.managers.DatabaseManager;
import com.example.btl_mobileapp.utils.AvatarCache;
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

        // Initialize Firebase Auth and AuthManager
        mAuth = FirebaseAuth.getInstance();
        FirebaseAuthSettings firebaseAuthSettings = mAuth.getFirebaseAuthSettings();
        firebaseAuthSettings.setAppVerificationDisabledForTesting(true);
        authManager = AuthManager.getInstance();

        // Setup Google Sign In
        setupGoogleSignIn();

        // Ánh xạ các button
        MaterialButton btProfile = findViewById(R.id.bt_profile);
        MaterialButton btAbout = findViewById(R.id.bt_about);
        btLinkGoogle = findViewById(R.id.bt_link_google);
        MaterialButton btLogout = findViewById(R.id.bt_logout);

        // Update UI based on current linked providers
        updateLinkGoogleButton();

        // Listeners
        btProfile.setOnClickListener(v -> {
            startActivity(new Intent(SettingActivity.this, SettingProfileActivity.class));
        });
        btAbout.setOnClickListener(v -> showAboutDialog());
        btLinkGoogle.setOnClickListener(v -> handleGoogleLinkAction());
        btLogout.setOnClickListener(v -> showLogoutDialog());
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
        boolean hasGoogle = authManager.getLinkedProviders().contains("google.com");
        if (hasGoogle) {
            showUnlinkGoogleDialog();
        } else {
            linkGoogleAccount();
        }
    }

    private void linkGoogleAccount() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
    }

    private void showUnlinkGoogleDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Hủy liên kết Google")
                .setMessage("Bạn có chắc chắn muốn hủy liên kết tài khoản Google?\n\nLưu ý: Bạn phải có ít nhất một phương thức đăng nhập.")
                .setPositiveButton("Hủy liên kết", (dialog, which) -> {
                    authManager.unlinkGoogleAccount(new AuthManager.AuthActionCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(SettingActivity.this, "Đã hủy liên kết tài khoản Google thành công", Toast.LENGTH_SHORT).show();
                            updateLinkGoogleButton();
                        }
                        @Override
                        public void onError(String error) {
                            Toast.makeText(SettingActivity.this, "Lỗi hủy liên kết: " + error, Toast.LENGTH_LONG).show();
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
                if (account == null || account.getIdToken() == null) {
                    throw new ApiException(new Status(CommonStatusCodes.INTERNAL_ERROR, "Failed to get Google account details."));
                }
                AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                handleGoogleCredential(credential, account);

            } catch (ApiException e) {
                // Improved error handling to give more specific feedback
                String errorMessage = "Đăng nhập Google thất bại. Vui lòng thử lại.";
                switch (e.getStatusCode()) {
                    case 12501: // SIGN_IN_CANCELLED
                        errorMessage = "Bạn đã hủy liên kết với Google.";
                        break;
                    case 7: // NETWORK_ERROR
                        errorMessage = "Lỗi mạng, vui lòng kiểm tra kết nối internet.";
                        break;
                    default:
                        Log.w(TAG, "Google sign in failed with code: " + e.getStatusCode(), e);
                        errorMessage = "Lỗi không xác định từ Google: " + e.getStatusCode();
                        break;
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void handleGoogleCredential(AuthCredential credential, GoogleSignInAccount account) {
        authManager.linkGoogleAccount(credential, new AuthManager.AuthActionCallback() {
            @Override
            public void onSuccess() {
                checkEmailAndFinalizeLink(account);
            }
            @Override
            public void onError(String error) {
                Log.e(TAG, "Google linking error: " + error);
                // More specific error message for already linked account
                if (error.contains("credential-already-in-use")) {
                    Toast.makeText(SettingActivity.this, "Tài khoản Google này đã được liên kết với một người dùng khác.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(SettingActivity.this, "Lỗi liên kết Google: " + error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void checkEmailAndFinalizeLink(GoogleSignInAccount account) {
        FirebaseUser user = mAuth.getCurrentUser();
        String googleEmail = account.getEmail();

        if (user == null || googleEmail == null) {
            Toast.makeText(SettingActivity.this, "Đã liên kết Google thành công!", Toast.LENGTH_SHORT).show();
            updateLinkGoogleButton();
            if (user != null) showAccountLinkSuccessDialog(user);
            return;
        }

        DatabaseManager.getInstance().checkEmailExists(googleEmail, new DatabaseManager.DatabaseCallback<com.example.btl_mobileapp.models.User>() {
            @Override
            public void onSuccess(com.example.btl_mobileapp.models.User existingUser) {
                if (existingUser != null && !existingUser.getUserId().equals(user.getUid())) {
                    // Email belongs to another user in our database
                    showEmailAlreadyLinkedDialog(googleEmail, existingUser);
                    // Unlink the just-linked credential because it causes a conflict
                    authManager.unlinkGoogleAccount(new AuthManager.AuthActionCallback() {
                        @Override
                        public void onSuccess() { Log.d(TAG, "Unlinked due to email conflict."); updateLinkGoogleButton(); }
                        @Override
                        public void onError(String error) { Log.e(TAG, "Failed to auto-unlink: " + error); }
                    });
                } else {
                    // No conflict, proceed to update email in database
                    DatabaseManager.getInstance().updateUserEmail(user.getUid(), googleEmail, new AuthManager.AuthActionCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(SettingActivity.this, "Đã liên kết và cập nhật email Google!", Toast.LENGTH_SHORT).show();
                            updateLinkGoogleButton();
                            showAccountLinkSuccessDialog(user);
                        }
                        @Override
                        public void onError(String error) {
                            Toast.makeText(SettingActivity.this, "Liên kết Google thành công, nhưng lỗi cập nhật email.", Toast.LENGTH_LONG).show();
                            updateLinkGoogleButton();
                            showAccountLinkSuccessDialog(user);
                        }
                    });
                }
            }
            @Override
            public void onError(String error) {
                Toast.makeText(SettingActivity.this, "Lỗi kiểm tra email: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }


    private void showAccountLinkSuccessDialog(FirebaseUser user) {
        StringBuilder message = new StringBuilder("Tài khoản đã được liên kết thành công!\n\n");
        message.append("Thông tin tài khoản:\n");

        if (user.getDisplayName() != null) message.append("Tên: ").append(user.getDisplayName()).append("\n");
        if (user.getEmail() != null) message.append("Email: ").append(user.getEmail()).append("\n");
        if (user.getPhoneNumber() != null) message.append("SĐT: ").append(user.getPhoneNumber()).append("\n");

        message.append("\nPhương thức đăng nhập:\n");
        for (String provider : authManager.getLinkedProviders()) {
            switch (provider) {
                case "google.com": message.append("✓ Google\n"); break;
                case "phone": message.append("✓ Số điện thoại\n"); break;
                case "password": message.append("✓ Email/Mật khẩu\n"); break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Liên kết thành công!")
                .setMessage(message.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void showEmailAlreadyLinkedDialog(String email, com.example.btl_mobileapp.models.User existingUser) {
        StringBuilder message = new StringBuilder();
        message.append("❌ Không thể liên kết\n\n");
        message.append("Email: ").append(email).append("\n");
        message.append("đã được dùng cho tài khoản khác:\n\n");

        if (existingUser.getName() != null) message.append("👤 Tên: ").append(existingUser.getName()).append("\n");
        if (existingUser.getPhoneNumber() != null) message.append("📱 SĐT: ").append(existingUser.getPhoneNumber()).append("\n");

        message.append("\n💡 Vui lòng sử dụng một tài khoản Google khác.");

        new AlertDialog.Builder(this)
                .setTitle("Email đã được sử dụng")
                .setMessage(message.toString())
                .setPositiveButton("Thử lại", (dialog, which) -> linkGoogleAccount())
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
        mAuth.signOut();
        try {
            java.io.File f = AvatarCache.getCachedFile(this);
            if (f.exists()) f.delete();
        } catch (Exception ignored) {}

        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        Toast.makeText(this, "Đã đăng xuất thành công", Toast.LENGTH_SHORT).show();
    }
}

