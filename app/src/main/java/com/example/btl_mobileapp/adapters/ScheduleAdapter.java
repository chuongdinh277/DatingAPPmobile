package com.example.btl_mobileapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btl_mobileapp.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder> {

    private List<ScheduleItem> schedules = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ScheduleItem item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setSchedules(List<ScheduleItem> schedules) {
        this.schedules = schedules;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule, parent, false);
        return new ScheduleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
        ScheduleItem item = schedules.get(position);
        holder.tvContent.setText(item.getContent());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });

        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            ScheduleItem current = schedules.get(pos);
            FirebaseFirestore.getInstance()
                    .collection("couple_plans")
                    .document(current.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        schedules.remove(pos);
                        notifyItemRemoved(pos);
                    })
                    .addOnFailureListener(e -> Toast.makeText(v.getContext(), "Xóa thất bại", Toast.LENGTH_SHORT).show());
        });
    }

    @Override
    public int getItemCount() {
        return schedules.size();
    }

    static class ScheduleViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent;
        ImageButton btnDelete;

        public ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tvContent);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }

    // Bên trong file ScheduleAdapter.java

    public static class ScheduleItem {
        private String id;
        private String content;

        public ScheduleItem(String id, String content) {
            this.id = id;
            this.content = content;
        }

        public String getId() {
            return id;
        }

        public String getContent() {
            return content;
        }

        // --- THÊM PHƯƠNG THỨC NÀY VÀO ---
        // Phương thức này dùng để cập nhật nội dung của một kế hoạch đã có.
        public void setContent(String content) {
            this.content = content;
        }
    }
    // Thêm vào file ScheduleAdapter.java
    public void addItem(ScheduleItem newItem) {
        schedules.add(newItem);
        notifyItemInserted(schedules.size() - 1);
    }

    public void updateItem(ScheduleItem itemToUpdate, String newContent) {
        int position = schedules.indexOf(itemToUpdate);
        if (position != -1) {
            schedules.get(position).setContent(newContent);
            notifyItemChanged(position);
        }
    }
}