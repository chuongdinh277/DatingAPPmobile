package com.example.couple_app.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.couple_app.R;
import com.example.couple_app.managers.DatabaseManager;
import com.example.couple_app.models.User;
import com.example.couple_app.utils.AvatarCache;
import com.example.couple_app.utils.NameCache;
import com.example.couple_app.utils.DateUtils;
import com.example.couple_app.utils.FragmentUtils;
import com.example.couple_app.viewmodels.UserProfileData;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HomeMain1Fragment extends Fragment {
    private static final ExecutorService IMAGE_EXECUTOR = Executors.newFixedThreadPool(2);
    private UserProfileData profileData;

    private Runnable loveCounterRunnable;
    private Timestamp startLoveDate = null;
    private final Handler loveCounterHandler = new Handler(Looper.getMainLooper());


    private TextView txtYears, txtMonths, txtWeeks, txtDays, txtTimer, txtStartDate;


    public HomeMain1Fragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout
        View root = inflater.inflate(R.layout.homemain1, container, false);
        // Get shared profile data instance
        profileData = UserProfileData.getInstance();

        // Initialize TextViews for love counter
        txtYears = root.findViewById(R.id.txtYears);
        txtMonths = root.findViewById(R.id.txtMonths);
        txtWeeks = root.findViewById(R.id.txtWeeks);
        txtDays = root.findViewById(R.id.txtDays);
        txtTimer = root.findViewById(R.id.txtTimer);
        txtStartDate = root.findViewById(R.id.txtStartDate);

        // After inflating, bind user info into included layout
        bindUserProfile(root);

        return root;
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

        // Check if data is already loaded
        if (profileData.isLoaded() && profileData.hasCurrentUserData()) {
            // Use cached data - no need to reload
            displayCachedData(avtLeft, txtNameLeft, tvAgeLeft, tvZodiacLeft,
                            avtRight, txtNameRight, tvAgeRight, tvZodiacRight);
            return;
        }

        // Set default values using FragmentUtils
        FragmentUtils.safeSetText(this, txtNameLeft, "");
        FragmentUtils.safeSetText(this, txtNameRight, "");
        FragmentUtils.safeSetText(this, tvAgeLeft, "");
        FragmentUtils.safeSetText(this, tvZodiacLeft, "");
        FragmentUtils.safeSetText(this, tvAgeRight, "");
        FragmentUtils.safeSetText(this, tvZodiacRight, "");

        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser == null) return;

        String uid = fbUser.getUid();

        // Try to load from cache first
        Bitmap cachedAvatar = AvatarCache.getCachedBitmap(requireContext());
        if (cachedAvatar != null && avtLeft != null) {
            avtLeft.setImageBitmap(cachedAvatar);
            profileData.setCurrentUserAvatar(cachedAvatar);
        }

        Bitmap cachedPartnerAvatar = AvatarCache.getPartnerCachedBitmap(requireContext());
        if (cachedPartnerAvatar != null && avtRight != null) {
            avtRight.setImageBitmap(cachedPartnerAvatar);
            profileData.setPartnerAvatar(cachedPartnerAvatar);
        }

        // Load current user data from database
        DatabaseManager.getInstance().getUser(uid, new DatabaseManager.DatabaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (!isAdded()) return;

                // Display current user's name
                String displayName = null;
                if (user != null && !TextUtils.isEmpty(user.getName())) {
                    displayName = user.getName();
                } else if (!TextUtils.isEmpty(fbUser.getDisplayName())) {
                    displayName = fbUser.getDisplayName();
                } else if (!TextUtils.isEmpty(fbUser.getEmail())) {
                    displayName = fbUser.getEmail();
                }
                if (!TextUtils.isEmpty(displayName)) {
                    FragmentUtils.safeSetText(HomeMain1Fragment.this, txtNameLeft, displayName);
                    profileData.setCurrentUserName(displayName);
                    profileData.setCurrentUserId(uid);
                    NameCache.setCurrentName(requireContext(), displayName);
                }

                // Display current user's age and zodiac using DateUtils
                if (user != null && user.getDateOfBirth() != null) {
                    int age = DateUtils.calculateAge(user.getDateOfBirth());
                    String zodiac = DateUtils.getZodiacSign(user.getDateOfBirth());

                    profileData.setCurrentUserAge(age);
                    profileData.setCurrentUserZodiac(zodiac);
                    profileData.setCurrentUserDateOfBirth(user.getDateOfBirth());

                    if (age > 0) {
                        FragmentUtils.safeSetText(HomeMain1Fragment.this, tvAgeLeft, String.valueOf(age));
                    }
                    if (!TextUtils.isEmpty(zodiac)) {
                        FragmentUtils.safeSetText(HomeMain1Fragment.this, tvZodiacLeft, zodiac);
                    }
                }

                // Load current user's avatar
                if (cachedAvatar == null && user != null && !TextUtils.isEmpty(user.getProfilePicUrl())) {
                    loadImageAsync(user.getProfilePicUrl(), bmp -> {
                        if (bmp != null && avtLeft != null) {
                            avtLeft.setImageBitmap(bmp);
                            profileData.setCurrentUserAvatar(bmp);
                            AvatarCache.saveBitmapToCache(requireContext(), bmp);
                        }
                    });
                } else if (cachedAvatar == null && fbUser.getPhotoUrl() != null) {
                    loadImageAsync(fbUser.getPhotoUrl().toString(), bmp -> {
                        if (bmp != null && avtLeft != null) {
                            avtLeft.setImageBitmap(bmp);
                            profileData.setCurrentUserAvatar(bmp);
                            AvatarCache.saveBitmapToCache(requireContext(), bmp);
                        }
                    });
                }

                // Load partner information
                if (user != null && !TextUtils.isEmpty(user.getPartnerId())) {
                    String partnerId = user.getPartnerId();
                    profileData.setPartnerId(partnerId);
                    NameCache.setPartnerId(requireContext(), partnerId);

                    // Fetch partner data
                    DatabaseManager.getInstance().getUser(partnerId, new DatabaseManager.DatabaseCallback<User>() {
                        @Override
                        public void onSuccess(User partner) {
                            if (!isAdded()) return;

                            // Display partner's name
                            String partnerName = partner != null && !TextUtils.isEmpty(partner.getName())
                                    ? partner.getName() : "";
                            FragmentUtils.safeSetText(HomeMain1Fragment.this, txtNameRight, partnerName);
                            profileData.setPartnerName(partnerName);
                            NameCache.setPartnerName(requireContext(), partnerName);

                            // Display partner's age and zodiac using DateUtils
                            if (partner != null && partner.getDateOfBirth() != null) {
                                int partnerAge = DateUtils.calculateAge(partner.getDateOfBirth());
                                String partnerZodiac = DateUtils.getZodiacSign(partner.getDateOfBirth());

                                profileData.setPartnerAge(partnerAge);
                                profileData.setPartnerZodiac(partnerZodiac);
                                profileData.setPartnerDateOfBirth(partner.getDateOfBirth());

                                if (partnerAge > 0) {
                                    FragmentUtils.safeSetText(HomeMain1Fragment.this, tvAgeRight, String.valueOf(partnerAge));
                                }
                                if (!TextUtils.isEmpty(partnerZodiac)) {
                                    FragmentUtils.safeSetText(HomeMain1Fragment.this, tvZodiacRight, partnerZodiac);
                                }
                            }

                            // Load partner's avatar
                            if (cachedPartnerAvatar == null && partner != null && !TextUtils.isEmpty(partner.getProfilePicUrl())) {
                                loadImageAsync(partner.getProfilePicUrl(), bmp -> {
                                    if (bmp != null && avtRight != null) {
                                        avtRight.setImageBitmap(bmp);
                                        profileData.setPartnerAvatar(bmp);
                                        AvatarCache.savePartnerBitmapToCache(requireContext(), bmp);
                                    }
                                });
                            }

                            // Mark data as loaded
                            profileData.setLoaded(true);
                        }

                        @Override
                        public void onError(String error) {
                            if (!isAdded()) return;
                            profileData.setLoaded(true);
                        }
                    });
                } else {
                    // No partner
                    NameCache.clearPartner(requireContext());
                    FragmentUtils.safeSetText(HomeMain1Fragment.this, txtNameRight, "");
                    FragmentUtils.safeSetText(HomeMain1Fragment.this, tvAgeRight, "");
                    FragmentUtils.safeSetText(HomeMain1Fragment.this, tvZodiacRight, "");
                    profileData.setLoaded(true);
                }

                // Load startLoveDate for love counter
                if (user != null && user.getStartLoveDate() != null) {
                    startLoveDate = user.getStartLoveDate();
                    startLoveCounter();
                }
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

                        // Format and display the start date
                        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
                        String formattedDate = sdf.format(startDate);
                        safeSetText(txtStartDate, formattedDate);

                        // Schedule next update
                        loveCounterHandler.postDelayed(this, 1000);
                    }
                };
                loveCounterHandler.post(loveCounterRunnable);
            }

            private void safeSetText(@Nullable TextView tv, @Nullable String text) {
                if (tv != null && isAdded()) {
                    requireActivity().runOnUiThread(() -> tv.setText(text != null ? text : ""));
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                // Use fallback from Firebase Auth
                String fallbackName = fbUser.getDisplayName();
                if (TextUtils.isEmpty(fallbackName)) fallbackName = fbUser.getEmail();
                if (!TextUtils.isEmpty(fallbackName)) {
                    FragmentUtils.safeSetText(HomeMain1Fragment.this, txtNameLeft, fallbackName);
                    profileData.setCurrentUserName(fallbackName);
                }
                profileData.setLoaded(true);
            }
        });
    }

    private void displayCachedData(ImageView avtLeft, TextView txtNameLeft, TextView tvAgeLeft, TextView tvZodiacLeft,
                                   ImageView avtRight, TextView txtNameRight, TextView tvAgeRight, TextView tvZodiacRight) {
        // Display current user data from cache
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

        // Display partner data from cache
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

    private interface BitmapCallback { void onBitmap(Bitmap bmp); }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Stop the love counter to prevent memory leaks
        if (loveCounterRunnable != null) {
            loveCounterHandler.removeCallbacks(loveCounterRunnable);
        }
    }

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
                conn.setInstanceFollowRedirects(true);
                conn.connect();
                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    try (InputStream is = conn.getInputStream()) {
                        bmp = BitmapFactory.decodeStream(is);
                    }
                }
            } catch (Exception ignored) {
            } finally {
                if (conn != null) conn.disconnect();
            }
            if (!isAdded()) return;
            final Bitmap result = bmp;
            requireActivity().runOnUiThread(() -> {
                if (callback != null) callback.onBitmap(result);
            });
        });
    }
}
