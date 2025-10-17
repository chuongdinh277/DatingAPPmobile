package com.example.couple_app.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.couple_app.R;
import com.example.couple_app.utils.LoginPreferences;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class GarticActivity extends AppCompatActivity {

    private TextView tvRoomId, tvScore, tvCurrentWord, tvTimer;
    private FrameLayout gameBoard;
    private EditText etMessage;
    private Button btnSend;

    private String userId, roomId;
    private DatabaseReference roomRef;

    private String otherPlayerId;
    private String currentWord;
    private boolean isMyTurn;

    private CountDownTimer turnTimer;
    private static final int TURN_TIME_MS = 15_000;

    private Map<String, Integer> scores = new HashMap<>();
    private final int WIN_SCORE = 10;

    private DrawingView drawingView;

    private String[] wordList = {"Cat","Dog","House","Car","Tree","Apple","Sun","Moon"};

    private boolean isGuessingPhase = false;

    private ValueEventListener drawingDataListener;
    private ValueEventListener currentWordListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gartic);

        tvRoomId = findViewById(R.id.tvRoomId);
        tvScore = findViewById(R.id.tvScore);
        tvCurrentWord = findViewById(R.id.tvCurrentWord);
        tvTimer = findViewById(R.id.tvTimer);
        gameBoard = findViewById(R.id.gameBoard);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);

        userId = LoginPreferences.getUserId(this);
        roomId = getIntent().getStringExtra("roomId");
        tvRoomId.setText("Phòng: #" + roomId);

        roomRef = FirebaseDatabase.getInstance().getReference("rooms").child(roomId);
        roomRef.child("players").child(userId).onDisconnect().removeValue();

        drawingView = new DrawingView(this);
        gameBoard.addView(drawingView);
        // Wire drawing to room for realtime strokes display
        drawingView.setRoomId(roomId);

        btnSend.setOnClickListener(v -> sendGuess());

        initGame();
    }


    private void initGame() {
        roomRef.child("players").addListenerForSingleValueEvent(new SimpleValueListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot p : snapshot.getChildren()) {
                    String id = p.getKey();
                    if (!id.equals(userId)) otherPlayerId = id;
                    scores.put(id, 0);
                }
                updateScoreUI();

                // Reset điểm và trạng thái
                Map<String,Object> resetData = new HashMap<>();
                resetData.put("scores/"+userId,0);
                if (otherPlayerId != null) resetData.put("scores/"+otherPlayerId,0);
                resetData.put("status","waiting");
                resetData.put("currentWord", null);
                resetData.put("drawingData", null);
                roomRef.updateChildren(resetData);

                // Chọn lượt đầu tiên nếu chưa có
                roomRef.child("currentTurn").addListenerForSingleValueEvent(new SimpleValueListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(!snapshot.exists() && otherPlayerId!=null){
                            String first = Math.random()<0.5?userId:otherPlayerId;
                            roomRef.child("currentTurn").setValue(first);
                        }
                    }
                });

                // Lắng nghe lượt
                roomRef.child("currentTurn").addValueEventListener(new SimpleValueListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String turnId = snapshot.getValue(String.class);
                        if(turnId!=null) startTurn(turnId);
                    }
                });

                // Lắng nghe điểm
                roomRef.child("scores").addValueEventListener(new SimpleValueListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for(DataSnapshot s : snapshot.getChildren()){
                            scores.put(s.getKey(),s.getValue(Integer.class));
                        }
                        updateScoreUI();
                    }
                });
            }
        });
    }

    private void startTurn(String turnId){
        isMyTurn = turnId.equals(userId);

        // 🔹 Reset UI đầu lượt
        drawingView.clearCanvas();
        drawingView.showImage(null);
        tvCurrentWord.setText("");
        etMessage.setEnabled(!isMyTurn);
        btnSend.setEnabled(!isMyTurn);
        isGuessingPhase = !isMyTurn;
        currentWord = null;

        // 🔹 Remove listener cũ
        if(drawingDataListener != null)
            roomRef.child("drawingData").removeEventListener(drawingDataListener);
        if(currentWordListener != null)
            roomRef.child("currentWord").removeEventListener(currentWordListener);

        if(isMyTurn){
            tvCurrentWord.setText("Đang chọn từ...");
            tvTimer.setText("30s");

            // Chọn từ mới
            roomRef.child("currentWord").addListenerForSingleValueEvent(new SimpleValueListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if(snapshot.getValue()==null){
                        String word = wordList[new Random().nextInt(wordList.length)];
                        roomRef.child("currentWord").setValue(word);
                    }
                }
            });

            currentWordListener = roomRef.child("currentWord").addValueEventListener(new SimpleValueListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    currentWord = snapshot.getValue(String.class);
                    Log.d("DEBUG_WORD", "currentWord from Firebase: " + currentWord); // in ra terminal
                    if(currentWord!=null && isMyTurn)
                        tvCurrentWord.setText("Từ bí mật: " + currentWord);
                }
            });

            startTimerForDrawer();
        } else {
            tvCurrentWord.setText("Đang đoán...");

            drawingDataListener = roomRef.child("drawingData").addValueEventListener(new SimpleValueListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if(snapshot.exists() && snapshot.getValue()!=null){
                        String base64 = snapshot.getValue(String.class);
                        if(!base64.isEmpty()){
                            Bitmap bmp = base64ToBitmap(base64);
                            drawingView.showImage(bmp);

                            // Chỉ enable đoán khi từ bí mật đã có
                            roomRef.child("currentWord").addListenerForSingleValueEvent(new SimpleValueListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    currentWord = snapshot.getValue(String.class);
                                    if(currentWord != null){
                                        etMessage.setEnabled(true);
                                        btnSend.setEnabled(true);
                                        startTimerForGuesser();
                                    }
                                }
                            });
                        }
                    }
                }
            });

        }
    }

    private void startTimerForDrawer(){
        if(turnTimer!=null) turnTimer.cancel();
        turnTimer = new CountDownTimer(TURN_TIME_MS,1000){
            @Override
            public void onTick(long millisUntilFinished){
                tvTimer.setText((millisUntilFinished/1000)+"s");
            }
            @Override
            public void onFinish(){
                tvTimer.setText("Hết thời gian vẽ!");
                Bitmap bmp = drawingView.exportBitmap();
                roomRef.child("drawingData").setValue(bitmapToBase64(bmp));
            }
        }.start();
    }

    private void startTimerForGuesser(){
        if(turnTimer!=null) turnTimer.cancel();
        turnTimer = new CountDownTimer(TURN_TIME_MS,1000){
            @Override
            public void onTick(long millisUntilFinished){
                tvTimer.setText((millisUntilFinished/1000)+"s");
            }
            @Override
            public void onFinish(){
                tvTimer.setText("Hết thời gian đoán!");
                endGuessingRound();
            }
        }.start();
    }

    private void sendGuess(){
        String guess = etMessage.getText().toString().trim();
        Log.d("DEBUG_WORD", "User guessed: " + guess); // in ra terminal
        if(guess.isEmpty() || isMyTurn) return;

        if(currentWord!=null && guess.equalsIgnoreCase(currentWord)){
            Toast.makeText(this,"🎉 Đoán đúng!",Toast.LENGTH_SHORT).show();
            int newScore = scores.get(userId)+1;
            scores.put(userId,newScore);
            roomRef.child("scores").child(userId).setValue(newScore);

            if(newScore>=WIN_SCORE){
                Toast.makeText(this,"🏆 Bạn thắng!",Toast.LENGTH_LONG).show();
                roomRef.child("status").setValue("finished");
                if(turnTimer!=null) turnTimer.cancel();
            } else {
                if(turnTimer!=null) turnTimer.cancel();
                endGuessingRound();
            }
        } else {
            Toast.makeText(this,"Sai rồi!",Toast.LENGTH_SHORT).show();
        }

        etMessage.setText("");
        updateScoreUI();
    }

    private void endGuessingRound(){
        // 🔹 Reset Firebase
        Map<String,Object> resetData = new HashMap<>();
        resetData.put("currentWord", null);
        resetData.put("drawingData", null);
        resetData.put("status","nextRound");
        roomRef.updateChildren(resetData);

        // 🔹 Reset UI ngay
        drawingView.clearCanvas();
        drawingView.showImage(null);
        tvCurrentWord.setText("");
        etMessage.setEnabled(false);
        btnSend.setEnabled(false);
        isGuessingPhase = false;
        currentWord = null;

        // 🔹 Swap lượt sau delay nhỏ
        new Handler().postDelayed(this::swapTurn,500);
    }

    private void swapTurn(){
        String nextTurn = isMyTurn ? otherPlayerId : userId;
        roomRef.child("currentTurn").setValue(nextTurn);
    }

    private void updateScoreUI(){
        int myScore = scores.getOrDefault(userId,0);
        int otherScore = scores.getOrDefault(otherPlayerId,0);
        tvScore.setText("Điểm: "+myScore+" - "+otherScore);
    }

    private abstract static class SimpleValueListener implements ValueEventListener{
        @Override
        public void onCancelled(@NonNull DatabaseError error){}
    }

    private String bitmapToBase64(Bitmap bitmap){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100,baos);
        byte[] bytes = baos.toByteArray();
        return Base64.encodeToString(bytes,Base64.DEFAULT);
    }

    private Bitmap base64ToBitmap(String base64){
        byte[] bytes = Base64.decode(base64,Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        removePlayerFromRoom();
    }

    @Override
    protected void onStop(){
        super.onStop();
        if(isFinishing()) removePlayerFromRoom();
    }

    private void removePlayerFromRoom(){
        if(roomId==null || userId==null) return;
        DatabaseReference playerRef = roomRef.child("players").child(userId);
        playerRef.removeValue().addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                roomRef.child("players").get().addOnSuccessListener(snapshot->{
                    if(!snapshot.exists()){
                        roomRef.removeValue();
                    } else {
                        String remainingId = snapshot.getChildren().iterator().next().getKey();
                        if(remainingId!=null){
                            Map<String,Object> updates = new HashMap<>();
                            updates.put("hostId",remainingId);
                            updates.put("status","waiting");
                            roomRef.updateChildren(updates);
                        }
                    }
                });
            }
        });
    }
}
