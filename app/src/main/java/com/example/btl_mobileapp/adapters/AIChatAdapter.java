package com.example.btl_mobileapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btl_mobileapp.R;
import com.example.btl_mobileapp.managers.ChatMemoryManager;
import com.example.btl_mobileapp.models.MessageChatBot;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class AIChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_AI = 2;

    private final ChatMemoryManager memory;

    public AIChatAdapter() {
        this.memory = ChatMemoryManager.getInstance();
    }


    @Override
    public int getItemViewType(int position) {
        return position % 2 == 0 ? VIEW_TYPE_USER : VIEW_TYPE_AI;
    }
    // Bởi vì là trong đoạn chat Ai và người dùng thì người dùng hỏi 1 câu thì Ai phải trả lời xong đã người dùng mới hỏi tiếp


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            return new UserMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            return new AIMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int index = position / 2;

        if (getItemViewType(position) == VIEW_TYPE_USER) {
            if (index < memory.getMessages("user").size()) {
                MessageChatBot message = memory.getMessages("user").get(index);
                ((UserMessageViewHolder) holder).bind(message);
            }
        } else {
            if (index < memory.getMessages("AI").size()) {
                MessageChatBot message = memory.getMessages("AI").get(index);
                ((AIMessageViewHolder) holder).bind(message);
            }
        }
    }


    @Override
    public int getItemCount() {
        int userSize = memory.getMessages("user").size();
        int aiSize = memory.getMessages("AI").size();
        return Math.max(userSize, aiSize) * 2;
    }


    public void addMessageUser(MessageChatBot newMessage) {
        memory.getMessages("user").add(newMessage);
        notifyItemInserted(memory.getMessages("user").size() - 1);
    }

    public void addMessageAI(MessageChatBot newMessage) {
        memory.getMessages("AI").add(newMessage);
        notifyItemInserted(memory.getMessages("AI").size() - 1);
    }

    // --- ViewHolder cho User ---
    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvMessage, tvTime;

        UserMessageViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message_sent);
            tvTime = itemView.findViewById(R.id.tv_time_sent);
        }

        void bind(MessageChatBot message) {
            tvMessage.setText(message.getContent());
            tvTime.setText(formatTime(message.getTimestamp()));
        }
    }

    // --- ViewHolder cho AI ---
    static class AIMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvMessage, tvTime, tvSenderName;

        AIMessageViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message_received);
            tvTime = itemView.findViewById(R.id.tv_time_received);
            tvSenderName = itemView.findViewById(R.id.tv_sender_name);
        }

        void bind(MessageChatBot message) {
            tvMessage.setText(message.getContent());
            tvSenderName.setText("Chat-bot AI");
            tvTime.setText(formatTime(message.getTimestamp()));
        }
    }

    private static String formatTime(long timestamp) {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return timeFormat.format(new java.util.Date(timestamp));
    }
}
