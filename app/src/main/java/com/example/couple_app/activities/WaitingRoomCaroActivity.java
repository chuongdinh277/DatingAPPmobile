package com.example.couple_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.couple_app.R;
import com.example.couple_app.utils.LoginPreferences;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WaitingRoomCaroActivity extends AppCompatActivity {

    private TextView tvRoomId, tvStatus;
    private LinearLayout playerList;
    private Button btnStartGame;

    private String userId;
    private String coupleId;
    private DatabaseReference roomRef;

    private ValueEventListener playersListener;
    private ValueEventListener hostListener;
    private ValueEventListener statusListener;

    private boolean isHost = false;
    private long currentPlayerCount = 0;
    private boolean leavingForGame = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_room_caro);

        tvRoomId = findViewById(R.id.tvRoomId);
        tvStatus = findViewById(R.id.tvStatus);
        playerList = findViewById(R.id.playerList);
        btnStartGame = findViewById(R.id.btnStartGame);

        userId = LoginPreferences.getUserId(this);
        coupleId = getIntent().getStringExtra("coupleId"); // Nhận từ Intent

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "❌ Lỗi: chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnStartGame.setVisibility(Button.GONE);

        if (coupleId == null || coupleId.isEmpty()) {
            // Nếu chưa có coupleId, tìm phòng đang chờ trong Firebase
            findWaitingRoom();
        } else {
            // Nếu đã có coupleId → join thẳng
            joinRoom(coupleId);
        }
    }

    // Tìm phòng đang chờ
    private void findWaitingRoom() {
        DatabaseReference roomsRef = FirebaseDatabase.getInstance().getReference("caroRooms");
        roomsRef.orderByChild("status").equalTo("waiting")
                .limitToFirst(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // Có phòng đang chờ → join
                            for (DataSnapshot roomSnap : snapshot.getChildren()) {
                                coupleId = roomSnap.getKey();
                                joinRoom(coupleId);
                                return;
                            }
                        } else {
                            // Không có phòng → tạo phòng mới
                            coupleId = generateCoupleId();
                            joinRoom(coupleId);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(WaitingRoomCaroActivity.this, "🔥 Firebase lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private String generateCoupleId() {
        return "CARO_" + System.currentTimeMillis();
    }

    private void joinRoom(String roomId) {
        tvRoomId.setText("🔗 Mã phòng: " + roomId);
        roomRef = FirebaseDatabase.getInstance().getReference("caroRooms").child(roomId);

        roomRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                MutableData players = currentData.child("players");
                String status = currentData.child("status").getValue(String.class);

                if (currentData.getValue() == null) {
                    // Tạo phòng mới
                    Map<String, Object> playerMap = new HashMap<>();
                    playerMap.put(userId, true);
                    currentData.child("players").setValue(playerMap);
                    currentData.child("hostId").setValue(userId);
                    currentData.child("status").setValue("waiting");
                    currentData.child("coupleId").setValue(roomId);
                    currentData.child("board").setValue(null);
                    currentData.child("turn").setValue("X");
                    currentData.child("winner").setValue("");
                    return Transaction.success(currentData);
                }

                if ("playing".equals(status)) return Transaction.abort();

                int count = 0;
                for (MutableData child : players.getChildren()) {
                    if (child.getValue() != null) count++;
                }
                if (count >= 2) return Transaction.abort();

                players.child(userId).setValue(true);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                if (error != null || !committed) {
                    Toast.makeText(WaitingRoomCaroActivity.this, "⚠️ Phòng đang chơi hoặc đã đủ người!", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                isHost = userId.equals(currentData.child("hostId").getValue(String.class));
                roomRef.onDisconnect().removeValue();

                listenForPlayers();
                listenForHost();
                listenForStatus();
            }
        });

        btnStartGame.setOnClickListener(v -> {
            if (isHost && currentPlayerCount == 2) {
                startGame();
            } else {
                Toast.makeText(this, "⚠️ Chỉ chủ phòng khi đủ 2 người mới có thể bắt đầu!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void listenForPlayers() {
        if (playersListener != null) roomRef.child("players").removeEventListener(playersListener);

        playersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentPlayerCount = snapshot.getChildrenCount();
                List<String> players = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    players.add(child.getKey());
                }

                playerList.removeAllViews();
                for (String player : players) {
                    TextView tv = new TextView(WaitingRoomCaroActivity.this);
                    tv.setText("• " + player);
                    tv.setTextSize(16);
                    tv.setPadding(8, 8, 8, 8);
                    tv.setGravity(Gravity.CENTER_HORIZONTAL);
                    playerList.addView(tv);
                }
                updateStartButtonAndStatus();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        roomRef.child("players").addValueEventListener(playersListener);
    }

    private void listenForHost() {
        if (hostListener != null) roomRef.child("hostId").removeEventListener(hostListener);

        hostListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isHost = userId.equals(snapshot.getValue(String.class));
                updateStartButtonAndStatus();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        roomRef.child("hostId").addValueEventListener(hostListener);
    }

    private void listenForStatus() {
        if (statusListener != null) roomRef.child("status").removeEventListener(statusListener);

        statusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String status = snapshot.getValue(String.class);
                if ("playing".equals(status)) goToGame();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        roomRef.child("status").addValueEventListener(statusListener);
    }

    private void updateStartButtonAndStatus() {
        runOnUiThread(() -> {
            if (currentPlayerCount == 2) {
                if (isHost) {
                    btnStartGame.setVisibility(Button.VISIBLE);
                    tvStatus.setText("✅ Đã đủ người, bạn có thể bắt đầu!");
                } else {
                    btnStartGame.setVisibility(Button.GONE);
                    tvStatus.setText("⌛ Đang chờ chủ phòng bắt đầu...");
                }
            } else {
                btnStartGame.setVisibility(Button.GONE);
                tvStatus.setText("👥 Người chơi: " + currentPlayerCount + "/2");
            }
        });
    }

    private void startGame() {
        roomRef.child("status").setValue("playing");
    }

    private void goToGame() {
        leavingForGame = true;
        removeAllListeners();
        Intent intent = new Intent(this, CaroActivity.class);
        intent.putExtra("roomId", coupleId);
        startActivity(intent);
        finish();
    }

    private void removeAllListeners() {
        if (playersListener != null) roomRef.child("players").removeEventListener(playersListener);
        if (hostListener != null) roomRef.child("hostId").removeEventListener(hostListener);
        if (statusListener != null) roomRef.child("status").removeEventListener(statusListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (leavingForGame || roomRef == null || userId == null) return;

        // Khi 1 người rời → reset phòng
        roomRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                currentData.child("players").setValue(null);
                currentData.child("board").setValue(null);
                currentData.child("status").setValue("waiting");
                currentData.child("hostId").setValue(null);
                currentData.child("winner").setValue("");
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {}
        });
    }
}
