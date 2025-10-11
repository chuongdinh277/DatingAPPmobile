package com.example.btl_mobileapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.btl_mobileapp.R;
import com.example.btl_mobileapp.utils.LoginPreferences;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class GameListActivity extends BaseActivity {

    private LinearLayout btnGameGartic;
    private LinearLayout btnGameCaro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setBottomBarColor(R.color.game_bottom_bar_color);
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

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show();
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
