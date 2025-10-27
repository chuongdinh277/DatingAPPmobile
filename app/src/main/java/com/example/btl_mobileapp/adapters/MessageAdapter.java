package com.example.btl_mobileapp.adapters;

import android.content.Context; // <-- BỔ SUNG
import android.graphics.Bitmap; // <-- BỔ SUNG
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btl_mobileapp.R;
import com.example.btl_mobileapp.models.Message;
import com.example.btl_mobileapp.utils.AvatarCache; // <-- BỔ SUNG
import com.google.firebase.auth.FirebaseAuth;
import de.hdodenhof.circleimageview.CircleImageView; // <-- BỔ SUNG

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private List<Message> messages;
    private String currentUserId;
    private SimpleDateFormat timeFormat;
    private Context context; // <-- BỔ SUNG

    // ✅ SỬA Constructor: Thêm Context để lấy AvatarCache
    public MessageAdapter(List<Message> messages, Context context) {
        this.messages = messages;
        this.context = context; // <-- BỔ SUNG
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

    // --- ViewHolder cho tin nhắn GỬI (Không đổi) ---
    class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;

        SentMessageViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message_sent);
            tvTime = itemView.findViewById(R.id.tv_time_sent);
        }

        void bind(Message message) {
            tvMessage.setText(message.getMessage());
            if (message.getTimestamp() != null) {
                tvTime.setText(formatTime(message.getTimestamp()));
            }
        }
    }

    // --- ViewHolder cho tin nhắn NHẬN (Đã sửa) ---
    class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime, tvSenderName;
        CircleImageView ivAvatar; // <-- BỔ SUNG: Thêm ImageView cho avatar

        ReceivedMessageViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message_received);
            tvTime = itemView.findViewById(R.id.tv_time_received);
            tvSenderName = itemView.findViewById(R.id.tv_sender_name);
            ivAvatar = itemView.findViewById(R.id.iv_avatar_received); // <-- BỔ SUNG: Tìm ID của avatar
        }

        void bind(Message message) {
            tvMessage.setText(message.getMessage());
            tvSenderName.setText(message.getSenderName() != null ? message.getSenderName() : "Partner");
            if (message.getTimestamp() != null) {
                tvTime.setText(formatTime(message.getTimestamp()));
            }

            // <-- BỔ SUNG: Logic load avatar từ cache -->
            if (ivAvatar != null) {
                Bitmap partnerAvatar = AvatarCache.getPartnerCachedBitmap(context);
                if (partnerAvatar != null) {
                    ivAvatar.setImageBitmap(partnerAvatar);
                } else {
                    // Nếu cache rỗng, dùng ảnh mặc định từ XML
                    ivAvatar.setImageResource(R.drawable.ic_default_avatar);
                }
            }
        }
    }

    // --- Các hàm helper (Không đổi) ---
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