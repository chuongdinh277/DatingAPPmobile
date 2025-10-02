package com.example.couple_app.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;

import com.example.couple_app.R;
import com.example.couple_app.managers.AuthManager;
import com.example.couple_app.managers.DatabaseManager;
import com.example.couple_app.models.User;
import com.example.couple_app.utils.AvatarCache;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingProfileActivity extends BaseActivity {
    private static final String TAG = "SettingProfileActivity";

    private ImageView ivUserImage;
    private MaterialButton btUpload;
    private ActivityResultLauncher<String> pickImageLauncher;
    private Uri selectedImageUri;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ExecutorService ioExecutor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_profile);

        setActiveButton("settings");

        ioExecutor = Executors.newSingleThreadExecutor();

        ivUserImage = findViewById(R.id.iv_user_image);
        btUpload = findViewById(R.id.bt_upload);
        ImageButton btnEdit = findViewById(R.id.btn_edit);

        // Initially hidden, click Edit to show upload button
        btUpload.setVisibility(View.INVISIBLE);
        btnEdit.setOnClickListener(v -> btUpload.setVisibility(
                btUpload.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE));

        // Prepare image picker
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                selectedImageUri = uri;
                // Preview immediately
                ivUserImage.setImageURI(uri);
                // Start upload
                uploadSelectedImage();
            }
        });

        btUpload.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        // Populate current user info and avatar
        populateUserInfo();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ioExecutor != null) {
            ioExecutor.shutdownNow();
        }
    }

    private void populateUserInfo() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(this, "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        // First try cache for instant display
        try {
            Bitmap cached = AvatarCache.getCachedBitmap(this);
            if (cached != null) {
                ivUserImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                ivUserImage.setImageBitmap(cached);
            }
        } catch (Exception ignore) { }

        // Text fields
        try {
            TextView tvFirst = findViewById(R.id.tv_firstname_value);
            TextView tvLast = findViewById(R.id.tv_lastname_value);
            TextView tvUsername = findViewById(R.id.tv_username_value);
            TextView tvGmail = findViewById(R.id.tv_gmail_value);
            TextView tvPhone = findViewById(R.id.tv_phone_value);

            String displayName = firebaseUser.getDisplayName();
            String email = firebaseUser.getEmail();
            String phone = firebaseUser.getPhoneNumber();

            if (displayName != null && !displayName.isEmpty()) {
                String[] parts = displayName.trim().split("\\s+");
                if (parts.length > 1) {
                    tvLast.setText(parts[parts.length - 1]);
                    StringBuilder firstBuilder = new StringBuilder();
                    for (int i = 0; i < parts.length - 1; i++) {
                        if (i > 0) firstBuilder.append(' ');
                        firstBuilder.append(parts[i]);
                    }
                    tvFirst.setText(firstBuilder.toString());
                } else {
                    tvFirst.setText(displayName);
                    tvLast.setText("-");
                }
                tvUsername.setText(displayName);
            } else {
                tvFirst.setText("-");
                tvLast.setText("-");
                tvUsername.setText("-");
            }

            tvGmail.setText(email != null ? email : "-");
            tvPhone.setText(phone != null ? phone : "-");
        } catch (Exception e) {
            Log.w(TAG, "Failed to populate text fields", e);
        }

        // If we already showed cache, we can skip network fetch unless cache is missing
        if (AvatarCache.hasCache(this)) {
            return;
        }

        // Load avatar from Firestore user if available
        DatabaseManager.getInstance().getUser(firebaseUser.getUid(), new DatabaseManager.DatabaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                String url = user != null ? user.getProfilePicUrl() : null;
                if (url != null && !url.isEmpty()) {
                    loadImageFromUrl(url);
                } else if (firebaseUser.getPhotoUrl() != null) {
                    loadImageFromUrl(firebaseUser.getPhotoUrl().toString());
                } else {
                    ivUserImage.setImageResource(R.drawable.user_icon);
                }
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "getUser error: " + error);
                if (firebaseUser.getPhotoUrl() != null) {
                    loadImageFromUrl(firebaseUser.getPhotoUrl().toString());
                } else {
                    ivUserImage.setImageResource(R.drawable.user_icon);
                }
            }
        });
    }

    private void loadImageFromUrl(String urlStr) {
        if (ioExecutor == null) ioExecutor = Executors.newSingleThreadExecutor();
        ioExecutor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(20000);
                conn.setInstanceFollowRedirects(true);
                conn.connect();
                int code = conn.getResponseCode();
                InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
                Bitmap bmp;
                try (InputStream autoClose = is) {
                    bmp = BitmapFactory.decodeStream(autoClose);
                }
                if (bmp != null) {
                    // Save to cache for next time
                    AvatarCache.saveBitmapToCache(getApplicationContext(), bmp);
                    Bitmap finalBmp = bmp;
                    mainHandler.post(() -> {
                        ivUserImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        ivUserImage.setImageBitmap(finalBmp);
                    });
                } else {
                    throw new Exception("Bitmap decode null");
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to load avatar: " + e.getMessage());
                mainHandler.post(() -> ivUserImage.setImageResource(R.drawable.user_icon));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    private void uploadSelectedImage() {
        if (selectedImageUri == null) return;

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(this, "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        String apiKey = getString(R.string.imgbb_api_key);
        if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("REPLACE_")) {
            new AlertDialog.Builder(this)
                    .setTitle("Thiếu API key imgbb")
                    .setMessage("Hãy thêm khoá imgbb_api_key trong strings.xml để tải ảnh lên.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        AlertDialog progress = new AlertDialog.Builder(this)
                .setView(getLayoutInflater().inflate(R.layout.view_progress, null))
                .setCancelable(false)
                .create();
        try { progress.show(); } catch (Exception ignored) {}

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Read and downscale to avoid huge payload
                Bitmap bitmap = decodeSampledBitmapFromUri(selectedImageUri, 800, 800);
                if (bitmap == null) throw new Exception("Không đọc được ảnh");
                final Bitmap uploadBmp = bitmap;

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                uploadBmp.compress(Bitmap.CompressFormat.JPEG, 85, baos);
                byte[] bytes = baos.toByteArray();
                String base64 = Base64.encodeToString(bytes, Base64.NO_WRAP);

                String uploadUrl = "https://api.imgbb.com/1/upload?key=" + URLEncoder.encode(apiKey, "UTF-8");
                URL url = new URL(uploadUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(30000);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String body = "image=" + URLEncoder.encode(base64, "UTF-8");
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.getBytes());
                }

                int code = conn.getResponseCode();
                InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
                String resp = readAll(is);
                conn.disconnect();

                if (code < 200 || code >= 300) {
                    throw new Exception("Upload thất bại: " + code + " - " + resp);
                }

                JSONObject json = new JSONObject(resp);
                JSONObject data = json.getJSONObject("data");
                String urlReturned = data.getString("url");

                // Save to Firestore and FirebaseAuth profile
                DatabaseManager.getInstance().updateUserProfile(firebaseUser.getUid(), null, urlReturned,
                        new AuthManager.AuthActionCallback() {
                            @Override
                            public void onSuccess() {
                                AuthManager.getInstance().updateProfile(null, urlReturned, new AuthManager.AuthActionCallback() {
                                    @Override
                                    public void onSuccess() {
                                        // Save uploaded bitmap to cache immediately
                                        AvatarCache.saveBitmapToCache(getApplicationContext(), uploadBmp);
                                        mainHandler.post(() -> {
                                            try { progress.dismiss(); } catch (Exception ignored) {}
                                            Toast.makeText(SettingProfileActivity.this, "Cập nhật ảnh đại diện thành công", Toast.LENGTH_SHORT).show();
                                            ivUserImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                            ivUserImage.setImageBitmap(uploadBmp);
                                        });
                                    }

                                    @Override
                                    public void onError(String error) {
                                        // Still update cache and UI because Firestore already saved URL
                                        AvatarCache.saveBitmapToCache(getApplicationContext(), uploadBmp);
                                        mainHandler.post(() -> {
                                            try { progress.dismiss(); } catch (Exception ignored) {}
                                            Toast.makeText(SettingProfileActivity.this, "Đã lưu URL ảnh, nhưng cập nhật hồ sơ Auth thất bại", Toast.LENGTH_LONG).show();
                                            ivUserImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                            ivUserImage.setImageBitmap(uploadBmp);
                                        });
                                    }
                                });
                            }

                            @Override
                            public void onError(String error) {
                                mainHandler.post(() -> {
                                    try { progress.dismiss(); } catch (Exception ignored) {}
                                    Toast.makeText(SettingProfileActivity.this, "Lỗi lưu Firestore: " + error, Toast.LENGTH_LONG).show();
                                });
                            }
                        });

            } catch (Exception e) {
                Log.e(TAG, "Upload error", e);
                mainHandler.post(() -> {
                    try { progress.dismiss(); } catch (Exception ignored) {}
                    new AlertDialog.Builder(SettingProfileActivity.this)
                            .setTitle("Tải ảnh thất bại")
                            .setMessage(e.getMessage() != null ? e.getMessage() : "Lỗi không xác định")
                            .setPositiveButton("Đóng", null)
                            .show();
                });
            }
        });
    }

    private Bitmap decodeSampledBitmapFromUri(Uri uri, int reqWidth, int reqHeight) {
        try {
            // First decode with inJustDecodeBounds=true to check dimensions
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            try (InputStream is1 = getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(is1, null, options);
            }
            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            try (InputStream is2 = getContentResolver().openInputStream(uri)) {
                return BitmapFactory.decodeStream(is2, null, options);
            }
        } catch (Exception e) {
            Log.e(TAG, "decodeSampledBitmapFromUri error", e);
            return null;
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private String readAll(InputStream is) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) != -1) {
            baos.write(buf, 0, n);
        }
        return baos.toString();
    }
}
