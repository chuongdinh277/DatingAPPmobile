package com.example.couple_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.couple_app.R;
import com.example.couple_app.utils.LoginPreferences;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import androidx.appcompat.app.AlertDialog;

public class GameListActivity extends BaseActivity {

    private LinearLayout btnGameGartic;
    private LinearLayout btnGameCaro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gamelist);

        btnGameGartic = findViewById(R.id.btnGame1);
        btnGameCaro = findViewById(R.id.btnGame2);

        // Xử lý click Gartic
        btnGameGartic.setOnClickListener(v -> openWaitingRoom("Gartic"));

        // Xử lý click Caro
        btnGameCaro.setOnClickListener(v -> openWaitingRoom("Caro"));
    }

    /**
     * Mở phòng chờ theo gameType
     */
    private void openWaitingRoom(String gameType) {
        String userId = LoginPreferences.getUserId(this);

        // Fallback to FirebaseAuth currentUser if LoginPreferences is empty
        if (userId == null || userId.isEmpty()) {
            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if (firebaseUser != null) {
                userId = firebaseUser.getUid();
                // Persist to LoginPreferences so future calls use it
                LoginPreferences.saveLoginState(this, true, firebaseUser.getEmail(), firebaseUser.getDisplayName(), userId);
            }
        }

        if (userId == null || userId.isEmpty()) {
            // Show dialog offering to go to login
            new AlertDialog.Builder(this)
                .setTitle("Chưa đăng nhập")
                .setMessage("Bạn cần đăng nhập để chơi trò chơi. Bạn có muốn chuyển tới màn hình đăng nhập không?")
                .setPositiveButton("Đăng nhập", (dialog, which) -> {
                    Intent intent = new Intent(GameListActivity.this, WelcomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                })
                .setNegativeButton("Huỷ", (dialog, which) -> dialog.dismiss())
                .show();
            return;
        }

        // Lấy coupleId từ Firebase
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("coupleId");

        userRef.get().addOnSuccessListener(snapshot -> {
            String coupleId = snapshot.exists() ? snapshot.getValue(String.class) : null;

            Intent intent;
            if ("Caro".equals(gameType)) {
                intent = new Intent(GameListActivity.this, WaitingRoomCaroActivity.class);
            } else { // Gartic
                intent = new Intent(GameListActivity.this, WaitingRoomGarticActivity.class);
            }

            intent.putExtra("coupleId", coupleId); // truyền coupleId
            startActivity(intent);

        }).addOnFailureListener(e -> {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi khi lấy coupleID!", Toast.LENGTH_SHORT).show();
        });
    }
}
