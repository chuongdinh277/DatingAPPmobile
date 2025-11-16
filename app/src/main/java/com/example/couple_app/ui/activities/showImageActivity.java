package com.example.couple_app.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.example.couple_app.R;
import com.example.couple_app.data.local.DatabaseManager;
import com.example.couple_app.managers.ImageManager;
import com.example.couple_app.managers.NotificationManager;
import com.example.couple_app.data.local.StorageManager;
import com.example.couple_app.data.model.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.io.IOException;

public class showImageActivity extends BaseActivity {
    private static final String TAG = "ShowImageActivity";

    private ImageView ivImage;
    private TextView tvTimeImage;
    private FloatingActionButton btCamera;
    private ImageButton btDelete;
    private ImageButton btLib;

    private String coupleId;
    private String imageUrl;
    private User user;

    private ActivityResultLauncher<Intent> takePictureLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private Uri photoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_image_scene);

        initCameraLauncher();
        initPermissionLauncher();

        initViews();
        loadInfoUser();
        loadData();
        onClick();
    }

    private void initViews() {
        ivImage = findViewById(R.id.iv_image);
        tvTimeImage = findViewById(R.id.tv_timeImage);
        btCamera = findViewById(R.id.bt_camera);
        btDelete = findViewById(R.id.bt_delete);
        btLib = findViewById(R.id.bt_lib);
    }

    private void loadData() {
        Intent intent = getIntent();
        imageUrl = intent.getStringExtra("imageUrl");
        Long timestamp = intent.getLongExtra("timestamp", 0);
        coupleId = intent.getStringExtra("coupleId");

        // Load ảnh bằng Glide từ URL
        Glide.with(ivImage.getContext())
                .load(imageUrl)
                .placeholder(R.drawable.user_icon)
                .error(R.drawable.user_icon)
                .into(ivImage);

        // Hiển thị thời gian
        if (timestamp > 0) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
                String formattedTime = sdf.format(new java.util.Date(timestamp));
                tvTimeImage.setText(formattedTime);
            } catch (Exception e) {
                tvTimeImage.setText("");
            }
        }
    }

    private void loadInfoUser() {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() != null) {
            String userId = firebaseAuth.getCurrentUser().getUid();
            DatabaseManager.getInstance().getUser(userId, new DatabaseManager.DatabaseCallback<User>() {
                @Override
                public void onSuccess(User result) {
                    user = result;
                }

                @Override
                public void onError(String error) {
                    user = null;
                }
            });
        }
    }

    private void onClick() {
        btCamera.setOnClickListener(v -> checkCameraPermissionAndOpen());

        btDelete.setOnClickListener(v -> {
            if (imageUrl != null && imageUrl.contains("firebase")) {
                StorageManager.getInstance().deleteImage(imageUrl, new StorageManager.DeleteCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Image deleted from storage");
                        Toast.makeText(showImageActivity.this, "Đã xóa ảnh", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error deleting: " + error);
                        Toast.makeText(showImageActivity.this, "Lỗi xóa ảnh: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, "Không thể xóa ảnh này", Toast.LENGTH_SHORT).show();
            }
        });

        btLib.setOnClickListener(v -> {
            finish(); // Just go back instead of starting new activity
        });
    }

    private void initCameraLauncher() {
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Toast.makeText(this, "Chụp ảnh thành công!", Toast.LENGTH_SHORT).show();
                        uploadToFirebaseStorage(photoUri);
                    } else {
                        Toast.makeText(this, "Không chụp ảnh nào", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void initPermissionLauncher() {
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openCamera();
                    } else {
                        Toast.makeText(this, "Cần quyền truy cập camera để chụp ảnh", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void checkCameraPermissionAndOpen() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            File photoFile = createImageFile();
            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                takePictureLauncher.launch(takePictureIntent);
            }
        } catch (IOException e) {
            Toast.makeText(this, "Lỗi khi tạo file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String fileName = "photo_" + System.currentTimeMillis();
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(fileName, ".jpg", storageDir);
    }

    private void uploadToFirebaseStorage(Uri imageUri) {
        if (imageUri == null || coupleId == null || user == null) {
            Toast.makeText(this, "Lỗi: Thiếu thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Đang tải ảnh lên...", Toast.LENGTH_SHORT).show();

        // BƯỚC 1: Upload file lên Firebase Storage
        StorageManager.getInstance().uploadImageFromUri(imageUri, coupleId, new StorageManager.UploadCallback() {
            @Override
            public void onSuccess(String downloadUrl) {
                Log.d(TAG, "Storage upload success: " + downloadUrl);

                // BƯỚC 2: Lưu URL vào Realtime Database
                ImageManager.getInstance().uploadImage(downloadUrl, coupleId, user.getUserId(), new ImageManager.ImageCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            Toast.makeText(showImageActivity.this, "Tải ảnh thành công!", Toast.LENGTH_SHORT).show();

                            // Send notification
                            if (user.getPartnerId() != null) {
                                NotificationManager.getInstance().createNotification(
                                        user.getUserId(),
                                        user.getPartnerId(),
                                        user.getName() + " đã tải ảnh mới",
                                        new NotificationManager.NotificationCallback() {
                                            @Override
                                            public void onSuccess() {
                                                Log.d(TAG, "Notification sent");
                                            }

                                            @Override
                                            public void onError(String error) {
                                                Log.e(TAG, "Notification error: " + error);
                                            }
                                        }
                                );
                            }
                            finish();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() ->
                                Toast.makeText(showImageActivity.this, "Lỗi lưu DB: " + error, Toast.LENGTH_SHORT).show()
                        );
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Toast.makeText(showImageActivity.this, "Lỗi tải ảnh: " + error, Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onProgress(int progress) {
                Log.d(TAG, "Upload progress: " + progress + "%");
            }
        });
    }
}

