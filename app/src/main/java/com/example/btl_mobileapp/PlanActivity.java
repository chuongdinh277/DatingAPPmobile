package com.example.btl_mobileapp;

import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Calendar;

public class PlanActivity extends BaseActivity {

    private CalendarView calendarView;
    private TextView tvMonthYear;
    private TextView tvTodayTitle;
    private RecyclerView rvSchedule;
    private ScheduleAdapter scheduleAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.plan);

        calendarView = findViewById(R.id.calendarView);
        tvMonthYear = findViewById(R.id.tvMonthYear);
        tvTodayTitle = findViewById(R.id.tvTodayTitle);
        rvSchedule = findViewById(R.id.rvSchedule);

        rvSchedule.setLayoutManager(new LinearLayoutManager(this));
        scheduleAdapter = new ScheduleAdapter();
        rvSchedule.setAdapter(scheduleAdapter);

        Calendar today = Calendar.getInstance();
        updateMonthYearText(today.get(Calendar.YEAR), today.get(Calendar.MONTH));

        tvMonthYear.setOnClickListener(v -> {
            View calendarContainer = findViewById(R.id.calendarContainer);
            if (calendarContainer.getVisibility() == View.VISIBLE) {
                calendarContainer.setVisibility(View.GONE);
            } else {
                calendarContainer.setVisibility(View.VISIBLE);
            }
        });


        LinearLayout calendarContainer = findViewById(R.id.calendarContainer);

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            updateMonthYearText(year, month);
            tvTodayTitle.setText("Lịch trình ngày " + dayOfMonth + "/" + (month + 1) + "/" + year);
            loadScheduleForDate(year, month, dayOfMonth);
            calendarContainer.setVisibility(View.GONE); // <-- ẨN CONTAINER CHỨ KHÔNG PHẢI CHỈ CALENDARVIEW
        });

        loadScheduleForDate(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));
    }

    private void updateMonthYearText(int year, int monthZeroBased) {
        String text = "Tháng " + (monthZeroBased + 1) + ", " + year;
        tvMonthYear.setText(text);
    }

    private void loadScheduleForDate(int year, int month, int day) {
        scheduleAdapter.setSchedules(
                (day == 20)
                        ? new String[]{"10h-14h: Tổ chức học và giảng dạy", "18h-22h: Di chuyển lên nội đô\nSắp xếp đồ\nDọn dẹp phòng"}
                        : new String[]{"09h-10h: Hẹn cafe"}
        );
    }
}
