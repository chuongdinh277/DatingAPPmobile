package com.example.btl_mobileapp.managers;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btl_mobileapp.R;
import com.example.btl_mobileapp.activities.BaseActivity;
import com.example.btl_mobileapp.adapters.MessengerAdapter;
import com.example.btl_mobileapp.models.Messenger;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MessengerActivity extends BaseActivity {

    private RecyclerView rvMessenger;
    private EditText etMessenger;
    private ImageButton btSend;

    private MessengerAdapter adapter;
    private List<Messenger> messageList;

    private FirebaseAuth mAuth;
    private DatabaseReference messagesRef;
    private FirebaseFirestore firestore;

    private String currentUserId;
    private String pairId;
    private String otherUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.messenger);

        rvMessenger = findViewById(R.id.rv_messenger);
        etMessenger = findViewById(R.id.et_messenger);
        btSend = findViewById(R.id.bt_sendMessage);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();

        messageList = new ArrayList<>();
        adapter = new MessengerAdapter(messageList, currentUserId);
        rvMessenger.setLayoutManager(new LinearLayoutManager(this));
        rvMessenger.setAdapter(adapter);

        // 1️⃣ Lấy pairId từ Firestore của user hiện tại
        firestore.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if(doc.exists()){
                        pairId = doc.getString("pairId");
                        otherUserId = doc.getString("otherUserId"); // lưu khi ghép đôi

                        if(pairId != null && otherUserId != null){
                            // 2️⃣ Khởi tạo reference Realtime Database cho pairId
                            messagesRef = FirebaseDatabase.getInstance().getReference("messages").child(pairId);

                            // 3️⃣ Lắng nghe tin nhắn realtime
                            listenMessages();
                        } else {
                            Toast.makeText(this, "Bạn chưa ghép đôi, không thể chat", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // Gửi tin nhắn
        btSend.setOnClickListener(v -> {
            String msgText = etMessenger.getText().toString().trim();
            if(TextUtils.isEmpty(msgText)){
                Toast.makeText(this, "Nhập tin nhắn!", Toast.LENGTH_SHORT).show();
                return;
            }
            sendMessage(msgText);
            etMessenger.setText("");
        });
    }

    // 4️⃣ Lắng nghe tin nhắn realtime
    private void listenMessages(){
        if(messagesRef == null) return;

        messagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();
                for(DataSnapshot ds : snapshot.getChildren()){
                    Messenger msg = ds.getValue(Messenger.class);
                    if(msg != null){
                        messageList.add(msg);
                    }
                }
                adapter.setMessages(messageList);
                rvMessenger.scrollToPosition(messageList.size() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // 5️⃣ Gửi tin nhắn
    private void sendMessage(String messageText){
        if(messagesRef == null) return;

        long timestamp = System.currentTimeMillis();
        String key = messagesRef.push().getKey();
        Messenger message = new Messenger(currentUserId, otherUserId, messageText, timestamp);
        messagesRef.child(key).setValue(message)
                .addOnSuccessListener(aVoid -> {
                    // Có thể hiển thị toast nếu muốn
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Gửi thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
