package com.example.couple_app.activities;

import static android.content.ContentValues.TAG;

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
                // Đọc dữ liệu hiện tại từ Firebase
                String hostId = currentData.child("hostId").getValue(String.class);
                String status = currentData.child("status").getValue(String.class);
                MutableData playersNode = currentData.child("players");

                // === LOGIC TẠO PHÒNG HOẶC SỬA PHÒNG LỖI ===
                // Nếu phòng chưa tồn tại HOẶC tồn tại nhưng không có chủ phòng
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
                // Nếu phòng đang chơi, không cho vào
                if ("playing".equals(status)) return Transaction.abort();

                // Nếu người chơi đã có trong phòng, không cần làm gì thêm
                if (playersNode.hasChild(userId)) return Transaction.success(currentData);

                // Nếu phòng đã đủ 2 người, không cho vào
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
                // Thiết lập an toàn khi mất kết nối: chỉ xóa người chơi này
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
                if (!snapshot.exists()) return; // Phòng đã bị hủy

                currentPlayerCount = snapshot.getChildrenCount();
                List<String> playerIds = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    playerIds.add(child.getKey());
                }
                updatePlayerListUI(playerIds);

                // Kiểm tra xem chủ phòng cũ có còn trong phòng không
                roomRef.child("hostId").get().addOnSuccessListener(hostSnapshot -> {
                    currentHostId = hostSnapshot.getValue(String.class);
                    // Nếu chủ phòng cũ đã thoát
                    if (currentHostId != null && !playerIds.contains(currentHostId)) {
                        // Người còn lại sẽ trở thành chủ phòng mới
                        String newHostId = playerIds.isEmpty() ? null : playerIds.get(0);
                        if (newHostId != null) {
                            roomRef.child("hostId").setValue(newHostId);
                            currentHostId = newHostId;
                        }
                    }
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
        gameData.put("scores", null); // Xóa điểm cũ
        gameData.put("currentTurn", null); // Xóa lượt cũ
        gameData.put("currentWord", null);
        gameData.put("drawingData", null);
        roomRef.updateChildren(gameData);
    }

    private void goToGame() {
        isLeavingForGame = true; // Đánh dấu để không kích hoạt logic rời phòng
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

        // Nếu người dùng rời đi (không phải để vào game)
        if (!isLeavingForGame && userId != null) {
            // Chỉ xóa người chơi này khỏi danh sách
            roomRef.child("players").child(userId).removeValue();
        }
    }
}