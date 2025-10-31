package com.example.btl_mobileapp.adapters;

import android.util.Log;
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

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date; // <-- THÊM
import java.util.List;
import java.util.Locale;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder> {

    private List<ScheduleItem> schedules = new ArrayList<>();
    private OnItemClickListener listener;
    private OnItemDeleteListener deleteListener;

    // --- (Interface và Setters giữ nguyên) ---
    public interface OnItemClickListener {
        void onItemClick(ScheduleItem item);
    }
    public interface OnItemDeleteListener {
        void onItemDelete(ScheduleItem item, int position);
    }
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
    public void setOnItemDeleteListener(OnItemDeleteListener deleteListener) {
        this.deleteListener = deleteListener;
    }
    public void setSchedules(List<ScheduleItem> schedules) {
        this.schedules = schedules;
        notifyDataSetChanged();
    }
    public void removeItem(int position) {
        schedules.remove(position);
        notifyItemRemoved(position);
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

        holder.tvContent.setText(item.getTitle());
        holder.tvTime.setText(item.getTime());
        holder.tvDate.setText(item.getDate());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });

        // --- SỬA LẠI LOGIC ẨN NÚT XÓA ---
        boolean shouldHideDelete = false;
        Calendar itemDate = parseDateString(item.getDate());

        if (itemDate != null) {
            if (isPastDate(itemDate)) {
                // 1. Nếu là ngày quá khứ -> Ẩn
                shouldHideDelete = true;
            } else if (isToday(itemDate)) {
                // 2. Nếu là hôm nay, kiểm tra thêm giờ
                Calendar itemTime = parseTimeString(item.getTime());
                if (itemTime != null && isPastTime(itemTime)) {
                    // Nếu giờ đã qua -> Ẩn
                    shouldHideDelete = true;
                }
            }
        }

        // Áp dụng logic
        holder.btnDelete.setVisibility(shouldHideDelete ? View.GONE : View.VISIBLE);
        // --- KẾT THÚC LOGIC ẨN ---

        holder.btnDelete.setOnClickListener(v -> {
            int currentPosition = holder.getBindingAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION) return;
            if (deleteListener != null) {
                deleteListener.onItemDelete(schedules.get(currentPosition), currentPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return schedules.size();
    }

    // --- (ViewHolder giữ nguyên) ---
    static class ScheduleViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent, tvTime, tvDate;
        ImageButton btnDelete;

        public ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvDate = itemView.findViewById(R.id.tvDate);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }

    // --- (Model ScheduleItem giữ nguyên) ---
    public static class ScheduleItem implements Serializable {
        // (toàn bộ code model của bạn y hệt)
        private String id;
        private String title;
        private String time;
        private String date;
        private String details;
        private String coupleId;
        public ScheduleItem() {}
        public ScheduleItem(String id, String title, String time, String date, String details, String coupleId) {
            this.id = id; this.title = title; this.time = time; this.date = date; this.details = details; this.coupleId = coupleId;
        }
        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getTime() { return time; }
        public String getDate() { return date; }
        public String getDetails() { return details; }
        public String getCoupleId() { return coupleId; }
        public void setId(String id) { this.id = id; }
        public void setTitle(String title) { this.title = title; }
        public void setTime(String time) { this.time = time; }
        public void setDate(String date) { this.date = date; }
        public void setDetails(String details) { this.details = details; }
        public void setCoupleId(String coupleId) { this.coupleId = coupleId; }
    }

    // --- CÁC HÀM TRỢ GIÚP KIỂM TRA (SAO CHÉP TỪ ACTIVITY) ---
    private Calendar parseDateString(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try {
            Calendar cal = Calendar.getInstance();
            Date parsedDate = sdf.parse(dateStr);
            if(parsedDate != null) {
                cal.setTime(parsedDate);
                return cal;
            }
        } catch (ParseException e) {
            Log.e("ScheduleAdapter", "Lỗi parse ngày: " + dateStr, e);
        }
        return null;
    }

    private boolean isPastDate(Calendar dateToCompare) {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar compareClone = (Calendar) dateToCompare.clone();
        compareClone.set(Calendar.HOUR_OF_DAY, 0);
        compareClone.set(Calendar.MINUTE, 0);
        compareClone.set(Calendar.SECOND, 0);
        compareClone.set(Calendar.MILLISECOND, 0);

        return compareClone.before(today);
    }

    private boolean isToday(Calendar dateToCompare) {
        Calendar today = Calendar.getInstance();
        return today.get(Calendar.YEAR) == dateToCompare.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == dateToCompare.get(Calendar.DAY_OF_YEAR);
    }

    private Calendar parseTimeString(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) return null;
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        try {
            Date parsedTime = sdf.parse(timeStr);
            if (parsedTime != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(parsedTime);
                return cal;
            }
        } catch (ParseException e) {
            Log.e("ScheduleAdapter", "Lỗi parse giờ: " + timeStr, e);
        }
        return null;
    }

    private boolean isPastTime(Calendar itemTime) {
        if (itemTime == null) return false;
        Calendar now = Calendar.getInstance();
        Calendar planTime = Calendar.getInstance();
        planTime.set(Calendar.HOUR_OF_DAY, itemTime.get(Calendar.HOUR_OF_DAY));
        planTime.set(Calendar.MINUTE, itemTime.get(Calendar.MINUTE));
        planTime.set(Calendar.SECOND, 0);
        planTime.set(Calendar.MILLISECOND, 0);
        return planTime.before(now);
    }

    // ... (Hàm addItem, updateItem giữ nguyên) ...
}