package com.example.btl_mobileapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    private static final String TAG = "WaitingRoomGartic";
    private TextView tvRoomId, tvStatus;
    private LinearLayout playerList;
    private Button btnStartGame;

    private String userId;
    private String coupleId;
    private DatabaseReference roomRef;
    private ValueEventListener playersListener;
    private ValueEventListener statusListener;

    private boolean isHost = false;
    private long currentPlayerCount = 0;
    private boolean isLeavingForGame = false;
    private String currentHostId;

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
                startGame();
            } else {
                Toast.makeText(this, "⚠️ Cần đủ 2 người chơi để bắt đầu!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void joinOrCreateRoomTransaction() {
        roomRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                String hostId = currentData.child("hostId").getValue(String.class);
                String status = currentData.child("status").getValue(String.class);
                MutableData playersNode = currentData.child("players");

                // === LOGIC TẠO PHÒNG HOẶC SỬA PHÒNG LỖI ===
                if (currentData.getValue() == null || hostId == null) {
                    Map<String, Object> playerMap = new HashMap<>();
                    playerMap.put(userId, true);

                    currentData.child("players").setValue(playerMap);
                    currentData.child("hostId").setValue(userId); // Đặt người vào đầu tiên làm chủ phòng
                    currentData.child("status").setValue("waiting");
                    currentData.child("coupleId").setValue(coupleId);
                    return Transaction.success(currentData);
                }

                // === LOGIC VÀO PHÒNG ĐÃ TỒN TẠI ===
                if ("playing".equals(status)) return Transaction.abort();

                if (playersNode.hasChild(userId)) return Transaction.success(currentData);

                if (playersNode.getChildrenCount() >= 2) return Transaction.abort();

                // Thêm người chơi mới vào phòng
                playersNode.child(userId).setValue(true);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot snapshot) {
                if (error != null) {
                    Toast.makeText(WaitingRoomGarticActivity.this, "🔥 Firebase lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                if (!committed) {
                    Toast.makeText(WaitingRoomGarticActivity.this, "⚠️ Phòng đang chơi hoặc đã đầy!", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                Log.d(TAG, "Vào phòng thành công!");
                // Thiết lập an toàn khi mất kết nối
                roomRef.child("players").child(userId).onDisconnect().removeValue();

                // Bắt đầu lắng nghe các thay đổi trong phòng
                attachListeners();
            }
        });
    }

    private void attachListeners() {
        tvRoomId.setText("🔗 Mã phòng: " + coupleId);

        // Lắng nghe danh sách người chơi
        if (playersListener != null) roomRef.child("players").removeEventListener(playersListener);
        playersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    // Phòng đã bị xóa hoàn toàn (do người cuối cùng thoát)
                    Toast.makeText(WaitingRoomGarticActivity.this, "Phòng đã bị hủy!", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }

                currentPlayerCount = snapshot.getChildrenCount();
                List<String> playerIds = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    playerIds.add(child.getKey());
                }
                updatePlayerListUI(playerIds);

                // ✅ Lấy HostId và cập nhật trạng thái Host (Đã tối giản, vì logic chuyển host nằm trong Transaction)
                roomRef.child("hostId").get().addOnSuccessListener(hostSnapshot -> {
                    currentHostId = hostSnapshot.getValue(String.class);
                    isHost = userId.equals(currentHostId);
                    updateStatusUI();
                });
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        };
        roomRef.child("players").addValueEventListener(playersListener);

        // Lắng nghe trạng thái của game
        if (statusListener != null) roomRef.child("status").removeEventListener(statusListener);
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

    private void updatePlayerListUI(List<String> players) {
        playerList.removeAllViews();
        for (String player : players) {
            TextView tv = new TextView(WaitingRoomGarticActivity.this);
            String displayText = "• " + player;
            if (player.equals(currentHostId)) {
                displayText += " (Chủ phòng 👑)";
            }
            tv.setText(displayText);
            tv.setTextSize(16);
            tv.setPadding(8, 8, 8, 8);
            tv.setGravity(Gravity.CENTER_HORIZONTAL);
            playerList.addView(tv);
        }
    }

    private void updateStatusUI() {
        runOnUiThread(() -> {
            if (currentPlayerCount == 2) {
                if (isHost) {
                    btnStartGame.setVisibility(Button.VISIBLE);
                    tvStatus.setText("✅ Đã đủ người, bạn có thể bắt đầu!");
                } else {
                    btnStartGame.setVisibility(Button.GONE);
                    tvStatus.setText("⌛ Chờ chủ phòng bắt đầu...");
                }
            } else {
                btnStartGame.setVisibility(Button.GONE);
                tvStatus.setText("👥 Người chơi: " + currentPlayerCount + "/2. Đang chờ người khác...");
            }
        });
    }

    private void startGame() {
        // Chủ phòng sẽ reset dữ liệu game cũ và đổi trạng thái sang "playing"
        Map<String, Object> gameData = new HashMap<>();
        gameData.put("status", "playing");
        gameData.put("scores", null);
        gameData.put("currentTurn", null);
        gameData.put("currentWord", null);
        gameData.put("drawingData", null);
        roomRef.updateChildren(gameData);
    }

    private void goToGame() {
        isLeavingForGame = true;
        removeAllListeners();
        Intent intent = new Intent(this, GarticActivity.class);
        intent.putExtra("roomId", coupleId);
        startActivity(intent);
        finish();
    }

    private void removeAllListeners() {
        if (playersListener != null) roomRef.child("players").removeEventListener(playersListener);
        if (statusListener != null) roomRef.child("status").removeEventListener(statusListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeAllListeners();
        if (!isLeavingForGame && userId != null) {
            removePlayerAndCleanUpTransaction();
        }
    }
    private void removePlayerAndCleanUpTransaction() {
        roomRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                MutableData playersNode = currentData.child("players");
                String hostId = currentData.child("hostId").getValue(String.class);

                if (playersNode.hasChild(userId)) {
                    playersNode.child(userId).setValue(null);
                } else {
                    return Transaction.success(currentData);
                }

                long remainingPlayers = playersNode.getChildrenCount();

                if (remainingPlayers == 0) {
                    currentData.setValue(null);
                } else if (userId.equals(hostId)) {
                    for (MutableData child : playersNode.getChildren()) {
                        String newHostId = child.getKey();
                        if (newHostId != null) {
                            currentData.child("hostId").setValue(newHostId);
                            break;
                        }
                    }
                }
                currentData.child("status").setValue("waiting");

                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot snapshot) {
                if (error != null) {
                    Log.e(TAG, "Lỗi dọn dẹp phòng: " + error.getMessage());
                } else if (committed && snapshot != null && !snapshot.exists()) {
                    Log.d(TAG, "Phòng đã được dọn dẹp hoàn toàn.");
                }
            }
        });
    }
}