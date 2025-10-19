package com.example.btl_mobileapp.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btl_mobileapp.R;
import com.example.btl_mobileapp.adapters.ImageAdapter;
import com.example.btl_mobileapp.managers.DatabaseManager;
import com.example.btl_mobileapp.managers.ImageManager;
import com.example.btl_mobileapp.managers.NotificationManager;
import com.example.btl_mobileapp.models.Couple;
import com.example.btl_mobileapp.models.Image;
import com.example.btl_mobileapp.models.Notification;
import com.example.btl_mobileapp.models.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class ImageActivity extends BaseActivity {
    private static final String TAG = "ImageActivity";
    private RecyclerView rcImages;
    private ImageAdapter imageAdapter;

    private FloatingActionButton btCamera;

    private List<Image> imageList;

    private ActivityResultLauncher<Intent> takePictureLauncher;
    private Uri photoUri;

    private String coupleId;

    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lib_image_scene);
        loadObject();
        rcImages = findViewById(R.id.rv_lib);
        btCamera = findViewById(R.id.bt_camera);
        setImageList();
        setupRecyclerView();
        btCamera.setOnClickListener(v -> openCamera());
    }

    private void setupRecyclerView() {
        imageAdapter = new ImageAdapter(imageList, this);
        GridLayoutManager layoutManager = new GridLayoutManager(this,4);
        layoutManager.setStackFromEnd(false);
        rcImages.setLayoutManager(layoutManager);
        rcImages.setAdapter(imageAdapter);
    }

    public void loadObject() {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
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
                coupleId = result.getCoupleId();
            }

            @Override
            public void onError(String error) {
                coupleId = null;
            }
        });

        if (coupleId == null) {
            Log.e(TAG, "CoupleId is null");
        }
    }

    private void setImageList() {
        ImageManager.getInstance().getListImages(coupleId, new ImageManager.ListImageCallback() {
            @Override
            public void onSuccess(List<Image> images) {
                imageList = images;
                imageAdapter.setImages(imageList);
                imageAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String errorMessage) {
                Log.e("TAG", "Lỗi khi lấy danh sách thông tin hình ảnh : " + errorMessage);
            }
        });
    }

    private void initCameraLauncher() {
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Toast.makeText(this, "Chụp ảnh thành công!", Toast.LENGTH_SHORT).show();
                        uploadToImgbb(photoUri);
                    } else {
                        Toast.makeText(this, "Không chụp ảnh nào", Toast.LENGTH_SHORT).show();
                    }
                }
        );

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

    private void uploadToImgbb(Uri imageUri) {
        new Thread(() -> {
            try {
                File file = getFileFromUri(imageUri);

                String apiKey = getString(R.string.imgbb_api_key);;

                okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();

                okhttp3.RequestBody requestBody = new okhttp3.MultipartBody.Builder()
                        .setType(okhttp3.MultipartBody.FORM)
                        .addFormDataPart("key", apiKey)
                        .addFormDataPart(
                                "image",
                                file.getName(),
                                okhttp3.RequestBody.create(file, okhttp3.MediaType.parse("image/jpeg"))
                        )
                        .build();

                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url("https://api.imgbb.com/1/upload")
                        .post(requestBody)
                        .build();

                okhttp3.Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    String json = response.body().string();
                    org.json.JSONObject jsonObject = new org.json.JSONObject(json);
                    String imageUrl = jsonObject.getJSONObject("data").getString("url");

                    runOnUiThread(() -> {
                        Toast.makeText(this, "Upload thành công!", Toast.LENGTH_SHORT).show();
                    });

                    ImageManager.getInstance().uploadImage(imageUrl, coupleId, new ImageManager.ImageCallback() {
                        @Override
                        public void onSuccess() {
                            runOnUiThread(() -> {
                                Toast.makeText(ImageActivity.this, "Lưu DB thành công!", Toast.LENGTH_SHORT).show();
                                NotificationManager.getInstance().createNotification(user.getUserId(), user.getPartnerId(), user.getName() + " đã tải ảnh mới", new NotificationManager.NotificationCallback() {
                                    @Override
                                    public void onSuccess() {
                                        Log.d(TAG, "Thêm thông báo thành công");
                                    }

                                    @Override
                                    public void onError(String error) {
                                        Log.e(TAG, "Lỗi thêm thông báo:");
                                    }
                                });
                            });
                        }
                        @Override
                        public void onError(String error) {
                            runOnUiThread(() ->
                                    Toast.makeText(ImageActivity.this, "Lỗi DB: " + error, Toast.LENGTH_SHORT).show()
                            );
                        }
                    });

                } else {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Upload thất bại: " + response.message(), Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Lỗi upload: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private File getFileFromUri(Uri uri) throws IOException {
        File tempFile = new File(getCacheDir(), "temp_image.jpg");
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             OutputStream outputStream = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, bytesRead, bytesRead);
            }
        }
        return tempFile;
    }


}
