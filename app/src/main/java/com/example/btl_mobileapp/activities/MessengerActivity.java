package com.example.btl_mobileapp.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btl_mobileapp.R;
import com.example.btl_mobileapp.adapters.MessageAdapter;
import com.example.btl_mobileapp.managers.ChatManager;
import com.example.btl_mobileapp.managers.DatabaseManager;
import com.example.btl_mobileapp.models.ChatMessage;
import com.example.btl_mobileapp.models.Message;
import com.example.btl_mobileapp.models.Couple;
import com.example.btl_mobileapp.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MessengerActivity extends BaseActivity {
    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend;
    private TextView tvPartnerName;
    private View loadingOverlay;

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

    // Ẩn thanh menu ở màn hình message

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.messenger);

        // Không highlight tab vì đã ẩn menu
        // setActiveButton("message");

        initViews();
        getIntentData();
        setupRecyclerView();
        setupClickListeners();

        // Hiển thị overlay và đảm bảo lấy couple/partner trước khi load messages
        showLoading(true);
        ensureCoupleThenLoadMessages();
    }

    private void initViews() {
        rvMessages = findViewById(R.id.rv_messenger);
        etMessage = findViewById(R.id.et_messenger);
        btnSend = findViewById(R.id.bt_sendMessage);
        // Use the messenger title TextView for partner name display
        tvPartnerName = findViewById(R.id.tv_messenger);
        loadingOverlay = findViewById(R.id.loading_overlay);

        // Nút back mới
        View backBtn = findViewById(R.id.btn_back_messenger);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> onBackPressed());
        }

        chatManager = ChatManager.getInstance();
        databaseManager = DatabaseManager.getInstance();
        mAuth = FirebaseAuth.getInstance();
        messageList = new ArrayList<>();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            // Get name from user data or use display name
            databaseManager.getUser(currentUserId, new DatabaseManager.DatabaseCallback<>() {
                @Override
                public void onSuccess(com.example.btl_mobileapp.models.User user) {
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

        // Nếu có sẵn partnerName thì hiển thị, nếu không sẽ hiển thị sau khi fetch
        if (tvPartnerName != null && partnerName != null && !partnerName.isEmpty()) {
            tvPartnerName.setText(partnerName);
        }
    }

    private void ensureCoupleThenLoadMessages() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Nếu chưa có coupleId/partnerName, fetch từ DB trước
        if (coupleId == null || coupleId.isEmpty() || partnerName == null || partnerName.isEmpty()) {
            databaseManager.getCoupleByUserId(currentUser.getUid(), new DatabaseManager.DatabaseCallback<>() {
                @Override
                public void onSuccess(Couple couple) {
                    coupleId = couple.getCoupleId();
                    String uid = currentUser.getUid();
                    partnerId = couple.getUser1Id().equals(uid) ? couple.getUser2Id() : couple.getUser1Id();

                    // Lấy tên đối phương
                    databaseManager.getUser(partnerId, new DatabaseManager.DatabaseCallback<>() {
                        @Override
                        public void onSuccess(User partner) {
                            partnerName = partner != null && partner.getName() != null ? partner.getName() : "Partner";
                            if (tvPartnerName != null) tvPartnerName.setText(partnerName);
                            // Sau khi có đủ info, load messages
                            loadMessages();
                        }

                        @Override
                        public void onError(String error) {
                            partnerName = "Partner";
                            if (tvPartnerName != null) tvPartnerName.setText(partnerName);
                            loadMessages();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    showLoading(false);
                    Toast.makeText(MessengerActivity.this, "You need to pair with someone first", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        } else {
            // Đã có đủ info từ intent, load luôn
            loadMessages();
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

        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }

    private void loadMessages() {
        if (coupleId == null || coupleId.isEmpty()) {
            Toast.makeText(this, "Error: Couple ID not found", Toast.LENGTH_SHORT).show();
            showLoading(false);
            return;
        }

        showLoading(true);

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

                showLoading(false);
            }

            @Override
            public void onMessageSent() {
                // Not used here
            }

            @Override
            public void onError(String error) {
                Log.e("MessengerActivity", "Error loading chat history: " + error);
                Toast.makeText(MessengerActivity.this, "Error loading messages: " + error, Toast.LENGTH_SHORT).show();
                showLoading(false);
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
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        if (coupleId == null || coupleId.isEmpty()) {
            Toast.makeText(this, "Error: Couple ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "Error: User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable send button to prevent multiple sends
        btnSend.setEnabled(false);

        // Send message using ChatManager
        chatManager.sendMessage(coupleId, currentUserId, messageText, new ChatManager.ChatCallback() {
            @Override
            public void onMessagesReceived(List<ChatMessage> messages) {
                // Not used here
            }

            @Override
            public void onMessageSent() {
                // Clear input field and re-enable send button
                runOnUiThread(() -> {
                    etMessage.setText("");
                    btnSend.setEnabled(true);
                });
                Log.d("MessengerActivity", "Message sent successfully");
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MessengerActivity.this, "Failed to send message: " + error, Toast.LENGTH_SHORT).show();
                    btnSend.setEnabled(true);
                });
                Log.e("MessengerActivity", "Error sending message: " + error);
            }
        });
    }

    private void showLoading(boolean show) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove real-time listener to prevent memory leaks
        if (messageListener != null && coupleId != null) {
            chatManager.removeChildMessageListener(coupleId, messageListener);
        }
    }
}