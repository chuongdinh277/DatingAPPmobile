package com.example.couple_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.couple_app.R;
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
    @SuppressWarnings("deprecation")
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Check if user is already logged in before showing welcome screen
        checkLoginState();

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        @SuppressWarnings("deprecation")
        GoogleSignInClient client = GoogleSignIn.getClient(this, gso);
        mGoogleSignInClient = client;

        // Initialize Google Sign-In launcher
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        @SuppressWarnings("deprecation")
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
    }

    private void checkLoginState() {
        // Check SharedPreferences first
        if (LoginPreferences.isLoggedIn(this)) {
            // Also check Firebase Auth state
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                // User is still authenticated, go to home
                navigateToHome(currentUser);
            } else {
                // Firebase session expired, clear saved login state
                LoginPreferences.clearLoginState(this);
            }
        }
    }

    private void navigateToHome(FirebaseUser user) {
        // First check if user needs pairing
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
                Intent intent = new Intent(WelcomeActivity.this, HomeMainActivity.class);
                if (user != null) {
                    intent.putExtra("user_name", user.getDisplayName() != null ? user.getDisplayName() : LoginPreferences.getUserName(WelcomeActivity.this));
                    intent.putExtra("user_email", user.getEmail());
                    intent.putExtra("user_id", user.getUid());
                    intent.putExtra("coupleId", couple.getCoupleId());
                }
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String error) {
                // User is not paired yet, go to PairingActivity
                Intent intent = new Intent(WelcomeActivity.this, PairingActivity.class);
                startActivity(intent);
                finish();
            }
        });
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

                            Toast.makeText(this, "Google sign in successful!", Toast.LENGTH_SHORT).show();
                            navigateToHome(user);
                        }
                    } else {
                        // Sign in failed
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check login state every time the activity starts
        checkLoginState();
    }
}
