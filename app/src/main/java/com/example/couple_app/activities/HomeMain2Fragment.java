package com.example.couple_app.activities;

// Thêm các import cần thiết
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.couple_app.R;
import com.example.couple_app.utils.FragmentUtils;
import com.example.couple_app.viewmodels.UserProfileData;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


public class HomeMain2Fragment extends Fragment {
    private static final String TAG = "HomeMain2Fragment";
    private UserProfileData profileData;

    // Các Views cho bộ đếm ngày yêu
    private TextView tvDaysCount;
    private TextView tvSince;

    // --- Biến mới để đồng bộ nền ---
    private SharedPreferences sharedPreferences;
    private View rootLayout; // Dùng View cho chung, hoặc ConstraintLayout nếu chắc chắn
    private ImageView imgBackground; // ImageView nền toàn màn hình

    // Sao chép các hằng số SharedPreferences từ HomeMain1Fragment
    private static final String PREFS_NAME = "AppSettings";
    private static final String PREF_BACKGROUND_URL = "saved_background_url";
    private static final String PREF_BACKGROUND_SOURCE_TYPE = "background_source_type";
    private static final String SOURCE_TYPE_RESOURCE = "resource";
    private static final String SOURCE_TYPE_URL = "url";
    // ---------------------------------

    public HomeMain2Fragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.homemain2, container, false);
        profileData = UserProfileData.getInstance();

        // --- Cập nhật onCreateView ---
        // 1. Tìm root layout bằng ID đã thêm
        rootLayout = root.findViewById(R.id.root_homemain2);
        // 1b. Tìm ImageView nền và đảm bảo scaleType
        imgBackground = root.findViewById(R.id.img_background2);
        if (rootLayout != null) {
            // Xóa nền mặc định (nếu có) để chỉ dùng ImageView
            rootLayout.setBackground(null);
        }
        if (imgBackground != null) {
            imgBackground.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }

        // 2. Khởi tạo SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // 3. Tải nền đã lưu ngay lập tức
        loadSavedBackground();
        // -----------------------------

        // Find Love Counter Views
        tvDaysCount = root.findViewById(R.id.tvDaysCount);
        tvSince = root.findViewById(R.id.tvSince);

        // Bind user profile information
        bindUserProfile(root);

        // Tính và hiển thị Ngày Yêu
        calculateAndDisplayLoveDays();

        return root;
    }

    // --- Thêm hàm onResume ---
    /**
     * Tải lại nền mỗi khi tab này được quay lại
     * (Phòng trường hợp người dùng đổi nền ở HomeMain1Fragment)
     */
    @Override
    public void onResume() {
        super.onResume();
        if (sharedPreferences != null && imgBackground != null) {
            Log.d(TAG, "onResume: Reloading saved background.");
            loadSavedBackground();
        }
    }
    // -------------------------

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        tvDaysCount = null;
        tvSince = null;
        rootLayout = null; // Thêm
        imgBackground = null; // Giải phóng tham chiếu ImageView nền
    }

    // ...existing code...

    // --- BẮT ĐẦU: Sao chép 2 HÀM TỪ HomeMain1Fragment ---

    /**
     * Tải nền đã lưu (URL hoặc Resource ID) và hiển thị lên ImageView nền
     */
    private void loadSavedBackground() {
        if (sharedPreferences != null) {
            String savedUrlOrId = sharedPreferences.getString(PREF_BACKGROUND_URL, null);
            String sourceType = sharedPreferences.getString(PREF_BACKGROUND_SOURCE_TYPE, SOURCE_TYPE_URL);

            if (savedUrlOrId != null && !savedUrlOrId.isEmpty()) {
                if (SOURCE_TYPE_RESOURCE.equals(sourceType)) {
                    // Tải từ Resource ID (Nền mặc định)
                    try {
                        int resourceId = Integer.parseInt(savedUrlOrId);
                        if (imgBackground != null) {
                            imgBackground.setImageResource(resourceId);
                            Log.d(TAG, "Loading background from saved Resource ID: " + resourceId);
                        }
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Invalid Resource ID saved in SharedPreferences.");
                        loadBackgroundFromUrl(savedUrlOrId); // Thử tải như URL nếu lỗi
                    }
                } else {
                    // Tải từ URL (Ảnh tùy chỉnh)
                    Log.d(TAG, "Loading background from SharedPreferences URL: " + savedUrlOrId);
                    loadBackgroundFromUrl(savedUrlOrId);
                }
            } else {
                Log.d(TAG, "No background data found in SharedPreferences. Setting default.");
                // Đặt nền mặc định nếu không có gì trong SharedPreferences
                if (imgBackground != null) {
                    imgBackground.setImageResource(R.drawable.background_home);
                }
            }
        }
    }

    /**
     * Tải nền từ URL và hiển thị lên ImageView nền (centerCrop, tránh kéo giãn)
     */
    private void loadBackgroundFromUrl(String url) {
        if (url == null || url.isEmpty() || !isAdded() || imgBackground == null) {
            Log.w(TAG, "Cannot load background: URL is null/empty, or fragment/layout not ready.");
            if (imgBackground != null && imgBackground.getDrawable() == null) {
                imgBackground.setImageResource(R.drawable.background_home);
            }
            return;
        }

        Log.d(TAG, "Loading background from URL using Glide: " + url);
        Glide.with(this)
                .load(url)
                .centerCrop()
                .error(R.drawable.background_home)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imgBackground);
    }

    // --- KẾT THÚC: Sao chép 2 HÀM ---

    // ...existing code...
    private void calculateAndDisplayLoveDays() {
        Timestamp startLoveDate = profileData.getCurrentUserStartLoveDate();

        if (startLoveDate == null) {
            safeSetText(tvDaysCount, "0 ngày");
            safeSetText(tvSince, "Chưa thiết lập ngày yêu");
            return;
        }

        Date startDate = startLoveDate.toDate();
        Date currentDate = new Date();
        long diffInMillis = currentDate.getTime() - startDate.getTime();

        if (diffInMillis < 0) {
            safeSetText(tvDaysCount, "Lỗi");
            safeSetText(tvSince, "Ngày bắt đầu không hợp lệ");
            return;
        }

        // 1. Tính tổng số ngày
        // Tính tổng mili giây -> chia cho số mili giây trong 1 ngày
        long totalDays = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);
        // Cộng thêm 1 để bao gồm ngày bắt đầu (tính trọn ngày)
        totalDays++;

        // 2. Định dạng ngày bắt đầu
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String formattedDate = "Từ " + sdf.format(startDate);

        // 3. Cập nhật UI
        safeSetText(tvDaysCount, totalDays + " ngày");
        safeSetText(tvSince, formattedDate);
    }
    private void bindUserProfile(View root) {
        View userInfo = root.findViewById(R.id.layoutUserInfo);
        if (userInfo == null) return;

        // Views for left user (current user)
        ImageView avtLeft = userInfo.findViewById(R.id.avtLeft);
        TextView txtNameLeft = userInfo.findViewById(R.id.txtNameLeft);
        TextView tvAgeLeft = userInfo.findViewById(R.id.tvAgeLeft);
        TextView tvZodiacLeft = userInfo.findViewById(R.id.tvZodiacLeft);

        // Views for right user (partner)
        ImageView avtRight = userInfo.findViewById(R.id.avtRight);
        TextView txtNameRight = userInfo.findViewById(R.id.txtNameRight);
        TextView tvAgeRight = userInfo.findViewById(R.id.tvAgeRight);
        TextView tvZodiacRight = userInfo.findViewById(R.id.tvZodiacRight);

        // Check if data is already loaded - use cached data
        if (profileData.isLoaded() && profileData.hasCurrentUserData()) {
            displayCachedData(avtLeft, txtNameLeft, tvAgeLeft, tvZodiacLeft,
                    avtRight, txtNameRight, tvAgeRight, tvZodiacRight);
            return;
        }

        // Just display what we have from cache for now
        displayCachedData(avtLeft, txtNameLeft, tvAgeLeft, tvZodiacLeft,
                avtRight, txtNameRight, tvAgeRight, tvZodiacRight);
    }

    private void displayCachedData(ImageView avtLeft, TextView txtNameLeft, TextView tvAgeLeft, TextView tvZodiacLeft,
                                   ImageView avtRight, TextView txtNameRight, TextView tvAgeRight, TextView tvZodiacRight) {
        // Current user
        if (profileData.getCurrentUserAvatar() != null) {
            avtLeft.setImageBitmap(profileData.getCurrentUserAvatar());
        }
        FragmentUtils.safeSetText(this, txtNameLeft, profileData.getCurrentUserName());
        if (profileData.getCurrentUserAge() > 0) {
            FragmentUtils.safeSetText(this, tvAgeLeft, String.valueOf(profileData.getCurrentUserAge()));
        }
        if (!TextUtils.isEmpty(profileData.getCurrentUserZodiac())) {
            FragmentUtils.safeSetText(this, tvZodiacLeft, profileData.getCurrentUserZodiac());
        }

        // Partner
        if (profileData.getPartnerAvatar() != null) {
            avtRight.setImageBitmap(profileData.getPartnerAvatar());
        }
        if (!TextUtils.isEmpty(profileData.getPartnerName())) {
            FragmentUtils.safeSetText(this, txtNameRight, profileData.getPartnerName());
        }
        if (profileData.getPartnerAge() > 0) {
            FragmentUtils.safeSetText(this, tvAgeRight, String.valueOf(profileData.getPartnerAge()));
        }
        if (!TextUtils.isEmpty(profileData.getPartnerZodiac())) {
            FragmentUtils.safeSetText(this, tvZodiacRight, profileData.getPartnerZodiac());
        }
    }
    private void safeSetText(@Nullable TextView tv, @Nullable String text) {
        if (tv != null && isAdded()) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                tv.setText(text != null ? text : "");
            } else {
                requireActivity().runOnUiThread(() -> tv.setText(text != null ? text : ""));
            }
        }
    }

}