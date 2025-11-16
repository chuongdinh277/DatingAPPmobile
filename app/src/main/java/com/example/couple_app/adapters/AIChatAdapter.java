package com.example.couple_app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.couple_app.R;
import com.example.couple_app.models.Message;
import com.example.couple_app.models.MessageChatBot;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class AIChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_AI = 2;

    private List<MessageChatBot> messages;
    private String currentUserId;
    private final SimpleDateFormat timeFormat;

    public AIChatAdapter(List<MessageChatBot> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    @Override
    public int getItemViewType(int position) {
        MessageChatBot message = messages.get(position);
        return message.getSenderId().equals(currentUserId) ? VIEW_TYPE_USER : VIEW_TYPE_AI;
    }

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
        MessageChatBot message = messages.get(position);

        if (holder instanceof UserMessageViewHolder) {
            ((UserMessageViewHolder) holder).bind(message);
        } else if (holder instanceof AIMessageViewHolder) {
            ((AIMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void addMessage(MessageChatBot newMessage) {
        messages.add(newMessage);
        notifyItemInserted(messages.size() - 1);
    }

    public void updateMessages(List<MessageChatBot> newMessages) {
        this.messages = newMessages;
        notifyDataSetChanged();
    }

    public void updateLastAIMessage(String newContent) {
        if (!messages.isEmpty()) {
            MessageChatBot lastMessage = messages.get(messages.size() - 1);
            if (lastMessage.isFromAI()) {
                lastMessage.setContent(newContent);
                notifyItemChanged(messages.size() - 1);
            }
        }
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
            tvSenderName.setText("AI Assistant");
            tvTime.setText(formatTime(message.getTimestamp()));
        }
    }

    private static String formatTime(long timestamp) {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return timeFormat.format(new java.util.Date(timestamp));
    }
}
