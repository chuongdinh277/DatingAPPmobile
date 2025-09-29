package com.example.btl_mobileapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {

    private String[] schedules = new String[0];

    public void setSchedules(String[] newSchedules) {
        this.schedules = newSchedules;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ScheduleAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleAdapter.ViewHolder holder, int position) {
        holder.tvContent.setText(schedules[position]);
    }

    @Override
    public int getItemCount() {
        return schedules.length;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent;
        ViewHolder(View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tvContent);
        }
    }
}
