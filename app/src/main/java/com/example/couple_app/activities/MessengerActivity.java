package com.example.couple_app.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.couple_app.R;
import com.example.couple_app.adapters.MessageAdapter;
import com.example.couple_app.managers.ChatManager;
import com.example.couple_app.managers.DatabaseManager;
import com.example.couple_app.models.ChatMessage;
import com.example.couple_app.models.Message;
import com.example.couple_app.models.Couple;
import com.example.couple_app.models.User;
import com.example.couple_app.utils.AvatarCache;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessengerActivity extends BaseActivity {
    private static final ExecutorService IMAGE_EXECUTOR = Executors.newFixedThreadPool(2);

    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend;
    private TextView tvPartnerName;
    private View loadingOverlay;
    private ImageView ivMessageAvatar;

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

    // Ẩn thanh header và menu ở màn hình message
    @Override
    protected boolean shouldShowBottomBar() {
        return false;
    }

    @Override
    protected boolean shouldShowHeader() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.messenger);

        initViews();
        getIntentData();
        setupRecyclerView();
        setupClickListeners();

        // Lấy và lưu FCM token
        registerFCMToken();

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
        ivMessageAvatar = findViewById(R.id.iv_messenger_avatar);

        // Nút back mới - chỉnh về HomeMainActivity
        View backBtn = findViewById(R.id.btn_back_messenger);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> {
                Intent intent = new Intent(MessengerActivity.this, HomeMainActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                overridePendingTransition(0, 0);
            });
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

        // Thử load ảnh từ cache ngay lập tức (trước khi fetch data)
        loadAvatarFromCacheFirst();

        // Nếu chưa có coupleId/partnerName, fetch từ DB trước
        if (coupleId == null || coupleId.isEmpty() || partnerName == null || partnerName.isEmpty()) {
            databaseManager.getCoupleByUserId(currentUser.getUid(), new DatabaseManager.DatabaseCallback<>() {
                @Override
                public void onSuccess(Couple couple) {
                    coupleId = couple.getCoupleId();
                    String uid = currentUser.getUid();
                    partnerId = couple.getUser1Id().equals(uid) ? couple.getUser2Id() : couple.getUser1Id();

                    // Lấy thông tin đối phương
                    databaseManager.getUser(partnerId, new DatabaseManager.DatabaseCallback<>() {
                        @Override
                        public void onSuccess(User partner) {
                            partnerName = partner != null && partner.getName() != null ? partner.getName() : "Partner";
                            if (tvPartnerName != null) tvPartnerName.setText(partnerName);

                            // Load ảnh đối phương (sẽ dùng cache nếu có)
                            loadPartnerAvatar(partner);

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
            // Đã có đủ info từ intent, load ảnh và messages
            if (partnerId != null && !partnerId.isEmpty()) {
                databaseManager.getUser(partnerId, new DatabaseManager.DatabaseCallback<>() {
                    @Override
                    public void onSuccess(User partner) {
                        loadPartnerAvatar(partner);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("MessengerActivity", "Error loading partner info: " + error);
                        // Vẫn hiển thị ảnh mặc định nếu có lỗi
                        setDefaultAvatar();
                    }
                });
            }
            loadMessages();
        }
    }

    /**
     * Load ảnh từ cache trước (gọi ngay khi activity khởi tạo)
     */
    private void loadAvatarFromCacheFirst() {
        if (ivMessageAvatar == null) return;

        Bitmap cachedPartnerAvatar = AvatarCache.getPartnerCachedBitmap(this);
        if (cachedPartnerAvatar != null) {
            ivMessageAvatar.setImageBitmap(cachedPartnerAvatar);
            Log.d("MessengerActivity", "Loaded partner avatar from cache (early)");
        } else {
            // Hiển thị ảnh mặc định tạm thời
            setDefaultAvatar();
        }
    }

    /**
     * Set ảnh mặc định
     */
    private void setDefaultAvatar() {
        if (ivMessageAvatar != null) {
            ivMessageAvatar.setImageResource(R.drawable.ic_default_avatar);
        }
    }

    /**
     * Load ảnh đối phương với cache (giống HomeMain1Fragment)
     */
    private void loadPartnerAvatar(User partner) {
        if (partner == null || ivMessageAvatar == null) {
            setDefaultAvatar();
            return;
        }

        // Thử load từ cache trước
        Bitmap cachedPartnerAvatar = AvatarCache.getPartnerCachedBitmap(this);
        if (cachedPartnerAvatar != null) {
            ivMessageAvatar.setImageBitmap(cachedPartnerAvatar);
            Log.d("MessengerActivity", "Loaded partner avatar from cache");
            return;
        }

        // Nếu không có cache, load từ URL
        String profilePicUrl = partner.getProfilePicUrl();
        if (!TextUtils.isEmpty(profilePicUrl)) {
            loadImageAsync(profilePicUrl, bmp -> {
                if (bmp != null && ivMessageAvatar != null) {
                    ivMessageAvatar.setImageBitmap(bmp);
                    // Lưu vào cache để dùng lại
                    AvatarCache.savePartnerBitmapToCache(MessengerActivity.this, bmp);
                    Log.d("MessengerActivity", "Loaded and cached partner avatar from URL");
                } else {
                    // Fallback to default avatar
                    setDefaultAvatar();
                }
            });
        } else {
            // Không có URL, hiển thị ảnh mặc định
            setDefaultAvatar();
        }
    }

    /**
     * Callback interface cho load ảnh async
     */
    private interface BitmapCallback {
        void onBitmap(Bitmap bmp);
    }

    /**
     * Load ảnh async từ URL (giống HomeMain1Fragment)
     */
    private void loadImageAsync(String urlStr, BitmapCallback callback) {
        if (TextUtils.isEmpty(urlStr)) {
            if (callback != null) callback.onBitmap(null);
            return;
        }

        IMAGE_EXECUTOR.execute(() -> {
            Bitmap bmp = null;
            HttpURLConnection conn = null;
            try {
                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(20000);
                conn.setInstanceFollowRedirects(true);
                conn.connect();
                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    try (InputStream is = conn.getInputStream()) {
                        bmp = BitmapFactory.decodeStream(is);
                    }
                }
            } catch (Exception e) {
                Log.e("MessengerActivity", "Error loading image: " + e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }

            final Bitmap result = bmp;
            runOnUiThread(() -> {
                if (callback != null) callback.onBitmap(result);
            });
        });
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
                // Kiểm tra duplicate: Không thêm nếu đã có messageId này
                if (isMessageAlreadyExists(chatMessage.getMessageId())) {
                    Log.d("MessengerActivity", "Skipping duplicate message: " + chatMessage.getMessageId());
                    return;
                }

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

    /**
     * Kiểm tra xem message đã tồn tại trong list chưa
     */
    private boolean isMessageAlreadyExists(String messageId) {
        if (messageId == null || messageId.isEmpty()) {
            return false;
        }

        for (Message msg : messageList) {
            if (messageId.equals(msg.getMessageId())) {
                return true;
            }
        }
        return false;
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
                Log.d("MessengerActivity", "✅ Message sent successfully");

                // ✅ CLOUD FUNCTION sẽ TỰ ĐỘNG gửi notification
                // Khi message được lưu vào Firestore, Cloud Function sẽ:
                // 1. Phát hiện message mới
                // 2. Lấy FCM token của người nhận
                // 3. Gửi notification qua FCM API v1
                // KHÔNG CẦN code thêm gì ở đây!
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

    /**
     * Đăng ký FCM token cho thiết bị hiện tại
     */
    private void registerFCMToken() {
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.w("MessengerActivity", "Fetching FCM registration token failed", task.getException());
                    return;
                }

                // Get new FCM registration token
                String token = task.getResult();
                Log.d("MessengerActivity", "FCM Token: " + token);

                // Save token to Firestore
                if (currentUserId != null) {
                    databaseManager.updateUserFcmToken(currentUserId, token, new DatabaseManager.DatabaseActionCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d("MessengerActivity", "FCM token saved successfully");
                        }

                        @Override
                        public void onError(String error) {
                            Log.e("MessengerActivity", "Failed to save FCM token: " + error);
                        }
                    });
                }
            });
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
