package com.example.couple_app.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.example.couple_app.R;
import com.example.couple_app.data.model.Notification;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
    private List<Notification> notifications;


    private OnNotificationClickListener clickListener;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    public NotificationAdapter(List<Notification> notifications) {
        this.notifications = notifications;
    }

    public void setNotifications(List<Notification> notificationList) {
        this.notifications = notificationList;
        notifyDataSetChanged();
    }

    public void setOnNotificationClickListener(OnNotificationClickListener listener) {
        this.clickListener = listener;
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

        // Format timestamp
        String formattedTime = formatTimestamp(notification.getTimestamp());
        holder.timeNotification.setText(formattedTime);

        // No need to load avatar - we're using icon instead
    }

    private String formatTimestamp(long timestamp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date date = new Date(timestamp);
            return sdf.format(date);
        } catch (Exception e) {
            return "";
        }
    }

    // No longer needed - we don't show avatar anymore
    /*
    public void loadSenderImage(String senderId, NotificationViewHolder holder) {
        databaseManager = DatabaseManager.getInstance();
        databaseManager.getUser(senderId, new DatabaseManager.DatabaseCallback<>() {
            @Override
            public void onSuccess(com.example.couple_app.data.model.User user) {
                String imageSenderUrl = user.getProfilePicUrl() != null ? user.getProfilePicUrl() : null;
                Glide.with(holder.ivSenderImage.getContext())
                        .load(imageSenderUrl)
                        .placeholder(R.drawable.user_icon)
                        .error(R.drawable.user_icon)
                        .into(holder.ivSenderImage);
            }
            @Override
            public void onError(String error) {
                Glide.with(holder.ivSenderImage.getContext())
                        .load(R.drawable.user_icon)
                        .into(holder.ivSenderImage);
            }
        });
    }
    */


    @Override
    public int getItemCount() {
        return notifications.size();
    }

}
