package com.example.btl_mobileapp.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
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

public class HomeMain2Fragment extends Fragment {
    private static final ExecutorService IMAGE_EXECUTOR = Executors.newFixedThreadPool(2);

    // Views for Day Counter
    private TextView tvDaysCount, tvSince;

    // Views for User Profiles
    private ImageView avtLeft, avtRight, imgGenderLeft, imgGenderRight;
    private TextView txtNameLeft, txtNameRight, tvAgeLeft, tvAgeRight, tvZodiacLeft, tvZodiacRight;
    private LinearLayout userProfileLeft, userProfileRight;

    public HomeMain2Fragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.homemain2, container, false);
        initViews(root);
        loadCoupleData();
        return root;
    }

    private void initViews(View root) {
        // Day Counter Views
        tvDaysCount = root.findViewById(R.id.tvDaysCount);
        tvSince = root.findViewById(R.id.tvSince);

        // User Info Container
        View userInfo = root.findViewById(R.id.layoutUserInfo);
        userProfileLeft = userInfo.findViewById(R.id.userProfileLeft);
        avtLeft = userInfo.findViewById(R.id.avtLeft);
        txtNameLeft = userInfo.findViewById(R.id.txtNameLeft);
        tvAgeLeft = userInfo.findViewById(R.id.tvAgeLeft);
        tvZodiacLeft = userInfo.findViewById(R.id.tvZodiacLeft);
        imgGenderLeft = userInfo.findViewById(R.id.imgGenderLeft);

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

                // Update the day counter as soon as we have the date
                if (currentUser.getStartLoveDate() != null) {
                    updateDayCounter(currentUser.getStartLoveDate());
                }

                if (!TextUtils.isEmpty(currentUser.getPartnerId())) {
                    DatabaseManager.getInstance().getUser(currentUser.getPartnerId(), new DatabaseManager.DatabaseCallback<User>() {
                        @Override
                        public void onSuccess(User partnerUser) {
                            if (partnerUser != null && isAdded()) {
                                arrangeAndBindProfiles(currentUser, partnerUser);
                            }
                        }
                        @Override
                        public void onError(String error) {
                            arrangeAndBindProfiles(currentUser, null);
                        }
                    });
                } else {
                    arrangeAndBindProfiles(currentUser, null);
                }
            }
            @Override
            public void onError(String error) { /* Handle error */ }
        });
    }

    private void updateDayCounter(Timestamp startDate) {
        if (startDate == null || !isAdded()) return;

        long diffInMillis = new Date().getTime() - startDate.toDate().getTime();
        long daysBetween = TimeUnit.MILLISECONDS.toDays(diffInMillis) + 1; // Add 1 to be inclusive

        String sinceDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(startDate.toDate());

        safeSetText(tvDaysCount, String.format(Locale.getDefault(), "%d ngày", daysBetween));
        safeSetText(tvSince, "Từ " + sinceDate);
    }

    private void arrangeAndBindProfiles(User user1, @Nullable User user2) {
        User userLeft, userRight = null;

        if (user2 != null && "Male".equals(user2.getGender()) && "Female".equals(user1.getGender())) {
            // If user1 is female and user2 is male, swap them
            userLeft = user2;
            userRight = user1;
        } else {
            // Default: user1 on left, user2 on right (handles Male/Female, same gender, or single user)
            userLeft = user1;
            userRight = user2;
        }

        bindSingleProfile(userLeft, avtLeft, txtNameLeft, tvAgeLeft, tvZodiacLeft, imgGenderLeft);
        userProfileLeft.setVisibility(View.VISIBLE);

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
            switch (gender) {
                case "Male":
                    genderIcon.setImageResource(R.drawable.gender);
                    break;
                case "Female":
                    genderIcon.setImageResource(R.drawable.girl);
                    break;
                default:
                    // Hide icon if gender is "Other" or not set
                    genderIcon.setVisibility(View.GONE);
                    break;
            }
        } else {
            genderIcon.setVisibility(View.GONE);
        }
    }

    // --- Helper Methods (Copied from HomeMain1Fragment) ---

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

        } catch (ParseException e) { return ""; }
        return "";
    }

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
            } catch (Exception e) { /* Handle error */ }
        });
    }

    private void safeSetText(@Nullable TextView tv, @Nullable String text) {
        if (tv != null && isAdded()) {
            requireActivity().runOnUiThread(() -> tv.setText(text != null ? text : ""));
        }
    }

    private interface BitmapCallback { void onBitmap(Bitmap bmp); }
}
