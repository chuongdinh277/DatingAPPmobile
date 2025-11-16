package com.example.couple_app.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.couple_app.R;
import com.example.couple_app.adapters.ImageAdapter;
import com.example.couple_app.managers.DatabaseManager;
import com.example.couple_app.managers.ImageManager;
import com.example.couple_app.managers.NotificationManager;
import com.example.couple_app.managers.StorageManager;
import com.example.couple_app.models.Couple;
import com.example.couple_app.models.Image;
import com.example.couple_app.models.User;
import com.example.couple_app.viewmodels.ImageViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImageActivity extends BaseActivity {
    private static final String TAG = "ImageActivity";
    private RecyclerView rcImages;
    private ImageAdapter imageAdapter;
    private TextView tvPhotoCount;
    private ImageViewModel imageViewModel;

    private FloatingActionButton btCamera;


    private ActivityResultLauncher<Intent> takePictureLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private Uri photoUri;

    private String coupleId;

    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lib_image_scene);

        initCameraLauncher();
        initPermissionLauncher();

        rcImages = findViewById(R.id.rv_lib);
        btCamera = findViewById(R.id.bt_camera);
        tvPhotoCount = findViewById(R.id.tv_photo_count);

        // Initialize ViewModel
        imageViewModel = new ViewModelProvider(this).get(ImageViewModel.class);

        setupRecyclerView();
        setupObservers();
        loadObject();

        btCamera.setOnClickListener(v -> checkCameraPermissionAndOpen());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (coupleId != null) {
            // Refresh images when returning to activity
            imageViewModel.refreshImages();
        }
    }

    private void setupRecyclerView() {
        imageAdapter = new ImageAdapter(new ArrayList<>(), this);

        imageAdapter.setOnImageClickListener(image -> {
            Intent intent = new Intent(this, showImageActivity.class);
            intent.putExtra("imageUrl", image.getImageUrl());
            intent.putExtra("uploadedBy", image.getUploadedBy());
            intent.putExtra("timestamp", image.getTimestamp());
            intent.putExtra("coupleId", coupleId);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        GridLayoutManager layoutManager = new GridLayoutManager(this, 4);
        rcImages.setLayoutManager(layoutManager);
        rcImages.setAdapter(imageAdapter);
    }

    /**
     * Setup LiveData observers to react to ViewModel changes
     */
    private void setupObservers() {
        // Observe image list changes
        imageViewModel.getImages().observe(this, images -> {
            Log.d(TAG, "Images LiveData updated: " + images.size() + " images");
            imageAdapter.setImages(images);
        });

        // Observe image count changes
        imageViewModel.getImageCount().observe(this, count -> {
            Log.d(TAG, "Image count updated: " + count);
            updatePhotoCount(count);
        });

        // Observe loading state
        imageViewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                Log.d(TAG, "Loading images...");
            } else {
                Log.d(TAG, "Loading complete");
            }
        });

        // Observe errors
        imageViewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Log.e(TAG, "Error from ViewModel: " + error);
                Toast.makeText(this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void loadObject() {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_LONG).show();
            return;
        }

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

        DatabaseManager.getInstance().getCoupleByUserId(userId, new DatabaseManager.DatabaseCallback<Couple>() {
            @Override
            public void onSuccess(Couple result) {
                if (result != null && result.getCoupleId() != null) {
                    coupleId = result.getCoupleId();
                    // Use ViewModel to load images
                    imageViewModel.loadImages(coupleId);
                }
            }

            @Override
            public void onError(String error) {
                coupleId = null;
                Toast.makeText(ImageActivity.this, "Không thể tải thông tin cặp đôi", Toast.LENGTH_SHORT).show();
            }
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
                photoUri = FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".provider",
                        photoFile
                );
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

        Toast.makeText(this, "Đang tải ảnh lên...", Toast.LENGTH_SHORT).show();

        // BƯỚC 1: Upload file lên Firebase Storage
        StorageManager.getInstance().uploadImageFromUri(imageUri, coupleId, new StorageManager.UploadCallback() {
            @Override
            public void onSuccess(String downloadUrl) {

                // BƯỚC 2: Lưu URL vào Realtime Database
                ImageManager.getInstance().uploadImage(downloadUrl, coupleId, user.getUserId(), new ImageManager.ImageCallback() {
                    @Override
                    public void onSuccess() {

                        runOnUiThread(() -> {
                            Toast.makeText(ImageActivity.this, "Tải ảnh thành công!", Toast.LENGTH_SHORT).show();

                            // Refresh images via ViewModel
                            imageViewModel.refreshImages();

                            // Send notification to partner
                            if (user.getPartnerId() != null) {
                                NotificationManager.getInstance().createNotification(
                                    user.getUserId(),
                                    user.getPartnerId(),
                                    user.getName() + " đã tải ảnh mới",
                                    new NotificationManager.NotificationCallback() {
                                        @Override
                                        public void onSuccess() {
                                            Log.d(TAG, "Notification sent successfully");
                                        }

                                        @Override
                                        public void onError(String error) {
                                            Log.e(TAG, "Failed to send notification: " + error);
                                        }
                                    }
                                );
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "❌ BƯỚC 2 THẤT BẠI: " + error);
                        runOnUiThread(() ->
                            Toast.makeText(ImageActivity.this, "Lỗi lưu DB: " + error, Toast.LENGTH_SHORT).show()
                        );
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ BƯỚC 1 THẤT BẠI: " + error);
                runOnUiThread(() ->
                    Toast.makeText(ImageActivity.this, "Lỗi tải ảnh: " + error, Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onProgress(int progress) {
                Log.d(TAG, "Upload progress: " + progress + "%");
            }
        });
    }

    private void updatePhotoCount(int count) {
        if (tvPhotoCount != null) {
            runOnUiThread(() -> {
                String countText = count + " ảnh";
                tvPhotoCount.setText(countText);
            });
        }
    }


}
