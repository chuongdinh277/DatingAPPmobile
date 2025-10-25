package com.example.couple_app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.couple_app.R;
import com.example.couple_app.models.Message;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private List<Message> messages;
    private String currentUserId;
    private SimpleDateFormat timeFormat;

    public MessageAdapter(List<Message> messages) {
        this.messages = messages;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        if (message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);

        if (holder instanceof SentMessageViewHolder) {
            ((SentMessageViewHolder) holder).bind(message);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ((ReceivedMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void updateMessages(List<Message> newMessages) {
        this.messages = newMessages;
        notifyDataSetChanged();
    }

    class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        ImageView ivReadStatus;

        SentMessageViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message_sent);
            tvTime = itemView.findViewById(R.id.tv_time_sent);
            ivReadStatus = itemView.findViewById(R.id.iv_read_status);
        }

        void bind(Message message) {
            tvMessage.setText(message.getMessage());
            if (message.getTimestamp() != null) {
                tvTime.setText(formatTime(message.getTimestamp()));
            }

            // ✅ Chỉ hiển thị tick đôi khi người nhận ĐÃ ĐỌC tin nhắn
            if (ivReadStatus != null) {
                if (message.isRead()) {
                    // Người nhận đã xem -> Hiển thị tick đôi màu xanh
                    ivReadStatus.setVisibility(View.VISIBLE);
                    ivReadStatus.setImageResource(R.drawable.ic_check_double);
                    // Có thể đổi màu thành xanh để dễ nhận biết
                    ivReadStatus.setColorFilter(0xFF4FC3F7); // Màu xanh nhạt
                } else {
                    // Tin nhắn đã gửi nhưng chưa được đọc -> Hiển thị tick đơn màu trắng
                    ivReadStatus.setVisibility(View.VISIBLE);
                    ivReadStatus.setImageResource(R.drawable.ic_check_single);
                    ivReadStatus.setColorFilter(0xFFE8E8E8); // Màu trắng/xám nhạt
                }
            }
        }
    }

    class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime, tvSenderName;

        ReceivedMessageViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message_received);
            tvTime = itemView.findViewById(R.id.tv_time_received);
            tvSenderName = itemView.findViewById(R.id.tv_sender_name);
        }

        void bind(Message message) {
            tvMessage.setText(message.getMessage());
            tvSenderName.setText(message.getSenderName() != null ? message.getSenderName() : "Partner");
            if (message.getTimestamp() != null) {
                tvTime.setText(formatTime(message.getTimestamp()));
            }
        }
    }

    private String formatTime(Object timestampObj) {
        long millis = coerceToMillis(timestampObj);
        return timeFormat.format(new java.util.Date(millis));
    }

    private long coerceToMillis(Object ts) {
        if (ts == null) return System.currentTimeMillis();
        if (ts instanceof Long) return (Long) ts;
        if (ts instanceof Double) return ((Double) ts).longValue();
        // Fallback to current time
        return System.currentTimeMillis();
    }
}
