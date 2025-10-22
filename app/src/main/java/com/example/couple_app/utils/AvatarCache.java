package com.example.couple_app.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import com.example.couple_app.managers.DatabaseManager;
import com.example.couple_app.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AvatarCache {
    private static final String TAG = "AvatarCache";
    private static final String AVATAR_FILE = "avatar_cache.jpg";
    private static final String PARTNER_AVATAR_FILE = "partner_avatar_cache.jpg";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    // Cache for current user
    public static File getCachedFile(Context context) {
        return new File(context.getCacheDir(), AVATAR_FILE);
    }

    public static boolean hasCache(Context context) {
        File f = getCachedFile(context);
        return f.exists() && f.length() > 0;
    }

    public static Bitmap getCachedBitmap(Context context) {
        try {
            File f = getCachedFile(context);
            if (f.exists()) {
                return BitmapFactory.decodeFile(f.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.w(TAG, "getCachedBitmap error", e);
        }
        return null;
    }

    public static void saveBitmapToCache(Context context, Bitmap bmp) {
        EXECUTOR.execute(() -> {
            File f = getCachedFile(context);
            try (FileOutputStream fos = new FileOutputStream(f)) {
                bmp.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            } catch (Exception e) {
                Log.w(TAG, "saveBitmapToCache error", e);
            }
        });
    }

    // Cache for partner
    public static File getPartnerCachedFile(Context context) {
        return new File(context.getCacheDir(), PARTNER_AVATAR_FILE);
    }

    public static boolean hasPartnerCache(Context context) {
        File f = getPartnerCachedFile(context);
        return f.exists() && f.length() > 0;
    }

    public static Bitmap getPartnerCachedBitmap(Context context) {
        try {
            File f = getPartnerCachedFile(context);
            if (f.exists()) {
                return BitmapFactory.decodeFile(f.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.w(TAG, "getPartnerCachedBitmap error", e);
        }
        return null;
    }

    public static void savePartnerBitmapToCache(Context context, Bitmap bmp) {
        EXECUTOR.execute(() -> {
            File f = getPartnerCachedFile(context);
            try (FileOutputStream fos = new FileOutputStream(f)) {
                bmp.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            } catch (Exception e) {
                Log.w(TAG, "savePartnerBitmapToCache error", e);
            }
        });
    }

    public static void prefetch(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();

        DatabaseManager.getInstance().getUser(uid, new DatabaseManager.DatabaseCallback<User>() {
            @Override
            public void onSuccess(User u) {
                // Cache current user avatar
                String url = null;
                if (u != null && u.getProfilePicUrl() != null && !u.getProfilePicUrl().isEmpty()) {
                    url = u.getProfilePicUrl();
                } else if (user.getPhotoUrl() != null) {
                    url = user.getPhotoUrl().toString();
                }
                if (url != null) downloadToCache(context, url, false);

                // Cache partner avatar
                if (u != null && u.getPartnerId() != null && !u.getPartnerId().isEmpty()) {
                    prefetchPartner(context, u.getPartnerId());
                }
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "getUser error: " + error);
                if (user.getPhotoUrl() != null) {
                    downloadToCache(context, user.getPhotoUrl().toString(), false);
                }
            }
        });
    }

    public static void prefetchPartner(Context context, String partnerId) {
        DatabaseManager.getInstance().getUser(partnerId, new DatabaseManager.DatabaseCallback<User>() {
            @Override
            public void onSuccess(User partner) {
                if (partner != null && partner.getProfilePicUrl() != null && !partner.getProfilePicUrl().isEmpty()) {
                    downloadToCache(context, partner.getProfilePicUrl(), true);
                }
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "getPartner error: " + error);
            }
        });
    }

    private static void downloadToCache(Context context, String urlStr, boolean isPartner) {
        EXECUTOR.execute(() -> {
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
                    File cacheFile = isPartner ? getPartnerCachedFile(context) : getCachedFile(context);
                    try (InputStream is = conn.getInputStream();
                         FileOutputStream fos = new FileOutputStream(cacheFile)) {
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = is.read(buf)) != -1) {
                            fos.write(buf, 0, n);
                        }
                    }
                } else {
                    Log.w(TAG, "HTTP " + code + " while caching avatar");
                }
            } catch (Exception e) {
                Log.w(TAG, "downloadToCache error", e);
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    public static void clearCache(Context context) {
        getCachedFile(context).delete();
        getPartnerCachedFile(context).delete();
    }
}
