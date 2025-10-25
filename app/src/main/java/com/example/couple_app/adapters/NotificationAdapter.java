package com.example.couple_app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import com.example.couple_app.managers.AuthManager;
import com.example.couple_app.managers.DatabaseManager;

import com.example.couple_app.R;
import com.example.couple_app.models.Notification;
import com.example.couple_app.models.User;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
    private List<Notification> notifications;

    private DatabaseManager databaseManager;

    public NotificationAdapter(List<Notification> notifications) {
        this.notifications = notifications;
    }

    public void setNotifications(List<Notification> notificationList) {
        this.notifications = notificationList;
        notifyDataSetChanged();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        ImageView ivSenderImage;

        TextView contextNotification;

        TextView timeNotification;

        public NotificationViewHolder(@NotNull View itemView) {
            super(itemView);
            ivSenderImage = itemView.findViewById(R.id.iv_user_image);
            contextNotification = itemView.findViewById(R.id.tv_inforNoti);
            timeNotification = itemView.findViewById(R.id.tv_timeNoti);
        }
    }

    @NotNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notifications.get(position);

        holder.contextNotification.setText(notification.getMessage());
        holder.timeNotification.setText(notification.getTimestamp());

        loadSenderImage(notification.getSenderId(), holder);
    }

    public void loadSenderImage(String senderId, NotificationViewHolder holder) {
        final String[] imageSenderUrl = new String[1];
        databaseManager = DatabaseManager.getInstance();
        databaseManager.getUser(senderId, new DatabaseManager.DatabaseCallback<>() {
            @Override
            public void onSuccess(com.example.couple_app.models.User user) {
                imageSenderUrl[0] = user.getProfilePicUrl() != null ? user.getProfilePicUrl() : null;
            }
            @Override
            public void onError(String error) {
                imageSenderUrl[0] = null;
            }
        });

        Glide.with(holder.ivSenderImage.getContext())
                .load(imageSenderUrl)
                .placeholder(R.drawable.user_icon)
                .error(R.drawable.user_icon)
                .into(holder.ivSenderImage);
    }



    @Override
    public int getItemCount() {
        return notifications.size();
    }

}
