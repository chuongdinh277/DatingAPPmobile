package com.example.btl_mobileapp.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeMain1Fragment extends Fragment {
    private static final ExecutorService IMAGE_EXECUTOR = Executors.newFixedThreadPool(2);

    public HomeMain1Fragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout
        View root = inflater.inflate(R.layout.homemain1, container, false);
        // After inflating, bind user info into included layout
        bindUserProfile(root);
        return root;
    }

    private void bindUserProfile(View root) {
        View userInfo = root.findViewById(R.id.layoutUserInfo);
        if (userInfo == null) return;

        ImageView avtLeft = userInfo.findViewById(R.id.avtLeft);
        ImageView avtRight = userInfo.findViewById(R.id.avtRight);
        TextView txtNameLeft = userInfo.findViewById(R.id.txtNameLeft);
        TextView txtNameRight = userInfo.findViewById(R.id.txtNameRight);

        // Set default names until loaded
        safeSetText(txtNameLeft, "");
        safeSetText(txtNameRight, "");

        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser == null) return;

        String uid = fbUser.getUid();

        // Load current user's avatar from cache if present
        Bitmap cached = AvatarCache.getCachedBitmap(requireContext());
        if (cached != null && avtLeft != null) {
            avtLeft.setImageBitmap(cached);
        }

        // Try reading cached names/partner first
        String cachedCurrentName = NameCache.getCurrentName(requireContext());
        String cachedPartnerId = NameCache.getPartnerId(requireContext());
        String cachedPartnerName = NameCache.getPartnerName(requireContext());

        if (!TextUtils.isEmpty(cachedCurrentName)) {
            safeSetText(txtNameLeft, cachedCurrentName);
        } else {
            // Immediate fallback to Firebase profile if exists to make UI responsive
            String fallbackName = fbUser.getDisplayName();
            if (TextUtils.isEmpty(fallbackName)) fallbackName = fbUser.getEmail();
            if (!TextUtils.isEmpty(fallbackName)) {
                safeSetText(txtNameLeft, fallbackName);
            }
        }

        if (!TextUtils.isEmpty(cachedPartnerName)) {
            safeSetText(txtNameRight, cachedPartnerName);
        }

        // If we already know partnerId and have current name cached, we can skip fetching current user from Firestore
        boolean needFetchCurrentUser = TextUtils.isEmpty(cachedCurrentName) || TextUtils.isEmpty(cachedPartnerId);

        if (needFetchCurrentUser) {
            DatabaseManager.getInstance().getUser(uid, new DatabaseManager.DatabaseCallback<User>() {
                @Override
                public void onSuccess(User user) {
                    if (!isAdded()) return;
                    // Update current user's name and cache it
                    String displayName = null;
                    if (user != null && !TextUtils.isEmpty(user.getName())) displayName = user.getName();
                    if (TextUtils.isEmpty(displayName)) displayName = fbUser.getDisplayName();
                    if (TextUtils.isEmpty(displayName)) displayName = fbUser.getEmail();
                    if (TextUtils.isEmpty(displayName)) displayName = "";
                    safeSetText(txtNameLeft, displayName);
                    NameCache.setCurrentName(requireContext(), displayName);

                    // If no cached avatar, try to load from url
                    if (cached == null && user != null && !TextUtils.isEmpty(user.getProfilePicUrl())) {
                        loadImageAsync(user.getProfilePicUrl(), bmp -> {
                            if (bmp != null && avtLeft != null) avtLeft.setImageBitmap(bmp);
                        });
                    } else if (cached == null && fbUser.getPhotoUrl() != null) {
                        loadImageAsync(fbUser.getPhotoUrl().toString(), bmp -> {
                            if (bmp != null && avtLeft != null) avtLeft.setImageBitmap(bmp);
                        });
                    }

                    // Handle partner info
                    if (user != null && !TextUtils.isEmpty(user.getPartnerId())) {
                        String partnerId = user.getPartnerId();
                        String prevPartnerId = NameCache.getPartnerId(requireContext());
                        if (!TextUtils.equals(partnerId, prevPartnerId)) {
                            // Partner changed; reset cached partner name
                            NameCache.setPartner(requireContext(), partnerId, null);
                            safeSetText(txtNameRight, "");
                        } else {
                            NameCache.setPartnerId(requireContext(), partnerId);
                        }
                        // Fetch partner to get avatar and name (cache name after fetch)
                        fetchAndBindPartner(partnerId, avtRight, txtNameRight);
                    } else {
                        NameCache.clearPartner(requireContext());
                        safeSetText(txtNameRight, "");
                    }
                }

                @Override
                public void onError(String error) {
                    if (!isAdded()) return;
                    // Keep showing whatever we had
                }
            });
        } else {
            // We have partnerId from cache; use it
            String partnerId = cachedPartnerId;

            // For current user avatar fallback (if cache missing), we can try Firebase photo URL
            if (cached == null && fbUser.getPhotoUrl() != null) {
                loadImageAsync(fbUser.getPhotoUrl().toString(), bmp -> {
                    if (bmp != null && avtLeft != null) avtLeft.setImageBitmap(bmp);
                });
            }

            // Fetch partner to acquire avatar URL; update cached partner name if needed
            if (!TextUtils.isEmpty(partnerId)) {
                DatabaseManager.getInstance().getUser(partnerId, new DatabaseManager.DatabaseCallback<User>() {
                    @Override
                    public void onSuccess(User partner) {
                        if (!isAdded()) return;
                        if (partner != null) {
                            // Update partner name in cache if new
                            String pName = partner.getName();
                            if (!TextUtils.isEmpty(pName)) {
                                NameCache.setPartnerName(requireContext(), pName);
                                // Only set text if we didn't already have a cached name
                                if (TextUtils.isEmpty(cachedPartnerName)) {
                                    safeSetText(txtNameRight, pName);
                                }
                            }
                            // Load avatar
                            if (!TextUtils.isEmpty(partner.getProfilePicUrl())) {
                                loadImageAsync(partner.getProfilePicUrl(), bmp -> {
                                    if (bmp != null && avtRight != null) avtRight.setImageBitmap(bmp);
                                });
                            }
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded()) return;
                        // Keep showing cached partner name if any
                    }
                });
            } else {
                // No partner
                safeSetText(txtNameRight, "");
            }
        }
    }

    private void fetchAndBindPartner(String partnerId, ImageView avtRight, TextView txtNameRight) {
        DatabaseManager.getInstance().getUser(partnerId, new DatabaseManager.DatabaseCallback<User>() {
            @Override
            public void onSuccess(User partner) {
                if (!isAdded()) return;
                String partnerName = partner != null ? partner.getName() : null;
                if (TextUtils.isEmpty(partnerName)) partnerName = "";
                safeSetText(txtNameRight, partnerName);
                NameCache.setPartnerName(requireContext(), partnerName);

                if (partner != null && !TextUtils.isEmpty(partner.getProfilePicUrl())) {
                    String url = partner.getProfilePicUrl();
                    loadImageAsync(url, bmp -> {
                        if (bmp != null && avtRight != null) avtRight.setImageBitmap(bmp);
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                // Keep whatever is shown
            }
        });
    }

    private interface BitmapCallback { void onBitmap(Bitmap bmp); }

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

    private void safeSetText(@Nullable TextView tv, @Nullable String text) {
        if (tv == null) return;
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> tv.setText(text != null ? text : ""));
    }
}