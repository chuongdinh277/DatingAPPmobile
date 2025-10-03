package com.example.btl_mobileapp.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;
import com.example.btl_mobileapp.R;
import com.example.btl_mobileapp.managers.AuthManager;
import com.example.btl_mobileapp.managers.DatabaseManager;
import com.example.btl_mobileapp.models.Couple;
import com.example.btl_mobileapp.models.User;

public class PairingActivity extends BaseActivity {
    private static final String TAG = "PairingActivity";

    private TextView tvWelcome, tvYourPin, tvPinDisplay, tvInstructions;
    private EditText etPartnerPin;
    private TextInputLayout tilPartnerPin;
    private Button btnPair, btnLogout;
    private ProgressBar progressBar;

    private AuthManager authManager;
    private DatabaseManager databaseManager;
    private String currentUserId;
    private String userPin;

    // Ẩn thanh menu trên màn hình ghép cặp


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pairing);

        // Không hiển thị menu ở màn hình ghép cặp để người dùng tập trung vào việc ghép đôi.
        // setActiveButton("message");

        initViews();
        initManagers();
        setupClickListeners();

        // Handle system back to logout instead of navigating back to login
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                logout();
            }
        });

        // Check if user is signed in
        FirebaseUser currentUser = authManager.getCurrentUser();
        if (currentUser == null) {
            navigateToLogin();
            return;
        }

        currentUserId = currentUser.getUid();
        tvWelcome.setText("Xin chào, " + (currentUser.getDisplayName() != null ? currentUser.getDisplayName() : currentUser.getEmail()));

        // Check if user is already paired
        checkExistingPairing();
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tv_welcome);
        tvYourPin = findViewById(R.id.tv_your_pin);
        tvPinDisplay = findViewById(R.id.tv_pin_display);
        tvInstructions = findViewById(R.id.tv_instructions);
        etPartnerPin = findViewById(R.id.et_partner_pin);
        tilPartnerPin = findViewById(R.id.til_partner_pin);
        btnPair = findViewById(R.id.btn_pair);
        btnLogout = findViewById(R.id.btn_logout);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void initManagers() {
        authManager = AuthManager.getInstance();
        databaseManager = DatabaseManager.getInstance();
    }

    private void setupClickListeners() {
        btnPair.setOnClickListener(v -> pairWithPartner());
        btnLogout.setOnClickListener(v -> logout());
    }

    private void checkExistingPairing() {
        showLoading(true);

        databaseManager.getCoupleByUserId(currentUserId, new DatabaseManager.DatabaseCallback<Couple>() {
            @Override
            public void onSuccess(Couple couple) {
                showLoading(false);
                String coupleId = couple.getCoupleId();
                String partnerId = couple.getUser1Id().equals(currentUserId) ? couple.getUser2Id() : couple.getUser1Id();
                // Fetch partner name
                databaseManager.getUser(partnerId, new DatabaseManager.DatabaseCallback<User>() {
                    @Override
                    public void onSuccess(User partner) {
                        navigateToChat(coupleId, partner != null && partner.getName() != null ? partner.getName() : "Đối phương");
                    }

                    @Override
                    public void onError(String error) {
                        navigateToChat(coupleId, "Đối phương");
                    }
                });
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                Log.d(TAG, "User not paired yet: " + error);
                // User is not paired, generate PIN
                generateUserPin();
            }
        });
    }

    private void generateUserPin() {
        showLoading(true);

        databaseManager.generateAndSavePinForUser(currentUserId, new DatabaseManager.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String pin) {
                showLoading(false);
                userPin = pin;
                displayPin(pin);
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                Log.e(TAG, "Failed to generate PIN: " + error);
                Toast.makeText(PairingActivity.this, "Lỗi tạo mã PIN: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void displayPin(String pin) {
        tvPinDisplay.setText(pin);
        tvPinDisplay.setVisibility(View.VISIBLE);
        tvYourPin.setVisibility(View.VISIBLE);
        tvInstructions.setVisibility(View.VISIBLE);
        tilPartnerPin.setVisibility(View.VISIBLE);
        btnPair.setVisibility(View.VISIBLE);
    }

    private void pairWithPartner() {
        String partnerPin = etPartnerPin.getText().toString().trim();

        if (partnerPin.isEmpty()) {
            etPartnerPin.setError("Vui lòng nhập mã PIN của đối phương");
            return;
        }

        if (partnerPin.length() != 6) {
            etPartnerPin.setError("Mã PIN phải có 6 chữ số");
            return;
        }

        if (userPin != null && partnerPin.equals(userPin)) {
            etPartnerPin.setError("Bạn không thể ghép cặp với chính mình");
            return;
        }

        showLoading(true);
        btnPair.setEnabled(false);

        databaseManager.connectCoupleWithPin(currentUserId, partnerPin, new DatabaseManager.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String coupleId) {
                showLoading(false);
                Toast.makeText(PairingActivity.this, "Ghép cặp thành công!", Toast.LENGTH_LONG).show();
                // Fetch partner name and navigate
                databaseManager.getCoupleByUserId(currentUserId, new DatabaseManager.DatabaseCallback<Couple>() {
                    @Override
                    public void onSuccess(Couple couple) {
                        String partnerId = couple.getUser1Id().equals(currentUserId) ? couple.getUser2Id() : couple.getUser1Id();
                        databaseManager.getUser(partnerId, new DatabaseManager.DatabaseCallback<User>() {
                            @Override
                            public void onSuccess(User partner) {
                                navigateToChat(coupleId, partner != null && partner.getName() != null ? partner.getName() : "Đối phương");
                            }

                            @Override
                            public void onError(String error) {
                                navigateToChat(coupleId, "Đối phương");
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        navigateToChat(coupleId, "Đối phương");
                    }
                });
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                btnPair.setEnabled(true);
                Toast.makeText(PairingActivity.this, "Lỗi ghép cặp: " + error, Toast.LENGTH_LONG).show();
                etPartnerPin.setError("Vui lòng kiểm tra lại mã PIN");
            }
        });
    }

    private void navigateToChat(String coupleId, String partnerName) {
        // Lưu trạng thái đăng nhập sau khi pairing thành công
        saveLoginStateAfterPairing(coupleId, partnerName);

        // Chuyển trực tiếp đến MessengerActivity với menu navigation
        Intent intent = new Intent(this, HomeMainActivity.class);
        intent.putExtra("coupleId", coupleId);
        intent.putExtra("partnerName", partnerName);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        overridePendingTransition(0, 0);

        // Hiển thị thông báo đăng nhập thành công
        Toast.makeText(this, "Đã đăng nhập thành công! Chào mừng bạn đến với " + partnerName, Toast.LENGTH_LONG).show();

        finish();
    }

    private void saveLoginStateAfterPairing(String coupleId, String partnerName) {
        FirebaseUser currentUser = authManager.getCurrentUser();
        if (currentUser != null) {
            // Lưu thông tin đăng nhập với thông tin couple
            SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            // Lưu trạng thái đăng nhập cơ bản
            editor.putBoolean("isLoggedIn", true);
            editor.putString("userId", currentUser.getUid());
            editor.putString("userEmail", currentUser.getEmail());
            editor.putString("userName", currentUser.getDisplayName());

            // Lưu thông tin couple để tự động vào chat
            editor.putBoolean("isPaired", true);
            editor.putString("coupleId", coupleId);
            editor.putString("partnerName", partnerName);

            editor.apply();

            Log.d(TAG, "Login state saved after successful pairing");
        }
    }

    private void logout() {
        // Clear login state from SharedPreferences
        com.example.btl_mobileapp.utils.LoginPreferences.clearLoginState(this);

        // Sign out from Firebase
        authManager.signOut();

        // Navigate to login screen
        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnPair.setEnabled(!show);
    }
}