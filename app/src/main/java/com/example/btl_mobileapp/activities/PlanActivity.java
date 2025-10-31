package com.example.btl_mobileapp.activities;

import static com.example.btl_mobileapp.utils.LoginPreferences.getUserId;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.btl_mobileapp.R;
import com.example.btl_mobileapp.adapters.ScheduleAdapter;
import com.example.btl_mobileapp.models.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date; // <-- THÊM IMPORT NÀY
import java.util.List;
import java.util.Locale;

public class PlanActivity extends BaseActivity {

    // ... (các khai báo biến của bạn) ...
    private CalendarView calendarView;
    private TextView tvMonthYear;
    private TextView tvTodayTitle;
    private RecyclerView rvSchedule;
    private ScheduleAdapter scheduleAdapter;
    private FloatingActionButton fabAddPlan;
    private User currentUser;
    private Calendar selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.plan);

        // ... (ánh xạ các view) ...
        calendarView = findViewById(R.id.calendarView);
        tvMonthYear = findViewById(R.id.tvMonthYear);
        tvTodayTitle = findViewById(R.id.tvTodayTitle);
        rvSchedule = findViewById(R.id.rvSchedule);
        fabAddPlan = findViewById(R.id.fabAddPlan);

        rvSchedule.setLayoutManager(new LinearLayoutManager(this));
        scheduleAdapter = new ScheduleAdapter();
        rvSchedule.setAdapter(scheduleAdapter);

        // --- SỬA LẠI: Bấm vào item để SỬA/XEM ---
        scheduleAdapter.setOnItemClickListener(item -> {
            boolean isReadOnly = false;
            Calendar itemDate = parseDateString(item.getDate());

            if (itemDate != null) {
                if (isPastDate(itemDate)) {
                    // 1. Nếu là ngày quá khứ -> Khóa
                    isReadOnly = true;
                } else if (isToday(itemDate)) {
                    // 2. Nếu là hôm nay, kiểm tra thêm giờ
                    Calendar itemTime = parseTimeString(item.getTime());
                    if (itemTime != null && isPastTime(itemTime)) {
                        // Nếu giờ đã qua -> Khóa
                        isReadOnly = true;
                    }
                }
            }

            // Mở fragment VÀ gửi cờ "isReadOnly"
            PlanDetailSheetFragment fragment = PlanDetailSheetFragment.newInstance(item, isReadOnly);
            fragment.show(getSupportFragmentManager(), fragment.getTag());
        });

        // --- SỬA LẠI: Lắng nghe sự kiện XÓA ---
        scheduleAdapter.setOnItemDeleteListener((item, position) -> {
            Calendar itemDate = parseDateString(item.getDate());

            if (itemDate != null) {
                if (isPastDate(itemDate)) {
                    // 1. Kiểm tra ngày quá khứ
                    Toast.makeText(PlanActivity.this, "Không thể xóa kế hoạch quá khứ", Toast.LENGTH_SHORT).show();
                    return; // Không xóa
                }
                if (isToday(itemDate)) {
                    // 2. Kiểm tra giờ (nếu là hôm nay)
                    Calendar itemTime = parseTimeString(item.getTime());
                    if (itemTime != null && isPastTime(itemTime)) {
                        Toast.makeText(this, "Không thể xóa kế hoạch đã qua", Toast.LENGTH_SHORT).show();
                        return; // Dừng
                    }
                }
            }

            // Nếu không phải quá khứ, tiếp tục xóa
            FirebaseFirestore.getInstance()
                    .collection("couple_plans")
                    .document(item.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(PlanActivity.this, "Đã xóa kế hoạch", Toast.LENGTH_SHORT).show();
                        scheduleAdapter.removeItem(position); // Cập nhật UI
                    })
                    .addOnFailureListener(e -> Toast.makeText(PlanActivity.this, "Xóa thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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

        // --- NÚT THÊM KẾ HOẠCH (Giữ nguyên, không kiểm tra ở đây) ---
        fabAddPlan.setOnClickListener(v -> {
            String dateStr = String.format(Locale.getDefault(), "%02d/%02d/%d",
                    selectedDate.get(Calendar.DAY_OF_MONTH),
                    selectedDate.get(Calendar.MONTH) + 1,
                    selectedDate.get(Calendar.YEAR));

            PlanDetailSheetFragment fragment = PlanDetailSheetFragment.newInstance(dateStr);
            fragment.show(getSupportFragmentManager(), fragment.getTag());
        });
    }

    // --- CÁC HÀM TRỢ GIÚP KIỂM TRA (Đã có) ---
    private Calendar parseDateString(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try {
            Calendar cal = Calendar.getInstance();
            Date parsedDate = sdf.parse(dateStr); // Thêm check null
            if(parsedDate != null) {
                cal.setTime(parsedDate);
                return cal;
            }
        } catch (ParseException e) {
            Log.e("PlanActivity", "Lỗi parse ngày: " + dateStr, e);
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

    // --- THÊM CÁC HÀM KIỂM TRA GIỜ ---
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
            Log.e("PlanActivity", "Lỗi parse giờ: " + timeStr, e);
        }
        return null;
    }

    private boolean isPastTime(Calendar itemTime) {
        if (itemTime == null) return false; // Không có giờ, coi như chưa qua

        Calendar now = Calendar.getInstance(); // Giờ hiện tại

        Calendar planTime = Calendar.getInstance(); // Lấy ngày hôm nay
        planTime.set(Calendar.HOUR_OF_DAY, itemTime.get(Calendar.HOUR_OF_DAY));
        planTime.set(Calendar.MINUTE, itemTime.get(Calendar.MINUTE));
        planTime.set(Calendar.SECOND, 0);
        planTime.set(Calendar.MILLISECOND, 0);

        return planTime.before(now);
    }

    // --- (Các hàm còn lại giữ nguyên) ---

    private void updateMonthYearText(int year, int monthZeroBased) {
        tvMonthYear.setText(getString(R.string.schedule_month_year, monthZeroBased + 1, year));
    }

    private void loadCurrentUser() {
        String userId = getUserId(this);
        if (userId == null) return;
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentUser = doc.toObject(User.class);
                        loadPlansForDate(selectedDate.get(Calendar.YEAR),
                                selectedDate.get(Calendar.MONTH),
                                selectedDate.get(Calendar.DAY_OF_MONTH));
                    }
                });
    }

    public String getCoupleId() {
        if (currentUser == null || currentUser.getPartnerId() == null) {
            Toast.makeText(this, "Chưa có partner", Toast.LENGTH_SHORT).show();
            return null;
        }
        List<String> ids = Arrays.asList(currentUser.getUserId(), currentUser.getPartnerId());
        Collections.sort(ids);
        return ids.get(0) + "_" + ids.get(1);
    }

    private void loadPlansForDate(int year, int intMonth, int day) {
        String coupleId = getCoupleId();
        if (coupleId == null) {
            scheduleAdapter.setSchedules(new ArrayList<>());
            return;
        }
        String dateQuery = String.format(Locale.getDefault(), "%02d/%02d/%d", day, intMonth + 1, year);

        FirebaseFirestore.getInstance()
                .collection("couple_plans")
                .whereEqualTo("coupleId", coupleId)
                .whereEqualTo("date", dateQuery)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ScheduleAdapter.ScheduleItem> dayPlans = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        ScheduleAdapter.ScheduleItem item = doc.toObject(ScheduleAdapter.ScheduleItem.class);
                        item.setId(doc.getId());
                        dayPlans.add(item);
                    }
                    scheduleAdapter.setSchedules(dayPlans);
                });
    }

    @Override
    protected boolean shouldUseEdgeToEdge() { return true; }

    @Override
    protected void onResume() {
        super.onResume();
        // Tải lại khi quay về (để cập nhật sau khi Sửa/Thêm)
        loadPlansForDate(selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH));
    }
}