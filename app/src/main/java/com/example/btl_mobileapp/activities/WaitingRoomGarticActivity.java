package com.example.btl_mobileapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.btl_mobileapp.R;
import com.example.btl_mobileapp.utils.LoginPreferences;
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

public class WaitingRoomGarticActivity extends AppCompatActivity {

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
        setContentView(R.layout.activity_waiting_room_gartic);

        tvRoomId = findViewById(R.id.tvRoomId);
        tvStatus = findViewById(R.id.tvStatus);
        playerList = findViewById(R.id.playerList);
        btnStartGame = findViewById(R.id.btnStartGame);

        userId = LoginPreferences.getUserId(this);
        coupleId = LoginPreferences.getCoupleId(this);

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "❌ Lỗi: chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (coupleId == null || coupleId.isEmpty()) {
            Toast.makeText(this, "❌ Lỗi: không tìm thấy Couple ID!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnStartGame.setVisibility(Button.GONE);
        roomRef = FirebaseDatabase.getInstance().getReference("rooms").child(coupleId);

        joinOrCreateRoomTransaction();

        btnStartGame.setOnClickListener(v -> {
            if (isHost && currentPlayerCount == 2) {
                clearOldGameDataAndStart();
            } else {
                Toast.makeText(this, "⚠️ Chỉ chủ phòng khi đủ 2 người mới có thể bắt đầu!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearOldGameDataAndStart() {
        Map<String, Object> resetData = new HashMap<>();
        resetData.put("status", "playing");
        resetData.put("turn", "player1");
        resetData.put("points/player1", 0);
        resetData.put("points/player2", 0);
        resetData.put("drawingData", "");
        resetData.put("word", "");
        resetData.put("winner", "");
        resetData.put("gameStartTime", System.currentTimeMillis());

        roomRef.child("draws").removeValue()
                .addOnCompleteListener(task -> roomRef.updateChildren(resetData))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "🔥 Lỗi khi reset dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void joinOrCreateRoomTransaction() {
        roomRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                MutableData players = currentData.child("players");
                String status = currentData.child("status").getValue(String.class);

                if (currentData.getValue() == null) {
                    Map<String, Object> playerMap = new HashMap<>();
                    playerMap.put(userId, true);
                    currentData.child("players").setValue(playerMap);
                    currentData.child("hostId").setValue(userId);
                    currentData.child("status").setValue("waiting");
                    currentData.child("coupleId").setValue(coupleId);
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
                if (error != null) {
                    Toast.makeText(WaitingRoomGarticActivity.this, "🔥 Firebase lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                if (!committed) {
                    Toast.makeText(WaitingRoomGarticActivity.this, "⚠️ Phòng đang chơi hoặc đã đủ người!", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                // Thành công → set host, lắng nghe và onDisconnect
                tvRoomId.setText("🔗 Mã phòng: " + coupleId);
                String hostId = currentData.child("hostId").getValue(String.class);
                isHost = userId.equals(hostId);

                // Khi client mất kết nối (app crash/tắt đột ngột), reset phòng luôn
                roomRef.onDisconnect().setValue(null);

                listenForPlayers();
                listenForHost();
                listenForStatus();
            }
        });
    }

    private void listenForPlayers() {
        if (playersListener != null)
            roomRef.child("players").removeEventListener(playersListener);

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
                    TextView tv = new TextView(WaitingRoomGarticActivity.this);
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
        if (hostListener != null)
            roomRef.child("hostId").removeEventListener(hostListener);

        hostListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String hostId = snapshot.getValue(String.class);
                isHost = userId.equals(hostId);
                updateStartButtonAndStatus();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        roomRef.child("hostId").addValueEventListener(hostListener);
    }

    private void listenForStatus() {
        if (statusListener != null)
            roomRef.child("status").removeEventListener(statusListener);

        statusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String status = snapshot.getValue(String.class);
                if ("playing".equals(status)) {
                    goToGame();
                }
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

    private void goToGame() {
        leavingForGame = true;
        removeAllListeners();
        Intent intent = new Intent(this, GarticActivity.class);
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

        // Khi 1 người rời → reset toàn bộ phòng
        roomRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                currentData.child("players").setValue(null);
                currentData.child("draws").setValue(null);
                currentData.child("points").setValue(null);
                currentData.child("status").setValue("waiting");
                currentData.child("hostId").setValue(null);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {}
        });
    }
}
