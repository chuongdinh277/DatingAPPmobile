package com.example.couple_app.ui.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.couple_app.R;
import com.example.couple_app.ui.adapters.NotificationAdapter;
import com.example.couple_app.managers.NotificationManager;
import com.example.couple_app.data.model.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private static final String TAG = "NotificationActivity";
    private RecyclerView rcNotification;

    private NotificationAdapter notificationAdapter;

    private List<Notification> notificationList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notification_scene);
        rcNotification = findViewById(R.id.rc_notification);

        // Initialize back button
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Initialize empty list and RecyclerView
        notificationList = new ArrayList<>();
        setupRecyclerView();

        // Load notifications
        setNotificationList();

        // Mark all notifications as read when opening this activity
        markAllNotificationsAsRead();
    }

    private void setupRecyclerView() {
        notificationAdapter = new NotificationAdapter(notificationList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(false);
        rcNotification.setLayoutManager(layoutManager);
        rcNotification.setAdapter(notificationAdapter);
    }

    private void setNotificationList() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            Log.e(TAG, "❌ Chưa đăng nhập");
            return;
        }
        String userId = firebaseUser.getUid();

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Log.d(TAG, "Loading notifications for user: " + userId);
        Log.d(TAG, "Path: notifications/" + userId);

        // Call NotificationManager to fetch notifications for this user
        NotificationManager.getInstance().getListNotifications(userId, new NotificationManager.NotificationListCallback() {
            @Override
            public void onSuccess(List<Notification> notifications) {
                Log.d(TAG, "✅ Notifications loaded successfully");
                Log.d(TAG, "  Total notifications: " + (notifications != null ? notifications.size() : 0));

                // Update local list and notify adapter on success
                if (notifications == null || notifications.isEmpty()) {
                    Log.w(TAG, "⚠️ No notifications found for user: " + userId);
                    notificationList = new ArrayList<>();
                } else {
                    notificationList = notifications;
                    // Log each notification for debugging
                    for (int i = 0; i < notifications.size(); i++) {
                        Notification n = notifications.get(i);
                        Log.d(TAG, "  [" + i + "] " + n.getMessage() + " (from: " + n.getSenderId() + ")");
                    }
                }

                if (notificationAdapter != null) {
                    runOnUiThread(() -> {
                        notificationAdapter.setNotifications(notificationList);
                        Log.d(TAG, "✅ Adapter updated with " + notificationList.size() + " notifications");
                    });
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "❌ Lỗi khi lấy danh sách thông báo: " + errorMessage);
                runOnUiThread(() -> {
                    notificationList = new ArrayList<>();
                    if (notificationAdapter != null) {
                        notificationAdapter.setNotifications(notificationList);
                    }
                });
            }
        });
    }

    /**
     * Mark all notifications as read when user opens NotificationActivity
     */
    private void markAllNotificationsAsRead() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            return;
        }
        String userId = firebaseUser.getUid();

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Log.d(TAG, "Marking all notifications as read for user: " + userId);

        NotificationManager.getInstance().markAllAsRead(userId, new NotificationManager.NotificationCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "✅ All notifications marked as read successfully");
                // Badge will be automatically updated by BaseActivity's listener
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Failed to mark notifications as read: " + error);
            }
        });
    }

}
