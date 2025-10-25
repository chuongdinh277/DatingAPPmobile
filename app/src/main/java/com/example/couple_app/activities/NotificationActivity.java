package com.example.couple_app.activities;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.couple_app.R;
import com.example.couple_app.adapters.MessageAdapter;
import com.example.couple_app.adapters.NotificationAdapter;
import com.example.couple_app.managers.NotificationManager;
import com.google.firebase.auth.FirebaseAuth;
import com.example.couple_app.managers.DatabaseManager;
import com.example.couple_app.models.Notification;
import com.google.firebase.auth.FirebaseUser;

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
        setNotificationList();
        setupRecyclerView();
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
            Log.e(TAG, "Chưa đăng nhập");
            return;
        }
        String userId = firebaseUser.getUid();
        NotificationManager.getInstance().getListNotifications(userId, new NotificationManager.NotificationListCallback() {
            @Override
            public void onSuccess(List<Notification> notifications) {
                notificationList = notifications;
                notificationAdapter.setNotifications(notificationList);
                notificationAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String errorMessage) {
                Log.e("TAG", "Lỗi khi lấy danh sách thông báo: " + errorMessage);
            }
        });
    }

}
