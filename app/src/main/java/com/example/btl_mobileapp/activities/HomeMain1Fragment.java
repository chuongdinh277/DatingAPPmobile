package com.example.btl_mobileapp.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.btl_mobileapp.R;
import com.example.btl_mobileapp.managers.DatabaseManager;
import com.example.btl_mobileapp.models.User;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HomeMain1Fragment extends Fragment {
    private static final ExecutorService IMAGE_EXECUTOR = Executors.newFixedThreadPool(2);

    // Views for Love Counter
    private TextView txtYears, txtMonths, txtWeeks, txtDays, txtTimer;

    // Views for User Profiles
    private ImageView avtLeft, avtRight, imgGenderLeft, imgGenderRight;
    private TextView txtNameLeft, txtNameRight, tvAgeLeft, tvAgeRight, tvZodiacLeft, tvZodiacRight;
    private LinearLayout userProfileLeft, userProfileRight;


    private final Handler loveCounterHandler = new Handler(Looper.getMainLooper());
    private Runnable loveCounterRunnable;
    private Timestamp startLoveDate = null;

    public HomeMain1Fragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.homemain1, container, false);
        initViews(root);
        loadCoupleData();
        return root;
    }

    private void initViews(View root) {
        // Love Counter Views
        txtYears = root.findViewById(R.id.txtYears);
        txtMonths = root.findViewById(R.id.txtMonths);
        txtWeeks = root.findViewById(R.id.txtWeeks);
        txtDays = root.findViewById(R.id.txtDays);
        txtTimer = root.findViewById(R.id.txtTimer);

        // User Info Container
        View userInfo = root.findViewById(R.id.layoutUserInfo);

        // Left Profile Views
        userProfileLeft = userInfo.findViewById(R.id.userProfileLeft);
        avtLeft = userInfo.findViewById(R.id.avtLeft);
        txtNameLeft = userInfo.findViewById(R.id.txtNameLeft);
        tvAgeLeft = userInfo.findViewById(R.id.tvAgeLeft);
        tvZodiacLeft = userInfo.findViewById(R.id.tvZodiacLeft);
        imgGenderLeft = userInfo.findViewById(R.id.imgGenderLeft);

        // Right Profile Views
        userProfileRight = userInfo.findViewById(R.id.userProfileRight);
        avtRight = userInfo.findViewById(R.id.avtRight);
        txtNameRight = userInfo.findViewById(R.id.txtNameRight);
        tvAgeRight = userInfo.findViewById(R.id.tvAgeRight);
        tvZodiacRight = userInfo.findViewById(R.id.tvZodiacRight);
        imgGenderRight = userInfo.findViewById(R.id.imgGenderRight);
    }

    private void loadCoupleData() {
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser == null) return;

        DatabaseManager.getInstance().getUser(fbUser.getUid(), new DatabaseManager.DatabaseCallback<User>() {
            @Override
            public void onSuccess(User currentUser) {
                if (currentUser == null || !isAdded()) return;

                if (!TextUtils.isEmpty(currentUser.getPartnerId())) {
                    DatabaseManager.getInstance().getUser(currentUser.getPartnerId(), new DatabaseManager.DatabaseCallback<User>() {
                        @Override
                        public void onSuccess(User partnerUser) {
                            if (partnerUser != null && isAdded()) {
                                arrangeAndBindProfiles(currentUser, partnerUser);
                                startLoveDate = currentUser.getStartLoveDate();
                                if (startLoveDate != null) {
                                    startLoveCounter();
                                }
                            }
                        }
                        @Override
                        public void onError(String error) {
                            // Partner not found, just show current user
                            arrangeAndBindProfiles(currentUser, null);
                        }
                    });
                } else {
                    // No partner, just show current user
                    arrangeAndBindProfiles(currentUser, null);
                }
            }
            @Override
            public void onError(String error) { /* Handle error */ }
        });
    }

    private void arrangeAndBindProfiles(User user1, @Nullable User user2) {
        User userLeft = null, userRight = null;

        String gender1 = user1.getGender();

        if (user2 != null) {
            String gender2 = user2.getGender();
            // Male on left, Female on right is the priority
            if ("Male".equals(gender1) && "Female".equals(gender2)) {
                userLeft = user1;
                userRight = user2;
            } else if ("Female".equals(gender1) && "Male".equals(gender2)) {
                userLeft = user2;
                userRight = user1;
            } else {
                // Fallback: current user on left, partner on right
                userLeft = user1;
                userRight = user2;
            }
        } else {
            // Only one user, place them on the left
            userLeft = user1;
        }


        if (userLeft != null) {
            bindSingleProfile(userLeft, avtLeft, txtNameLeft, tvAgeLeft, tvZodiacLeft, imgGenderLeft);
            userProfileLeft.setVisibility(View.VISIBLE);
        }

        if (userRight != null) {
            bindSingleProfile(userRight, avtRight, txtNameRight, tvAgeRight, tvZodiacRight, imgGenderRight);
            userProfileRight.setVisibility(View.VISIBLE);
        } else {
            userProfileRight.setVisibility(View.INVISIBLE);
        }
    }


    private void bindSingleProfile(User user, ImageView avatar, TextView name, TextView age, TextView zodiac, ImageView genderIcon) {
        safeSetText(name, user.getName());
        loadImageAsync(user.getProfilePicUrl(), avatar::setImageBitmap);

        String dob = user.getDateOfBirth();
        if (!TextUtils.isEmpty(dob)) {
            safeSetText(age, String.valueOf(calculateAge(dob)));
            safeSetText(zodiac, getZodiacSign(dob));
        }

        String gender = user.getGender();
        if (gender != null) {
            genderIcon.setVisibility(View.VISIBLE);
            switch (gender) {
                case "Male":
                    genderIcon.setImageResource(R.drawable.gender); // Replace with your male icon
                    break;
                case "Female":
                    genderIcon.setImageResource(R.drawable.girl); // Replace with your female icon
                    break;
                default:
                    genderIcon.setVisibility(View.GONE);
                    break;
            }
        } else {
            genderIcon.setVisibility(View.GONE);
        }
    }

    private int calculateAge(String dobString) {
        if (TextUtils.isEmpty(dobString)) return 0;
        try {
            Date dob = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dobString);
            Calendar dobCal = Calendar.getInstance();
            dobCal.setTime(dob);
            Calendar today = Calendar.getInstance();
            int age = today.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR);
            if (today.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }
            return age;
        } catch (ParseException e) {
            return 0;
        }
    }

    private String getZodiacSign(String dobString) {
        if (TextUtils.isEmpty(dobString)) return "";
        try {
            Date dob = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dobString);
            Calendar cal = Calendar.getInstance();
            cal.setTime(dob);
            int day = cal.get(Calendar.DAY_OF_MONTH);
            int month = cal.get(Calendar.MONTH) + 1;

            if ((month == 3 && day >= 21) || (month == 4 && day <= 19)) return "Bạch Dương";
            if ((month == 4 && day >= 20) || (month == 5 && day <= 20)) return "Kim Ngưu";
            if ((month == 5 && day >= 21) || (month == 6 && day <= 20)) return "Song Tử";
            if ((month == 6 && day >= 21) || (month == 7 && day <= 22)) return "Cự Giải";
            if ((month == 7 && day >= 23) || (month == 8 && day <= 22)) return "Sư Tử";
            if ((month == 8 && day >= 23) || (month == 9 && day <= 22)) return "Xử Nữ";
            if ((month == 9 && day >= 23) || (month == 10 && day <= 22)) return "Thiên Bình";
            if ((month == 10 && day >= 23) || (month == 11 && day <= 21)) return "Bọ Cạp";
            if ((month == 11 && day >= 22) || (month == 12 && day <= 21)) return "Nhân Mã";
            if ((month == 12 && day >= 22) || (month == 1 && day <= 19)) return "Ma Kết";
            if ((month == 1 && day >= 20) || (month == 2 && day <= 18)) return "Bảo Bình";
            if ((month == 2 && day >= 19) || (month == 3 && day <= 20)) return "Song Ngư";

        } catch (ParseException e) {
            return "";
        }
        return "";
    }

    private void startLoveCounter() {
        loveCounterRunnable = new Runnable() {
            @Override
            public void run() {
                if (startLoveDate == null || !isAdded()) return;

                Date startDate = startLoveDate.toDate();
                Date currentDate = new Date();
                long diffInMillis = currentDate.getTime() - startDate.getTime();

                // --- Accurate Breakdown Calculation ---
                Calendar startCal = Calendar.getInstance();
                startCal.setTime(startDate);
                Calendar currentCal = Calendar.getInstance();
                currentCal.setTime(currentDate);

                int years = currentCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR);
                int months = currentCal.get(Calendar.MONTH) - startCal.get(Calendar.MONTH);
                int days = currentCal.get(Calendar.DAY_OF_MONTH) - startCal.get(Calendar.DAY_OF_MONTH);

                if (days < 0) {
                    months--;
                    // To get the days of the previous month, we need to adjust the current calendar
                    Calendar tempCal = (Calendar) currentCal.clone();
                    tempCal.add(Calendar.MONTH, -1);
                    days += tempCal.getActualMaximum(Calendar.DAY_OF_MONTH);
                }

                if (months < 0) {
                    years--;
                    months += 12;
                }

                // Now, from the remaining days, calculate weeks
                int weeks = days / 7;
                int remainingDays = days % 7;

                // Update UI with the breakdown
                safeSetText(txtYears, String.valueOf(years));
                safeSetText(txtMonths, String.valueOf(months));
                safeSetText(txtWeeks, String.valueOf(weeks));
                safeSetText(txtDays, String.valueOf(remainingDays));

                // Calculate HH:MM:SS timer for the current day
                long hours = TimeUnit.MILLISECONDS.toHours(diffInMillis) % 24;
                long minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis) % 60;
                long seconds = TimeUnit.MILLISECONDS.toSeconds(diffInMillis) % 60;
                safeSetText(txtTimer, String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));

                // Schedule next update
                loveCounterHandler.postDelayed(this, 1000);
            }
        };
        loveCounterHandler.post(loveCounterRunnable);
    }


    @Override
    public void onPause() {
        super.onPause();
        loveCounterHandler.removeCallbacks(loveCounterRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (startLoveDate != null) {
            startLoveCounter();
        }
    }

    // --- Helper Methods ---
    private void loadImageAsync(String urlStr, BitmapCallback callback) {
        if (TextUtils.isEmpty(urlStr)) return;
        IMAGE_EXECUTOR.execute(() -> {
            try {
                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap bmp = BitmapFactory.decodeStream(input);
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> callback.onBitmap(bmp));
                }
            } catch (Exception e) {
                // Handle error
            }
        });
    }

    private void safeSetText(@Nullable TextView tv, @Nullable String text) {
        if (tv != null && isAdded()) {
            requireActivity().runOnUiThread(() -> tv.setText(text != null ? text : ""));
        }
    }

    private interface BitmapCallback { void onBitmap(Bitmap bmp); }
}

