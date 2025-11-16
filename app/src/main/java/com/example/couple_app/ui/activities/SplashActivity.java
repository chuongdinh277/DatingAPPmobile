package com.example.couple_app.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

import com.example.couple_app.R;
import com.example.couple_app.data.local.DatabaseManager;
import com.example.couple_app.data.model.Couple;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/*
class kiểm tra trạng thái đăng nhập của người dùng khi khởi động ứng dụng
nếu đã đăng nhập, chuyển đến MainActivity
nếu chưa đăng nhập, chuyển đến LoginActivity
 */
public class SplashActivity extends AppCompatActivity {
    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DELAY = 500; // 2 seconds

    private FirebaseAuth mAuth;
    private DatabaseManager databaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize Firebase Auth and DatabaseManager
        mAuth = FirebaseAuth.getInstance();
        databaseManager = DatabaseManager.getInstance();

        // Check user status after a short delay
        new Handler(Looper.getMainLooper()).postDelayed(this::checkUserStatus, SPLASH_DELAY);
    }

    /**
     * Kiểm tra trạng thái người dùng:
     * - Nếu chưa đăng nhập -> WelcomeActivity
     * - Nếu đã đăng nhập nhưng chưa có couple -> PairingActivity
     * - Nếu đã đăng nhập và đã có couple -> HomeMainActivity
     */
    private void checkUserStatus() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // Người dùng chưa đăng nhập -> chuyển đến WelcomeActivity
            Log.d(TAG, "User not logged in, navigating to WelcomeActivity");
            MapToAuth();
        } else {
            // Người dùng đã đăng nhập -> kiểm tra xem đã có couple chưa
            Log.d(TAG, "User logged in, checking pairing status");
            String userId = currentUser.getUid();
            checkPairingStatus(userId);
        }
    }

    /**
     * Kiểm tra xem người dùng đã có couple chưa
     */
    private void checkPairingStatus(String userId) {
        databaseManager.getCoupleByUserId(userId, new DatabaseManager.DatabaseCallback<Couple>() {
            @Override
            public void onSuccess(Couple couple) {
                // Người dùng đã có couple -> chuyển đến HomeMainActivity
                Log.d(TAG, "User is paired, navigating to HomeMainActivity");
                String coupleId = couple.getCoupleId();
                String partnerId = couple.getUser1Id().equals(userId) ? couple.getUser2Id() : couple.getUser1Id();

                // Lưu thông tin couple vào SharedPreferences
                savePairingInfo(coupleId, partnerId);

                MapToMain(coupleId);
            }

            @Override
            public void onError(String error) {
                // Người dùng chưa có couple -> chuyển đến PairingActivity
                Log.d(TAG, "User not paired, navigating to PairingActivity");
                MapToPairing();
            }
        });
    }

    /**
     * Lưu thông tin ghép cặp vào SharedPreferences
     */
    private void savePairingInfo(String coupleId, String partnerId) {
        getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                .edit()
                .putBoolean("isPaired", true)
                .putString("coupleId", coupleId)
                .putString("partnerId", partnerId)
                .apply();
    }

    /**
     * Chuyển đến WelcomeActivity (màn hình đăng nhập)
     */
    private void MapToAuth() {
        Intent intent = new Intent(SplashActivity.this, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Chuyển đến PairingActivity (màn hình ghép cặp)
     */
    private void MapToPairing() {
        Intent intent = new Intent(SplashActivity.this, PairingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Chuyển đến HomeMainActivity nếu người dùng đã đăng nhập và đã ghép cặp
     */
    private void MapToMain(String coupleId) {
        Intent intent = new Intent(SplashActivity.this, HomeMainActivity.class);
        intent.putExtra("coupleId", coupleId);

        // Lấy thông tin user từ Firebase để truyền vào HomeMainActivity
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            intent.putExtra("user_name", currentUser.getDisplayName());
            intent.putExtra("user_email", currentUser.getEmail());
            intent.putExtra("user_id", currentUser.getUid());
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
