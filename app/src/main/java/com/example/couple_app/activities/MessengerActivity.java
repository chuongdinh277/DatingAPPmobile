package com.example.couple_app.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.couple_app.R;
import com.example.couple_app.adapters.MessageAdapter;
import com.example.couple_app.managers.ChatManager;
import com.example.couple_app.managers.DatabaseManager;
import com.example.couple_app.models.ChatMessage;
import com.example.couple_app.models.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;

import java.util.ArrayList;
import java.util.List;

public class MessengerActivity extends AppCompatActivity {
    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend, btnBack;
    private TextView tvPartnerName;

    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private ChatManager chatManager;
    private DatabaseManager databaseManager;
    private FirebaseAuth mAuth;
    private ChildEventListener messageListener;

    private String coupleId;
    private String partnerId;
    private String partnerName;
    private String currentUserId;
    private String currentUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.messenger);

        initViews();
        getIntentData();
        setupRecyclerView();
        setupClickListeners();
        loadMessages();
    }

    private void initViews() {
        rvMessages = findViewById(R.id.rv_messenger);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        btnBack = findViewById(R.id.btn_back);
        tvPartnerName = findViewById(R.id.tv_partner_name);

        chatManager = ChatManager.getInstance();
        databaseManager = DatabaseManager.getInstance();
        mAuth = FirebaseAuth.getInstance();
        messageList = new ArrayList<>();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            // Get name from user data or use display name
            databaseManager.getUser(currentUserId, new DatabaseManager.DatabaseCallback<com.example.couple_app.models.User>() {
                @Override
                public void onSuccess(com.example.couple_app.models.User user) {
                    currentUserName = user.getName() != null ? user.getName() : "You";
                }

                @Override
                public void onError(String error) {
                    currentUserName = currentUser.getDisplayName() != null ?
                        currentUser.getDisplayName() : "You";
                }
            });
        }
    }

    private void getIntentData() {
        coupleId = getIntent().getStringExtra("coupleId");
        partnerId = getIntent().getStringExtra("partnerId");
        partnerName = getIntent().getStringExtra("partnerName");

        if (tvPartnerName != null && partnerName != null) {
            tvPartnerName.setText(partnerName);
        }
    }

    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter(messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Start from bottom
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(messageAdapter);
    }

    private void setupClickListeners() {
        btnSend.setOnClickListener(v -> sendMessage());

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }

        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }

    private void loadMessages() {
        if (coupleId == null || coupleId.isEmpty()) {
            Toast.makeText(this, "Error: Couple ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Load chat history first
        chatManager.getChatHistory(coupleId, 50, new ChatManager.ChatCallback() {
            @Override
            public void onMessagesReceived(List<ChatMessage> chatMessages) {
                messageList.clear();
                for (ChatMessage chatMsg : chatMessages) {
                    Message message = convertChatMessageToMessage(chatMsg);
                    messageList.add(message);
                }
                messageAdapter.updateMessages(messageList);

                if (!messageList.isEmpty()) {
                    rvMessages.smoothScrollToPosition(messageList.size() - 1);
                }

                // Setup real-time listener for new messages
                setupRealTimeListener();
            }

            @Override
            public void onMessageSent() {
                // Not used here
            }

            @Override
            public void onError(String error) {
                Log.e("MessengerActivity", "Error loading chat history: " + error);
                Toast.makeText(MessengerActivity.this, "Error loading messages: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRealTimeListener() {
        // Get the timestamp of the last message to avoid duplicates
        long lastTimestamp = messageList.isEmpty() ? 0 :
            getTimestampFromMessage(messageList.get(messageList.size() - 1));

        messageListener = chatManager.listenForNewMessagesStream(coupleId, lastTimestamp, new ChatManager.MessageListener() {
            @Override
            public void onNewMessage(ChatMessage chatMessage) {
                Message message = convertChatMessageToMessage(chatMessage);
                messageList.add(message);
                messageAdapter.updateMessages(messageList);
                rvMessages.smoothScrollToPosition(messageList.size() - 1);
            }

            @Override
            public void onError(String error) {
                Log.e("MessengerActivity", "Error listening for new messages: " + error);
            }
        });
    }

    private Message convertChatMessageToMessage(ChatMessage chatMessage) {
        Message message = new Message();
        message.setMessageId(chatMessage.getMessageId());
        message.setSenderId(chatMessage.getSenderId());
        message.setMessage(chatMessage.getMessage());
        message.setTimestamp(chatMessage.getTimestamp());
        message.setCoupleId(coupleId);

        // Set sender name based on senderId
        if (chatMessage.getSenderId().equals(currentUserId)) {
            message.setSenderName(currentUserName != null ? currentUserName : "You");
        } else {
            message.setSenderName(partnerName != null ? partnerName : "Partner");
        }

        message.setMessageType("text");
        message.setRead(false);

        return message;
    }

    private long getTimestampFromMessage(Message message) {
        Object timestamp = message.getTimestamp();
        if (timestamp instanceof Long) {
            return (Long) timestamp;
        } else if (timestamp instanceof Double) {
            return ((Double) timestamp).longValue();
        }
        return 0L;
    }

    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();

        if (TextUtils.isEmpty(messageText)) {
            return;
        }

        if (coupleId == null || currentUserId == null) {
            Toast.makeText(this, "Error: Unable to send message", Toast.LENGTH_SHORT).show();
            return;
        }

        // Send message using ChatManager
        chatManager.sendMessage(coupleId, currentUserId, messageText, new ChatManager.ChatCallback() {
            @Override
            public void onMessagesReceived(List<ChatMessage> messages) {
                // Not used here
            }

            @Override
            public void onMessageSent() {
                // Clear input field
                runOnUiThread(() -> etMessage.setText(""));
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                    Toast.makeText(MessengerActivity.this, "Failed to send message: " + error, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove real-time listener to prevent memory leaks
        if (messageListener != null && coupleId != null) {
            chatManager.getChatHistory(coupleId, 1, new ChatManager.ChatCallback() {
                @Override
                public void onMessagesReceived(List<ChatMessage> messages) {
                    // Remove listener (this is a workaround since ChatManager doesn't have direct removeListener method)
                }

                @Override
                public void onMessageSent() {}

                @Override
                public void onError(String error) {}
            });
        }
    }
}
