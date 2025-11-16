package com.example.couple_app.ui.fragments;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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

import com.example.couple_app.R;
import com.example.couple_app.ui.adapters.MessageAdapter;
import com.example.couple_app.managers.ChatManager;
import com.example.couple_app.data.local.DatabaseManager;
import com.example.couple_app.data.model.ChatMessage;
import com.example.couple_app.data.model.Message;
import com.example.couple_app.data.model.User;
import com.example.couple_app.utils.AvatarCache;
import com.example.couple_app.utils.NotificationAPI;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Bottom sheet dialog fragment for chat messages
 * Shows partner info, messages, and input field
 */
public class ChatBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String TAG = "ChatBottomSheet";
    private static final String ARG_PARTNER_ID = "partner_id";
    private static final String ARG_PARTNER_NAME = "partner_name";
    private static final String ARG_PARTNER_AVATAR = "partner_avatar";
    private static final String ARG_COUPLE_ID = "couple_id";
    private static final float BOTTOM_SHEET_PEEK_HEIGHT_RATIO = 0.90f;
    private static final int MAX_MESSAGES_TO_LOAD = 20; // Load only 20 recent messages
    private static final ExecutorService IMAGE_EXECUTOR = Executors.newFixedThreadPool(2);

    // UI Components
    private CircleImageView ivChatAvatar;
    private TextView tvChatName;
    private TextView tvPartnerStatus;
    private View viewOnlineIndicator;
    private RecyclerView rvChatMessages;
    private EditText etChatMessage;
    private ImageButton btnChatSend;
    private View loadingContainer;
    private android.widget.ProgressBar progressLoading;
    private TextView tvLoadingText;

    // Data
    private String partnerId;
    private String partnerName;
    private String partnerAvatarUrl;
    private String coupleId;
    private String currentUserId;
    private String currentUserName;

    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private ChatManager chatManager;
    private DatabaseManager databaseManager;
    private ChildEventListener messageListener;
    private com.google.firebase.database.DatabaseReference chatRef; // Store reference to remove listener

    // Pagination
    private boolean isLoadingMore = false;
    private boolean hasMoreMessages = true;
    private String oldestMessageId = null;
    private long oldestTimestamp = Long.MAX_VALUE;

    // Flag to auto-dismiss when fragment is restored by system (process death / app relaunch)
    private boolean shouldAutoDismissOnRestore = false;

    public static ChatBottomSheetFragment newInstance(String partnerId, String partnerName,
                                                      String partnerAvatar, String coupleId) {
        Log.d(TAG, "Creating new ChatBottomSheetFragment instance for partner: " + partnerName);
        ChatBottomSheetFragment fragment = new ChatBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARTNER_ID, partnerId);
        args.putString(ARG_PARTNER_NAME, partnerName);
        args.putString(ARG_PARTNER_AVATAR, partnerAvatar);
        args.putString(ARG_COUPLE_ID, coupleId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void show(@NonNull androidx.fragment.app.FragmentManager manager, @Nullable String tag) {
        // ✅ FIX: Don't add to back stack to prevent system restore
        // Using show() instead of commit() prevents fragment from being saved in back stack
        try {
            androidx.fragment.app.FragmentTransaction ft = manager.beginTransaction();
            ft.add(this, tag);
            ft.commitAllowingStateLoss(); // Use commitAllowingStateLoss to avoid state issues
            Log.d(TAG, "Fragment shown without adding to back stack");
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error showing fragment: " + e.getMessage());
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ FIX: Prevent auto-restore when app restarts
        // If fragment is being restored by system (not created by user action), dismiss it
        if (savedInstanceState != null) {
            Log.d(TAG, "Fragment being restored by system - will auto-dismiss to prevent auto-show");
            shouldAutoDismissOnRestore = true;
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        // Set window soft input mode to adjust resize for keyboard
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        }

        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);

            if (bottomSheet != null) {
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);

                // Set peek height to 90% of screen
                int screenHeight = getResources().getDisplayMetrics().heightPixels;
                int peekHeight = (int) (screenHeight * BOTTOM_SHEET_PEEK_HEIGHT_RATIO);
                behavior.setPeekHeight(peekHeight);

                // Set behavior properties
                behavior.setHideable(true);
                behavior.setSkipCollapsed(false);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

                // Set max height
                ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
                layoutParams.height = peekHeight;
                bottomSheet.setLayoutParams(layoutParams);
            }
        });

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.d(TAG, "📱 onViewCreated() called - Fragment ID: " + this.hashCode());

        // ✅ If restored by system, dismiss immediately to prevent auto-show
        if (shouldAutoDismissOnRestore) {
            Log.w(TAG, "⚠️ Fragment restored by system - dismissing to prevent auto-show");
            dismissAllowingStateLoss();
            return;
        }

        // ✅ IMPROVED: Restore state from savedInstanceState first, then fallback to arguments
        if (savedInstanceState != null) {
            partnerId = savedInstanceState.getString("partnerId");
            partnerName = savedInstanceState.getString("partnerName");
            partnerAvatarUrl = savedInstanceState.getString("partnerAvatarUrl");
            coupleId = savedInstanceState.getString("coupleId");
            currentUserId = savedInstanceState.getString("currentUserId");
            currentUserName = savedInstanceState.getString("currentUserName");
            Log.d(TAG, "✅ Restored state from savedInstanceState - coupleId: " + coupleId);
        }

        // ✅ FIX: If fragment is being restored without arguments, it's system restore - dismiss it
        if (getArguments() == null && savedInstanceState != null) {
            Log.w(TAG, "⚠️ Fragment restored by system without arguments - dismissing to prevent auto-show");
            dismissAllowingStateLoss(); // Dismiss immediately without showing
            return;
        }

        // ✅ Fallback to arguments if any data is missing (system may have killed the fragment)
        if (getArguments() != null) {
            if (partnerId == null) partnerId = getArguments().getString(ARG_PARTNER_ID);
            if (partnerName == null) partnerName = getArguments().getString(ARG_PARTNER_NAME);
            if (partnerAvatarUrl == null) partnerAvatarUrl = getArguments().getString(ARG_PARTNER_AVATAR);
            if (coupleId == null) coupleId = getArguments().getString(ARG_COUPLE_ID);
        }

        // ✅ Validate critical data
        if (coupleId == null || partnerId == null) {
            Log.e(TAG, "❌ CRITICAL: Missing coupleId or partnerId - fragment cannot function!");
            if (getContext() != null) {
                android.widget.Toast.makeText(getContext(), "Lỗi: Thiếu thông tin cần thiết", android.widget.Toast.LENGTH_SHORT).show();
            }
            dismissAllowingStateLoss(); // Close fragment if critical data is missing
            return;
        }

        // Initialize managers
        chatManager = ChatManager.getInstance();
        databaseManager = DatabaseManager.getInstance();

        // Get current user info if not restored
        if (currentUserId == null && FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            currentUserName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            if (currentUserName == null) currentUserName = "You";
        }

        initializeViews(view);
        setupRecyclerView();
        setupListeners();
        loadPartnerInfo();
        loadMessages();

        // Force refresh current user's online status when opening chat
        com.example.couple_app.managers.UserPresenceManager.getInstance().refreshOnlineStatus();

        listenToPartnerStatus();
    }

    private void initializeViews(View view) {
        ivChatAvatar = view.findViewById(R.id.iv_chat_avatar);
        tvChatName = view.findViewById(R.id.tv_chat_name);
        tvPartnerStatus = view.findViewById(R.id.tv_partner_status);
        viewOnlineIndicator = view.findViewById(R.id.view_online_indicator);
        rvChatMessages = view.findViewById(R.id.rv_chat_messages);
        etChatMessage = view.findViewById(R.id.et_chat_message);
        btnChatSend = view.findViewById(R.id.btn_chat_send);
        loadingContainer = view.findViewById(R.id.loading_container);
        progressLoading = view.findViewById(R.id.progress_loading);
        tvLoadingText = view.findViewById(R.id.tv_loading_text);

        // Initially show loading and hide messages
        showLoading();
    }

    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);

        rvChatMessages.setLayoutManager(layoutManager);
        rvChatMessages.setAdapter(messageAdapter);

        // Add scroll listener for pagination (load more when scroll to top)
        rvChatMessages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                // Check if scrolled to top
                if (!recyclerView.canScrollVertically(-1) && dy < 0) {
                    // At the top, load more messages
                    if (!isLoadingMore && hasMoreMessages) {
                        loadMoreMessages();
                    }
                }
            }
        });
    }

    private void setupListeners() {
        btnChatSend.setOnClickListener(v -> sendMessage());

        etChatMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnChatSend.setEnabled(s.toString().trim().length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Auto scroll to bottom when keyboard appears
        etChatMessage.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && messageAdapter != null && messageAdapter.getItemCount() > 0) {
                // Delay to ensure keyboard is shown
                rvChatMessages.postDelayed(() -> {
                    rvChatMessages.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
                }, 0);
            }
        });
    }

    private void loadPartnerInfo() {
        if (partnerName != null) {
            tvChatName.setText(partnerName);
        }

        // Load avatar from cache first
        loadAvatarFromCacheFirst();

        // Then load full partner details
        if (partnerId != null && !partnerId.isEmpty()) {
            databaseManager.getUser(partnerId, new DatabaseManager.DatabaseCallback<User>() {
                @Override
                public void onSuccess(User partner) {
                    if (partner != null) {
                        if (partnerName == null && partner.getName() != null) {
                            partnerName = partner.getName();
                            tvChatName.setText(partnerName);
                        }
                        loadPartnerAvatar(partner);
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Error loading partner info: " + error);
                    setDefaultAvatar();
                }
            });
        }
    }

    /**
     * Load avatar from cache first (immediate display)
     */
    private void loadAvatarFromCacheFirst() {
        if (ivChatAvatar == null || getContext() == null) return;

        Bitmap cachedPartnerAvatar = AvatarCache.getPartnerCachedBitmap(getContext());
        if (cachedPartnerAvatar != null) {
            ivChatAvatar.setImageBitmap(cachedPartnerAvatar);
            Log.d(TAG, "Loaded partner avatar from cache");
        } else {
            setDefaultAvatar();
        }
    }

    /**
     * Set default avatar
     */
    private void setDefaultAvatar() {
        if (ivChatAvatar != null) {
            ivChatAvatar.setImageResource(R.drawable.ic_default_avatar);
        }
    }

    /**
     * Load partner avatar with cache (from MessengerActivity)
     */
    private void loadPartnerAvatar(User partner) {
        if (partner == null || ivChatAvatar == null || getContext() == null) {
            setDefaultAvatar();
            return;
        }

        // Try cache first
        Bitmap cachedPartnerAvatar = AvatarCache.getPartnerCachedBitmap(getContext());
        if (cachedPartnerAvatar != null) {
            ivChatAvatar.setImageBitmap(cachedPartnerAvatar);
            partnerAvatarUrl = partner.getProfilePicUrl();
            messageAdapter.setPartnerAvatarUrl(partnerAvatarUrl);
            Log.d(TAG, "Loaded partner avatar from cache");
            return;
        }

        // Load from URL
        String profilePicUrl = partner.getProfilePicUrl();
        if (!TextUtils.isEmpty(profilePicUrl)) {
            partnerAvatarUrl = profilePicUrl;
            messageAdapter.setPartnerAvatarUrl(partnerAvatarUrl);

            loadImageAsync(profilePicUrl, bmp -> {
                if (bmp != null && ivChatAvatar != null && getContext() != null) {
                    ivChatAvatar.setImageBitmap(bmp);
                    AvatarCache.savePartnerBitmapToCache(getContext(), bmp);
                    Log.d(TAG, "Loaded and cached partner avatar from URL");
                } else {
                    setDefaultAvatar();
                }
            });
        } else {
            setDefaultAvatar();
        }
    }

    /**
     * Callback interface for async image loading
     */
    private interface BitmapCallback {
        void onBitmap(Bitmap bmp);
    }

    /**
     * Load image async from URL (from MessengerActivity)
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
                Log.e(TAG, "Error loading image: " + e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }

            final Bitmap result = bmp;
            // ✅ Check if fragment is still attached before callback
            if (getActivity() != null && isAdded()) {
                getActivity().runOnUiThread(() -> {
                    if (callback != null && isAdded()) callback.onBitmap(result);
                });
            }
        });
    }

    private void loadMessages() {
        if (coupleId == null || coupleId.isEmpty()) {
            Log.e(TAG, "Cannot load messages: coupleId is null or empty");
            Toast.makeText(getContext(), "Error: Couple ID not found", Toast.LENGTH_SHORT).show();
            hideLoading();
            return;
        }

        showLoading();
        long startTime = System.currentTimeMillis();

        // Load chat history - only 20 recent messages for better performance
        chatManager.getChatHistory(coupleId, MAX_MESSAGES_TO_LOAD, new ChatManager.ChatCallback() {
            @Override
            public void onMessagesReceived(List<ChatMessage> chatMessages) {
                // ✅ Check if fragment is still attached
                if (getActivity() == null || !isAdded()) return;

                // Calculate remaining time to show loading (minimum 800ms)
                long elapsedTime = System.currentTimeMillis() - startTime;
                long remainingTime = Math.max(0, 0 - elapsedTime);

                new android.os.Handler().postDelayed(() -> {
                    // ✅ Double check before UI update
                    if (getActivity() == null || !isAdded() || messageAdapter == null) return;

                    getActivity().runOnUiThread(() -> {
                        messageList.clear();
                        for (ChatMessage chatMsg : chatMessages) {
                            Message message = convertChatMessageToMessage(chatMsg);
                            messageList.add(message);
                        }

                        // ✅ Insert date separators
                        insertDateSeparators(messageList);

                        messageAdapter.updateMessages(messageList);

                        // Track oldest message for pagination
                        if (!messageList.isEmpty()) {
                            Message oldestMsg = messageList.get(0);
                            oldestMessageId = oldestMsg.getMessageId();
                            oldestTimestamp = getTimestampFromMessage(oldestMsg);
                        }

                        // Check if there are more messages
                        hasMoreMessages = chatMessages.size() >= MAX_MESSAGES_TO_LOAD;

                        // Hide loading and show messages
                        hideLoading();

                        if (!messageList.isEmpty()) {
                            scrollToBottom();
                        }

                        // Setup real-time listener for new messages
                        setupRealTimeListener();

                        Log.d(TAG, "Loaded " + chatMessages.size() + " messages, hasMore: " + hasMoreMessages);
                    });
                }, remainingTime);
            }

            @Override
            public void onMessageSent() {
                // Not used here
            }

            @Override
            public void onError(String error) {
                if (getActivity() == null) return;

                // Calculate remaining time to show loading (minimum 800ms)
                long elapsedTime = System.currentTimeMillis() - startTime;
                long remainingTime = Math.max(0, 0 - elapsedTime);

                new android.os.Handler().postDelayed(() -> {
                    if (getActivity() == null) return;

                    getActivity().runOnUiThread(() -> {
                        Log.e(TAG, "Error loading chat history: " + error);
                        hideLoading();
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Lỗi tải tin nhắn: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }, remainingTime);
            }
        });
    }

    private void setupRealTimeListener() {
        // ✅ Check if fragment is still attached before setting up listener
        if (getActivity() == null || !isAdded() || coupleId == null) {
            Log.w(TAG, "Cannot setup real-time listener: fragment not attached or coupleId is null");
            return;
        }

        // Store chat reference for cleanup
        chatRef = com.google.firebase.database.FirebaseDatabase.getInstance()
            .getReference("chats").child(coupleId);

        // Get the timestamp of the last message to avoid duplicates
        long lastTimestamp = messageList.isEmpty() ? 0 :
            getTimestampFromMessage(messageList.get(messageList.size() - 1));

        messageListener = chatManager.listenForNewMessagesStream(coupleId, lastTimestamp, new ChatManager.MessageListener() {
            @Override
            public void onNewMessage(ChatMessage chatMessage) {
                // ✅ CRITICAL: Check if fragment is still valid before processing
                if (getActivity() == null || !isAdded() || messageAdapter == null) {
                    Log.w(TAG, "Fragment not attached, ignoring new message");
                    return;
                }

                // Check for duplicates
                if (isMessageAlreadyExists(chatMessage.getMessageId())) {
                    Log.d(TAG, "Skipping duplicate message: " + chatMessage.getMessageId());
                    return;
                }

                getActivity().runOnUiThread(() -> {
                    // ✅ Double check before UI update
                    if (!isAdded() || messageAdapter == null) return;

                    Message message = convertChatMessageToMessage(chatMessage);

                    // ✅ Check if we need to add date separator before this message
                    if (shouldAddDateSeparator(messageList, message)) {
                        Message dateSeparator = createDateSeparator(message.getTimestamp());
                        messageList.add(dateSeparator);
                    }

                    messageList.add(message);
                    messageAdapter.updateMessages(messageList);
                    scrollToBottom();

                    // Mark as read if from partner
                    if (!chatMessage.getSenderId().equals(currentUserId)) {
                        markMessageAsReadImmediately(chatMessage.getMessageId());
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error listening for new messages: " + error);
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

        // Set sender name
        if (chatMessage.getSenderId().equals(currentUserId)) {
            message.setSenderName(currentUserName != null ? currentUserName : "You");
        } else {
            message.setSenderName(partnerName != null ? partnerName : "Partner");
        }

        message.setMessageType("text");
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

    private void markMessageAsReadImmediately(String messageId) {
        if (coupleId != null && messageId != null) {
            chatManager.markMessageAsRead(coupleId, messageId, new ChatManager.MessageReadCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Message marked as read: " + messageId);
                    // Update in local list
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
                    Log.e(TAG, "Failed to mark message as read: " + error);
                }
            });
        }
    }

    private com.google.firebase.database.ValueEventListener presenceListener;
    private com.google.firebase.database.DatabaseReference partnerPresenceRef;

    private void listenToPartnerStatus() {
        if (partnerId == null) {
            Log.w(TAG, "Cannot listen to partner status: partnerId is null");
            return;
        }

        Log.d(TAG, "Setting up presence listener for partner: " + partnerId);

        // Listen to real-time presence status from Firebase Realtime Database
        com.example.couple_app.managers.UserPresenceManager presenceManager =
            com.example.couple_app.managers.UserPresenceManager.getInstance();

        partnerPresenceRef = presenceManager.getUserPresenceRef(partnerId);

        presenceListener = new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                // ✅ Check if fragment is still attached
                if (getActivity() == null || !isAdded()) {
                    Log.w(TAG, "Fragment not attached, ignoring presence update");
                    return;
                }

                Log.d(TAG, "Presence data received for partner: " + partnerId);
                Log.d(TAG, "Snapshot exists: " + snapshot.exists());

                getActivity().runOnUiThread(() -> {
                    // ✅ Double check before UI update
                    if (!isAdded() || tvPartnerStatus == null || viewOnlineIndicator == null) {
                        return;
                    }

                    if (snapshot.exists()) {
                        Boolean isOnline = snapshot.child("isOnline").getValue(Boolean.class);
                        Long lastSeen = snapshot.child("lastSeen").getValue(Long.class);

                        Log.d(TAG, "Partner isOnline: " + isOnline + ", lastSeen: " + lastSeen);

                        if (isOnline != null && isOnline) {
                            Log.d(TAG, "Setting partner status to ONLINE");
                            updateOnlineStatus(true);
                        } else {
                            Log.d(TAG, "Setting partner status to OFFLINE with lastSeen: " + lastSeen);
                            // Show offline with last seen time
                            updateOnlineStatus(false, lastSeen);
                        }
                    } else {
                        Log.w(TAG, "No presence data found for partner: " + partnerId);
                        // No presence data, show as offline
                        updateOnlineStatus(false);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                Log.e(TAG, "Error listening to partner status: " + error.getMessage());
            }
        };

        partnerPresenceRef.addValueEventListener(presenceListener);
        Log.d(TAG, "Presence listener attached for partner: " + partnerId);
    }

    private void updateOnlineStatus(boolean isOnline) {
        updateOnlineStatus(isOnline, null);
    }

    private void updateOnlineStatus(boolean isOnline, Long lastSeenTimestamp) {
        if (tvPartnerStatus == null || viewOnlineIndicator == null) return;

        if (isOnline) {
            tvPartnerStatus.setText(R.string.chat_online_status);
            viewOnlineIndicator.setVisibility(View.VISIBLE);
        } else {
            if (lastSeenTimestamp != null && lastSeenTimestamp > 0) {
                // Calculate and show "last seen" time
                String lastSeenText = formatLastSeen(lastSeenTimestamp);
                tvPartnerStatus.setText(lastSeenText);
            } else {
                tvPartnerStatus.setText(R.string.chat_offline_status);
            }
            viewOnlineIndicator.setVisibility(View.GONE);
        }
    }

    /**
     * Format last seen timestamp to readable text
     * Examples: "Hoạt động 2 phút trước", "Hoạt động 1 giờ trước", "Hoạt động hôm qua"
     */
    private String formatLastSeen(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        // Less than 1 minute
        if (diff < 60 * 1000) {
            return "Vừa hoạt động";
        }
        // Less than 1 hour
        else if (diff < 60 * 60 * 1000) {
            long minutes = diff / (60 * 1000);
            return "Hoạt động " + minutes + " phút trước";
        }
        // Less than 24 hours
        else if (diff < 24 * 60 * 60 * 1000) {
            long hours = diff / (60 * 60 * 1000);
            return "Hoạt động " + hours + " giờ trước";
        }
        // Less than 7 days
        else if (diff < 7 * 24 * 60 * 60 * 1000) {
            long days = diff / (24 * 60 * 60 * 1000);
            if (days == 1) {
                return "Hoạt động hôm qua";
            }
            return "Hoạt động " + days + " ngày trước";
        }
        // More than 7 days
        else {
            return "Không hoạt động";
        }
    }

    private void sendMessage() {
        String messageText = etChatMessage.getText().toString().trim();

        if (TextUtils.isEmpty(messageText)) {
            Toast.makeText(getContext(), "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        if (coupleId == null || coupleId.isEmpty()) {
            Toast.makeText(getContext(), "Error: Couple ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(getContext(), "Error: User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable send button to prevent multiple sends
        btnChatSend.setEnabled(false);

        // Send message via ChatManager
        chatManager.sendMessage(coupleId, currentUserId, messageText, new ChatManager.ChatCallback() {
            @Override
            public void onMessagesReceived(List<ChatMessage> messages) {
                // Not used here
            }

            @Override
            public void onMessageSent() {
                // ✅ Check if fragment is still attached
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded() || etChatMessage == null || btnChatSend == null) return;

                        etChatMessage.setText("");
                        btnChatSend.setEnabled(true);

                        // Send notification to partner
                        if (partnerId != null && !partnerId.isEmpty()) {
                            sendNotificationToPartner(messageText);
                        }
                    });
                }
                Log.d(TAG, "Message sent successfully");
            }

            @Override
            public void onError(String error) {
                // ✅ Check if fragment is still attached
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded() || btnChatSend == null) return;

                        btnChatSend.setEnabled(true);
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Error sending message: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                Log.e(TAG, "Error sending message: " + error);
            }
        });
    }

    /**
     * Send notification to partner via backend API
     */
    private void sendNotificationToPartner(String messageText) {
        if (partnerId == null || TextUtils.isEmpty(messageText)) return;

        String senderName = currentUserName != null ? currentUserName : "Someone";

        NotificationAPI.sendNotification(
            partnerId,                    // toUserId
            senderName,                   // title
            messageText,                  // body
            coupleId,                     // coupleId
            currentUserId,                // fromUserId
            senderName,                   // fromUsername
            new NotificationAPI.NotificationCallback() {
                @Override
                public void onSuccess(String response) {
                    Log.d(TAG, "Notification sent successfully: " + response);
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Failed to send notification: " + error);
                }
            }
        );
    }

    /**
     * Load more (older) messages when scrolling to top
     */
    private void loadMoreMessages() {
        if (isLoadingMore || !hasMoreMessages || oldestTimestamp <= 0) {
            return;
        }

        isLoadingMore = true;
        Log.d(TAG, "Loading more messages before timestamp: " + oldestTimestamp);

        // Show loading text
        if (tvLoadingText != null) {
            tvLoadingText.setText("Đang tải thêm tin nhắn...");
        }

        chatManager.getChatHistoryBefore(coupleId, oldestTimestamp, MAX_MESSAGES_TO_LOAD, new ChatManager.ChatCallback() {
            @Override
            public void onMessagesReceived(List<ChatMessage> chatMessages) {
                // ✅ Check if fragment is still attached
                if (getActivity() == null || !isAdded()) return;

                getActivity().runOnUiThread(() -> {
                    // ✅ Double check before UI update
                    if (!isAdded() || messageAdapter == null) {
                        isLoadingMore = false;
                        return;
                    }

                    isLoadingMore = false;

                    if (chatMessages.isEmpty()) {
                        hasMoreMessages = false;
                        Log.d(TAG, "No more messages to load");
                        return;
                    }

                    // Get current scroll position
                    LinearLayoutManager layoutManager = (LinearLayoutManager) rvChatMessages.getLayoutManager();
                    int firstVisiblePosition = layoutManager != null ? layoutManager.findFirstVisibleItemPosition() : 0;
                    View firstVisibleView = layoutManager != null ? layoutManager.findViewByPosition(firstVisiblePosition) : null;
                    int offsetTop = firstVisibleView != null ? firstVisibleView.getTop() : 0;

                    // Add old messages to the beginning of the list
                    List<Message> oldMessages = new ArrayList<>();
                    for (ChatMessage chatMsg : chatMessages) {
                        Message message = convertChatMessageToMessage(chatMsg);
                        oldMessages.add(message);
                    }

                    messageList.addAll(0, oldMessages);

                    // ✅ Re-insert date separators for the entire list
                    insertDateSeparators(messageList);

                    messageAdapter.updateMessages(messageList);

                    // Update oldest message info
                    if (!messageList.isEmpty()) {
                        Message oldestMsg = messageList.get(0);
                        oldestMessageId = oldestMsg.getMessageId();
                        oldestTimestamp = getTimestampFromMessage(oldestMsg);
                    }

                    // Check if there are more messages
                    hasMoreMessages = chatMessages.size() >= MAX_MESSAGES_TO_LOAD;

                    // Restore scroll position
                    if (layoutManager != null) {
                        layoutManager.scrollToPositionWithOffset(firstVisiblePosition + oldMessages.size(), offsetTop);
                    }

                });
            }

            @Override
            public void onMessageSent() {
                // Not used here
            }

            @Override
            public void onError(String error) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    isLoadingMore = false;
                    Log.e(TAG, "Error loading more messages: " + error);
                    Toast.makeText(getContext(), "Lỗi tải thêm tin nhắn: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void scrollToBottom() {
        if (rvChatMessages != null && messageAdapter != null && messageAdapter.getItemCount() > 0) {
            rvChatMessages.postDelayed(() ->
                rvChatMessages.smoothScrollToPosition(messageAdapter.getItemCount() - 1), 100);
        }
    }

    /**
     * Show loading indicator and hide messages
     */
    private void showLoading() {
        if (loadingContainer != null) {
            loadingContainer.setVisibility(View.VISIBLE);
        }
        if (rvChatMessages != null) {
            rvChatMessages.setVisibility(View.GONE);
        }
    }

    /**
     * Hide loading indicator and show messages
     */
    private void hideLoading() {
        if (loadingContainer != null) {
            loadingContainer.setVisibility(View.GONE);
        }
        if (rvChatMessages != null) {
            rvChatMessages.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // ❌ REMOVED: Don't save state to prevent auto-restore when app restarts
        // If we save state, FragmentManager will auto-restore this fragment when user re-opens app
        // This causes the chat to automatically appear, which is not desired
        Log.d(TAG, "onSaveInstanceState called - NOT saving state to prevent auto-restore");
    }

    @Override
    public void dismiss() {
        // ✅ Hide keyboard and clear focus to avoid input event receiver warnings
        try {
            if (etChatMessage != null) {
                etChatMessage.clearFocus();
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                        requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(etChatMessage.getWindowToken(), 0);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error hiding keyboard on dismiss: " + e.getMessage());
        }

        // ✅ FIX: Force remove from FragmentManager to prevent restore
        try {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().beginTransaction()
                    .remove(this)
                    .commitAllowingStateLoss();
                Log.d(TAG, "Fragment removed from FragmentManager on dismiss");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error removing fragment: " + e.getMessage());
        }
        super.dismiss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        Log.d(TAG, "onDestroyView() called - cleaning up listeners and resources");

        // ✅ Clean up message listener properly
        if (messageListener != null && chatRef != null) {
            try {
                chatRef.removeEventListener(messageListener);
                Log.d(TAG, "Message listener removed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error removing message listener: " + e.getMessage());
            }
            messageListener = null;
            chatRef = null;
        }

        // ✅ Clean up presence listener
        if (presenceListener != null && partnerPresenceRef != null) {
            try {
                partnerPresenceRef.removeEventListener(presenceListener);
                Log.d(TAG, "Presence listener removed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error removing presence listener: " + e.getMessage());
            }
            presenceListener = null;
            partnerPresenceRef = null;
        }

        // ✅ Clear references to prevent memory leaks
        if (messageAdapter != null) {
            messageAdapter = null;
        }
        if (messageList != null) {
            messageList.clear();
        }

        Log.d(TAG, "onDestroyView() completed - all resources cleaned up");
    }

    /**
     * Insert date separators into message list
     * This method modifies the list in-place
     */
    private void insertDateSeparators(List<Message> messages) {
        if (messages == null || messages.isEmpty()) return;

        // Remove existing date separators first
        messages.removeIf(msg -> "date_separator".equals(msg.getMessageType()));

        // Insert date separators
        List<Message> result = new ArrayList<>();
        String lastDate = null;

        for (Message message : messages) {
            String currentDate = getDateString(message.getTimestamp());

            // Add date separator if date changed
            if (lastDate == null || !lastDate.equals(currentDate)) {
                Message dateSeparator = createDateSeparator(message.getTimestamp());
                result.add(dateSeparator);
                lastDate = currentDate;
            }

            result.add(message);
        }

        // Update original list
        messages.clear();
        messages.addAll(result);
    }

    /**
     * Check if we should add date separator before this message
     */
    private boolean shouldAddDateSeparator(List<Message> messages, Message newMessage) {
        if (messages.isEmpty()) return true;

        Message lastMessage = messages.get(messages.size() - 1);

        // Don't add separator after another separator
        if ("date_separator".equals(lastMessage.getMessageType())) {
            return false;
        }

        String lastDate = getDateString(lastMessage.getTimestamp());
        String newDate = getDateString(newMessage.getTimestamp());

        return !lastDate.equals(newDate);
    }

    /**
     * Create a date separator message
     */
    private Message createDateSeparator(Object timestamp) {
        Message separator = new Message();
        separator.setMessageType("date_separator");
        separator.setTimestamp(timestamp);
        separator.setMessageId("separator_" + System.currentTimeMillis());
        separator.setSenderId("system");
        separator.setMessage("");
        return separator;
    }

    /**
     * Get date string from timestamp (dd/MM/yyyy format)
     */
    private String getDateString(Object timestampObj) {
        long millis = getTimestampMillis(timestampObj);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(millis));
    }

    /**
     * Get timestamp in milliseconds
     */
    private long getTimestampMillis(Object timestampObj) {
        if (timestampObj instanceof Long) {
            return (Long) timestampObj;
        } else if (timestampObj instanceof Double) {
            return ((Double) timestampObj).longValue();
        }
        return System.currentTimeMillis();
    }

    @Override
    public int getTheme() {
        return R.style.BottomSheetDialogTheme;
    }
}

