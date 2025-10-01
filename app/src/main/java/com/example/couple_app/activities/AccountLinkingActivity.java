package com.example.couple_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.couple_app.R;
import com.example.couple_app.managers.AuthManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.List;

public class AccountLinkingActivity extends AppCompatActivity {
    private static final String TAG = "AccountLinking";
    private static final int RC_GOOGLE_SIGN_IN = 9001;

    private AuthManager authManager;
    private GoogleSignInClient googleSignInClient;
    private TextView tvCurrentProviders;
    private Button btnLinkGoogle, btnLinkPhone, btnUnlinkGoogle, btnUnlinkPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_linking);

        authManager = AuthManager.getInstance();
        initializeViews();
        setupGoogleSignIn();
        updateUI();
        setupClickListeners();
    }

    private void initializeViews() {
        tvCurrentProviders = findViewById(R.id.tvCurrentProviders);
        btnLinkGoogle = findViewById(R.id.btnLinkGoogle);
        btnLinkPhone = findViewById(R.id.btnLinkPhone);
        btnUnlinkGoogle = findViewById(R.id.btnUnlinkGoogle);
        btnUnlinkPhone = findViewById(R.id.btnUnlinkPhone);
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupClickListeners() {
        btnLinkGoogle.setOnClickListener(v -> linkGoogleAccount());
        btnLinkPhone.setOnClickListener(v -> linkPhoneAccount());
        btnUnlinkGoogle.setOnClickListener(v -> unlinkGoogleAccount());
        btnUnlinkPhone.setOnClickListener(v -> unlinkPhoneAccount());
    }

    private void updateUI() {
        List<String> providers = authManager.getLinkedProviders();
        StringBuilder sb = new StringBuilder("Phương thức đăng nhập hiện tại:\n");

        if (providers.isEmpty()) {
            sb.append("Chưa có phương thức nào được liên kết");
        } else {
            for (String provider : providers) {
                switch (provider) {
                    case "google.com":
                        sb.append("✓ Google\n");
                        break;
                    case "phone":
                        sb.append("✓ Số điện thoại\n");
                        break;
                    case "password":
                        sb.append("✓ Email/Mật khẩu\n");
                        break;
                    default:
                        sb.append("✓ ").append(provider).append("\n");
                }
            }
        }

        tvCurrentProviders.setText(sb.toString());

        // Enable/disable buttons based on current providers
        boolean hasGoogle = providers.contains("google.com");
        boolean hasPhone = providers.contains("phone");

        btnLinkGoogle.setEnabled(!hasGoogle);
        btnUnlinkGoogle.setEnabled(hasGoogle && providers.size() > 1);
        btnLinkPhone.setEnabled(!hasPhone);
        btnUnlinkPhone.setEnabled(hasPhone && providers.size() > 1);
    }

    private void linkGoogleAccount() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
    }

    private void linkPhoneAccount() {
        // Show dialog to enter phone number
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Liên kết số điện thoại");
        builder.setMessage("Tính năng này cần được implement với PhoneAuthProvider.\n" +
                "Bạn cần tạo UI để nhập số điện thoại và xác thực OTP.");
        builder.setPositiveButton("OK", null);
        builder.show();

        // TODO: Implement phone verification UI
        // Sau khi có PhoneAuthCredential từ verification process:
        // PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        // authManager.linkPhoneAccount(credential, callback);
    }

    private void unlinkGoogleAccount() {
        new AlertDialog.Builder(this)
                .setTitle("Hủy liên kết Google")
                .setMessage("Bạn có chắc chắn muốn hủy liên kết tài khoản Google?")
                .setPositiveButton("Hủy liên kết", (dialog, which) -> {
                    authManager.unlinkGoogleAccount(new AuthManager.AuthActionCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(AccountLinkingActivity.this,
                                    "Đã hủy liên kết tài khoản Google", Toast.LENGTH_SHORT).show();
                            updateUI();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(AccountLinkingActivity.this,
                                    "Lỗi: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void unlinkPhoneAccount() {
        new AlertDialog.Builder(this)
                .setTitle("Hủy liên kết số điện thoại")
                .setMessage("Bạn có chắc chắn muốn hủy liên kết số điện thoại?")
                .setPositiveButton("Hủy liên kết", (dialog, which) -> {
                    authManager.unlinkPhoneAccount(new AuthManager.AuthActionCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(AccountLinkingActivity.this,
                                    "Đã hủy liên kết số điện thoại", Toast.LENGTH_SHORT).show();
                            updateUI();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(AccountLinkingActivity.this,
                                    "Lỗi: " + error, Toast.LENGTH_LONG).show();
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
                        Toast.makeText(AccountLinkingActivity.this,
                                "Đã liên kết tài khoản Google thành công!", Toast.LENGTH_SHORT).show();
                        updateUI();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(AccountLinkingActivity.this,
                                "Lỗi liên kết Google: " + error, Toast.LENGTH_LONG).show();
                    }
                });

            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }
}
