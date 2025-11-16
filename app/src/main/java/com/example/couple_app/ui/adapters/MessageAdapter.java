
package com.example.couple_app.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.couple_app.R;
import com.example.couple_app.data.model.Message;
import com.google.firebase.auth.FirebaseAuth;
import de.hdodenhof.circleimageview.CircleImageView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    private static final int VIEW_TYPE_DATE_SEPARATOR = 3;

    private List<Message> messages;
    private String currentUserId;
    private SimpleDateFormat timeFormat;
    private SimpleDateFormat dateFormat;
    private String partnerAvatarUrl;

    public MessageAdapter(List<Message> messages) {
        this.messages = messages;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    }

    public void setPartnerAvatarUrl(String avatarUrl) {
        this.partnerAvatarUrl = avatarUrl;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);

        // Check if this is a date separator (special message type)
        if (message.getMessageType() != null && message.getMessageType().equals("date_separator")) {
            return VIEW_TYPE_DATE_SEPARATOR;
        }

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
        } else if (viewType == VIEW_TYPE_RECEIVED) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_date_separator, parent, false);
            return new DateSeparatorViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);

        if (holder instanceof SentMessageViewHolder) {
            ((SentMessageViewHolder) holder).bind(message);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ((ReceivedMessageViewHolder) holder).bind(message);
        } else if (holder instanceof DateSeparatorViewHolder) {
            ((DateSeparatorViewHolder) holder).bind(message);
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
        CircleImageView ivPartnerAvatar;

        ReceivedMessageViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message_received);
            tvTime = itemView.findViewById(R.id.tv_time_received);
            tvSenderName = itemView.findViewById(R.id.tv_sender_name);
            ivPartnerAvatar = itemView.findViewById(R.id.iv_partner_avatar);
        }

        void bind(Message message) {
            tvMessage.setText(message.getMessage());
            tvSenderName.setText(message.getSenderName() != null ? message.getSenderName() : "Partner");
            if (message.getTimestamp() != null) {
                tvTime.setText(formatTime(message.getTimestamp()));
            }

            // Show avatar only at the last message in a group (like Messenger)
            int position = getAdapterPosition();
            boolean showAvatar = shouldShowAvatar(position);

            if (ivPartnerAvatar != null) {
                if (showAvatar) {
                    ivPartnerAvatar.setVisibility(View.VISIBLE);
                    if (partnerAvatarUrl != null && !partnerAvatarUrl.isEmpty()) {
                        Glide.with(itemView.getContext())
                                .load(partnerAvatarUrl)
                                .placeholder(R.drawable.ic_default_avatar)
                                .error(R.drawable.ic_default_avatar)
                                .into(ivPartnerAvatar);
                    } else {
                        ivPartnerAvatar.setImageResource(R.drawable.ic_default_avatar);
                    }
                } else {
                    ivPartnerAvatar.setVisibility(View.INVISIBLE); // Keep space but invisible
                }
            }
        }

        private boolean shouldShowAvatar(int position) {
            if (position < 0 || position >= messages.size()) return true;

            // Show avatar if:
            // 1. This is the last message
            // 2. Next message is from different sender (current user)
            if (position == messages.size() - 1) return true;

            Message currentMessage = messages.get(position);
            Message nextMessage = messages.get(position + 1);

            // If next message is from current user (sent), show avatar
            return !currentMessage.getSenderId().equals(nextMessage.getSenderId());
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

    /**
     * ViewHolder for date separator
     */
    class DateSeparatorViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;

        DateSeparatorViewHolder(View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_date);
        }

        void bind(Message message) {
            if (message.getTimestamp() != null) {
                String dateText = formatDateSeparator(message.getTimestamp());
                tvDate.setText(dateText);
            }
        }
    }

    /**
     * Format date for separator (Hôm nay, Hôm qua, hoặc dd/MM/yyyy)
     */
    private String formatDateSeparator(Object timestampObj) {
        long millis = coerceToMillis(timestampObj);
        java.util.Calendar messageDate = java.util.Calendar.getInstance();
        messageDate.setTimeInMillis(millis);

        java.util.Calendar today = java.util.Calendar.getInstance();
        java.util.Calendar yesterday = java.util.Calendar.getInstance();
        yesterday.add(java.util.Calendar.DAY_OF_YEAR, -1);

        // Check if same day as today
        if (isSameDay(messageDate, today)) {
            return "Hôm nay";
        }
        // Check if same day as yesterday
        else if (isSameDay(messageDate, yesterday)) {
            return "Hôm qua";
        }
        // Otherwise show date
        else {
            return dateFormat.format(new java.util.Date(millis));
        }
    }

    /**
     * Check if two calendars represent the same day
     */
    private boolean isSameDay(java.util.Calendar cal1, java.util.Calendar cal2) {
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR);
    }
}
