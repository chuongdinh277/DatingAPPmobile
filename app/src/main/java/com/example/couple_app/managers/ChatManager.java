package com.example.couple_app.managers;

import android.util.Log;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.example.couple_app.models.ChatMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatManager {
    private static final String TAG = "ChatManager";
    private DatabaseReference database;
    private static ChatManager instance;

    // Database paths
    private static final String CHATS_PATH = "chats";

    private ChatManager() {
        try {
            // ✅ FIXED: Use correct database URL for Asia Southeast 1 region
            String databaseUrl = "https://couples-app-b83be-default-rtdb.asia-southeast1.firebasedatabase.app";
            database = FirebaseDatabase.getInstance(databaseUrl).getReference();

            // Enable logging for debugging
            FirebaseDatabase.getInstance(databaseUrl).setLogLevel(com.google.firebase.database.Logger.Level.DEBUG);

            Log.d(TAG, "ChatManager initialized with database URL: " + databaseUrl);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing ChatManager", e);
            // Fallback to default instance
            database = FirebaseDatabase.getInstance().getReference();
        }
    }

    public static synchronized ChatManager getInstance() {
        if (instance == null) {
            instance = new ChatManager();
        }
        return instance;
    }

    public interface ChatCallback {
        void onMessagesReceived(List<ChatMessage> messages);
        void onMessageSent();
        void onError(String error);
    }

    public interface MessageListener {
        void onNewMessage(ChatMessage message);
        void onError(String error);
    }

    public interface MessageReadCallback {
        void onSuccess();
        void onError(String error);
    }

    // Send a message
    public void sendMessage(String coupleId, String senderId, String messageText, ChatCallback callback) {
        if (coupleId == null || coupleId.trim().isEmpty()) {
            Log.e(TAG, "CoupleId is null or empty");
            callback.onError("Invalid couple ID");
            return;
        }

        if (senderId == null || senderId.trim().isEmpty()) {
            Log.e(TAG, "SenderId is null or empty");
            callback.onError("Invalid sender ID");
            return;
        }

        if (messageText == null || messageText.trim().isEmpty()) {
            Log.e(TAG, "Message text is null or empty");
            callback.onError("Message cannot be empty");
            return;
        }

        try {
            DatabaseReference chatRef = database.child(CHATS_PATH).child(coupleId);
            DatabaseReference newMessageRef = chatRef.push();

            ChatMessage message = new ChatMessage(senderId, messageText.trim());

            Log.d(TAG, "Attempting to send message to path: " + CHATS_PATH + "/" + coupleId);
            Log.d(TAG, "Message: " + messageText.trim());

            newMessageRef.setValue(message)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Message sent successfully to Firebase");
                    callback.onMessageSent();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending message to Firebase", e);
                    callback.onError("Failed to send message: " + e.getMessage());
                });
        } catch (Exception e) {
            Log.e(TAG, "Exception while sending message", e);
            callback.onError("Unexpected error: " + e.getMessage());
        }
    }

    // Stream new messages using ChildEventListener starting after a timestamp
    public ChildEventListener listenForNewMessagesStream(String coupleId, long startAfterTimestamp, MessageListener listener) {
        if (coupleId == null) {
            listener.onError("Invalid couple ID");
            return null;
        }
        DatabaseReference chatRef = database.child(CHATS_PATH).child(coupleId);
        Query query = chatRef.orderByChild("timestamp").startAt(startAfterTimestamp + 1);
        ChildEventListener childListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                ChatMessage msg = snapshot.getValue(ChatMessage.class);
                if (msg != null) {
                    msg.setMessageId(snapshot.getKey());
                    listener.onNewMessage(msg);
                }
            }
            @Override public void onChildChanged(DataSnapshot snapshot, String previousChildName) {}
            @Override public void onChildRemoved(DataSnapshot snapshot) {}
            @Override public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}
            @Override public void onCancelled(DatabaseError error) {
                Log.w(TAG, "Child listener cancelled", error.toException());
                listener.onError("Failed to listen for messages: " + error.getMessage());
            }
        };
        query.addChildEventListener(childListener);
        return childListener;
    }

    // Get chat history (last N messages) sorted by timestamp ascending
    public void getChatHistory(String coupleId, int limit, ChatCallback callback) {
        if (coupleId == null) {
            callback.onError("Invalid couple ID");
            return;
        }

        DatabaseReference chatRef = database.child(CHATS_PATH).child(coupleId);
        Query query = chatRef.orderByChild("timestamp").limitToLast(limit);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<ChatMessage> messages = new ArrayList<>();
                for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                    ChatMessage message = messageSnapshot.getValue(ChatMessage.class);
                    if (message != null) {
                        message.setMessageId(messageSnapshot.getKey());
                        messages.add(message);
                    }
                }
                // Sort by timestamp ascending to ensure correct order
                Collections.sort(messages, new Comparator<ChatMessage>() {
                    @Override
                    public int compare(ChatMessage a, ChatMessage b) {
                        long ta = coerceToMillis(a != null ? a.getTimestamp() : null);
                        long tb = coerceToMillis(b != null ? b.getTimestamp() : null);
                        return Long.compare(ta, tb);
                    }
                });
                callback.onMessagesReceived(messages);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.w(TAG, "Error getting chat history", error.toException());
                callback.onError("Failed to get chat history: " + error.getMessage());
            }
        });
    }

    /**
     * Get chat history before a specific timestamp (for pagination)
     * @param coupleId The couple ID
     * @param beforeTimestamp Load messages before this timestamp
     * @param limit Number of messages to load
     * @param callback Callback with results
     */
    public void getChatHistoryBefore(String coupleId, long beforeTimestamp, int limit, ChatCallback callback) {
        if (coupleId == null) {
            callback.onError("Invalid couple ID");
            return;
        }

        DatabaseReference chatRef = database.child(CHATS_PATH).child(coupleId);
        Query query = chatRef.orderByChild("timestamp")
                            .endBefore(beforeTimestamp)
                            .limitToLast(limit);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<ChatMessage> messages = new ArrayList<>();
                for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                    ChatMessage message = messageSnapshot.getValue(ChatMessage.class);
                    if (message != null) {
                        message.setMessageId(messageSnapshot.getKey());
                        messages.add(message);
                    }
                }
                // Sort by timestamp ascending to ensure correct order
                Collections.sort(messages, new Comparator<ChatMessage>() {
                    @Override
                    public int compare(ChatMessage a, ChatMessage b) {
                        long ta = coerceToMillis(a != null ? a.getTimestamp() : null);
                        long tb = coerceToMillis(b != null ? b.getTimestamp() : null);
                        return Long.compare(ta, tb);
                    }
                });
                callback.onMessagesReceived(messages);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.w(TAG, "Error getting chat history before timestamp", error.toException());
                callback.onError("Failed to get chat history: " + error.getMessage());
            }
        });
    }

    private static long coerceToMillis(Object ts) {
        if (ts == null) return 0L;
        if (ts instanceof Long) return (Long) ts;
        if (ts instanceof Double) return ((Double) ts).longValue();
        return 0L; // Unknown placeholder
    }

    public void removeChildMessageListener(String coupleId, ChildEventListener listener) {
        if (coupleId != null && listener != null) {
            DatabaseReference chatRef = database.child(CHATS_PATH).child(coupleId);
            chatRef.removeEventListener(listener);
        }
    }

    /**
     * Đánh dấu tin nhắn đã đọc
     */
    public void markMessageAsRead(String coupleId, String messageId, MessageReadCallback callback) {
        if (coupleId == null || messageId == null) {
            if (callback != null) callback.onError("Invalid coupleId or messageId");
            return;
        }

        DatabaseReference messageRef = database.child(CHATS_PATH).child(coupleId).child(messageId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("read", true);
        updates.put("readAt", ServerValue.TIMESTAMP);

        messageRef.updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Message marked as read: " + messageId);
                if (callback != null) callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error marking message as read", e);
                if (callback != null) callback.onError(e.getMessage());
            });
    }

    /**
     * Đánh dấu tất cả tin nhắn chưa đọc của một cuộc trò chuyện là đã đọc
     */
    public void markAllMessagesAsRead(String coupleId, String currentUserId, MessageReadCallback callback) {
        if (coupleId == null || currentUserId == null) {
            if (callback != null) callback.onError("Invalid parameters");
            return;
        }

        DatabaseReference chatRef = database.child(CHATS_PATH).child(coupleId);

        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Map<String, Object> updates = new HashMap<>();
                final int[] updateCount = {0}; // Use array to make it effectively final

                for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                    String messageId = messageSnapshot.getKey();
                    String senderId = messageSnapshot.child("senderId").getValue(String.class);
                    Boolean isRead = messageSnapshot.child("read").getValue(Boolean.class);

                    // Chỉ đánh dấu tin nhắn từ người khác và chưa đọc
                    if (senderId != null && !senderId.equals(currentUserId) &&
                        (isRead == null || !isRead)) {
                        updates.put(messageId + "/read", true);
                        updates.put(messageId + "/readAt", ServerValue.TIMESTAMP);
                        updateCount[0]++;
                    }
                }

                if (updateCount[0] > 0) {
                    chatRef.updateChildren(updates)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Marked " + updateCount[0] + " messages as read");
                            if (callback != null) callback.onSuccess();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error marking messages as read", e);
                            if (callback != null) callback.onError(e.getMessage());
                        });
                } else {
                    Log.d(TAG, "No unread messages to mark");
                    if (callback != null) callback.onSuccess();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error fetching messages to mark as read", error.toException());
                if (callback != null) callback.onError(error.getMessage());
            }
        });
    }

    /**
     * Lắng nghe thay đổi trạng thái đã đọc của tin nhắn
     */
    public ValueEventListener listenForMessageReadStatus(String coupleId, String messageId, MessageReadStatusListener listener) {
        if (coupleId == null || messageId == null) {
            listener.onError("Invalid parameters");
            return null;
        }

        DatabaseReference messageRef = database.child(CHATS_PATH).child(coupleId).child(messageId);

        ValueEventListener valueListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Boolean isRead = snapshot.child("read").getValue(Boolean.class);
                Object readAt = snapshot.child("readAt").getValue();

                if (isRead != null && isRead) {
                    listener.onMessageRead(readAt);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error listening for read status", error.toException());
                listener.onError(error.getMessage());
            }
        };

        messageRef.addValueEventListener(valueListener);
        return valueListener;
    }

    public interface MessageReadStatusListener {
        void onMessageRead(Object readAt);
        void onError(String error);
    }

    public interface ReadStatusChangeListener {
        void onReadStatusChanged(String messageId, boolean isRead, Object readAt);
        void onError(String error);
    }

    /**
     * ✅ Lắng nghe thay đổi trạng thái đã đọc của tin nhắn của user hiện tại
     */
    public void listenForReadStatusChanges(String coupleId, String currentUserId, ReadStatusChangeListener listener) {
        if (coupleId == null || currentUserId == null) {
            listener.onError("Invalid parameters");
            return;
        }

        DatabaseReference chatRef = database.child(CHATS_PATH).child(coupleId);

        Log.d(TAG, "🔵 Setting up read status listener for coupleId: " + coupleId + ", userId: " + currentUserId);

        // Lắng nghe thay đổi trên tất cả tin nhắn
        chatRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                // Không xử lý ở đây, chỉ xử lý khi có thay đổi
            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
                String messageId = snapshot.getKey();
                String senderId = snapshot.child("senderId").getValue(String.class);
                Boolean isRead = snapshot.child("read").getValue(Boolean.class);
                Object readAt = snapshot.child("readAt").getValue();

                Log.d(TAG, "🟡 Message changed: " + messageId + ", senderId: " + senderId +
                          ", isRead: " + isRead + ", currentUserId: " + currentUserId);

                // Chỉ thông báo khi tin nhắn của user hiện tại được đọc
                if (senderId != null && senderId.equals(currentUserId)) {
                    Log.d(TAG, "✅ Read status changed for user's message: " + messageId + " -> " + isRead);
                    listener.onReadStatusChanged(messageId, isRead != null && isRead, readAt);
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error listening for read status changes", error.toException());
                listener.onError(error.getMessage());
            }
        });
    }
}
