package com.example.couple_app.ui.fragments;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.couple_app.R;
import com.example.couple_app.data.model.Plan;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Calendar;
import java.util.Locale;

/**
 * Bottom sheet dialog fragment for displaying and editing plan details
 * Height is set to 85% of screen with draggable behavior
 */
public class PlanDetailBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String TAG = "PlanDetailBottomSheet";
    private static final float BOTTOM_SHEET_PEEK_HEIGHT_RATIO = 0.85f; // 85% of screen height

    // Arguments
    private static final String ARG_PLAN_ID = "plan_id";
    private static final String ARG_PLAN_TITLE = "plan_title";
    private static final String ARG_PLAN_DATE = "plan_date";
    private static final String ARG_PLAN_TIME = "plan_time";
    private static final String ARG_PLAN_DETAILS = "plan_details";
    private static final String ARG_IS_EDIT_MODE = "is_edit_mode";

    // UI Components
    private ImageView btnClose;
    private EditText etPlanTitle;
    private EditText etPlanTime;
    private EditText etPlanDate;
    private EditText etPlanDetails;
    private Button btnSave;

    // Data
    private String planId;
    private String planTitle;
    private String planDate;
    private String planTime;
    private String planDetails;
    private boolean isEditMode;

    // Callback interface
    private OnPlanSaveListener saveListener;

    public interface OnPlanSaveListener {
        void onPlanSave(String planId, String title, String time, String details);
        void onPlanCreate(String title, String date, String time, String details);
    }

    /**
     * Create new instance for adding a new plan
     */
    public static PlanDetailBottomSheetFragment newInstanceForAdd(String date) {
        PlanDetailBottomSheetFragment fragment = new PlanDetailBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PLAN_DATE, date);
        args.putBoolean(ARG_IS_EDIT_MODE, false);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Create new instance for editing an existing plan
     */
    public static PlanDetailBottomSheetFragment newInstanceForEdit(Plan plan) {
        PlanDetailBottomSheetFragment fragment = new PlanDetailBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PLAN_ID, plan.getId());
        args.putString(ARG_PLAN_TITLE, plan.getTitle());
        args.putString(ARG_PLAN_DATE, plan.getDate());
        args.putString(ARG_PLAN_TIME, plan.getTime());
        args.putString(ARG_PLAN_DETAILS, plan.getDetails());
        args.putBoolean(ARG_IS_EDIT_MODE, true);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnPlanSaveListener(OnPlanSaveListener listener) {
        this.saveListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);

            if (bottomSheet != null) {
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);

                // Set peek height to 85% of screen
                int screenHeight = getResources().getDisplayMetrics().heightPixels;
                int peekHeight = (int) (screenHeight * BOTTOM_SHEET_PEEK_HEIGHT_RATIO);
                behavior.setPeekHeight(peekHeight);

                // Set behavior properties
                behavior.setHideable(true);
                behavior.setSkipCollapsed(false);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

                // Set max height
                ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
                layoutParams.height = peekHeight;
                bottomSheet.setLayoutParams(layoutParams);
            }
        });

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_plan_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get arguments
        if (getArguments() != null) {
            planId = getArguments().getString(ARG_PLAN_ID);
            planTitle = getArguments().getString(ARG_PLAN_TITLE, "");
            planDate = getArguments().getString(ARG_PLAN_DATE, "");
            planTime = getArguments().getString(ARG_PLAN_TIME, "");
            planDetails = getArguments().getString(ARG_PLAN_DETAILS, "");
            isEditMode = getArguments().getBoolean(ARG_IS_EDIT_MODE, false);
        }

        // Initialize views
        initializeViews(view);

        // Populate data
        populateData();

        // Setup listeners
        setupListeners();
    }

    private void initializeViews(View view) {
        btnClose = view.findViewById(R.id.btn_close);
        etPlanTitle = view.findViewById(R.id.etPlanTitle);
        etPlanTime = view.findViewById(R.id.etPlanTime);
        etPlanDate = view.findViewById(R.id.etPlanDate);
        etPlanDetails = view.findViewById(R.id.etPlanDetails);
        btnSave = view.findViewById(R.id.btnSave);

        // Debug log
        if (btnClose == null) {
            android.util.Log.e("PlanDetail", "btnClose is NULL!");
        } else {
            android.util.Log.d("PlanDetail", "btnClose initialized successfully");
        }
    }

    private void populateData() {
        // Set date
        if (planDate != null && !planDate.isEmpty()) {
            // Convert yyyy-MM-dd to dd/MM/yy format
            String[] dateParts = planDate.split("-");
            if (dateParts.length == 3) {
                String formattedDate = dateParts[2] + "/" + dateParts[1] + "/" + dateParts[0].substring(2);
                etPlanDate.setText(formattedDate);
            } else {
                etPlanDate.setText(planDate);
            }
        }

        // Set data if editing
        if (isEditMode) {
            if (planTitle != null) etPlanTitle.setText(planTitle);
            if (planTime != null) etPlanTime.setText(planTime);
            if (planDetails != null) etPlanDetails.setText(planDetails);
        }
    }

    private void setupListeners() {
        // Close button
        if (btnClose != null) {
            btnClose.setClickable(true);
            btnClose.setOnClickListener(v -> {
                android.util.Log.d("PlanDetail", "Close button clicked!");
                try {
                    dismiss();
                    android.util.Log.d("PlanDetail", "Dismissed successfully");
                } catch (Exception e) {
                    android.util.Log.e("PlanDetail", "Error dismissing: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            android.util.Log.d("PlanDetail", "Close button listener set");
        } else {
            android.util.Log.e("PlanDetail", "Cannot set listener - btnClose is null!");
        }

        // Time picker on click
        if (etPlanTime != null) {
            etPlanTime.setOnClickListener(v -> showTimePicker());
            etPlanTime.setFocusable(false);
        }

        // Date picker on click - only allow changing date when creating new plan
        if (etPlanDate != null) {
            if (isEditMode) {
                // Disable date editing in edit mode
                etPlanDate.setEnabled(false);
                etPlanDate.setFocusable(false);
                etPlanDate.setAlpha(0.6f); // Visual indication that it's disabled
            } else {
                // Allow date selection when creating new plan
                etPlanDate.setOnClickListener(v -> showDatePicker());
                etPlanDate.setFocusable(false);
            }
        }

        // Save button
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> savePlan());
        }
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();

        // Parse current time if exists
        String currentTime = etPlanTime.getText().toString();
        if (currentTime.contains(":")) {
            String[] parts = currentTime.split(":");
            try {
                calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
                calendar.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
            } catch (Exception e) {
                // Use current time
            }
        }

        TimePickerDialog timePickerDialog = new TimePickerDialog(
            requireContext(),
            (view, hourOfDay, minute) -> {
                String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                etPlanTime.setText(time);
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true // 24-hour format
        );

        timePickerDialog.show();
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        // Parse current date if exists (dd/MM/yy format)
        String currentDate = etPlanDate.getText().toString();
        if (currentDate.contains("/")) {
            String[] parts = currentDate.split("/");
            try {
                int day = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]) - 1; // Month is 0-based
                int year = Integer.parseInt(parts[2]);
                if (year < 100) year += 2000; // Convert 2-digit year to 4-digit
                calendar.set(year, month, day);
            } catch (Exception e) {
                // Use current date
            }
        }

        android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
            requireContext(),
            (view, year, month, dayOfMonth) -> {
                String date = String.format(Locale.getDefault(), "%02d/%02d/%02d",
                    dayOfMonth, month + 1, year % 100);
                etPlanDate.setText(date);

                // Store full date format for saving
                planDate = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                    year, month + 1, dayOfMonth);
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void savePlan() {
        // Validate input
        String title = etPlanTitle.getText().toString().trim();
        String time = etPlanTime.getText().toString().trim();
        String details = etPlanDetails.getText().toString().trim();
        String dateInput = etPlanDate.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng nhập tiêu đề", Toast.LENGTH_SHORT).show();
            etPlanTitle.requestFocus();
            return;
        }

        // Convert date from dd/MM/yy to yyyy-MM-dd if needed
        if (!dateInput.isEmpty() && dateInput.contains("/")) {
            String[] parts = dateInput.split("/");
            if (parts.length == 3) {
                try {
                    int day = Integer.parseInt(parts[0]);
                    int month = Integer.parseInt(parts[1]);
                    int year = Integer.parseInt(parts[2]);
                    if (year < 100) year += 2000;
                    planDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month, day);
                } catch (Exception e) {
                    // Keep original planDate
                }
            }
        }

        // Call listener
        if (saveListener != null) {
            if (isEditMode) {
                saveListener.onPlanSave(planId, title, time, details);
            } else {
                saveListener.onPlanCreate(title, planDate, time, details);
            }
        }

        // Dismiss dialog
        dismiss();
    }

    @Override
    public int getTheme() {
        return R.style.BottomSheetDialogTheme;
    }
}

