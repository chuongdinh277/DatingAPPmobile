package com.example.couple_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.couple_app.R;
import com.example.couple_app.managers.AuthManager;
import com.example.couple_app.managers.DatabaseManager;
import com.example.couple_app.utils.LoginPreferences;
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
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);

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
                            handleGoogleSignInResult(account);
                        } catch (ApiException e) {
                            Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        // Get view references
        LinearLayout btnLoginPhone = findViewById(R.id.btnLoginPhone);
        LinearLayout btnLoginMail = findViewById(R.id.btnLoginMail); // This will be used for email login
        TextView tvSignUp = findViewById(R.id.tv_signup);

        // Handle Phone Login click
        btnLoginPhone.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, LoginByPhoneActivity.class);
            startActivity(intent);
        });

        // Handle Google Login click (using the mail button)
        btnLoginMail.setOnClickListener(v -> signInWithGoogle());

        // Handle Sign Up click
        if (tvSignUp != null) {
            tvSignUp.setOnClickListener(v -> {
                Intent intent = new Intent(WelcomeActivity.this, SignUpActivity.class);
                startActivity(intent);
            });
        }
    }

    private void signInWithGoogle() {
        // Sign out from any previous Google account to force account selection
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            // Always show account picker by launching sign-in intent
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void handleGoogleSignInResult(GoogleSignInAccount account) {
        if (account == null) {
            Toast.makeText(this, "Google Sign In Failed: No account selected", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    // Firebase sign-in success
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        saveUserDataAndNavigate(user, account);
                    } else {
                        Toast.makeText(this, "Firebase authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Firebase sign-in failed
                    String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                    Toast.makeText(this, "Firebase Authentication Failed: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });
    }

    private void saveUserDataAndNavigate(FirebaseUser user, GoogleSignInAccount account) {
        // Save login state to SharedPreferences
        LoginPreferences loginPreferences = new LoginPreferences(this);
        loginPreferences.saveLoginState(
            user.getUid(),
            account.getDisplayName() != null ? account.getDisplayName() : "Google User",
            account.getEmail(),
            null, // phone is null for Google login
            "google"
        );

        // Save Google user to database
        DatabaseManager.getInstance().createUserDocument(
            user,
            account.getDisplayName() != null ? account.getDisplayName() : "Google User",
            new AuthManager.AuthCallback() {
                @Override
                public void onSuccess(FirebaseUser firebaseUser) {
                    // Navigate to pairing after successful database save
                    navigateToPairing(account);
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(WelcomeActivity.this, "Lỗi lưu thông tin: " + error, Toast.LENGTH_SHORT).show();
                    // Still navigate to pairing even if database save fails
                    navigateToPairing(account);
                }
            }
        );
    }

    private void navigateToPairing(GoogleSignInAccount account) {
        Intent intent = new Intent(WelcomeActivity.this, PairingActivity.class);
        intent.putExtra("user_name", account.getDisplayName());
        intent.putExtra("user_email", account.getEmail());
        startActivity(intent);
        finish();
    }
}
