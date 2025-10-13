package com.example.btl_mobileapp.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.btl_mobileapp.R;
import com.example.btl_mobileapp.managers.DatabaseManager;
import com.example.btl_mobileapp.models.User;
import com.example.btl_mobileapp.utils.AvatarCache;
import com.example.btl_mobileapp.utils.NameCache;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeMain2Fragment extends Fragment {

    // Executor để tải ảnh bất đồng bộ
    private static final ExecutorService IMAGE_EXECUTOR = Executors.newFixedThreadPool(2);

    // Khai báo các biến View cho phần đếm ngày
    private TextView tvDaysCount, tvSince;

    public HomeMain2Fragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate layout
        View root = inflater.inflate(R.layout.homemain2, container, false);

        // Lấy tham chiếu tới các TextViews của phần đếm ngày
        tvDaysCount = root.findViewById(R.id.tvDaysCount);
        tvSince = root.findViewById(R.id.tvSince);

        // Bắt đầu quá trình tải và hiển thị thông tin
        bindUserProfile(root);

        return root;
    }

    /**
     * Phương thức chính để tải và hiển thị thông tin người dùng, partner, và ngày kỷ niệm.
     * @param root View gốc của Fragment.
     */
    private void bindUserProfile(View root) {
        View userInfo = root.findViewById(R.id.layoutUserInfo);
        if (userInfo == null) return;

        ImageView avtLeft = userInfo.findViewById(R.id.avtLeft);
        ImageView avtRight = userInfo.findViewById(R.id.avtRight);
        TextView txtNameLeft = userInfo.findViewById(R.id.txtNameLeft);
        TextView txtNameRight = userInfo.findViewById(R.id.txtNameRight);

        safeSetText(txtNameLeft, "");
        safeSetText(txtNameRight, "");

        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser == null) return;

        String uid = fbUser.getUid();

        // Tải avatar từ cache nếu có
        Bitmap cached = AvatarCache.getCachedBitmap(requireContext());
        if (cached != null && avtLeft != null) {
            avtLeft.setImageBitmap(cached);
        }

        // Tải tên từ cache nếu có
        String cachedCurrentName = NameCache.getCurrentName(requireContext());
        String cachedPartnerId = NameCache.getPartnerId(requireContext());
        String cachedPartnerName = NameCache.getPartnerName(requireContext());

        if (!TextUtils.isEmpty(cachedCurrentName)) {
            safeSetText(txtNameLeft, cachedCurrentName);
        } else {
            String fallbackName = fbUser.getDisplayName();
            if (TextUtils.isEmpty(fallbackName)) fallbackName = fbUser.getEmail();
            if (!TextUtils.isEmpty(fallbackName)) {
                safeSetText(txtNameLeft, fallbackName);
            }
        }

        if (!TextUtils.isEmpty(cachedPartnerName)) {
            safeSetText(txtNameRight, cachedPartnerName);
        }

        // Luôn tải dữ liệu từ Firestore để đảm bảo thông tin mới nhất
        DatabaseManager.getInstance().getUser(uid, new DatabaseManager.DatabaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (!isAdded() || user == null) return;

                // 1. CẬP NHẬT SỐ NGÀY
//                if (user.getAnniversaryDate() != null) {
//                    updateDayCounter(user.getAnniversaryDate());
//                }

                // 2. CẬP NHẬT TÊN VÀ AVATAR NGƯỜI DÙNG HIỆN TẠI
                String displayName = user.getName();
                if (TextUtils.isEmpty(displayName)) displayName = fbUser.getDisplayName();
                if (TextUtils.isEmpty(displayName)) displayName = fbUser.getEmail();
                safeSetText(txtNameLeft, displayName != null ? displayName : "");
                NameCache.setCurrentName(requireContext(), displayName);

                if (cached == null && !TextUtils.isEmpty(user.getProfilePicUrl())) {
                    loadImageAsync(user.getProfilePicUrl(), bmp -> {
                        if (bmp != null && avtLeft != null) avtLeft.setImageBitmap(bmp);
                    });
                } else if (cached == null && fbUser.getPhotoUrl() != null) {
                    loadImageAsync(fbUser.getPhotoUrl().toString(), bmp -> {
                        if (bmp != null && avtLeft != null) avtLeft.setImageBitmap(bmp);
                    });
                }

                // 3. CẬP NHẬT THÔNG TIN PARTNER
                if (!TextUtils.isEmpty(user.getPartnerId())) {
                    fetchAndBindPartner(user.getPartnerId(), avtRight, txtNameRight);
                    NameCache.setPartnerId(requireContext(), user.getPartnerId());
                } else {
                    NameCache.clearPartner(requireContext());
                    safeSetText(txtNameRight, "");
                   // if(avtRight != null) avtRight.setImageResource(R.drawable.ic_default_avatar); // Reset avatar
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                Log.e("HomeMain2Fragment", "Lỗi tải thông tin user: " + error);
            }
        });
    }

    /**
     * Tính toán và cập nhật số ngày lên giao diện.
     * @param anniversaryDateStr Ngày kỷ niệm dạng chuỗi "yyyy-MM-dd".
     */
    private void updateDayCounter(String anniversaryDateStr) {
        if (anniversaryDateStr == null || anniversaryDateStr.isEmpty() || !isAdded()) {
            return;
        }

        try {
            DateTimeFormatter dbFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate startDate = LocalDate.parse(anniversaryDateStr, dbFormatter);
            LocalDate today = LocalDate.now();

            long daysBetween = ChronoUnit.DAYS.between(startDate, today) + 1;

            DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String displayDate = startDate.format(displayFormatter);

            requireActivity().runOnUiThread(() -> {
                tvDaysCount.setText(String.format(Locale.getDefault(), "%d ngày", daysBetween));
                tvSince.setText(String.format("Từ %s", displayDate));
            });

        } catch (DateTimeParseException e) {
            Log.e("HomeMain2Fragment", "Lỗi parse ngày: " + anniversaryDateStr, e);
        }
    }

    /**
     * Tải thông tin của partner và hiển thị.
     */
    private void fetchAndBindPartner(String partnerId, ImageView avtRight, TextView txtNameRight) {
        DatabaseManager.getInstance().getUser(partnerId, new DatabaseManager.DatabaseCallback<User>() {
            @Override
            public void onSuccess(User partner) {
                if (!isAdded() || partner == null) return;

                String partnerName = partner.getName();
                safeSetText(txtNameRight, partnerName != null ? partnerName : "");
                NameCache.setPartnerName(requireContext(), partnerName);

                if (!TextUtils.isEmpty(partner.getProfilePicUrl())) {
                    loadImageAsync(partner.getProfilePicUrl(), bmp -> {
                        if (bmp != null && avtRight != null) avtRight.setImageBitmap(bmp);
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                Log.e("HomeMain2Fragment", "Lỗi tải thông tin partner: " + error);
            }
        });
    }

    private interface BitmapCallback { void onBitmap(Bitmap bmp); }

    /**
     * Tải ảnh từ URL một cách bất đồng bộ.
     */
    private void loadImageAsync(String urlStr, BitmapCallback callback) {
        if (TextUtils.isEmpty(urlStr)) { if (callback != null) callback.onBitmap(null); return; }
        IMAGE_EXECUTOR.execute(() -> {
            Bitmap bmp = null;
            HttpURLConnection conn = null;
            try {
                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(20000);
                conn.connect();
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    try (InputStream is = conn.getInputStream()) {
                        bmp = BitmapFactory.decodeStream(is);
                    }
                }
            } catch (Exception e) {
                Log.e("HomeMain2Fragment", "Lỗi tải ảnh: " + urlStr, e);
            } finally {
                if (conn != null) conn.disconnect();
            }
            if (!isAdded()) return;
            final Bitmap result = bmp;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (callback != null) callback.onBitmap(result);
                });
            }
        });
    }

    /**
     * Set text cho TextView một cách an toàn trên Main Thread.
     */
    private void safeSetText(@Nullable TextView tv, @Nullable String text) {
        if (tv == null || !isAdded() || getActivity() == null) return;
        getActivity().runOnUiThread(() -> tv.setText(text != null ? text : ""));
    }
}