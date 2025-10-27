package com.example.btl_mobileapp.managers;

import android.util.Log;
import com.example.btl_mobileapp.models.User;
import com.example.btl_mobileapp.utils.FCMNotificationSender;
import com.google.firebase.Timestamp;
import java.util.HashMap;
import java.util.Map;

/**
 * Manager để xử lý thông báo
 * GỬI TRỰC TIẾP từ app qua FCM API (không cần Cloud Functions)
 */
public class NotificationManager {
    private static final String TAG = "NotificationManager";
    private static NotificationManager instance;
    private static final String NOTIFICATIONS_COLLECTION = "notifications";

    private NotificationManager() {}

    public static synchronized NotificationManager getInstance() {
        if (instance == null) {
            instance = new NotificationManager();
        }
        return instance;
    }

    /**
     * Gửi thông báo tin nhắn mới TRỰC TIẾP đến đối phương
     * Sử dụng FCM API để gửi notification ngay lập tức
     */
    public void sendMessageNotification(String recipientUserId, String senderName, String messageText, String coupleId, String senderId) {
        // Lấy FCM token của người nhận
        DatabaseManager.getInstance().getUser(recipientUserId, new DatabaseManager.DatabaseCallback<User>() {
            @Override
            public void onSuccess(User recipient) {
                String fcmToken = recipient.getFcmToken();
                if (fcmToken != null && !fcmToken.isEmpty()) {
                    // Gửi notification TRỰC TIẾP qua FCM API
                    sendDirectNotification(fcmToken, senderName, messageText, coupleId, senderId, recipientUserId);
                } else {
                    Log.w(TAG, "Recipient doesn't have FCM token");
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error getting recipient user: " + error);
            }
        });
    }

    /**
     * Gửi notification trực tiếp qua FCM API
     */
    private void sendDirectNotification(String fcmToken, String senderName, String messageText,
                                        String coupleId, String senderId, String recipientUserId) {
        // Truncate message nếu quá dài
        String displayMessage = messageText.length() > 100
                ? messageText.substring(0, 97) + "..."
                : messageText;

        FCMNotificationSender.sendNotification(
                fcmToken,
                senderName,
                displayMessage,
                coupleId,
                senderId,
                senderName,
                new FCMNotificationSender.NotificationCallback() {
                    @Override
                    public void onSuccess(String response) {
                        Log.d(TAG, "✅ Notification sent successfully to " + recipientUserId);
                        // Optional: Lưu log vào Firestore để tracking
                        saveNotificationLog(recipientUserId, senderId, senderName, messageText, true, null);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "❌ Failed to send notification: " + error);
                        // Optional: Lưu log lỗi
                        saveNotificationLog(recipientUserId, senderId, senderName, messageText, false, error);

                        // Fallback: Tạo notification document cho Cloud Function xử lý (nếu có)
                        createNotificationDocumentFallback(recipientUserId, senderName, messageText, coupleId, senderId, fcmToken);
                    }
                }
        );
    }

    /**
     * Lưu log notification (optional - để tracking và debug)
     */
    private void saveNotificationLog(String recipientId, String senderId, String senderName,
                                     String messageText, boolean success, String error) {
        Map<String, Object> log = new HashMap<>();
        log.put("recipientId", recipientId);
        log.put("senderId", senderId);
        log.put("senderName", senderName);
        log.put("messageText", messageText);
        log.put("success", success);
        log.put("timestamp", Timestamp.now());

        if (error != null) {
            log.put("error", error);
        }

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("notification_logs")
                .add(log)
                .addOnSuccessListener(ref -> Log.d(TAG, "Notification log saved"))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving notification log", e));
    }

    /**
     * Tạo notification document làm fallback (nếu gửi trực tiếp thất bại)
     * Cloud Function sẽ xử lý nếu có
     */
    private void createNotificationDocumentFallback(String recipientUserId, String senderName, String messageText,
                                                    String coupleId, String senderId, String fcmToken) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("recipientUserId", recipientUserId);
        notification.put("fcmToken", fcmToken);
        notification.put("title", senderName);
        notification.put("body", messageText);
        notification.put("coupleId", coupleId);
        notification.put("senderId", senderId);
        notification.put("senderName", senderName);
        notification.put("type", "message");
        notification.put("timestamp", Timestamp.now());
        notification.put("sent", false);
        notification.put("isFallback", true);

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection(NOTIFICATIONS_COLLECTION)
                .add(notification)
                .addOnSuccessListener(ref -> Log.d(TAG, "Fallback notification document created"))
                .addOnFailureListener(e -> Log.e(TAG, "Error creating fallback notification", e));
    }

    /**
     * LEGACY METHOD - Tạo notification document trong Firestore
     * Giữ lại để tương thích với Cloud Function (nếu có)
     */
    @Deprecated
    private void createNotificationDocument(String recipientUserId, String senderName, String messageText,
                                            String coupleId, String senderId, String fcmToken) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("recipientUserId", recipientUserId);
        notification.put("fcmToken", fcmToken);
        notification.put("title", senderName);
        notification.put("body", messageText);
        notification.put("coupleId", coupleId);
        notification.put("senderId", senderId);
        notification.put("senderName", senderName);
        notification.put("type", "message");
        notification.put("timestamp", Timestamp.now());
        notification.put("sent", false);

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection(NOTIFICATIONS_COLLECTION)
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Notification document created: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating notification document", e);
                });
    }
}