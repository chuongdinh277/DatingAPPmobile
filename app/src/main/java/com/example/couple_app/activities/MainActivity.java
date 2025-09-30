package com.example.couple_app.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.example.couple_app.utils.LoginPreferences;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.couple_app.managers.DatabaseManager;
import com.example.couple_app.models.Couple;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private DatabaseManager databaseManager;
    private LoginPreferences loginPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize components
        mAuth = FirebaseAuth.getInstance();
        databaseManager = DatabaseManager.getInstance();
        loginPreferences = new LoginPreferences(this);

        // Check user status with saved login state
        checkUserStatus();
    }

    private void checkUserStatus() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Check both Firebase auth and saved login state
        if (currentUser != null && loginPreferences.isLoggedIn()) {
            // User is signed in and has saved login state, check if they are paired
            String userId = currentUser.getUid();
            databaseManager.getCoupleByUserId(userId, new DatabaseManager.DatabaseCallback<Couple>() {
                @Override
                public void onSuccess(Couple couple) {
                    // User is paired, go to home screen
                    Intent intent = new Intent(MainActivity.this, HomeMainActivity.class);
                    intent.putExtra("coupleId", couple.getCoupleId());
                    intent.putExtra("user_name", loginPreferences.getUserName());
                    intent.putExtra("user_email", loginPreferences.getUserEmail());
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onError(String error) {
                    // User is not paired, go to pairing screen
                    Intent intent = new Intent(MainActivity.this, PairingActivity.class);
                    intent.putExtra("user_name", loginPreferences.getUserName());
                    intent.putExtra("user_email", loginPreferences.getUserEmail());
                    startActivity(intent);
                    finish();
                }
            });
        } else {
            // User is not signed in or no saved login state, clear any stale data and go to welcome screen
            if (currentUser == null) {
                loginPreferences.clearLoginState();
            }
            startActivity(new Intent(MainActivity.this, WelcomeActivity.class));
            finish();
        }
    }
}