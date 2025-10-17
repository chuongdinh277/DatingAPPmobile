package com.example.couple_app.managers;

import android.util.Log;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.example.couple_app.models.ChatMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ChatManager {
    private static final String TAG = "ChatManager";
    private DatabaseReference database;
    private static ChatManager instance;

    // Database paths
    private static final String CHATS_PATH = "chats";

    private ChatManager() {
        try {
            // Use specific database URL for better connection
            String databaseUrl = "https://couples-app-b83be-default-rtdb.firebaseio.com/";
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
}
