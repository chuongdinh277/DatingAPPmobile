package com.example.couple_app.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utility class để gửi FCM notification trực tiếp từ app
 * Sử dụng FCM HTTP v1 API
 */
public class FCMNotificationSender {
    private static final String TAG = "FCMNotificationSender";
    private static final String FCM_API_URL = "https://fcm.googleapis.com/fcm/send";

    // ⚠️ QUAN TRỌNG: Lấy Server Key từ Firebase Console
    // Firebase Console → Project Settings → Cloud Messaging → Server Key
    // CHÚ Ý: Đây là cách LEGACY, không nên dùng cho production
    // Nên dùng Cloud Functions hoặc backend server

    // ❌ SAI: Bạn đang dùng Web API Key (bắt đầu bằng AIza...)
    // ✅ ĐÚNG: Cần dùng Server Key (bắt đầu bằng AAAA...)

    // Cách lấy Server Key ĐÚNG:
    // 1. Vào Firebase Console: https://console.firebase.google.com/
    // 2. Chọn project → Settings (⚙️) → Cloud Messaging
    // 3. Tìm "Cloud Messaging API (Legacy)"
    // 4. Nếu không thấy, click "Enable Cloud Messaging API (Legacy)"
    // 5. Copy "Server key" (bắt đầu bằng AAAA...)
    private static final String SERVER_KEY = "YOUR_SERVER_KEY_HERE"; // Thay bằng Server Key bắt đầu bằng AAAA...

    private static final ExecutorService executor = Executors.newFixedThreadPool(2);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Interface callback cho kết quả gửi notification
     */
    public interface NotificationCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    /**
     * Gửi notification đến một FCM token
     *
     * @param fcmToken FCM token của người nhận
     * @param title Tiêu đề notification
     * @param body Nội dung notification
     * @param coupleId ID của couple
     * @param senderId ID người gửi
     * @param senderName Tên người gửi
     * @param callback Callback kết quả
     */
    public static void sendNotification(
            String fcmToken,
            String title,
            String body,
            String coupleId,
            String senderId,
            String senderName,
            NotificationCallback callback
    ) {
        if (fcmToken == null || fcmToken.isEmpty()) {
            if (callback != null) {
                mainHandler.post(() -> callback.onError("FCM token is empty"));
            }
            return;
        }

        if (SERVER_KEY.equals("YOUR_SERVER_KEY_HERE")) {
            Log.e(TAG, "⚠️ SERVER_KEY chưa được cấu hình! Vui lòng cập nhật SERVER_KEY trong FCMNotificationSender.java");
            if (callback != null) {
                mainHandler.post(() -> callback.onError("Server key not configured"));
            }
            return;
        }

        executor.execute(() -> {
            try {
                JSONObject notification = new JSONObject();
                notification.put("title", title);
                notification.put("body", body);
                notification.put("sound", "default");
                notification.put("icon", "ic_notification");
                notification.put("color", "#FF6B9D");

                JSONObject data = new JSONObject();
                data.put("coupleId", coupleId != null ? coupleId : "");
                data.put("senderId", senderId != null ? senderId : "");
                data.put("senderName", senderName != null ? senderName : "");
                data.put("messageText", body != null ? body : "");
                data.put("type", "message");
                data.put("clickAction", "OPEN_MESSENGER");

                JSONObject message = new JSONObject();
                message.put("to", fcmToken);
                message.put("notification", notification);
                message.put("data", data);
                message.put("priority", "high");

                JSONObject androidConfig = new JSONObject();
                androidConfig.put("priority", "high");
                JSONObject androidNotification = new JSONObject();
                androidNotification.put("channelId", "couple_app_messages");
                androidNotification.put("sound", "default");
                androidConfig.put("notification", androidNotification);
                message.put("android", androidConfig);

                String response = sendPostRequest(FCM_API_URL, message.toString());

                Log.d(TAG, "✅ Notification sent successfully: " + response);

                if (callback != null) {
                    mainHandler.post(() -> callback.onSuccess(response));
                }
            } catch (JSONException e) {
                Log.e(TAG, "❌ Error creating JSON: " + e.getMessage(), e);
                if (callback != null) {
                    mainHandler.post(() -> callback.onError("JSON error: " + e.getMessage()));
                }
            } catch (IOException e) {
                Log.e(TAG, "❌ Error sending notification: " + e.getMessage(), e);
                if (callback != null) {
                    mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
                }
            }
        });
    }

    /**
     * Gửi HTTP POST request đến FCM API
     */
    private static String sendPostRequest(String urlString, String jsonPayload) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "key=" + SERVER_KEY);
            connection.setDoOutput(true);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);

            // Gửi payload
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Đọc response
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                return "Success: " + responseCode;
            } else {
                throw new IOException("HTTP error code: " + responseCode);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Gửi notification đơn giản (overload method)
     */
    public static void sendSimpleNotification(
            String fcmToken,
            String title,
            String body,
            NotificationCallback callback
    ) {
        sendNotification(fcmToken, title, body, null, null, null, callback);
    }
}
