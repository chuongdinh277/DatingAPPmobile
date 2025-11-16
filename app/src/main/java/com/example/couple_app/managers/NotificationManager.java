package com.example.couple_app.managers;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.couple_app.models.Notification;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class NotificationManager {
    private static final String TAG = "NotificationManager";
    private static NotificationManager instance;
    private final DatabaseReference rootRef;

    private static final String NOTIFICATION_PATH = "notifications";

    private NotificationManager() {
        String url = "https://couples-app-b83be-default-rtdb.asia-southeast1.firebasedatabase.app";
        rootRef = FirebaseDatabase.getInstance(url).getReference();

        Log.d(TAG, "NotificationManager initialized with URL: " + url);
    }

    public static synchronized NotificationManager getInstance() {
        if (instance == null) {
            instance = new NotificationManager();
        }
        return instance;
    }

    public interface NotificationCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface NotificationListCallback {
        void onSuccess(List<Notification> notifications);
        void onError(String error);
    }

    public interface NotificationSingleCallback {
        void onSuccess(Notification notification);
        void onError(String error);
    }

    public void createNotification(String senderId, String receiverId, String message, NotificationCallback callback) {
        try {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            Log.d(TAG, "createNotification called:");
            Log.d(TAG, "  senderId: " + senderId);
            Log.d(TAG, "  receiverId: " + receiverId);
            Log.d(TAG, "  message: " + message);
            Log.d(TAG, "  Database URL: https://couples-app-b83be-default-rtdb.firebaseio.com/");

            DatabaseReference receiverRef = rootRef.child(NOTIFICATION_PATH).child(receiverId);
            String notificationId = receiverRef.push().getKey();

            Log.d(TAG, "  Generated notificationId: " + notificationId);
            Log.d(TAG, "  Full path: " + NOTIFICATION_PATH + "/" + receiverId + "/" + notificationId);

            if (notificationId == null) {
                Log.e(TAG, "❌ Failed to generate notificationId");
                callback.onError("Không thể tạo ID cho thông báo");
                return;
            }

            Notification notification = new Notification(senderId, receiverId, message);
            Log.d(TAG, "  Notification object created:");
            Log.d(TAG, "    - senderId: " + notification.getSenderId());
            Log.d(TAG, "    - receiverId: " + notification.getReceiverId());
            Log.d(TAG, "    - message: " + notification.getMessage());
            Log.d(TAG, "    - timestamp: " + notification.getTimestamp());
            Log.d(TAG, "    - read: " + notification.getRead());
            Log.d(TAG, "    - type: " + notification.getType());

            Log.d(TAG, "  Attempting to save to: " + receiverRef.child(notificationId).toString());

            receiverRef.child(notificationId)
                    .setValue(notification)
                    // Lấy notificationId vừa tạo cập nhật vào bên trong thuộc tính của của đối tượng notification cho đồng bộ
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "✅ Notification saved to Realtime Database successfully");
                        receiverRef.child(notificationId).child("notificationId")
                                .setValue(notificationId)
                                .addOnSuccessListener(unused -> Log.d(TAG, "✅ notificationId field updated"))
                                .addOnFailureListener(e -> Log.e(TAG, "❌ Failed to update notificationId field", e));
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ Failed to save notification to Realtime Database", e);
                        Log.e(TAG, "❌ Error type: " + e.getClass().getName());
                        Log.e(TAG, "❌ Error message: " + e.getMessage());
                        if (e.getCause() != null) {
                            Log.e(TAG, "❌ Cause: " + e.getCause().getMessage());
                        }
                        e.printStackTrace();
                        callback.onError(e.getMessage() != null ? e.getMessage() : "Unknown error");
                    });
        } catch (Exception e) {
            Log.e(TAG, "❌ Exception in createNotification", e);
            e.printStackTrace();
            callback.onError("Exception: " + e.getMessage());
        }
    }

    public void getListNotifications(String receiverId, NotificationListCallback callback) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Log.d(TAG, "getListNotifications called for receiverId: " + receiverId);

        DatabaseReference receiverRef = rootRef.child(NOTIFICATION_PATH).child(receiverId);
        Log.d(TAG, "  Query path: " + receiverRef.toString());

        receiverRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "  onDataChange called");
                Log.d(TAG, "  Snapshot exists: " + snapshot.exists());
                Log.d(TAG, "  Children count: " + snapshot.getChildrenCount());

                List<Notification> list = new ArrayList<>();
                int index = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    Log.d(TAG, "  Processing child [" + index + "]: " + child.getKey());
                    try {
                        Notification n = child.getValue(Notification.class);
                        if (n != null) {
                            list.add(n);
                            Log.d(TAG, "    ✅ Parsed: " + n.getMessage());
                        } else {
                            Log.w(TAG, "    ⚠️ Notification is null after parsing");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "    ❌ Error parsing notification", e);
                    }
                    index++;
                }

                Log.d(TAG, "✅ Total notifications loaded: " + list.size());
                callback.onSuccess(list);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Database error: " + error.getMessage());
                Log.e(TAG, "❌ Error code: " + error.getCode());
                Log.e(TAG, "❌ Error details: " + error.getDetails());
                callback.onError(error.getMessage());
            }
        });
    }

    public void getNotification(String receiverId, String notificationId, NotificationSingleCallback callback) {
        DatabaseReference notiRef = rootRef.child(NOTIFICATION_PATH).child(receiverId).child(notificationId);
        notiRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Notification n = snapshot.getValue(Notification.class);
                if (n != null) {
                    callback.onSuccess(n);
                } else {
                    callback.onError("Không tìm thấy thông báo");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    public void deleteNotification(String receiverId, String notificationId, NotificationCallback callback) {
        DatabaseReference notiRef = rootRef.child(NOTIFICATION_PATH).child(receiverId).child(notificationId);
        notiRef.removeValue()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void deleteAllNotifications(String receiverId, NotificationCallback callback) {
        DatabaseReference receiverRef = rootRef.child(NOTIFICATION_PATH).child(receiverId);
        receiverRef.removeValue()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void markAsRead(String receiverId, String notificationId, NotificationCallback callback) {
        DatabaseReference notiRef = rootRef.child(NOTIFICATION_PATH).child(receiverId).child(notificationId);
        notiRef.child("read").setValue(true)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Marked notification as read: " + notificationId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to mark as read", e);
                    callback.onError(e.getMessage());
                });
    }

    public interface UnreadCountCallback {
        void onCount(int count);
        void onError(String error);
    }

    public void getUnreadCount(String receiverId, UnreadCountCallback callback) {
        DatabaseReference receiverRef = rootRef.child(NOTIFICATION_PATH).child(receiverId);
        receiverRef.orderByChild("read").equalTo(false).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count = (int) snapshot.getChildrenCount();
                callback.onCount(count);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    public void listenForUnreadCount(String receiverId, UnreadCountCallback callback) {
        DatabaseReference receiverRef = rootRef.child(NOTIFICATION_PATH).child(receiverId);
        receiverRef.orderByChild("read").equalTo(false).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count = (int) snapshot.getChildrenCount();
                callback.onCount(count);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    /**
     * Mark all notifications as read for a user
     */
    public void markAllAsRead(String receiverId, NotificationCallback callback) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Log.d(TAG, "markAllAsRead called for receiverId: " + receiverId);

        DatabaseReference receiverRef = rootRef.child(NOTIFICATION_PATH).child(receiverId);

        // Get all unread notifications
        receiverRef.orderByChild("read").equalTo(false).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    Log.d(TAG, "  No unread notifications to mark");
                    callback.onSuccess();
                    return;
                }

                Log.d(TAG, "  Found " + snapshot.getChildrenCount() + " unread notifications");
                int totalCount = (int) snapshot.getChildrenCount();
                final int[] markedCount = {0};
                final boolean[] hasError = {false};

                // Mark each notification as read
                for (DataSnapshot child : snapshot.getChildren()) {
                    String notificationId = child.getKey();
                    receiverRef.child(notificationId).child("read").setValue(true)
                        .addOnSuccessListener(aVoid -> {
                            markedCount[0]++;
                            Log.d(TAG, "    ✅ Marked " + notificationId + " as read (" + markedCount[0] + "/" + totalCount + ")");

                            if (markedCount[0] == totalCount && !hasError[0]) {
                                Log.d(TAG, "✅ All notifications marked as read");
                                callback.onSuccess();
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (!hasError[0]) {
                                hasError[0] = true;
                                Log.e(TAG, "❌ Failed to mark notification as read: " + notificationId, e);
                                callback.onError("Failed to mark some notifications as read");
                            }
                        });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Database error: " + error.getMessage());
                callback.onError(error.getMessage());
            }
        });
    }
}
