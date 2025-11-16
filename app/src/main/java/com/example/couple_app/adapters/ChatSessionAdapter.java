package com.example.couple_app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.couple_app.R;
import com.example.couple_app.models.ChatSession;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying chat session history
 */
public class ChatSessionAdapter extends RecyclerView.Adapter<ChatSessionAdapter.SessionViewHolder> {

    private List<ChatSession> sessions = new ArrayList<>();
    private OnSessionClickListener listener;

    public interface OnSessionClickListener {
        void onSessionClick(ChatSession session);
        void onSessionDelete(ChatSession session, int position);
        void onSessionEdit(ChatSession session, int position);
    }

    public ChatSessionAdapter(OnSessionClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_session, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        ChatSession session = sessions.get(position);
        holder.bind(session, listener, position);
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    public void setSessions(List<ChatSession> sessions) {
        this.sessions = sessions != null ? sessions : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void removeSession(int position) {
        if (position >= 0 && position < sessions.size()) {
            sessions.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void updateSession(int position, ChatSession session) {
        if (position >= 0 && position < sessions.size()) {
            sessions.set(position, session);
            notifyItemChanged(position);
        }
    }

    static class SessionViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvPreview;
        TextView tvDate;
        TextView tvMessageCount;
        ImageButton btnDelete;
        ImageButton btnEdit;
        View itemView;

        SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            tvTitle = itemView.findViewById(R.id.tv_session_title);
            tvPreview = itemView.findViewById(R.id.tv_session_preview);
            tvDate = itemView.findViewById(R.id.tv_session_date);
            tvMessageCount = itemView.findViewById(R.id.tv_message_count);
            btnDelete = itemView.findViewById(R.id.btn_delete_session);
            btnEdit = itemView.findViewById(R.id.btn_edit_session);
        }

        void bind(ChatSession session, OnSessionClickListener listener, int position) {
            tvTitle.setText(session.getDisplayTitle());
            tvPreview.setText(session.getPreviewText());
            tvDate.setText(formatDate(session.getUpdatedAt()));
            tvMessageCount.setText(String.valueOf(session.getMessageCount()));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSessionClick(session);
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSessionDelete(session, position);
                }
            });

            btnEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSessionEdit(session, position);
                }
            });
        }

        private String formatDate(Date date) {
            if (date == null) return "";

            long now = System.currentTimeMillis();
            long diff = now - date.getTime();

            // Less than 1 minute
            if (diff < 60 * 1000) {
                return "Vừa xong";
            }
            // Less than 1 hour
            else if (diff < 60 * 60 * 1000) {
                long minutes = diff / (60 * 1000);
                return minutes + " phút trước";
            }
            // Less than 1 day
            else if (diff < 24 * 60 * 60 * 1000) {
                long hours = diff / (60 * 60 * 1000);
                return hours + " giờ trước";
            }
            // Less than 7 days
            else if (diff < 7 * 24 * 60 * 60 * 1000) {
                long days = diff / (24 * 60 * 60 * 1000);
                return days + " ngày trước";
            }
            // More than 7 days, show date
            else {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                return sdf.format(date);
            }
        }
    }
}

