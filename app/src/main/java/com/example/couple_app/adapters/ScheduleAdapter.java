package com.example.couple_app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.couple_app.R;
import com.example.couple_app.models.Plan;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter hiển thị danh sách kế hoạch trong RecyclerView
 * Trách nhiệm: Chỉ hiển thị dữ liệu và xử lý sự kiện UI
 */
public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder> {

    private List<Plan> schedules = new ArrayList<>();
    private OnItemClickListener onItemClickListener;
    private OnDeleteClickListener onDeleteClickListener;

    /**
     * Interface để xử lý sự kiện click vào item
     */
    public interface OnItemClickListener {
        void onItemClick(Plan plan);
    }

    /**
     * Interface để xử lý sự kiện click nút xóa
     */
    public interface OnDeleteClickListener {
        void onDeleteClick(Plan plan, int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.onDeleteClickListener = listener;
    }

    public void setSchedules(List<Plan> schedules) {
        this.schedules = schedules != null ? schedules : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void removePlan(int position) {
        if (position >= 0 && position < schedules.size()) {
            schedules.remove(position);
            notifyItemRemoved(position);
        }
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
        Plan plan = schedules.get(position);
        holder.tvContent.setText(plan.getContent());

        // Xử lý click vào item để chỉnh sửa
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(plan);
            }
        });

        // Xử lý click nút xóa
        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && onDeleteClickListener != null) {
                onDeleteClickListener.onDeleteClick(plan, pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return schedules.size();
    }

    /**
     * ViewHolder chứa các view trong mỗi item
     */
    static class ScheduleViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent;
        ImageButton btnDelete;

        public ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tvContent);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
