package com.example.btl_mobileapp.activities;

import android.app.Dialog;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btl_mobileapp.R;
import com.example.btl_mobileapp.adapters.MessageAdapter;
import com.example.btl_mobileapp.managers.ChatManager;
import com.example.btl_mobileapp.managers.DatabaseManager;
import com.example.btl_mobileapp.models.ChatMessage;
import com.example.btl_mobileapp.models.Message;
import com.example.btl_mobileapp.models.User;
import com.example.btl_mobileapp.utils.AvatarCache;
import com.example.btl_mobileapp.utils.NotificationAPI;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.hdodenhof.circleimageview.CircleImageView; // Thêm import này

public class ChatBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String TAG = "ChatBottomSheet";
    private static final ExecutorService IMAGE_EXECUTOR = Executors.newFixedThreadPool(2);

    // ✅ Static flag để track xem Chat có đang active không
    private static boolean isChatActive = false;
    private static String activeChatCoupleId = null;

    // --- Các View ---
    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend;
    private TextView tvPartnerName;
    private View loadingOverlay;
    private CircleImageView ivPartnerAvatar; // Đổi tên từ ivMessageAvatar

    // --- Các Manager và Dữ liệu ---
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private ChatManager chatManager;
    private DatabaseManager databaseManager;
    private FirebaseAuth mAuth;
    private ChildEventListener messageListener;

    // --- Dữ liệu được truyền vào ---
    private String coupleId;
    private String partnerId;
    private String partnerName;
    private String currentUserId;
    private String currentUserName;
    private User partner; // Đối tượng User của partner

    // --- Constructor để truyền dữ liệu (Giống code cũ của bạn) ---
    public static ChatBottomSheetFragment newInstance(String coupleId, String partnerId, String partnerName, User partner) {
        ChatBottomSheetFragment fragment = new ChatBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString("coupleId", coupleId);
        args.putString("partnerId", partnerId);
        args.putString("partnerName", partnerName);
        args.putSerializable("partner", partner); // Truyền đối tượng User
        fragment.setArguments(args);
        return fragment;
    }

    // (TÙY CHỌN) Thêm style để có bo góc
    @Override
    public int getTheme() {
        return R.style.BottomSheetDialogTheme; // Cần định nghĩa style này trong themes.xml
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- THAY THẾ CHO getIntentData() ---
        // Lấy dữ liệu từ Arguments
        if (getArguments() != null) {
            coupleId = getArguments().getString("coupleId");
            partnerId = getArguments().getString("partnerId");
            partnerName = getArguments().getString("partnerName");
            partner = (User) getArguments().getSerializable("partner");
        } else {
            // Nếu không có arguments, đóng fragment
            Toast.makeText(getContext(), "Lỗi: Không có dữ liệu chat.", Toast.LENGTH_SHORT).show();
            dismiss();
        }

        // Khởi tạo các manager và list (Giống hệt MessengerActivity)
        initManagersAndData();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Sử dụng layout XML của fragment
        return inflater.inflate(R.layout.fragment_chat_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // --- Đây là nơi logic của Activity.onCreate() được chuyển đến ---
        initViews(view);
        setupRecyclerView();
        setupClickListeners();

        // Lấy và lưu FCM token (Giống hệt)
        registerFCMToken();

        // --- LOGIC MỚI (ĐƠN GIẢN HƠN) ---
        // Không cần ensureCoupleThenLoadMessages() nữa
        // vì chúng ta đã có đủ dữ liệu từ Arguments
        showLoading(true);
        loadReceiverDetails(); // Hiển thị tên và avatar
        loadMessages(); // Tải tin nhắn
    }

    // --- TOÀN BỘ LOGIC BÊN DƯỚI LÀ COPY TỪ MESSENGERACTIVITY VÀ SỬA LẠI ---

    // SỬA LẠI: Dùng 'view.findViewById'
    private void initViews(View view) {
        // Sử dụng ID từ file 'fragment_chat_bottom_sheet.xml'
        rvMessages = view.findViewById(R.id.rv_chat_messages);
        etMessage = view.findViewById(R.id.et_chat_message);
        btnSend = view.findViewById(R.id.btn_chat_send);
        tvPartnerName = view.findViewById(R.id.tv_chat_name);
        ivPartnerAvatar = view.findViewById(R.id.iv_chat_avatar);
        loadingOverlay = view.findViewById(R.id.loading_overlay); // Cần thêm ID này vào XML


    }

    // Giữ nguyên từ MessengerActivity
    private void initManagersAndData() {
        chatManager = ChatManager.getInstance();
        databaseManager = DatabaseManager.getInstance();
        mAuth = FirebaseAuth.getInstance();
        messageList = new ArrayList<>();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            // Lấy tên người dùng hiện tại
            databaseManager.getUser(currentUserId, new DatabaseManager.DatabaseCallback<>() {
                @Override
                public void onSuccess(User user) {
                    currentUserName = user.getName() != null ? user.getName() : "You";
                }
                @Override
                public void onError(String error) {
                    currentUserName = "You";
                }
            });
        }
    }

    // HÀM MỚI: (Tách ra từ ensureCouple...)
    private void loadReceiverDetails() {
        // Hiển thị tên
        if (tvPartnerName != null && partnerName != null) {
            tvPartnerName.setText(partnerName);
        }

        // Load ảnh đại diện
        loadPartnerAvatar(partner);
    }

    // SỬA LẠI: Dùng requireContext()
    private void loadPartnerAvatar(User partner) {
        if (partner == null || ivPartnerAvatar == null) {
            setDefaultAvatar();
            return;
        }

        // Thử load từ cache trước
        Bitmap cachedPartnerAvatar = AvatarCache.getPartnerCachedBitmap(requireContext());
        if (cachedPartnerAvatar != null) {
            ivPartnerAvatar.setImageBitmap(cachedPartnerAvatar);
            if (messageAdapter != null) messageAdapter.notifyDataSetChanged();
            return;
        }

        // Nếu không có cache, load từ URL
        String profilePicUrl = partner.getProfilePicUrl();
        if (!TextUtils.isEmpty(profilePicUrl)) {
            loadImageAsync(profilePicUrl, bmp -> {
                if (bmp != null && ivPartnerAvatar != null) {
                    ivPartnerAvatar.setImageBitmap(bmp);
                    // Lưu vào cache
                    AvatarCache.savePartnerBitmapToCache(requireContext(), bmp);
                    if (messageAdapter != null) messageAdapter.notifyDataSetChanged();
                } else {
                    setDefaultAvatar();
                }
            });
        } else {
            setDefaultAvatar();
        }
    }

    // Giữ nguyên
    private void setDefaultAvatar() {
        if (ivPartnerAvatar != null) {
            ivPartnerAvatar.setImageResource(R.drawable.ic_default_avatar);
        }
    }

    // SỬA LẠI: Dùng getActivity().runOnUiThread()
    private void loadImageAsync(String urlStr, BitmapCallback callback) {
        if (TextUtils.isEmpty(urlStr)) {
            if (callback != null) callback.onBitmap(null);
            return;
        }

        IMAGE_EXECUTOR.execute(() -> {
            Bitmap bmp = null;
            // ... (Logic tải ảnh y hệt) ...
            try {
                URL url = new URL(urlStr);
                // ...
                try (InputStream is = ((HttpURLConnection) url.openConnection()).getInputStream()) {
                    bmp = BitmapFactory.decodeStream(is);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading image: " + e.getMessage());
            }

            final Bitmap result = bmp;
            if (getActivity() != null) { // <-- Sửa lại
                getActivity().runOnUiThread(() -> {
                    if (callback != null) callback.onBitmap(result);
                });
            }
        });
    }

    // Giữ nguyên
    private interface BitmapCallback {
        void onBitmap(Bitmap bmp);
    }

    // SỬA LẠI: Dùng requireContext()
    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter(messageList, requireContext());
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(messageAdapter);
    }

    // Giữ nguyên
    private void setupClickListeners() {
        btnSend.setOnClickListener(v -> sendMessage());
        // (Không cần onEditorActionListener cho BottomSheet)
    }

    // SỬA LẠI: Dùng getContext()
    private void loadMessages() {
        if (coupleId == null || coupleId.isEmpty()) {
            Toast.makeText(getContext(), "Error: Couple ID not found", Toast.LENGTH_SHORT).show();
            showLoading(false);
            return;
        }

        // showLoading(true) đã được gọi ở onViewCreated

        // Load chat history
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

                // Setup real-time listener
                setupRealTimeListener();
                showLoading(false);
            }
            @Override
            public void onMessageSent() {}
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading chat history: " + error);
                Toast.makeText(getContext(), "Error loading messages: " + error, Toast.LENGTH_SHORT).show();
                showLoading(false);
            }
        });
    }

    // Giữ nguyên
    private void setupRealTimeListener() {
        long lastTimestamp = messageList.isEmpty() ? 0 :
                getTimestampFromMessage(messageList.get(messageList.size() - 1));

        messageListener = chatManager.listenForNewMessagesStream(coupleId, lastTimestamp, new ChatManager.MessageListener() {
            @Override
            public void onNewMessage(ChatMessage chatMessage) {
                if (isMessageAlreadyExists(chatMessage.getMessageId())) return;
                Message message = convertChatMessageToMessage(chatMessage);
                messageList.add(message);
                messageAdapter.updateMessages(messageList);
                rvMessages.smoothScrollToPosition(messageList.size() - 1);
            }
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error listening for new messages: " + error);
            }
        });
    }

    // Giữ nguyên
    private boolean isMessageAlreadyExists(String messageId) {
        if (messageId == null) return false;
        for (Message msg : messageList) {
            if (messageId.equals(msg.getMessageId())) return true;
        }
        return false;
    }

    // Giữ nguyên
    private Message convertChatMessageToMessage(ChatMessage chatMessage) {
        Message message = new Message();
        // ... (Logic y hệt) ...
        message.setMessageId(chatMessage.getMessageId());
        message.setSenderId(chatMessage.getSenderId());
        message.setMessage(chatMessage.getMessage());
        message.setTimestamp(chatMessage.getTimestamp());
        message.setCoupleId(coupleId);
        message.setSenderName(Objects.equals(chatMessage.getSenderId(), currentUserId) ? currentUserName : partnerName);
        message.setMessageType("text");
        return message;
    }

    // Giữ nguyên
    private long getTimestampFromMessage(Message message) {
        Object timestamp = message.getTimestamp();
        if (timestamp instanceof Long) return (Long) timestamp;
        if (timestamp instanceof Double) return ((Double) timestamp).longValue();
        return 0L;
    }

    // SỬA LẠI: Dùng getContext() và getActivity().runOnUiThread()
    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(messageText)) {
            Toast.makeText(getContext(), "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }
        if (coupleId == null || currentUserId == null) {
            Toast.makeText(getContext(), "Error: User/Couple info missing", Toast.LENGTH_SHORT).show();
            return;
        }
        btnSend.setEnabled(false);

        chatManager.sendMessage(coupleId, currentUserId, messageText, new ChatManager.ChatCallback() {
            @Override
            public void onMessageSent() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> { // <-- Sửa lại
                        etMessage.setText("");
                        btnSend.setEnabled(true);
                        if (partnerId != null) {
                            sendNotificationToPartner(messageText);
                        }
                    });
                }
            }
            @Override
            public void onMessagesReceived(List<ChatMessage> messages) {}
            @Override
            public void onError(String error) {
                if (getActivity() != null) { // <-- Sửa lại
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Failed to send message: " + error, Toast.LENGTH_SHORT).show();
                        btnSend.setEnabled(true);
                    });
                }
            }
        });
    }

    // Giữ nguyên
    private void sendNotificationToPartner(String messageText) {
        String title = currentUserName != null ? currentUserName : "New Message";
        NotificationAPI.sendNotification(
                partnerId, title, messageText, coupleId, currentUserId, currentUserName,
                new NotificationAPI.NotificationCallback() {
                    @Override public void onSuccess(String response) { Log.d(TAG, "✅ Notif sent: " + response); }
                    @Override public void onError(String error) { Log.e(TAG, "⚠️ Notif failed: " + error); }
                }
        );
    }

    // Giữ nguyên
    private void showLoading(boolean show) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    // Giữ nguyên
    private void registerFCMToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM token failed", task.getException());
                        return;
                    }
                    String token = task.getResult();
                    Log.d(TAG, "FCM Token: " + token);
                    if (currentUserId != null) {
                        databaseManager.updateUserFcmToken(currentUserId, token, new DatabaseManager.DatabaseActionCallback() {
                            @Override public void onSuccess() { Log.d(TAG, "FCM token saved"); }
                            @Override public void onError(String error) { Log.e(TAG, "FCM token save failed: " + error); }
                        });
                    }
                });
    }

    // --- CÁC PHƯƠNG THỨC LIFECYCLE VÀ STATIC (Giữ nguyên) ---

    @Override
    public void onResume() {
        super.onResume();
        isChatActive = true;
        activeChatCoupleId = coupleId;
        updateChatViewingStatus(true);
        Log.d(TAG, "🟢 Chat Fragment is now ACTIVE");
    }

    @Override
    public void onPause() {
        super.onPause();
        isChatActive = false;
        activeChatCoupleId = null;
        updateChatViewingStatus(false);
        Log.d(TAG, "🔴 Chat Fragment is now INACTIVE");
    }

    // Giữ nguyên
    private void updateChatViewingStatus(boolean isViewing) {
        if (currentUserId != null && coupleId != null) {
            databaseManager.updateUserChatViewingStatus(currentUserId, isViewing ? coupleId : null,
                    new DatabaseManager.DatabaseActionCallback() {
                        @Override public void onSuccess() { Log.d(TAG, "✅ Chat viewing status updated: " + isViewing); }
                        @Override public void onError(String error) { Log.e(TAG, "❌ Failed update status: " + error); }
                    });
        }
    }

    // Đổi tên cho nhất quán (từ isViewingChat)
    public static boolean isChatFragmentActive(String coupleId) {
        return isChatActive && coupleId != null && coupleId.equals(activeChatCoupleId);
    }

    // Đổi sang onDestroyView() để dọn dẹp view
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Gỡ bỏ listener khi view bị hủy
        if (messageListener != null && coupleId != null) {
            chatManager.removeChildMessageListener(coupleId, messageListener);
            messageListener = null;
        }
    }

    // Giữ nguyên code để kiểm soát chiều cao BottomSheet (từ file cũ của bạn)
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), getTheme());
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
            FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
                int desiredHeight = (int) (screenHeight * 0.85); // Cao 85%
                ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
                layoutParams.height = desiredHeight;
                bottomSheet.setLayoutParams(layoutParams);
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setDraggable(true); // Cho phép kéo
            }
        });
        return dialog;
    }
}