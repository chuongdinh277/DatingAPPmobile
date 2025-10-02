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
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

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

    public static void prefetch(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();

        DatabaseManager.getInstance().getUser(uid, new DatabaseManager.DatabaseCallback<User>() {
            @Override
            public void onSuccess(User u) {
                String url = null;
                if (u != null && u.getProfilePicUrl() != null && !u.getProfilePicUrl().isEmpty()) {
                    url = u.getProfilePicUrl();
                } else if (user.getPhotoUrl() != null) {
                    url = user.getPhotoUrl().toString();
                }
                if (url != null) downloadToCache(context, url);
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "getUser error: " + error);
                if (user.getPhotoUrl() != null) {
                    downloadToCache(context, user.getPhotoUrl().toString());
                }
            }
        });
    }

    private static void downloadToCache(Context context, String urlStr) {
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
                    try (InputStream is = conn.getInputStream();
                         FileOutputStream fos = new FileOutputStream(getCachedFile(context))) {
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
}

