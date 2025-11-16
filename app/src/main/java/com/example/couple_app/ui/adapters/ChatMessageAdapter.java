package com.example.couple_app.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.couple_app.R;
import com.example.couple_app.data.model.ChatBotMessage;

import java.util.ArrayList;
import java.util.List;

import io.noties.markwon.Markwon;

/**
 * Adapter for chatbot messages RecyclerView
 * Handles displaying user and bot messages with different layouts
 */
public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder> {

    private final List<ChatBotMessage> messages;
    private Markwon markwon;

    public ChatMessageAdapter() {
        this.messages = new ArrayList<>();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Initialize Markwon if not already initialized
        if (markwon == null) {
            markwon = Markwon.create(parent.getContext());
        }

        int layoutId = viewType == ChatBotMessage.TYPE_USER
            ? R.layout.item_chat_message_user
            : R.layout.item_chat_message_bot;

        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatBotMessage message = messages.get(position);
        holder.bind(message, markwon);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getType();
    }

    /**
     * Add new message to the list
     */
    public void addMessage(@NonNull ChatBotMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    /**
     * Remove last message (used for removing typing indicator)
     */
    public void removeLastMessage() {
        if (!messages.isEmpty()) {
            int lastIndex = messages.size() - 1;
            messages.remove(lastIndex);
            notifyItemRemoved(lastIndex);
        }
    }

    /**
     * Set messages list (replaces all existing messages)
     */
    public void setMessages(@NonNull List<ChatBotMessage> newMessages) {
        messages.clear();
        messages.addAll(newMessages);
        notifyDataSetChanged();
    }

    /**
     * Clear all messages
     */
    public void clearMessages() {
        int size = messages.size();
        messages.clear();
        notifyItemRangeRemoved(0, size);
    }

    /**
     * Update the content of the last bot message (for streaming)
     */
    public void updateLastBotMessage(@NonNull String newContent) {
        if (!messages.isEmpty()) {
            ChatBotMessage lastMessage = messages.get(messages.size() - 1);
            if (lastMessage.isBot()) {
                ChatBotMessage updatedMessage = new ChatBotMessage(newContent, ChatBotMessage.TYPE_BOT, lastMessage.getTimestamp());
                messages.set(messages.size() - 1, updatedMessage);
                notifyItemChanged(messages.size() - 1);
            }
        }
    }

    /**
     * ViewHolder for chat messages
     */
    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageText;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
        }

        public void bind(@NonNull ChatBotMessage message, Markwon markwon) {
            if (messageText != null) {
                if (message.isBot() && markwon != null) {
                    // Render markdown for bot messages
                    markwon.setMarkdown(messageText, message.getMessage());
                } else {
                    // Plain text for user messages
                    messageText.setText(message.getMessage());
                }
            }
        }
    }
}
