package com.example.couple_app.activities;

import static com.example.couple_app.utils.LoginPreferences.getUserId;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.couple_app.R;
import com.example.couple_app.adapters.ScheduleAdapter;
import com.example.couple_app.models.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class PlanActivity extends BaseActivity {

    private CalendarView calendarView;
    private TextView tvMonthYear;
    private TextView tvTodayTitle;
    private RecyclerView rvSchedule;
    private ScheduleAdapter scheduleAdapter;
    private FloatingActionButton fabAddPlan;
    private LinearLayout llAddPlan;
    private EditText etNewPlan;
    private Button btnSavePlan;

    private User currentUser;
    private Calendar selectedDate;

    private ScheduleAdapter.ScheduleItem editingItem = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.plan);

        calendarView = findViewById(R.id.calendarView);
        tvMonthYear = findViewById(R.id.tvMonthYear);
        tvTodayTitle = findViewById(R.id.tvTodayTitle);
        rvSchedule = findViewById(R.id.rvSchedule);
        fabAddPlan = findViewById(R.id.fabAddPlan);
        llAddPlan = findViewById(R.id.llAddPlan);
        etNewPlan = findViewById(R.id.etNewPlan);
        btnSavePlan = findViewById(R.id.btnSavePlan);

        rvSchedule.setLayoutManager(new LinearLayoutManager(this));
        scheduleAdapter = new ScheduleAdapter();
        rvSchedule.setAdapter(scheduleAdapter);

        scheduleAdapter.setOnItemClickListener(item -> {
            editingItem = item;
            llAddPlan.setVisibility(View.VISIBLE);
            etNewPlan.setText(item.getContent());
            etNewPlan.requestFocus();
        });

        selectedDate = Calendar.getInstance();
        updateMonthYearText(selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH));
        calendarView.setDate(selectedDate.getTimeInMillis());

        tvTodayTitle.setText(getString(
                R.string.schedule_today_title_full,
                selectedDate.get(Calendar.DAY_OF_MONTH),
                selectedDate.get(Calendar.MONTH) + 1,
                selectedDate.get(Calendar.YEAR)));

        loadCurrentUser();

        tvMonthYear.setOnClickListener(v -> {
            LinearLayout calendarContainer = findViewById(R.id.calendarContainer);
            calendarContainer.setVisibility(
                    calendarContainer.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE
            );
        });

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate.set(year, month, dayOfMonth);
            updateMonthYearText(year, month);
            tvTodayTitle.setText(getString(R.string.schedule_day_title, dayOfMonth, month + 1, year));
            loadPlansForDate(year, month, dayOfMonth);
        });

        fabAddPlan.setOnClickListener(v -> {
            if (llAddPlan.getVisibility() == View.VISIBLE) {
                llAddPlan.setVisibility(View.GONE);
                editingItem = null;
                etNewPlan.setText("");
            } else {
                llAddPlan.setVisibility(View.VISIBLE);
                etNewPlan.requestFocus();
            }
        });

        btnSavePlan.setOnClickListener(v -> savePlan());
    }

    private void updateMonthYearText(int year, int monthZeroBased) {
        tvMonthYear.setText(getString(R.string.schedule_month_year, monthZeroBased + 1, year));
    }

    private void loadCurrentUser() {
        String userId = getUserId(this);
        if (userId == null) return;

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentUser = doc.toObject(User.class);

                        // ✅ Chỉ load lịch hôm nay sau khi có currentUser
                        loadPlansForDate(selectedDate.get(Calendar.YEAR),
                                selectedDate.get(Calendar.MONTH),
                                selectedDate.get(Calendar.DAY_OF_MONTH));
                    }
                });
    }

    private String getCoupleId() {
        if (currentUser == null || currentUser.getPartnerId() == null) {
            Toast.makeText(this, "Chưa có partner", Toast.LENGTH_SHORT).show();
            return null;
        }
        List<String> ids = Arrays.asList(currentUser.getUserId(), currentUser.getPartnerId());
        Collections.sort(ids);
        return ids.get(0) + "_" + ids.get(1);
    }

    private void loadPlansForDate(int year, int month, int day) {
        String coupleId = getCoupleId();
        if (coupleId == null) {
            scheduleAdapter.setSchedules(new ArrayList<>());
            return;
        }

        String dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day);

        FirebaseFirestore.getInstance()
                .collection("couple_plans")
                .whereEqualTo("coupleId", coupleId)
                .whereEqualTo("date", dateStr)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ScheduleAdapter.ScheduleItem> dayPlans = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String content = doc.getString("content");
                        if (content != null) {
                            dayPlans.add(new ScheduleAdapter.ScheduleItem(doc.getId(), content));
                        }
                    }
                    scheduleAdapter.setSchedules(dayPlans);
                });
    }

    private void savePlan() {
        String content = etNewPlan.getText().toString().trim();
        if (content.isEmpty()) return;

        String coupleId = getCoupleId();
        if (coupleId == null) return;

        String dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH) + 1,
                selectedDate.get(Calendar.DAY_OF_MONTH));

        if (editingItem != null) {
            FirebaseFirestore.getInstance()
                    .collection("couple_plans")
                    .document(editingItem.getId())
                    .update("content", content)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Đã cập nhật kế hoạch", Toast.LENGTH_SHORT).show();
                        etNewPlan.setText("");
                        editingItem = null;
                        llAddPlan.setVisibility(View.GONE);
                        loadPlansForDate(selectedDate.get(Calendar.YEAR),
                                selectedDate.get(Calendar.MONTH),
                                selectedDate.get(Calendar.DAY_OF_MONTH));
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi cập nhật", Toast.LENGTH_SHORT).show());
        } else {
            FirebaseFirestore.getInstance()
                    .collection("couple_plans")
                    .add(new PlanItem(coupleId, dateStr, content))
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(this, "Đã thêm kế hoạch", Toast.LENGTH_SHORT).show();
                        etNewPlan.setText("");
                        llAddPlan.setVisibility(View.GONE);
                        loadPlansForDate(selectedDate.get(Calendar.YEAR),
                                selectedDate.get(Calendar.MONTH),
                                selectedDate.get(Calendar.DAY_OF_MONTH));
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi thêm: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    public static class PlanItem {
        private String coupleId;
        private String date;
        private String content;

        public PlanItem() {}
        public PlanItem(String coupleId, String date, String content) {
            this.coupleId = coupleId;
            this.date = date;
            this.content = content;
        }

        public String getCoupleId() { return coupleId; }
        public String getDate() { return date; }
        public String getContent() { return content; }
    }

    @Override
    protected boolean pinBottomBarOverIme() {
        return true;
    }

    @Override
    protected boolean shouldUseEdgeToEdge() {
        // Đồng bộ với Home/Settings để bottom bar không bị đẩy lên
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Highlight nút Plan trên bottom bar
        setActiveButton("plan");
    }
}
