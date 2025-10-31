package com.example.btl_mobileapp.activities; // (Đảm bảo đúng tên package)

import android.app.Dialog;
import android.content.res.Resources;
import android.graphics.Color; // <-- THÊM IMPORT
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.btl_mobileapp.R;
import com.example.btl_mobileapp.adapters.ScheduleAdapter;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PlanDetailSheetFragment extends BottomSheetDialogFragment {

    // --- Biến để truyền dữ liệu ---
    private static final String ARG_ITEM = "plan_item";
    private static final String ARG_DATE = "plan_date";
    private static final String ARG_READ_ONLY = "is_read_only"; // <-- Key cho chế độ "chỉ xem"

    private ScheduleAdapter.ScheduleItem currentItem;
    private String newPlanDate;
    private boolean isReadOnly = false; // <-- Cờ "chỉ xem"

    // --- View ---
    private EditText etPlanTitle, etPlanTime, etPlanDate, etPlanDetails;
    private View btnSave;
    private ImageView btnClose;

    // --- HÀM 1: TẠO MỚI (chỉ truyền ngày) ---
    public static PlanDetailSheetFragment newInstance(String selectedDate) {
        PlanDetailSheetFragment fragment = new PlanDetailSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DATE, selectedDate);
        // isReadOnly mặc định là false khi tạo mới
        fragment.setArguments(args);
        return fragment;
    }

    // --- HÀM 2: SỬA (Thêm cờ isReadOnly) ---
    public static PlanDetailSheetFragment newInstance(ScheduleAdapter.ScheduleItem item, boolean isReadOnly) {
        PlanDetailSheetFragment fragment = new PlanDetailSheetFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ITEM, item);
        args.putBoolean(ARG_READ_ONLY, isReadOnly); // <-- Truyền cờ vào
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            if (getArguments().containsKey(ARG_ITEM)) {
                // SỬA
                currentItem = (ScheduleAdapter.ScheduleItem) getArguments().getSerializable(ARG_ITEM);
                isReadOnly = getArguments().getBoolean(ARG_READ_ONLY); // <-- Lấy cờ ra
            } else if (getArguments().containsKey(ARG_DATE)) {
                // MỚI
                newPlanDate = getArguments().getString(ARG_DATE);
                isReadOnly = false; // Mới thì không bao giờ read-only
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_plan_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ view
        etPlanTitle = view.findViewById(R.id.etPlanTitle);
        etPlanTime = view.findViewById(R.id.etPlanTime);
        etPlanDate = view.findViewById(R.id.etPlanDate);
        etPlanDetails = view.findViewById(R.id.etPlanDetails);
        btnClose = view.findViewById(R.id.btn_close);
        btnSave = view.findViewById(R.id.btnSave);

        // Hiển thị dữ liệu
        if (currentItem != null) {
            // Đang SỬA: Điền tất cả thông tin cũ
            etPlanTitle.setText(currentItem.getTitle());
            etPlanTime.setText(currentItem.getTime());
            etPlanDate.setText(currentItem.getDate());
            etPlanDetails.setText(currentItem.getDetails());
        } else {
            // Đang TẠO MỚI: Chỉ điền ngày đã chọn
            etPlanDate.setText(newPlanDate);
        }

        // Set Listener
        btnClose.setOnClickListener(v -> dismiss());
        btnSave.setOnClickListener(v -> savePlan());

        // --- LOGIC: VÔ HIỆU HÓA NẾU CHỈ XEM ---
        if (isReadOnly) {
            etPlanTitle.setEnabled(false);
            etPlanTime.setEnabled(false);
            etPlanDate.setEnabled(false);
            etPlanDetails.setEnabled(false);
            btnSave.setVisibility(View.GONE); // Ẩn nút "Lưu"

            // (Tùy chọn) Đổi màu nền của EditText để trông giống TextView
            etPlanTitle.setBackground(null);
            etPlanTime.setBackground(null);
            etPlanDate.setBackground(null);
            etPlanDetails.setBackground(null);
        }
    }

    // --- HÀM LƯU (VỚI LOGIC KIỂM TRA) ---
    private void savePlan() {
        String title = etPlanTitle.getText().toString().trim();
        String timeStr = etPlanTime.getText().toString().trim();
        String dateStr = etPlanDate.getText().toString().trim();
        String details = etPlanDetails.getText().toString().trim();

        if (title.isEmpty() || dateStr.isEmpty()) {
            Toast.makeText(getContext(), "Tiêu đề và Ngày không được trống", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- BẮT ĐẦU KIỂM TRA ---

        // 1. Kiểm tra định dạng ngày (dd/MM/yyyy)
        Calendar parsedDate = parseDate(dateStr);
        if (parsedDate == null) {
            Toast.makeText(getContext(), "Lỗi: Sai định dạng ngày (phải là dd/MM/yyyy)", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Kiểm tra ngày quá khứ
        if (isPastDate(parsedDate)) {
            Toast.makeText(getContext(), "Lỗi: Không thể lên kế hoạch cho ngày quá khứ", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. Kiểm tra giờ (nếu là ngày hôm nay)
        if (isToday(parsedDate) && !timeStr.isEmpty()) {
            Calendar parsedTime = parseTime(timeStr);

            // 3a. Kiểm tra định dạng giờ (HH:mm)
            if (parsedTime == null) {
                Toast.makeText(getContext(), "Lỗi: Sai định dạng giờ (phải là HH:mm, ví dụ 07:00)", Toast.LENGTH_SHORT).show();
                return;
            }

            // 3b. Kiểm tra giờ quá khứ
            Calendar now = Calendar.getInstance();
            Calendar planTimeToday = (Calendar) now.clone();
            planTimeToday.set(Calendar.HOUR_OF_DAY, parsedTime.get(Calendar.HOUR_OF_DAY));
            planTimeToday.set(Calendar.MINUTE, parsedTime.get(Calendar.MINUTE));
            planTimeToday.set(Calendar.SECOND, 0);

            if (planTimeToday.before(now)) {
                Toast.makeText(getContext(), "Lỗi: Giờ này đã qua trong ngày hôm nay", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // --- KẾT THÚC KIỂM TRA ---

        String coupleId = (currentItem != null) ? currentItem.getCoupleId() : getCoupleIdFromActivity();

        if (coupleId == null) {
            Toast.makeText(getContext(), "Lỗi: Không tìm thấy coupleId", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> planData = new HashMap<>();
        planData.put("title", title);
        planData.put("time", timeStr);
        planData.put("date", dateStr);
        planData.put("details", details);
        planData.put("coupleId", coupleId);

        if (currentItem != null) {
            // SỬA
            FirebaseFirestore.getInstance()
                    .collection("couple_plans")
                    .document(currentItem.getId())
                    .update(planData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Đã cập nhật kế hoạch", Toast.LENGTH_SHORT).show();
                        dismiss();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi cập nhật", Toast.LENGTH_SHORT).show());
        } else {
            // TẠO MỚI
            FirebaseFirestore.getInstance()
                    .collection("couple_plans")
                    .add(planData)
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(getContext(), "Đã thêm kế hoạch", Toast.LENGTH_SHORT).show();
                        dismiss();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi thêm: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    // --- CÁC HÀM TRỢ GIÚP KIỂM TRA ---

    private Calendar parseDate(String dateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        sdf.setLenient(false);
        try {
            Date date = sdf.parse(dateStr);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            return cal;
        } catch (ParseException e) {
            return null;
        }
    }

    private Calendar parseTime(String timeStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        sdf.setLenient(false);
        try {
            Date time = sdf.parse(timeStr);
            Calendar cal = Calendar.getInstance();
            cal.setTime(time);
            return cal;
        } catch (ParseException e) {
            return null;
        }
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

    private String getCoupleIdFromActivity() {
        if (getActivity() instanceof PlanActivity) {
            return ((PlanActivity) getActivity()).getCoupleId();
        }
        return null;
    }

    // --- CÀI ĐẶT STYLE VÀ CHIỀU CAO CHO BOTTOMSHEET ---

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(d -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) d;
            FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
                int desiredHeight = (int) (screenHeight * 0.85); // 85%

                ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
                layoutParams.height = desiredHeight;
                bottomSheet.setLayoutParams(layoutParams);

                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setDraggable(true);
            }
        });
        return dialog;
    }

    @Override
    public int getTheme() {
        // Đảm bảo bạn đã định nghĩa style này trong res/values/themes.xml
        return R.style.BottomSheetDialogTheme;
    }
}