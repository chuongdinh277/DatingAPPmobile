package com.example.couple_app.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.couple_app.R;
import com.example.couple_app.utils.LoginPreferences;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class CaroActivity extends AppCompatActivity {

    private TextView tvTurn, tvScore;
    private GridLayout gridBoard;
    private Button btnExit, btnReset;

    private String userId;
    private String roomId;
    private DatabaseReference roomRef;
    private Map<String, String> board = new HashMap<>();
    private String mySymbol, currentTurn;
    private boolean gameEnded = false;

    private int myScore = 0;
    private int opponentScore = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caro);

        tvTurn = findViewById(R.id.tvTurn);
        tvScore = findViewById(R.id.tvScore);
        gridBoard = findViewById(R.id.gridBoard);
        btnExit = findViewById(R.id.btnExit);
        btnReset = findViewById(R.id.btnReset);

        userId = LoginPreferences.getUserId(this);
        roomId = getIntent().getStringExtra("roomId");
        roomRef = FirebaseDatabase.getInstance().getReference("caroRooms").child(roomId);

        initBoardUI();
        listenBoardChanges();
        listenTurn();
        listenWinner();

        btnExit.setOnClickListener(v -> finish());
        btnReset.setOnClickListener(v -> resetBoard());
    }

    //... các phần code khác giữ nguyên ...

    //... các phần code khác giữ nguyên ...

    private void initBoardUI() {
        gridBoard.removeAllViews();
        // Chờ GridLayout được vẽ xong để tính kích thước ô
        gridBoard.post(() -> {
            int gridSize = gridBoard.getWidth();
            // Định nghĩa margin giữa các ô
            int margin = 8;
            int totalMargin = 2 * margin; // có 2 khoảng trống giữa 3 ô
            int buttonSize = (gridSize - totalMargin) / 3;

            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    Button btn = new Button(this);
                    // Dùng nền trắng cho các ô để các đường viền xuất hiện
                    btn.setBackgroundResource(R.drawable.bg_caro_cell);
                    btn.setTextColor(Color.TRANSPARENT);
                    GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                    params.rowSpec = GridLayout.spec(i);
                    params.columnSpec = GridLayout.spec(j);
                    params.width = buttonSize;
                    params.height = buttonSize;
                    // Thêm margin để tạo khoảng cách giữa các ô
                    if (i > 0) params.topMargin = margin;
                    if (j > 0) params.leftMargin = margin;
                    btn.setLayoutParams(params);
                    final int row = i, col = j;
                    btn.setOnClickListener(v -> makeMove(row, col));
                    gridBoard.addView(btn);
                }
            }
        });
    }

//... các phần code khác giữ nguyên ...

//... các phần code khác giữ nguyên ...

    private void makeMove(int row, int col) {
        if (gameEnded) return;
        if (!mySymbol.equals(currentTurn)) {
            Toast.makeText(this, "Không phải lượt của bạn", Toast.LENGTH_SHORT).show();
            return;
        }

        String key = "" + row + col;
        if (!"".equals(board.getOrDefault(key, ""))) {
            Toast.makeText(this, "Ô này đã được đánh", Toast.LENGTH_SHORT).show();
            return;
        }

        board.put(key, mySymbol);
        roomRef.child("board").child(key).setValue(mySymbol)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) switchTurn();
                });
    }

    private void switchTurn() {
        String nextTurn = currentTurn.equals("X") ? "O" : "X";
        roomRef.child("turn").setValue(nextTurn);
    }

    private void listenBoardChanges() {
        roomRef.child("board").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                board.clear();
                // Reset tất cả các ô trên bảng
                for (int i = 0; i < gridBoard.getChildCount(); i++) {
                    Button btn = (Button) gridBoard.getChildAt(i);
                    btn.setBackgroundResource(R.drawable.bg_caro_cell);
                }

                for (DataSnapshot cell : snapshot.getChildren()) {
                    String key = cell.getKey();
                    String value = cell.getValue(String.class);
                    board.put(key, value);

                    int row = Character.getNumericValue(key.charAt(0));
                    int col = Character.getNumericValue(key.charAt(1));
                    Button btn = (Button) gridBoard.getChildAt(row * 3 + col);

                    // Hiển thị hình ảnh thay vì chữ
                    if ("X".equals(value)) {
                        btn.setBackgroundResource(R.drawable.ic_caro_x);
                    } else if ("O".equals(value)) {
                        btn.setBackgroundResource(R.drawable.ic_caro_o);
                    }
                }
                checkWinner();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void listenTurn() {
        roomRef.child("turn").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentTurn = snapshot.getValue(String.class);

                roomRef.child("hostId").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        String hostId = snap.getValue(String.class);
                        mySymbol = userId.equals(hostId) ? "X" : "O";
                        tvTurn.setText("Lượt của: " + currentTurn);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void listenWinner() {
        roomRef.child("winner").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String winner = snapshot.getValue(String.class);
                if (winner != null && !winner.isEmpty() && !winner.equals("waiting")) {
                    handleWinner(winner);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void checkWinner() {
        String[][] b = new String[3][3];
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                b[i][j] = board.getOrDefault("" + i + j, "");

        String winner = null;
        for (int i = 0; i < 3; i++)
            if (!b[i][0].isEmpty() && b[i][0].equals(b[i][1]) && b[i][1].equals(b[i][2]))
                winner = b[i][0];
        for (int j = 0; j < 3; j++)
            if (!b[0][j].isEmpty() && b[0][j].equals(b[1][j]) && b[1][j].equals(b[2][j]))
                winner = b[0][j];
        if (!b[0][0].isEmpty() && b[0][0].equals(b[1][1]) && b[1][1].equals(b[2][2]))
            winner = b[0][0];
        if (!b[0][2].isEmpty() && b[0][2].equals(b[1][1]) && b[1][1].equals(b[2][0]))
            winner = b[0][2];

        if (winner == null) {
            boolean draw = true;
            for (int i = 0; i < 3; i++)
                for (int j = 0; j < 3; j++)
                    if (b[i][j].isEmpty()) draw = false;
            if (draw) winner = "draw";
        }

        if (winner != null && !winner.equals("waiting")) {
            roomRef.child("winner").setValue(winner);
            gameEnded = true;
        }
    }

    private void handleWinner(String winner) {
        if (winner.equals(mySymbol)) myScore++;
        else if (!winner.equals("draw")) opponentScore++;

        tvScore.setText("Điểm: Bạn " + myScore + " - Đối thủ " + opponentScore);

        if (myScore >= 5 || opponentScore >= 5) {
            gameEnded = true;
            String msg = myScore >= 5 ? "🎉 Bạn thắng trận!" : "😢 Đối thủ thắng trận!";
            new AlertDialog.Builder(this)
                    .setTitle("Kết quả chung cuộc")
                    .setMessage(msg)
                    .setPositiveButton("OK", (dialog, which) -> finish())
                    .setCancelable(false)
                    .show();
        } else {
            Toast.makeText(this, winner.equals("draw") ? "🤝 Hòa!" : "🎉 Người thắng: " + winner, Toast.LENGTH_SHORT).show();
            resetBoard();
        }
    }

    private void resetBoard() {
        gameEnded = false;
        board.clear();
        roomRef.child("board").setValue(null);
        roomRef.child("turn").setValue("X");
        roomRef.child("winner").setValue("waiting");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (roomRef != null) roomRef.removeValue();
    }
}