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
import com.example.couple_app.managers.PlanRepository;
import com.example.couple_app.models.Plan;
import com.example.couple_app.models.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;

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

    private PlanRepository planRepository;
    private User currentUser;
    private Calendar selectedDate;

    private Plan editingPlan = null;

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

        planRepository = new PlanRepository();

        // Set listener để edit plan
        scheduleAdapter.setOnItemClickListener(plan -> {
            editingPlan = plan;
            llAddPlan.setVisibility(View.VISIBLE);
            etNewPlan.setText(plan.getContent());
            etNewPlan.requestFocus();
        });

        // Set listener để delete plan
        scheduleAdapter.setOnDeleteClickListener((plan, position) -> {
            planRepository.deletePlan(plan.getId())
                    .addOnSuccessListener(aVoid -> {
                        scheduleAdapter.removePlan(position);
                        Toast.makeText(this, "Đã xóa kế hoạch", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                        Toast.makeText(this, "Xóa thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
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
                editingPlan = null;
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

        planRepository.getPlansByDate(coupleId, dateStr)
                .addOnSuccessListener(querySnapshot -> {
                    List<Plan> plans = PlanRepository.convertToPlanList(querySnapshot);
                    scheduleAdapter.setSchedules(plans);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    scheduleAdapter.setSchedules(new ArrayList<>());
                });
    }

    private void savePlan() {
        String content = etNewPlan.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập nội dung", Toast.LENGTH_SHORT).show();
            return;
        }

        String coupleId = getCoupleId();
        if (coupleId == null) return;

        String dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH) + 1,
                selectedDate.get(Calendar.DAY_OF_MONTH));

        // Nếu đang edit plan có sẵn
        if (editingPlan != null) {
            planRepository.updatePlanContent(editingPlan.getId(), content)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Đã cập nhật kế hoạch", Toast.LENGTH_SHORT).show();
                        resetPlanForm();
                        loadPlansForDate(selectedDate.get(Calendar.YEAR),
                                selectedDate.get(Calendar.MONTH),
                                selectedDate.get(Calendar.DAY_OF_MONTH));
                    })
                    .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        }
        // Nếu thêm mới plan
        else {
            Plan newPlan = new Plan(coupleId, dateStr, content);
            planRepository.addPlan(newPlan)
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(this, "Đã thêm kế hoạch", Toast.LENGTH_SHORT).show();
                        resetPlanForm();
                        loadPlansForDate(selectedDate.get(Calendar.YEAR),
                                selectedDate.get(Calendar.MONTH),
                                selectedDate.get(Calendar.DAY_OF_MONTH));
                    })
                    .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi thêm: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        }
    }

    /**
     * Reset form nhập plan
     */
    private void resetPlanForm() {
        etNewPlan.setText("");
        editingPlan = null;
        llAddPlan.setVisibility(View.GONE);
    }

    @Override
    protected boolean shouldUseEdgeToEdge() {
        // Đồng bộ với Home/Settings để bottom bar không bị đẩy lên
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

    }
}
