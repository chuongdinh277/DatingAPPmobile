package com.example.couple_app.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.example.couple_app.R;
import com.example.couple_app.activities.MessengerActivity;
import com.example.couple_app.managers.DatabaseManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Service nhận thông báo đẩy từ Firebase Cloud Messaging (FCM)
 */
public class FirebaseCloudMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "couple_app_messages";
    private static final String CHANNEL_NAME = "Messages";
    private static int notificationId = 0;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "📩 From: " + remoteMessage.getFrom());

        // Check if message contains data payload
        Map<String, String> data = remoteMessage.getData();
        if (data.size() > 0) {
            Log.d(TAG, "📦 Message data payload: " + data);
        }

        // Check if message contains notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "🔔 Message Notification Title: " + remoteMessage.getNotification().getTitle());
            Log.d(TAG, "🔔 Message Notification Body: " + remoteMessage.getNotification().getBody());
        }

        // ✅ Xử lý notification (dù có notification payload hay chỉ có data)
        String title = null;
        String body = null;

        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        }

        // Lấy data từ payload (đồng bộ với backend: fromUserId, fromUsername)
        String coupleId = data.get("coupleId");
        String fromUserId = data.get("fromUserId");
        String fromUsername = data.get("fromUsername");

        // Fallback cho compatibility với code cũ
        if (fromUserId == null) fromUserId = data.get("senderId");
        if (fromUsername == null) fromUsername = data.get("senderName");

        Log.d(TAG, "📝 Parsed data - coupleId: " + coupleId + ", fromUserId: " + fromUserId + ", fromUsername: " + fromUsername);

        // ✅ KIỂM TRA XEM USER CÓ ĐANG XEM CHAT KHÔNG
        if (coupleId != null && MessengerActivity.isViewingChat(coupleId)) {
            Log.d(TAG, "🚫 User is currently viewing this chat - notification suppressed");
            return; // Không hiển thị notification
        }

        // Nếu có title và body, hiển thị notification
        if (title != null && body != null) {
            sendNotification(title, body, coupleId, fromUserId, fromUsername);
        } else {
            Log.w(TAG, "⚠️ Notification title or body is null, skipping notification display");
        }
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "🔄 Refreshed FCM token: " + token);

        // Update token in Firestore
        sendRegistrationToServer(token);
    }

    private void sendNotification(String title, String messageBody, String coupleId, String senderId, String senderName) {
        // ✅ Tạo notification ID duy nhất để tránh ghi đè
        int notifId = notificationId++;
        if (notificationId > 1000) notificationId = 0; // Reset sau 1000 để tránh overflow

        Log.d(TAG, "🔔 Creating notification with ID: " + notifId);

        Intent intent = new Intent(this, MessengerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // ✅ Đồng bộ với MessengerActivity intent extras
        if (coupleId != null) intent.putExtra("coupleId", coupleId);
        if (senderId != null) intent.putExtra("partnerId", senderId);
        if (senderName != null) intent.putExtra("partnerName", senderName);

        Log.d(TAG, "📤 Intent extras - coupleId: " + coupleId + ", partnerId: " + senderId + ", partnerName: " + senderName);

        PendingIntent pendingIntent = PendingIntent.getActivity(
            this,
            notifId, // ✅ Dùng notifId riêng cho mỗi notification
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // ✅ Tạo notification channel trước (Android 8.0+)
        createNotificationChannel();

        // ✅ Lấy âm thanh thông báo (tùy chỉnh hoặc mặc định)
        Uri soundUri = getNotificationSoundUri();

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Đảm bảo icon này tồn tại
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        // ✅ Thêm style cho message dài
        if (messageBody != null && messageBody.length() > 50) {
            notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(messageBody));
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify(notifId, notificationBuilder.build());
            Log.d(TAG, "✅ Notification displayed successfully with ID: " + notifId);
        } else {
            Log.e(TAG, "❌ NotificationManager is null");
        }
    }

    /**
     * ✅ Lấy URI âm thanh thông báo
     * Ưu tiên: Custom sound → Default system sound
     */
    private Uri getNotificationSoundUri() {
        try {
            // Thử lấy custom sound từ res/raw/relax_message_tone.ogg
            int soundResId = getResources().getIdentifier("relax_message_tone", "raw", getPackageName());
            if (soundResId != 0) {
                Uri customSoundUri = Uri.parse("android.resource://" + getPackageName() + "/" + soundResId);
                Log.d(TAG, "🔊 Using custom notification sound");
                return customSoundUri;
            } else {
                Log.d(TAG, "🔊 Custom sound not found, using default system sound");
            }
        } catch (Exception e) {
            Log.w(TAG, "⚠️ Error loading custom sound: " + e.getMessage());
        }

        // Fallback: Dùng âm thanh mặc định của hệ thống
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    }

    /**
     * ✅ Tạo notification channel (Android 8.0+)
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager != null) {
                NotificationChannel existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID);

                // Chỉ tạo nếu chưa tồn tại
                if (existingChannel == null) {
                    // ✅ Lấy âm thanh thông báo
                    Uri soundUri = getNotificationSoundUri();

                    // ✅ Cấu hình AudioAttributes cho notification channel
                    AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build();

                    NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                    );
                    channel.setDescription("Notifications for new messages from your partner");
                    channel.enableVibration(true);
                    channel.enableLights(true);
                    channel.setShowBadge(true);
                    channel.setSound(soundUri, audioAttributes); // ✅ Set custom sound

                    notificationManager.createNotificationChannel(channel);
                    Log.d(TAG, "✅ Notification channel created: " + CHANNEL_ID);
                } else {
                    Log.d(TAG, "ℹ️ Notification channel already exists: " + CHANNEL_ID);
                }
            }
        }
    }

    private void sendRegistrationToServer(String token) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            Log.d(TAG, "💾 Updating FCM token for user: " + userId);

            DatabaseManager.getInstance().updateUserFcmToken(userId, token, new DatabaseManager.DatabaseActionCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "✅ FCM token updated successfully in Firestore");
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "❌ Failed to update FCM token: " + error);
                }
            });
        } else {
            Log.w(TAG, "⚠️ Cannot update FCM token: user not logged in");
        }
    }
}
