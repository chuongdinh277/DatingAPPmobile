package com.example.couple_app.ui.activities;

import android.app.DatePickerDialog;
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
import com.example.couple_app.R;
import com.example.couple_app.managers.AuthManager;
import com.example.couple_app.data.local.DatabaseManager;
import com.example.couple_app.data.model.Couple;
import com.example.couple_app.data.model.User;
import com.google.firebase.Timestamp;

import java.util.Calendar;
import java.util.GregorianCalendar;

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
    private com.google.firebase.firestore.ListenerRegistration pairingListener;

    // Ẩn thanh menu trên màn hình ghép cặp
    @Override
    protected boolean shouldShowBottomBar() {
        return false;
    }

    @Override
    protected boolean shouldShowHeader() {
        return false;
    }


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

                if (couple == null) {
                    Log.e(TAG, "Couple object is null in checkExistingPairing");
                    generateUserPin();
                    return;
                }

                String coupleId = couple.getCoupleId();
                String user1Id = couple.getUser1Id();
                String user2Id = couple.getUser2Id();

                if (user1Id == null || user2Id == null || coupleId == null) {
                    Log.e(TAG, "Couple data incomplete");
                    generateUserPin();
                    return;
                }

                String partnerId = user1Id.equals(currentUserId) ? user2Id : user1Id;

                // Fetch partner name
                databaseManager.getUser(partnerId, new DatabaseManager.DatabaseCallback<User>() {
                    @Override
                    public void onSuccess(User partner) {
                        String partnerName = (partner != null && partner.getName() != null)
                            ? partner.getName()
                            : "Đối phương";
                        navigateToChat(coupleId, partnerName);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error fetching partner in checkExistingPairing: " + error);
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
                // Start listening for pairing from partner
                startListeningForPairing();
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

    private void startListeningForPairing() {
        // Listen for changes in the current user's document
        // When partner enters our PIN, our partnerId field will be updated
        pairingListener = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users")
            .document(currentUserId)
            .addSnapshotListener((documentSnapshot, error) -> {
                if (error != null) {
                    Log.e(TAG, "Error listening for pairing: " + error.getMessage());
                    return;
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {
                    String partnerId = documentSnapshot.getString("partnerId");

                    // If partnerId is set, we've been paired by our partner
                    if (partnerId != null && !partnerId.isEmpty()) {
                        Log.d(TAG, "Detected pairing by partner! PartnerId: " + partnerId);

                        // Stop listening
                        if (pairingListener != null) {
                            pairingListener.remove();
                            pairingListener = null;
                        }

                        // Get coupleId and navigate
                        handleAutomaticPairing();
                    }
                }
            });
    }

    private void handleAutomaticPairing() {
        showLoading(true);
        Toast.makeText(this, "Đối phương đã ghép đôi với bạn!", Toast.LENGTH_SHORT).show();

        // Fetch couple info and navigate
        databaseManager.getCoupleByUserId(currentUserId, new DatabaseManager.DatabaseCallback<Couple>() {
            @Override
            public void onSuccess(Couple couple) {
                if (couple == null) {
                    showLoading(false);
                    Log.e(TAG, "Couple object is null in handleAutomaticPairing");
                    return;
                }

                String coupleId = couple.getCoupleId();
                String user1Id = couple.getUser1Id();
                String user2Id = couple.getUser2Id();

                if (user1Id == null || user2Id == null || coupleId == null) {
                    showLoading(false);
                    Log.e(TAG, "Couple data incomplete in handleAutomaticPairing");
                    return;
                }

                String partnerId = user1Id.equals(currentUserId) ? user2Id : user1Id;

                // Fetch partner name
                databaseManager.getUser(partnerId, new DatabaseManager.DatabaseCallback<User>() {
                    @Override
                    public void onSuccess(User partner) {
                        showLoading(false);
                        String partnerName = (partner != null && partner.getName() != null)
                            ? partner.getName()
                            : "Đối phương";
                        navigateToChat(coupleId, partnerName);
                    }

                    @Override
                    public void onError(String error) {
                        showLoading(false);
                        Log.e(TAG, "Error fetching partner in handleAutomaticPairing: " + error);
                        navigateToChat(coupleId, "Đối phương");
                    }
                });
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                Log.e(TAG, "Error fetching couple in handleAutomaticPairing: " + error);
            }
        });
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

        // Stop listening since we're actively pairing
        if (pairingListener != null) {
            pairingListener.remove();
            pairingListener = null;
        }

        showLoading(true);
        btnPair.setEnabled(false);

        databaseManager.connectCoupleWithPin(currentUserId, partnerPin, new DatabaseManager.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String coupleId) {
                // Add a small delay to ensure Firestore has saved the data
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    // Instead of navigating immediately, fetch partner info then prompt for start date
                    fetchPartnerInfoAndPromptDate(coupleId);
                }, 500); // 500ms delay
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

    private void fetchPartnerInfoAndPromptDate(String coupleId) {
        // Fetch the fresh couple doc to determine partner id and name
        databaseManager.getCoupleByUserId(currentUserId, new DatabaseManager.DatabaseCallback<Couple>() {
            @Override
            public void onSuccess(Couple couple) {
                if (couple == null) {
                    Log.e(TAG, "Couple object is null when fetching partner info after pairing");
                    showLoading(false);
                    // fallback navigate
                    navigateToChat(coupleId, "Đối phương");
                    return;
                }

                String user1Id = couple.getUser1Id();
                String user2Id = couple.getUser2Id();
                if (user1Id == null || user2Id == null) {
                    Log.e(TAG, "User IDs are null in couple object (after pairing)");
                    showLoading(false);
                    navigateToChat(coupleId, "Đối phương");
                    return;
                }

                String partnerId = user1Id.equals(currentUserId) ? user2Id : user1Id;

                databaseManager.getUser(partnerId, new DatabaseManager.DatabaseCallback<User>() {
                    @Override
                    public void onSuccess(User partner) {
                        showLoading(false);
                        String partnerName = (partner != null && partner.getName() != null) ? partner.getName() : "Đối phương";
                        // Prompt the initiating user to pick a start date before navigating
                        showStartDatePickerAndSave(coupleId, partnerName);
                    }

                    @Override
                    public void onError(String error) {
                        showLoading(false);
                        Log.e(TAG, "Error fetching partner after pairing: " + error);
                        showStartDatePickerAndSave(coupleId, "Đối phương");
                    }
                });
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                Log.e(TAG, "Error fetching couple after pairing: " + error);
                // fallback navigate
                navigateToChat(coupleId, "Đối phương");
            }
        });
    }

    private void showStartDatePickerAndSave(String coupleId, String partnerName) {
        // Show a DatePickerDialog; default to today
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dpd = new DatePickerDialog(this, (view, y, m, d) -> {
            // User selected date y, m (0-based), d
            Calendar selected = new GregorianCalendar(y, m, d);
            java.util.Date date = selected.getTime();
            Timestamp ts = new Timestamp(date);

            showLoading(true);
            databaseManager.setStartDateForCoupleAndUsers(coupleId, ts, new DatabaseManager.DatabaseActionCallback() {
                @Override
                public void onSuccess() {
                    showLoading(false);
                    // After saving, navigate to chat/main
                    navigateToChat(coupleId, partnerName);
                }

                @Override
                public void onError(String error) {
                    showLoading(false);
                    Log.e(TAG, "Failed to save start date: " + error);
                    Toast.makeText(PairingActivity.this, "Lỗi lưu ngày bắt đầu: " + error, Toast.LENGTH_LONG).show();
                    // Still navigate to chat even if saving failed, to avoid blocking user
                    navigateToChat(coupleId, partnerName);
                }
            });

        }, year, month, day);

        dpd.setTitle("Chọn ngày bắt đầu yêu");
        dpd.setCancelable(false);
        // Provide a Cancel handling: if user cancels, we'll navigate anyway
        dpd.setButton(DatePickerDialog.BUTTON_NEGATIVE, "Bỏ qua", (dialog, which) -> {
            if (which == DatePickerDialog.BUTTON_NEGATIVE) {
                dialog.dismiss();
                // Navigate without saving custom date (server's start date remains)
                navigateToChat(coupleId, partnerName);
            }
        });

        dpd.show();
    }


    private void fetchPartnerInfoAndNavigate(String coupleId) {
        databaseManager.getCoupleByUserId(currentUserId, new DatabaseManager.DatabaseCallback<Couple>() {
            @Override
            public void onSuccess(Couple couple) {
                if (couple == null) {
                    Log.e(TAG, "Couple object is null");
                    navigateToChat(coupleId, "Đối phương");
                    return;
                }

                String user1Id = couple.getUser1Id();
                String user2Id = couple.getUser2Id();

                if (user1Id == null || user2Id == null) {
                    Log.e(TAG, "User IDs are null in couple object");
                    navigateToChat(coupleId, "Đối phương");
                    return;
                }

                String partnerId = user1Id.equals(currentUserId) ? user2Id : user1Id;

                databaseManager.getUser(partnerId, new DatabaseManager.DatabaseCallback<User>() {
                    @Override
                    public void onSuccess(User partner) {
                        showLoading(false);
                        String partnerName = (partner != null && partner.getName() != null)
                            ? partner.getName()
                            : "Đối phương";
                        navigateToChat(coupleId, partnerName);
                    }

                    @Override
                    public void onError(String error) {
                        showLoading(false);
                        Log.e(TAG, "Error fetching partner: " + error);
                        navigateToChat(coupleId, "Đối phương");
                    }
                });
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                Log.e(TAG, "Error fetching couple: " + error);
                // Still navigate even if we can't fetch couple info
                navigateToChat(coupleId, "Đối phương");
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
        com.example.couple_app.utils.LoginPreferences.clearLoginState(this);

        // Clear background image data from AppSettings SharedPreferences
        android.content.SharedPreferences appSettingsPrefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        appSettingsPrefs.edit()
            .remove("saved_background_url")
            .remove("background_source_type")
            .commit();

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up listener to prevent memory leaks
        if (pairingListener != null) {
            pairingListener.remove();
            pairingListener = null;
        }
    }
}
