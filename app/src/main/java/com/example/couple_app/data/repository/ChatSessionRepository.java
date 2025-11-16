package com.example.couple_app.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.couple_app.data.model.ChatSession;
import com.example.couple_app.data.model.ChatSessionMessage;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for managing chat session history in Firestore
 * Handles CRUD operations for chat sessions and messages
 */
public class ChatSessionRepository {

    private static final String TAG = "ChatSessionRepository";
    private static final String COLLECTION_CHAT_SESSIONS = "chat_sessions";
    private static final String COLLECTION_MESSAGES = "messages";

    private final FirebaseFirestore db;

    public ChatSessionRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Create or update a chat session
     */
    public void createOrUpdateSession(@NonNull ChatSession session, @NonNull OnSessionOperationListener listener) {
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("userId", session.getUserId());
        sessionData.put("title", session.getTitle());
        sessionData.put("lastMessage", session.getLastMessage());
        sessionData.put("messageCount", session.getMessageCount());
        sessionData.put("updatedAt", FieldValue.serverTimestamp());

        // Add createdAt only for new sessions
        if (session.getCreatedAt() == null) {
            sessionData.put("createdAt", FieldValue.serverTimestamp());
        }

        db.collection(COLLECTION_CHAT_SESSIONS)
                .document(session.getSessionId())
                .set(sessionData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Session created/updated: " + session.getSessionId());
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating/updating session", e);
                    listener.onError(e.getMessage());
                });
    }

    /**
     * Save a message to a chat session
     */
    public void saveMessage(@NonNull String sessionId, @NonNull ChatSessionMessage message,
                           @NonNull OnMessageOperationListener listener) {
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("content", message.getContent());
        messageData.put("sender", message.getSender());
        messageData.put("sessionId", sessionId);
        messageData.put("timestamp", FieldValue.serverTimestamp());

        // Add message to subcollection
        db.collection(COLLECTION_CHAT_SESSIONS)
                .document(sessionId)
                .collection(COLLECTION_MESSAGES)
                .add(messageData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Message saved: " + documentReference.getId());

                    // Update session's lastMessage and messageCount
                    updateSessionLastMessage(sessionId, message.getContent());

                    listener.onSuccess(documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving message", e);
                    listener.onError(e.getMessage());
                });
    }

    /**
     * Update session's last message and increment message count
     */
    private void updateSessionLastMessage(@NonNull String sessionId, @NonNull String lastMessage) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", lastMessage);
        updates.put("messageCount", FieldValue.increment(1));
        updates.put("updatedAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION_CHAT_SESSIONS)
                .document(sessionId)
                .update(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Session metadata updated"))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating session metadata", e));
    }

    /**
     * Get all chat sessions for a user, ordered by most recent
     * Index-free: use documentId prefix range query to fetch user's sessions, then sort by updatedAt DESC locally
     * Expected sessionId format: session_{userId}_{timestamp}
     */
    public void getUserSessions(@NonNull String userId, @NonNull OnSessionsLoadedListener listener) {
        String prefix = "session_" + userId + "_";
        String upper = prefix + "\uf8ff"; // High code point to include all with the prefix

        Log.d(TAG, "Querying sessions by documentId prefix for user: " + userId);

        db.collection(COLLECTION_CHAT_SESSIONS)
                .orderBy(FieldPath.documentId(), Query.Direction.ASCENDING)
                .startAt(prefix)
                .endAt(upper)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<ChatSession> sessions = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        ChatSession session = document.toObject(ChatSession.class);
                        session.setSessionId(document.getId());
                        sessions.add(session);
                    }

                    // Sort by updatedAt DESC locally (fall back to created order if null)
                    sessions.sort((a, b) -> {
                        if (a.getUpdatedAt() == null && b.getUpdatedAt() == null) return 0;
                        if (a.getUpdatedAt() == null) return 1;
                        if (b.getUpdatedAt() == null) return -1;
                        return b.getUpdatedAt().compareTo(a.getUpdatedAt());
                    });

                    Log.d(TAG, "Loaded " + sessions.size() + " sessions for user (prefix query)");
                    listener.onSessionsLoaded(sessions);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading sessions with prefix query", e);
                    // Fallback to no-index method as a safety net
                    getUserSessionsNoIndex(userId, listener);
                });
    }

    /**
     * Get all messages for a specific chat session, ordered by timestamp
     */
    public void getSessionMessages(@NonNull String sessionId, @NonNull OnMessagesLoadedListener listener) {
        db.collection(COLLECTION_CHAT_SESSIONS)
                .document(sessionId)
                .collection(COLLECTION_MESSAGES)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<ChatSessionMessage> messages = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        ChatSessionMessage message = document.toObject(ChatSessionMessage.class);
                        message.setMessageId(document.getId());
                        messages.add(message);
                    }
                    Log.d(TAG, "Loaded " + messages.size() + " messages for session " + sessionId);
                    listener.onMessagesLoaded(messages);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading messages", e);
                    listener.onError(e.getMessage());
                });
    }

    /**
     * Delete a chat session and all its messages
     */
    public void deleteSession(@NonNull String sessionId, @NonNull OnSessionOperationListener listener) {
        // First, delete all messages in the session
        db.collection(COLLECTION_CHAT_SESSIONS)
                .document(sessionId)
                .collection(COLLECTION_MESSAGES)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Delete all messages
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        document.getReference().delete();
                    }

                    // Then delete the session itself
                    db.collection(COLLECTION_CHAT_SESSIONS)
                            .document(sessionId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Session deleted: " + sessionId);
                                listener.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error deleting session", e);
                                listener.onError(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting messages", e);
                    listener.onError(e.getMessage());
                });
    }

    /**
     * Update session title
     */
    public void updateSessionTitle(@NonNull String sessionId, @NonNull String title,
                                   @NonNull OnSessionOperationListener listener) {
        db.collection(COLLECTION_CHAT_SESSIONS)
                .document(sessionId)
                .update("title", title)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Session title updated: " + sessionId);
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating session title", e);
                    listener.onError(e.getMessage());
                });
    }

    /**
     * Get user sessions without orderBy (fallback method when index not ready)
     * This doesn't require composite index but results won't be sorted by Firestore
     */
    public void getUserSessionsNoIndex(@NonNull String userId, @NonNull OnSessionsLoadedListener listener) {
        Log.d(TAG, "Using fallback query (no index required)");

        db.collection(COLLECTION_CHAT_SESSIONS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<ChatSession> sessions = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        ChatSession session = document.toObject(ChatSession.class);
                        session.setSessionId(document.getId());
                        sessions.add(session);
                    }

                    // Sort in Java instead of Firestore
                    java.util.Collections.sort(sessions, (a, b) -> {
                        if (a.getUpdatedAt() == null) return 1;
                        if (b.getUpdatedAt() == null) return -1;
                        return b.getUpdatedAt().compareTo(a.getUpdatedAt());
                    });

                    Log.d(TAG, "Loaded " + sessions.size() + " sessions (sorted locally)");
                    listener.onSessionsLoaded(sessions);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading sessions (fallback)", e);
                    listener.onError(e.getMessage() != null ? e.getMessage() : "Lỗi không xác định");
                });
    }

    /**
     * Check if a session exists
     */
    public void sessionExists(@NonNull String sessionId, @NonNull OnSessionExistsListener listener) {
        db.collection(COLLECTION_CHAT_SESSIONS)
                .document(sessionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    listener.onResult(documentSnapshot.exists());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking session existence", e);
                    listener.onResult(false);
                });
    }

    // Callback interfaces
    public interface OnSessionOperationListener {
        void onSuccess();
        void onError(String error);
    }

    public interface OnMessageOperationListener {
        void onSuccess(String messageId);
        void onError(String error);
    }

    public interface OnSessionsLoadedListener {
        void onSessionsLoaded(List<ChatSession> sessions);
        void onError(String error);
    }

    public interface OnMessagesLoadedListener {
        void onMessagesLoaded(List<ChatSessionMessage> messages);
        void onError(String error);
    }

    public interface OnSessionExistsListener {
        void onResult(boolean exists);
    }
}
