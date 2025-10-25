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
        String url = "https://couples-app-b83be-default-rtdb.firebaseio.com/";
        rootRef = FirebaseDatabase.getInstance(url).getReference();
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
        DatabaseReference receiverRef = rootRef.child(NOTIFICATION_PATH).child(receiverId);
        String notificationId = receiverRef.push().getKey();

        if (notificationId == null) {
            callback.onError("Không thể tạo ID cho thông báo");
            return;
        }

        Notification notification = new Notification(senderId, receiverId, message);

        receiverRef.child(notificationId)
                .setValue(notification)
                // Lấy notificationId vừa tạo cập nhật vào bên trong thuộc tính của của đối tượng notification cho đồng bộ
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Tạo notification thành công");
                    receiverRef.child(notificationId).child("notificationId")
                            .setValue(notificationId)
                            .addOnSuccessListener(unused -> Log.d(TAG, "Cập nhật notificationId thành công"))
                            .addOnFailureListener(e -> Log.e(TAG, "Lỗi khi cập nhật notificationId", e));
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi tạo notification", e);
                    callback.onError(e.getMessage());
                });
    }

    public void getListNotifications(String receiverId, NotificationListCallback callback) {
        DatabaseReference receiverRef = rootRef.child(NOTIFICATION_PATH).child(receiverId);
        receiverRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Notification> list = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Notification n = child.getValue(Notification.class);
                    if (n != null) {
                        list.add(n);
                    }
                }
                callback.onSuccess(list);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
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
}
