package com.example.couple_app.fragments;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.couple_app.R;
import com.example.couple_app.adapters.ChatMessageAdapter;
import com.example.couple_app.models.ChatBotMessage;
import com.example.couple_app.models.ChatSession;
import com.example.couple_app.models.ChatSessionMessage;
import com.example.couple_app.managers.AIChatManager;
import com.example.couple_app.repositories.ChatSessionRepository;
import com.example.couple_app.utils.NetworkUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;


/**
 * Bottom sheet dialog fragment for chatbot interface
 * Displays chatbot with draggable handle and rounded corners
 * Height is set to 90% of screen with draggable behavior
 */
public class ChatbotBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String TAG = "ChatbotBottomSheet";
    private static final float BOTTOM_SHEET_PEEK_HEIGHT_RATIO = 0.90f; // 90% of screen height
    private static final String PREF_NAME = "chatbot_prefs";
    private static final String KEY_SESSION_ID = "session_id";
    // New: argument keys to resume an existing session
    private static final String ARG_SESSION_ID = "arg_session_id";
    private static final String ARG_SESSION_TITLE = "arg_session_title";

    private RecyclerView chatRecyclerView;
    private EditText inputMessage;
    private ImageButton btnSend;
    private ImageButton btnHistory;
    private ImageButton btnNewSession; // '+' button
    private com.example.couple_app.views.TypingIndicatorView typingIndicator;

    private ChatMessageAdapter adapter;
    private ChatSessionRepository sessionRepository;
    private String sessionId;
    private String currentUserId;
    private boolean sessionCreatedInFirestore = false; // Track if session created
    private boolean resumeExistingSession = false; // New: whether to resume existing

    private Handler updateHandler = new Handler(Looper.getMainLooper());
    private Runnable updateRunnable;

    // Factory method to create fragment with a specific session
    public static ChatbotBottomSheetFragment newInstance(@NonNull String sessionId, @Nullable String title) {
        ChatbotBottomSheetFragment fragment = new ChatbotBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SESSION_ID, sessionId);
        if (title != null) args.putString(ARG_SESSION_TITLE, title);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_chatbot, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Restore state from savedInstanceState if exists (fragment recreate)
        if (savedInstanceState != null) {
            sessionId = savedInstanceState.getString("sessionId");
            sessionCreatedInFirestore = savedInstanceState.getBoolean("sessionCreatedInFirestore", false);
            resumeExistingSession = savedInstanceState.getBoolean("resumeExistingSession", false);
            Log.d(TAG, "Restored state from savedInstanceState: sessionId=" + sessionId);
        }

        // Initialize components
        initializeViews(view);

        // Only initialize sessionId if not restored from savedInstanceState
        if (sessionId == null) {
            initializeSessionId();
        }

        setupRecyclerView();
        setupClickListeners();

        // If resuming an existing session, load its messages; otherwise show welcome message
        if (resumeExistingSession) {
            loadExistingMessages();
        } else {
            // Clear any old messages from adapter before starting new conversation
            if (adapter != null && adapter.getItemCount() > 0) {
                adapter.clearMessages();
                Log.d(TAG, "Cleared old messages from adapter for new conversation");
            }
            addBotMessage(getString(R.string.chatbot_welcome_message));
        }

        // Setup keyboard listener to scroll to bottom when keyboard appears
        inputMessage.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // Delay to ensure keyboard is shown
                chatRecyclerView.postDelayed(this::scrollToBottom, 300);
            }
        });

        // Add global layout listener to detect keyboard show/hide and scroll accordingly
        view.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (inputMessage != null && inputMessage.hasFocus()) {
                // Scroll to bottom when keyboard appears and input has focus
                chatRecyclerView.post(this::scrollToBottom);
            }
        });
    }

    /**
     * Initialize UI views
     */
    private void initializeViews(@NonNull View view) {
        chatRecyclerView = view.findViewById(R.id.chat_recycler_view);
        inputMessage = view.findViewById(R.id.input_message);
        btnSend = view.findViewById(R.id.btn_send);
        btnHistory = view.findViewById(R.id.btn_history);
        btnNewSession = view.findViewById(R.id.btn_new_session);
        typingIndicator = view.findViewById(R.id.typing_indicator);

        if (chatRecyclerView == null || inputMessage == null || btnSend == null) {
            Log.e(TAG, "Failed to initialize views");
        }


    }

    /**
     * Initialize or retrieve session ID
     * - If resuming from history (args provided): use that sessionId
     * - Otherwise: always create a brand-new session id for a fresh conversation
     */
    private void initializeSessionId() {
        if (getContext() == null) return;

        // Get current userId from Firebase Auth
        com.google.firebase.auth.FirebaseUser firebaseUser =
            com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        String userId = firebaseUser != null ? firebaseUser.getUid() : "guest";

        // Check if a sessionId is provided via arguments (resume existing conversation)
        String providedSessionId = null;
        Bundle args = getArguments();
        if (args != null) {
            providedSessionId = args.getString(ARG_SESSION_ID);
        }

        if (providedSessionId != null && !providedSessionId.isEmpty()) {
            // Resume provided session (e.g., selected from chat history)
            sessionId = providedSessionId;
            resumeExistingSession = true;
            sessionCreatedInFirestore = true; // Avoid re-creating session doc
            Log.d(TAG, "Resuming existing session: " + sessionId);
        } else {
            // Always create a new session id on open for a fresh conversation
            sessionId = "session_" + userId + "_" + System.currentTimeMillis();
            resumeExistingSession = false;
            sessionCreatedInFirestore = false;
            Log.d(TAG, "Starting new session: " + sessionId);
        }
    }

    /**
     * Setup RecyclerView with adapter and layout manager
     */
    private void setupRecyclerView() {
        adapter = new ChatMessageAdapter();
        sessionRepository = new ChatSessionRepository();

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);

        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(adapter);

        // Get current userId
        com.google.firebase.auth.FirebaseUser firebaseUser =
            com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            currentUserId = firebaseUser.getUid();
        }
    }

    /**
     * Setup click listeners for send button
     */
    private void setupClickListeners() {
        btnSend.setOnClickListener(v -> sendMessage());

        // Also send on Enter key
        inputMessage.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });

        // History button
        if (btnHistory != null) {
            btnHistory.setOnClickListener(v -> openChatHistory());
        }
        if (btnNewSession != null) {
            btnNewSession.setOnClickListener(v -> startNewConversation());
        }
    }

    /**
     * Open chat history activity
     */
    private void openChatHistory() {
        if (getContext() != null) {
            android.content.Intent intent = new android.content.Intent(getContext(),
                com.example.couple_app.activities.ChatHistoryActivity.class);
            startActivity(intent);
        }
    }

    /**
     * Send message to chatbot backend
     */
    private void sendMessage() {
        String message = inputMessage.getText().toString().trim();

        if (message.isEmpty()) {
            Toast.makeText(getContext(), R.string.error_empty_message, Toast.LENGTH_SHORT).show();
            return;
        }
        // Check internet connection
        if (getContext() != null && !NetworkUtils.isNetworkAvailable(getContext())) {
            Toast.makeText(getContext(), R.string.error_no_internet, Toast.LENGTH_SHORT).show();
            return;
        }


        // Add user message to UI
        addUserMessage(message);

        // Save user message to Firestore
        saveMessageToFirestore(message, "user");

        // Clear input
        inputMessage.setText("");

        // Disable send button while processing
        btnSend.setEnabled(false);

        // Show typing indicator animation
        showTypingIndicator();

        // Get userId from Firebase Auth
        String userId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
                ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        // Log request details
        Log.d(TAG, "━━━━━━━━━━━━��━━━━━━━━━━━━━━━━━");
        Log.d(TAG, "Sending chat request:");
        Log.d(TAG, "  Message: " + message);
        Log.d(TAG, "  Session ID: " + sessionId);
        Log.d(TAG, "  User ID: " + (userId != null ? userId : "NULL - not logged in"));
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // Add empty bot message for streaming
        addBotMessage("");

        // Send to AI backend with streaming
        AIChatManager.sendMessageToAI(message, userId, sessionId, new AIChatManager.AICallback() {
            private final StringBuilder content = new StringBuilder();

            @Override
            public void onToken(String token) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        content.append(token);
                        // Debounce UI update
                        if (updateRunnable != null) {
                            updateHandler.removeCallbacks(updateRunnable);
                        }
                        updateRunnable = () -> {
                            adapter.updateLastBotMessage(content.toString());
                            scrollToBottom();
                        };
                        updateHandler.postDelayed(updateRunnable, 100); // 100ms delay
                    });
                }
            }

            @Override
            public void onDone(String fullAnswer) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Cancel any pending update
                        if (updateRunnable != null) {
                            updateHandler.removeCallbacks(updateRunnable);
                        }
                        // Hide typing indicator
                        hideTypingIndicator();

                        // Update with final answer
                        adapter.updateLastBotMessage(fullAnswer);

                        // Save bot response to Firestore
                        saveMessageToFirestore(fullAnswer, "bot");

                        // Re-enable send button
                        btnSend.setEnabled(true);
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Cancel any pending update
                        if (updateRunnable != null) {
                            updateHandler.removeCallbacks(updateRunnable);
                        }
                        // Hide typing indicator
                        hideTypingIndicator();

                        // Show error message
                        String errorMsg = getString(R.string.chatbot_error) + ": " + error;
                        adapter.updateLastBotMessage(errorMsg);

                        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();

                        // Re-enable send button
                        btnSend.setEnabled(true);
                    });
                }
            }
        });
    }

    /**
     * Add user message to chat
     */
    private void addUserMessage(@NonNull String message) {
        ChatBotMessage chatMessage = new ChatBotMessage(message, ChatBotMessage.TYPE_USER);
        adapter.addMessage(chatMessage);
        scrollToBottom();
    }

    /**
     * Add bot message to chat
     */
    private void addBotMessage(@NonNull String message) {
        ChatBotMessage chatMessage = new ChatBotMessage(message, ChatBotMessage.TYPE_BOT);
        adapter.addMessage(chatMessage);
        scrollToBottom();
    }

    /**
     * Remove last bot message (for removing typing indicator)
     */
    private void removeLastBotMessage() {
        if (adapter.getItemCount() > 0) {
            adapter.removeLastMessage();
        }
    }

    /**
     * Scroll RecyclerView to bottom
     */
    private void scrollToBottom() {
        if (adapter.getItemCount() > 0) {
            chatRecyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
        }
    }

    /**
     * Show typing indicator animation
     */
    private void showTypingIndicator() {
        if (typingIndicator != null) {
            typingIndicator.setVisibility(View.VISIBLE);
            typingIndicator.startAnimation();
            // Scroll to show typing indicator
            chatRecyclerView.postDelayed(() -> {
                chatRecyclerView.smoothScrollToPosition(adapter.getItemCount());
            }, 100);
        }
    }

    /**
     * Hide typing indicator animation
     */
    private void hideTypingIndicator() {
        if (typingIndicator != null) {
            typingIndicator.stopAnimation();
            typingIndicator.setVisibility(View.GONE);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        // Set window to adjust for keyboard - use ADJUST_PAN to pan the entire sheet up
        Window window = dialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        }

        // Configure bottom sheet behavior
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
            View bottomSheetInternal = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);

            if (bottomSheetInternal != null) {
                BottomSheetBehavior<?> behavior = BottomSheetBehavior.from(bottomSheetInternal);

                // Calculate dimensions: 90% of screen height, 10% gap at top
                int screenHeight = getResources().getDisplayMetrics().heightPixels;
                int peekHeight = (int) (screenHeight * BOTTOM_SHEET_PEEK_HEIGHT_RATIO); // 90% height
                int expandedOffset = screenHeight - peekHeight; // 10% offset from top

                // Set the height of the bottom sheet view
                ViewGroup.LayoutParams layoutParams = bottomSheetInternal.getLayoutParams();
                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                bottomSheetInternal.setLayoutParams(layoutParams);

                // Set bottom sheet behavior
                behavior.setPeekHeight(peekHeight);
                behavior.setMaxHeight(peekHeight);
                behavior.setFitToContents(false);
                behavior.setExpandedOffset(expandedOffset); // Leave 10% gap at top
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
                behavior.setHideable(true);
                behavior.setDraggable(true);
            }
        });

        return dialog;
    }

    @Override
    public int getTheme() {
        // Use custom theme for bottom sheet
        return R.style.BottomSheetDialogTheme;
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        // Do not clear messages or reset flags to preserve conversation when sheet is hidden
        Log.d(TAG, "Chatbot dismissed - preserving current conversation state");
        // Notify host to remember current sessionId for quick resume within the same Activity
        if (getActivity() instanceof ChatbotHost && sessionId != null) {
            ((ChatbotHost) getActivity()).onChatbotDismiss(sessionId);
        }
    }

    /**
     * Save message to Firestore
     * Creates session on first message (lazy initialization)
     */
    private void saveMessageToFirestore(@NonNull String content, @NonNull String sender) {
        if (sessionRepository == null || sessionId == null || currentUserId == null) {
            Log.w(TAG, "Cannot save message - repository or session not initialized");
            return;
        }

        // Create session in Firestore if this is the first message
        if (!sessionCreatedInFirestore) {
            createSessionInFirestore(() -> {
                // After session created, save the message
                saveMessageToFirestoreInternal(content, sender);
            });
            sessionCreatedInFirestore = true;
        } else {
            // Session already exists, just save message
            saveMessageToFirestoreInternal(content, sender);
        }
    }

    /**
     * Create session in Firestore (called on first message only)
     */
    private void createSessionInFirestore(Runnable onComplete) {
        ChatSession session = new ChatSession(sessionId, currentUserId);
        session.setTitle("Cuộc trò chuyện mới");
        session.setMessageCount(0);
        session.setLastMessage("");

        Log.d(TAG, "Creating chat session in Firestore: " + sessionId);

        sessionRepository.createOrUpdateSession(session, new ChatSessionRepository.OnSessionOperationListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "✅ Chat session created in Firestore successfully");
                sessionCreatedInFirestore = true;
                if (onComplete != null) {
                    onComplete.run();
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error creating session: " + error);
                // Still try to save message even if session creation fails
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }

    /**
     * Internal method to save message (after session is created)
     */
    private void saveMessageToFirestoreInternal(@NonNull String content, @NonNull String sender) {
        ChatSessionMessage message = new ChatSessionMessage(content, sender, sessionId);
        sessionRepository.saveMessage(sessionId, message, new ChatSessionRepository.OnMessageOperationListener() {
            @Override
            public void onSuccess(String messageId) {
                Log.d(TAG, "Message saved to Firestore: " + messageId);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error saving message to Firestore: " + error);
            }
        });
    }

    /**
     * Clear all chat messages and create new session
     * This is for starting a completely new conversation
     */
    public void startNewConversation() {
        if (adapter != null) {
            adapter.clearMessages();
            Log.d(TAG, "Chat messages cleared");
        }

        // Generate a new session id immediately
        String userId = null;
        com.google.firebase.auth.FirebaseUser firebaseUser =
            com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            userId = firebaseUser.getUid();
        }
        if (userId == null) {
            userId = "guest";
        }
        sessionId = "session_" + userId + "_" + System.currentTimeMillis();
        resumeExistingSession = false;
        sessionCreatedInFirestore = false;
        Log.d(TAG, "New conversation started with session: " + sessionId);

        // Notify Activity to clear cached session id
        if (getActivity() instanceof ChatbotHost) {
            ((ChatbotHost) getActivity()).onChatbotDismiss(sessionId);
        }

        // Show welcome message for the new conversation
        addBotMessage(getString(R.string.chatbot_welcome_message));
    }

    /**
     * Load existing messages for the current session and display them
     */
    private void loadExistingMessages() {
        if (sessionRepository == null || sessionId == null) return;

        sessionRepository.getSessionMessages(sessionId, new ChatSessionRepository.OnMessagesLoadedListener() {
            @Override
            public void onMessagesLoaded(List<ChatSessionMessage> messages) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (messages == null || messages.isEmpty()) {
                            // No messages found - show welcome message for fresh session
                            Log.d(TAG, "No messages found in session, showing welcome message");
                            addBotMessage(getString(R.string.chatbot_welcome_message));
                        } else {
                            // Convert to ChatBotMessage list
                            List<ChatBotMessage> chatBotMessages = new ArrayList<>();
                            for (ChatSessionMessage msg : messages) {
                                chatBotMessages.add(msg.toChatBotMessage());
                            }
                            adapter.setMessages(chatBotMessages);
                            scrollToBottom();
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to load existing messages: " + error);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Không thể tải hội thoại trước đó", Toast.LENGTH_SHORT).show();
                }
                // On error, show welcome message to allow user to start chatting
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> addBotMessage(getString(R.string.chatbot_welcome_message)));
                }
            }
        });
    }
}
