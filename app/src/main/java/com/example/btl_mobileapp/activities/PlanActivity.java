package com.example.btl_mobileapp.activities;

import static com.example.btl_mobileapp.utils.LoginPreferences.getUserId;

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

import com.example.btl_mobileapp.R;
import com.example.btl_mobileapp.adapters.ScheduleAdapter;
import com.example.btl_mobileapp.models.User;
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

        // --- Khởi tạo Views (không thay đổi) ---
        calendarView = findViewById(R.id.calendarView);
        tvMonthYear = findViewById(R.id.tvMonthYear);
        tvTodayTitle = findViewById(R.id.tvTodayTitle);
        rvSchedule = findViewById(R.id.rvSchedule);
        fabAddPlan = findViewById(R.id.fabAddPlan);
        llAddPlan = findViewById(R.id.llAddPlan);
        etNewPlan = findViewById(R.id.etNewPlan);
        btnSavePlan = findViewById(R.id.btnSavePlan);

        rvSchedule.setLayoutManager(new LinearLayoutManager(this));

        // SỬA LỖI: Thay đổi dòng này để sử dụng hàm khởi tạo mặc định (không có tham số)
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
                editingItem = null; // Đảm bảo reset trạng thái edit khi đóng
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
        if (userId == null) {
            Toast.makeText(this, "Không tìm thấy User ID. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentUser = doc.toObject(User.class);
                        // Chỉ load lịch hôm nay sau khi đã có thông tin currentUser
                        loadPlansForDate(selectedDate.get(Calendar.YEAR),
                                selectedDate.get(Calendar.MONTH),
                                selectedDate.get(Calendar.DAY_OF_MONTH));
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải thông tin người dùng.", Toast.LENGTH_SHORT).show();
                });
    }

    private String getCoupleId() {
        if (currentUser == null || currentUser.getPartnerId() == null || currentUser.getPartnerId().isEmpty()) {
            Toast.makeText(this, "Bạn chưa kết đôi.", Toast.LENGTH_SHORT).show();
            return null;
        }
        List<String> ids = Arrays.asList(currentUser.getUserId(), currentUser.getPartnerId());
        Collections.sort(ids);
        return ids.get(0) + "_" + ids.get(1);
    }

    private void loadPlansForDate(int year, int month, int day) {
        // CẢI THIỆN: Thêm kiểm tra currentUser để tránh lỗi khi người dùng thao tác nhanh
        if (currentUser == null) {
            // Dữ liệu người dùng chưa tải xong, không làm gì cả
            return;
        }

        String coupleId = getCoupleId();
        if (coupleId == null) {
            scheduleAdapter.setSchedules(new ArrayList<>()); // Xóa danh sách cũ nếu không có coupleId
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
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải kế hoạch.", Toast.LENGTH_SHORT).show();
                });
    }

    private void savePlan() {
        String content = etNewPlan.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Nội dung không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }

        // CẢI THIỆN: Thêm kiểm tra currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Thông tin người dùng chưa sẵn sàng", Toast.LENGTH_SHORT).show();
            return;
        }

        String coupleId = getCoupleId();
        if (coupleId == null) return;

        String dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH) + 1,
                selectedDate.get(Calendar.DAY_OF_MONTH));

        if (editingItem != null) {
            // --- Chế độ cập nhật ---
            FirebaseFirestore.getInstance()
                    .collection("couple_plans")
                    .document(editingItem.getId())
                    .update("content", content)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Đã cập nhật kế hoạch", Toast.LENGTH_SHORT).show();
                        // TỐI ƯU: Cập nhật trực tiếp trên adapter thay vì load lại từ server
                        scheduleAdapter.updateItem(editingItem, content);
                        resetInputForm();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi cập nhật", Toast.LENGTH_SHORT).show());
        } else {
            // --- Chế độ thêm mới ---
            PlanItem newPlan = new PlanItem(coupleId, dateStr, content);
            FirebaseFirestore.getInstance()
                    .collection("couple_plans")
                    .add(newPlan)
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(this, "Đã thêm kế hoạch", Toast.LENGTH_SHORT).show();
                        // TỐI ƯU: Thêm trực tiếp vào adapter thay vì load lại từ server
                        scheduleAdapter.addItem(new ScheduleAdapter.ScheduleItem(docRef.getId(), content));
                        resetInputForm();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi thêm mới: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    // TỐI ƯU: Tách hàm để reset form nhập liệu
    private void resetInputForm() {
        etNewPlan.setText("");
        editingItem = null;
        llAddPlan.setVisibility(View.GONE);
    }

    // --- Định nghĩa lớp PlanItem (không thay đổi) ---
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

    // --- Override các phương thức của BaseActivity (không thay đổi) ---

}