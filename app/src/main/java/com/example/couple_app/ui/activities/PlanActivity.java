package com.example.couple_app.ui.activities;

import static com.example.couple_app.utils.LoginPreferences.getUserId;

import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.couple_app.R;
import com.example.couple_app.ui.adapters.ScheduleAdapter;
import com.example.couple_app.ui.fragments.PlanDetailBottomSheetFragment;
import com.example.couple_app.managers.NotificationManager;
import com.example.couple_app.data.repository.PlanRepository;
import com.example.couple_app.data.model.Plan;
import com.example.couple_app.data.model.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
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
    private ImageButton btnSavePlan;

    private PlanRepository planRepository;
    private User currentUser;
    private Calendar selectedDate;

    private Plan editingPlan = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.plan);

        // Initialize views with null checks
        calendarView = findViewById(R.id.calendarView);
        tvMonthYear = findViewById(R.id.tvMonthYear);
        tvTodayTitle = findViewById(R.id.tvTodayTitle);
        rvSchedule = findViewById(R.id.rvSchedule);
        fabAddPlan = findViewById(R.id.fabAddPlan);
        llAddPlan = findViewById(R.id.llAddPlan);
        etNewPlan = findViewById(R.id.etNewPlan);
        btnSavePlan = findViewById(R.id.btnSavePlan);

        // Check if views are properly initialized
        if (calendarView == null || tvMonthYear == null || tvTodayTitle == null ||
            rvSchedule == null || fabAddPlan == null || llAddPlan == null ||
            etNewPlan == null || btnSavePlan == null) {
            Toast.makeText(this, "Lỗi khởi tạo giao diện", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Hide old input form - using bottom sheet instead
        llAddPlan.setVisibility(View.GONE);

        rvSchedule.setLayoutManager(new LinearLayoutManager(this));
        scheduleAdapter = new ScheduleAdapter();
        rvSchedule.setAdapter(scheduleAdapter);

        planRepository = new PlanRepository();

        // Set listener để edit plan - open bottom sheet
        scheduleAdapter.setOnItemClickListener(plan -> {
            showPlanDetailBottomSheet(plan, true);
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
            if (calendarContainer != null) {
                calendarContainer.setVisibility(
                        calendarContainer.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE
                );
            }
        });

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate.set(year, month, dayOfMonth);
            updateMonthYearText(year, month);
            tvTodayTitle.setText(getString(R.string.schedule_day_title, dayOfMonth, month + 1, year));
            loadPlansForDate(year, month, dayOfMonth);
        });

        fabAddPlan.setOnClickListener(v -> {
            String dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH) + 1,
                    selectedDate.get(Calendar.DAY_OF_MONTH));
            showPlanDetailBottomSheet(null, false);
        });
    }

    private void updateMonthYearText(int year, int monthZeroBased) {
        tvMonthYear.setText(getString(R.string.schedule_month_year, monthZeroBased + 1, year));
    }

    private void loadCurrentUser() {
        // ✅ LUÔN lấy userId từ Firebase Auth trước (tin cậy nhất)
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        String userId = null;

        if (firebaseUser != null) {
            userId = firebaseUser.getUid();
            android.util.Log.d("PlanActivity", "Got userId from Firebase Auth: " + userId);
        } else {
            // Fallback: thử lấy từ SharedPreferences
            userId = getUserId(this);
            android.util.Log.w("PlanActivity", "No Firebase user, trying SharedPreferences: " + userId);
        }

        // Validate userId
        if (userId == null || userId.trim().isEmpty() || userId.equals("users")) {
            Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        final String validUserId = userId;
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(validUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        android.util.Log.d("PlanActivity", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                        android.util.Log.d("PlanActivity", "Loading User document:");
                        android.util.Log.d("PlanActivity", "  Document ID: " + doc.getId());
                        android.util.Log.d("PlanActivity", "  Raw coupleId from Firestore: " + doc.getString("coupleId"));
                        android.util.Log.d("PlanActivity", "  Raw partnerId from Firestore: " + doc.getString("partnerId"));

                        currentUser = doc.toObject(User.class);

                        if (currentUser != null) {
                            // Set userId if not already set
                            if (currentUser.getUserId() == null || currentUser.getUserId().isEmpty()) {
                                currentUser.setUserId(validUserId);
                            }

                            // Log after deserialization
                            android.util.Log.d("PlanActivity", "After toObject(User.class):");
                            android.util.Log.d("PlanActivity", "  currentUser.getUserId(): " + currentUser.getUserId());
                            android.util.Log.d("PlanActivity", "  currentUser.getPartnerId(): " + currentUser.getPartnerId());



                            // ⚠️ FIX: Nếu partnerId null trong object nhưng có trong Firestore, set lại
                            if (currentUser.getPartnerId() == null && doc.contains("partnerId")) {
                                String partnerIdFromDoc = doc.getString("partnerId");
                                android.util.Log.w("PlanActivity", "⚠️ partnerId was NULL in object, setting from Firestore: " + partnerIdFromDoc);
                                currentUser.setPartnerId(partnerIdFromDoc);
                            }

                            // ✅ Chỉ load lịch hôm nay sau khi có currentUser
                            loadPlansForDate(selectedDate.get(Calendar.YEAR),
                                    selectedDate.get(Calendar.MONTH),
                                    selectedDate.get(Calendar.DAY_OF_MONTH));
                        } else {
                            Toast.makeText(this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        Toast.makeText(this, "Không tìm thấy tài khoản", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải thông tin người dùng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    // Callback interface for async coupleId retrieval
    private interface CoupleIdCallback {
        void onSuccess(String coupleId);
        void onFailure();
    }

    /**
     * Get coupleId by querying couples collection
     * ✅ ĐÚNG: Query từ couples collection, không đọc từ User
     */
    private void getCoupleId(CoupleIdCallback callback) {
        if (currentUser == null) {
            android.util.Log.e("PlanActivity", "❌ getCoupleId: currentUser is NULL");
            Toast.makeText(this, "Chưa có thông tin người dùng", Toast.LENGTH_SHORT).show();
            callback.onFailure();
            return;
        }

        String userId = currentUser.getUserId();
        android.util.Log.d("PlanActivity", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        android.util.Log.d("PlanActivity", "Querying couples collection for userId: " + userId);

        // Query couples where user1Id = userId
        FirebaseFirestore.getInstance()
            .collection("couples")
            .whereEqualTo("user1Id", userId)
            .limit(1)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                if (!querySnapshot.isEmpty()) {
                    String coupleId = querySnapshot.getDocuments().get(0).getId();
                    android.util.Log.d("PlanActivity", "✅ Found couple (as user1): " + coupleId);
                    callback.onSuccess(coupleId);
                } else {
                    // Try querying with user2Id
                    android.util.Log.d("PlanActivity", "Not found as user1, trying user2...");
                    FirebaseFirestore.getInstance()
                        .collection("couples")
                        .whereEqualTo("user2Id", userId)
                        .limit(1)
                        .get()
                        .addOnSuccessListener(querySnapshot2 -> {
                            if (!querySnapshot2.isEmpty()) {
                                String coupleId = querySnapshot2.getDocuments().get(0).getId();
                                android.util.Log.d("PlanActivity", "✅ Found couple (as user2): " + coupleId);
                                callback.onSuccess(coupleId);
                            } else {
                                android.util.Log.e("PlanActivity", "❌ No couple found for this user");
                                Toast.makeText(this, "Chưa có partner", Toast.LENGTH_SHORT).show();
                                callback.onFailure();
                            }
                        })
                        .addOnFailureListener(e -> {
                            android.util.Log.e("PlanActivity", "❌ Error querying couples (user2): " + e.getMessage());
                            Toast.makeText(this, "Lỗi tải thông tin couple", Toast.LENGTH_SHORT).show();
                            callback.onFailure();
                        });
                }
            })
            .addOnFailureListener(e -> {
                android.util.Log.e("PlanActivity", "❌ Error querying couples (user1): " + e.getMessage());
                Toast.makeText(this, "Lỗi tải thông tin couple", Toast.LENGTH_SHORT).show();
                callback.onFailure();
            });
    }



    private void loadPlansForDate(int year, int month, int day) {
        getCoupleId(new CoupleIdCallback() {
            @Override
            public void onSuccess(String coupleId) {
                loadPlansForDateWithCoupleId(coupleId, year, month, day);
            }

            @Override
            public void onFailure() {
                scheduleAdapter.setSchedules(new ArrayList<>());
            }
        });
    }

    private void loadPlansForDateWithCoupleId(String coupleId, int year, int month, int day) {

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




    /**
     * Show bottom sheet to add or edit plan
     */
    private void showPlanDetailBottomSheet(Plan plan, boolean isEditMode) {
        PlanDetailBottomSheetFragment fragment;

        if (isEditMode && plan != null) {
            // Edit existing plan
            fragment = PlanDetailBottomSheetFragment.newInstanceForEdit(plan);
        } else {
            // Add new plan
            String dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH) + 1,
                    selectedDate.get(Calendar.DAY_OF_MONTH));
            fragment = PlanDetailBottomSheetFragment.newInstanceForAdd(dateStr);
        }

        // Set listener for save action
        fragment.setOnPlanSaveListener(new PlanDetailBottomSheetFragment.OnPlanSaveListener() {
            @Override
            public void onPlanSave(String planId, String title, String time, String details) {
                // Update existing plan
                String hourPart = time.split(":")[0];
                String minutePart = time.split(":")[1];

                String hourNow = String.format(Locale.getDefault(), "%02d",
                        selectedDate.get(Calendar.HOUR_OF_DAY));
                String minuteNow = String.format(Locale.getDefault(), "%02d",
                        selectedDate.get(Calendar.MINUTE));
                if (Integer.parseInt(hourPart) < Integer.parseInt(hourNow) ||
                        (Integer.parseInt(hourPart) == Integer.parseInt(hourNow) &&
                                Integer.parseInt(minutePart) < Integer.parseInt(minuteNow))) {
                    Toast.makeText(PlanActivity.this, "Không thể cập nhật kế hoạch cho thời gian đã qua", Toast.LENGTH_SHORT).show();
                    return;
                }

                planRepository.updatePlan(planId, title, time, details)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(PlanActivity.this, "Đã cập nhật kế hoạch", Toast.LENGTH_SHORT).show();
                            loadPlansForDate(selectedDate.get(Calendar.YEAR),
                                    selectedDate.get(Calendar.MONTH),
                                    selectedDate.get(Calendar.DAY_OF_MONTH));
                        })
                        .addOnFailureListener(e ->
                            Toast.makeText(PlanActivity.this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );
            }

            @Override
            public void onPlanCreate(String title, String date, String time, String details) {
                // Create new plan
                getCoupleId(new CoupleIdCallback() {
                    @Override
                    public void onSuccess(String coupleId) {
                        Plan newPlan = new Plan(coupleId, title, date, time, details);
                        if(checkPlanPassed(newPlan)) {
                            planRepository.addPlan(newPlan)
                                    .addOnSuccessListener(docRef -> {
                                        Toast.makeText(PlanActivity.this, "Đã thêm kế hoạch", Toast.LENGTH_SHORT).show();
                                        loadPlansForDate(selectedDate.get(Calendar.YEAR),
                                                selectedDate.get(Calendar.MONTH),
                                                selectedDate.get(Calendar.DAY_OF_MONTH));

                                        // Send notification to partner
                                        if (currentUser != null && currentUser.getPartnerId() != null) {
                                            String userName = currentUser.getName() != null ? currentUser.getName() : "Partner";
                                            String partnerId = currentUser.getPartnerId();
                                            String userId = currentUser.getUserId();
                                            String message = userName + " đã tạo kế hoạch mới: " + title;

                                            android.util.Log.d("PlanActivity", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                                            android.util.Log.d("PlanActivity", "Creating notification:");
                                            android.util.Log.d("PlanActivity", "  Sender ID: " + userId);
                                            android.util.Log.d("PlanActivity", "  Receiver ID (Partner): " + partnerId);
                                            android.util.Log.d("PlanActivity", "  Message: " + message);

                                            NotificationManager.getInstance().createNotification(
                                                userId,
                                                partnerId,
                                                message,
                                                new NotificationManager.NotificationCallback() {
                                                    @Override
                                                    public void onSuccess() {
                                                        android.util.Log.d("PlanActivity", "✅ Notification sent successfully to Realtime Database");
                                                    }

                                                    @Override
                                                    public void onError(String error) {
                                                        android.util.Log.e("PlanActivity", "❌ Failed to send notification: " + error);
                                                    }
                                                }
                                            );
                                        } else {
                                            android.util.Log.w("PlanActivity", "⚠️ Cannot send notification - currentUser or partnerId is null");
                                            if (currentUser == null) {
                                                android.util.Log.w("PlanActivity", "  currentUser is NULL");
                                            } else if (currentUser.getPartnerId() == null) {
                                                android.util.Log.w("PlanActivity", "  partnerId is NULL");
                                            }
                                        }
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(PlanActivity.this, "Lỗi thêm: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                    );

                        }else{
                            Toast.makeText(PlanActivity.this, "Không thể thêm kế hoạch cho thời gian đã qua", Toast.LENGTH_SHORT).show();
                        }

                    }

                    @Override
                    public void onFailure() {
                        // Error already shown in getCoupleId
                    }
                });
            }
        });

        // Show the fragment
        fragment.show(getSupportFragmentManager(), "PlanDetailBottomSheet");
    }

    /**
     * Reset form nhập plan
     */
    private void resetPlanForm() {
        etNewPlan.setText("");
        editingPlan = null;
        llAddPlan.setVisibility(View.GONE);
    }

    private boolean checkPlanPassed(Plan plan){
        String dateStr = plan.getDate(); // expected format: "yyyy-MM-dd"
        String timeStr = plan.getTime(); // expected format: "HH:mm"

        String[] dateParts = dateStr.split("-");
        String[] timeParts = timeStr.split(":");

        Calendar planCalendar = Calendar.getInstance();
        planCalendar.set(Calendar.YEAR, Integer.parseInt(dateParts[0]));
        planCalendar.set(Calendar.MONTH, Integer.parseInt(dateParts[1]) - 1); // Month is 0-based
        planCalendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateParts[2]));
        planCalendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeParts[0]));
        planCalendar.set(Calendar.MINUTE, Integer.parseInt(timeParts[1]));
        planCalendar.set(Calendar.SECOND, 0);
        planCalendar.set(Calendar.MILLISECOND, 0);

        Calendar now = Calendar.getInstance();

        if (planCalendar.before(now)) {
            Toast.makeText(this, "Không thể thêm kế hoạch cho thời gian đã qua", Toast.LENGTH_SHORT).show();
            return false;
        }
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

    }
}
