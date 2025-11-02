package com.example.couple_app.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
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
import com.example.couple_app.utils.NotificationAPI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessengerActivity extends BaseActivity {
    private static final ExecutorService IMAGE_EXECUTOR = Executors.newFixedThreadPool(2);

    // ✅ Static flag để track xem MessengerActivity có đang active không
    private static boolean isMessengerActive = false;
    private static String activeMessengerCoupleId = null;

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
            // Kiểm tra xem có phải là sự kiện gửi tin nhắn không
            if (actionId == EditorInfo.IME_ACTION_SEND || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                sendMessage();
                return true;
            }
            return false;
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

                // ✅ Nếu tin nhắn từ người khác và Activity đang active → Đánh dấu đã đọc ngay lập tức
                if (!chatMessage.getSenderId().equals(currentUserId) && isMessengerActive) {
                    markMessageAsReadImmediately(chatMessage.getMessageId());
                }
            }

            @Override
            public void onError(String error) {
                Log.e("MessengerActivity", "Error listening for new messages: " + error);
            }
        });

        // ✅ Thêm listener để cập nhật trạng thái đã đọc real-time
        setupReadStatusListener();
    }

    /**
     * ✅ Đánh dấu tin nhắn đã đọc ngay lập tức khi nhận được (nếu đang mở chat)
     */
    private void markMessageAsReadImmediately(String messageId) {
        if (coupleId != null && messageId != null) {
            chatManager.markMessageAsRead(coupleId, messageId, new ChatManager.MessageReadCallback() {
                @Override
                public void onSuccess() {
                    Log.d("MessengerActivity", "✅ Message marked as read immediately: " + messageId);
                    // Cập nhật trong danh sách local
                    for (Message msg : messageList) {
                        if (msg.getMessageId().equals(messageId)) {
                            msg.setRead(true);
                            msg.setReadAt(System.currentTimeMillis());
                            break;
                        }
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e("MessengerActivity", "❌ Failed to mark message as read: " + error);
                }
            });
        }
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

        // ✅ Đọc trạng thái đã đọc trực tiếp từ ChatMessage (Firebase đã parse sẵn)
        message.setRead(chatMessage.isRead());
        message.setReadAt(chatMessage.getReadAt());

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

        // Gửi tin nhắn qua ChatManager (Firebase)
        chatManager.sendMessage(coupleId, currentUserId, messageText, new ChatManager.ChatCallback() {
            @Override
            public void onMessagesReceived(List<ChatMessage> messages) {
                // Not used here
            }

            @Override
            public void onMessageSent() {
                runOnUiThread(() -> {
                    etMessage.setText("");
                    btnSend.setEnabled(true);

                    // ✅ SAU KHI GỬI TIN NHẮN THÀNH CÔNG, GỬI NOTIFICATION QUA BACKEND
                    if (partnerId != null && !partnerId.isEmpty()) {
                        sendNotificationToPartner(messageText);
                    }
                });
                Log.d("MessengerActivity", "✅ Message sent via ChatManager");
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

    /**
     * Gửi notification đến partner qua backend API
     */
    private void sendNotificationToPartner(String messageText) {
        String title = currentUserName != null ? currentUserName : "New Message";

        NotificationAPI.sendNotification(
            partnerId,
            title,
            messageText,
            coupleId,
            currentUserId,
            currentUserName,
            new NotificationAPI.NotificationCallback() {
                @Override
                public void onSuccess(String response) {
                    Log.d("MessengerActivity", "✅ Notification sent successfully: " + response);
                }

                @Override
                public void onError(String error) {
                    Log.e("MessengerActivity", "⚠️ Failed to send notification: " + error);

                    // ✅ Kiểm tra nếu lỗi là do partner chưa có FCM token
                    if (error.contains("does not have FCM token") || error.contains("400")) {
                        Log.w("MessengerActivity", "💡 Partner needs to login on their device to receive push notifications");
                        // Không hiển thị lỗi cho user vì tin nhắn đã được gửi thành công
                        // Partner sẽ nhận được tin nhắn khi họ mở app
                    }
                    // Notification chỉ là bonus feature, không ảnh hưởng đến việc gửi tin nhắn
                }
            }
        );
    }

    private void showLoading(boolean show) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        // ✅ Đánh dấu MessengerActivity đang active
        isMessengerActive = true;
        activeMessengerCoupleId = coupleId;

        // ✅ Ensure FCM token is registered (in case it wasn't registered before)
        ensureFCMTokenRegistered();

        // ✅ Cập nhật trạng thái "đang xem chat" lên Firestore
        updateChatViewingStatus(true);

        // ✅ Đánh dấu tất cả tin nhắn chưa đọc là đã đọc
        markAllMessagesAsReadInView();

        Log.d("MessengerActivity", "🟢 Messenger is now ACTIVE - notifications will be suppressed");
    }

    /**
     * ✅ Đánh dấu tất cả tin nhắn chưa đọc là đã đọc khi xem chat
     */
    private void markAllMessagesAsReadInView() {
        if (coupleId != null && currentUserId != null) {
            chatManager.markAllMessagesAsRead(coupleId, currentUserId, new ChatManager.MessageReadCallback() {
                @Override
                public void onSuccess() {
                    Log.d("MessengerActivity", "✅ All unread messages marked as read");
                    // Cập nhật UI để hiển thị trạng thái đã đọc
                    updateMessageReadStatusInList();
                }

                @Override
                public void onError(String error) {
                    Log.e("MessengerActivity", "❌ Failed to mark messages as read: " + error);
                }
            });
        }
    }

    /**
     * ✅ Cập nhật trạng thái đã đọc trong danh sách tin nhắn
     */
    private void updateMessageReadStatusInList() {
        boolean updated = false;
        for (Message message : messageList) {
            // Chỉ cập nhật tin nhắn từ người khác
            if (!message.getSenderId().equals(currentUserId) && !message.isRead()) {
                message.setRead(true);
                message.setReadAt(System.currentTimeMillis());
                updated = true;
            }
        }

        if (updated) {
            runOnUiThread(() -> messageAdapter.notifyDataSetChanged());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // ✅ Đánh dấu MessengerActivity không còn active
        isMessengerActive = false;
        activeMessengerCoupleId = null;

        // ✅ Cập nhật trạng thái "không xem chat" lên Firestore
        updateChatViewingStatus(false);

        Log.d("MessengerActivity", "🔴 Messenger is now INACTIVE - notifications will be shown");
    }

    /**
     * ✅ Cập nhật trạng thái đang xem chat lên Firestore
     */
    private void updateChatViewingStatus(boolean isViewing) {
        if (currentUserId != null && coupleId != null) {
            databaseManager.updateUserChatViewingStatus(currentUserId, isViewing ? coupleId : null,
                new DatabaseManager.DatabaseActionCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d("MessengerActivity", "✅ Chat viewing status updated: " + isViewing);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("MessengerActivity", "❌ Failed to update chat viewing status: " + error);
                    }
                });
        }
    }

    /**
     * ✅ Kiểm tra xem user có đang xem chat với coupleId này không (static method)
     */
    public static boolean isViewingChat(String coupleId) {
        return isMessengerActive && coupleId != null && coupleId.equals(activeMessengerCoupleId);
    }

    /**
     * ✅ Ensure FCM token is registered for the current user
     */
    private void ensureFCMTokenRegistered() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.w("MessengerActivity", "Cannot register FCM token: userId is null");
            return;
        }

        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.w("MessengerActivity", "❌ Failed to get FCM token", task.getException());
                    return;
                }

                String token = task.getResult();
                if (token != null && !token.isEmpty()) {
                    Log.d("MessengerActivity", "✅ FCM Token retrieved: " + token.substring(0, Math.min(20, token.length())) + "...");

                    // Save token to Firestore
                    databaseManager.updateUserFcmToken(currentUserId, token, new DatabaseManager.DatabaseActionCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d("MessengerActivity", "✅ FCM token registered successfully");
                        }

                        @Override
                        public void onError(String error) {
                            Log.e("MessengerActivity", "❌ Failed to register FCM token: " + error);
                        }
                    });
                } else {
                    Log.w("MessengerActivity", "⚠️ FCM token is null or empty");
                }
            });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ✅ Clear trạng thái khi destroy
        isMessengerActive = false;
        activeMessengerCoupleId = null;

        // Remove real-time listener to prevent memory leaks
        if (messageListener != null && coupleId != null) {
            chatManager.removeChildMessageListener(coupleId, messageListener);
        }
    }

    /**
     * ✅ Lắng nghe thay đổi trạng thái đã đọc của tất cả tin nhắn
     */
    private void setupReadStatusListener() {
        chatManager.listenForReadStatusChanges(coupleId, currentUserId, new ChatManager.ReadStatusChangeListener() {
            @Override
            public void onReadStatusChanged(String messageId, boolean isRead, Object readAt) {
                Log.d("MessengerActivity", "🟢 Received read status change: messageId=" + messageId + ", isRead=" + isRead);

                // Tìm và cập nhật tin nhắn trong danh sách
                runOnUiThread(() -> {
                    boolean found = false;
                    for (int i = 0; i < messageList.size(); i++) {
                        Message msg = messageList.get(i);
                        if (msg.getMessageId() != null && msg.getMessageId().equals(messageId)) {
                            Log.d("MessengerActivity", "🔵 Found message at position " + i + ", updating read status from " + msg.isRead() + " to " + isRead);
                            msg.setRead(isRead);
                            msg.setReadAt(readAt);
                            messageAdapter.notifyItemChanged(i);
                            found = true;
                            Log.d("MessengerActivity", "✅ Updated read status for message: " + messageId + " isRead=" + isRead);
                            break;
                        }
                    }
                    if (!found) {
                        Log.w("MessengerActivity", "⚠️ Message not found in list: " + messageId);
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e("MessengerActivity", "❌ Error listening for read status changes: " + error);
            }
        });
    }
}
