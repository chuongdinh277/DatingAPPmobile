package com.example.couple_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.example.couple_app.R;
import com.example.couple_app.managers.AuthManager;
import com.example.couple_app.managers.ChatManager;
import com.example.couple_app.models.ChatMessage;
import com.example.couple_app.adapters.ChatAdapter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";

    private TextView tvPartnerName, tvCoupleId;
    private RecyclerView rvMessages;
    private EditText etMessage;
    private Button btnSend, btnLogout;

    private AuthManager authManager;
    private ChatManager chatManager;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList;
    private ChildEventListener messageListener;
    private Set<String> seenMessageIds;

    private String coupleId;
    private String partnerName;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initViews();
        initManagers();
        getIntentData();
        setupRecyclerView();
        setupClickListeners();
        seenMessageIds = new HashSet<>();
        loadChatHistory();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Toast.makeText(ChatActivity.this, "Nhấn nút Đăng xuất để thoát", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initViews() {
        tvPartnerName = findViewById(R.id.tv_partner_name);
        tvCoupleId = findViewById(R.id.tv_couple_id);
        rvMessages = findViewById(R.id.rv_messages);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        btnLogout = findViewById(R.id.btn_logout);
    }

    private void initManagers() {
        authManager = AuthManager.getInstance();
        chatManager = ChatManager.getInstance();
    }

    private void getIntentData() {
        Intent intent = getIntent();
        coupleId = intent.getStringExtra("coupleId");
        partnerName = intent.getStringExtra("partnerName");

        FirebaseUser currentUser = authManager.getCurrentUser();
        if (currentUser == null) {
            navigateToLogin();
            return;
        }

        currentUserId = currentUser.getUid();

        if (coupleId == null || partnerName == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin cặp đôi", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        tvPartnerName.setText("\uD83D\uDC95 " + partnerName);
        tvCoupleId.setText("Couple ID: " + coupleId);
    }

    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList, currentUserId);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Start from bottom
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(chatAdapter);
    }

    private void setupClickListeners() {
        btnSend.setOnClickListener(v -> sendMessage());
        btnLogout.setOnClickListener(v -> logout());

        // Send message when enter is pressed
        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }

    private void loadChatHistory() {
        chatManager.getChatHistory(coupleId, 50, new ChatManager.ChatCallback() {
            @Override
            public void onMessagesReceived(List<ChatMessage> messages) {
                messageList.clear();
                messageList.addAll(messages);
                // Seed de-dup set
                seenMessageIds.clear();
                long lastTs = 0L;
                for (ChatMessage m : messages) {
                    if (m.getMessageId() != null) seenMessageIds.add(m.getMessageId());
                    long ts = coerceToMillis(m != null ? m.getTimestamp() : null);
                    if (ts > lastTs) lastTs = ts;
                }
                chatAdapter.notifyDataSetChanged();
                scrollToBottom();
                // Start streaming only messages after the last history timestamp
                startListeningForMessages(lastTs);
            }

            @Override
            public void onMessageSent() { }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading chat history: " + error);
                Toast.makeText(ChatActivity.this, "Lỗi tải lịch sử chat: " + error, Toast.LENGTH_SHORT).show();
                // Even if history fails, start listening from now
                startListeningForMessages(System.currentTimeMillis());
            }
        });
    }

    private void startListeningForMessages(long startAfterTs) {
        // Remove any previous listener
        if (messageListener != null) {
            chatManager.removeChildMessageListener(coupleId, messageListener);
        }
        messageListener = chatManager.listenForNewMessagesStream(coupleId, startAfterTs, new ChatManager.MessageListener() {
            @Override
            public void onNewMessage(ChatMessage message) {
                if (message == null) return;
                String id = message.getMessageId();
                if (id != null && seenMessageIds.contains(id)) {
                    return; // de-dup
                }
                // Don't duplicate own message added locally in onMessageSent()
                if (message.getSenderId() != null && message.getSenderId().equals(currentUserId)) {
                    // Skip: local echo already added
                    if (id != null) seenMessageIds.add(id);
                    return;
                }
                messageList.add(message);
                if (id != null) seenMessageIds.add(id);
                chatAdapter.notifyItemInserted(messageList.size() - 1);
                scrollToBottom();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error listening for messages: " + error);
            }
        });
    }

    private long coerceToMillis(Object ts) {
        if (ts == null) return 0L;
        if (ts instanceof Long) return (Long) ts;
        if (ts instanceof Double) return ((Double) ts).longValue();
        return 0L;
    }

    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();

        if (messageText.isEmpty()) {
            return;
        }

        btnSend.setEnabled(false);

        chatManager.sendMessage(coupleId, currentUserId, messageText, new ChatManager.ChatCallback() {
            @Override
            public void onMessagesReceived(List<ChatMessage> messages) {
                // Not used for sending
            }

            @Override
            public void onMessageSent() {
                etMessage.setText("");
                btnSend.setEnabled(true);

                // Add message to local list immediately for better UX
                ChatMessage newMessage = new ChatMessage(currentUserId, messageText);
                messageList.add(newMessage);
                chatAdapter.notifyItemInserted(messageList.size() - 1);
                scrollToBottom();
            }

            @Override
            public void onError(String error) {
                btnSend.setEnabled(true);
                Log.e(TAG, "Error sending message: " + error);
                Toast.makeText(ChatActivity.this, "Lỗi gửi tin nhắn: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void scrollToBottom() {
        if (!messageList.isEmpty()) {
            rvMessages.smoothScrollToPosition(messageList.size() - 1);
        }
    }

    private void logout() {
        authManager.signOut();
        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) {
            chatManager.removeChildMessageListener(coupleId, messageListener);
        }
    }
}
