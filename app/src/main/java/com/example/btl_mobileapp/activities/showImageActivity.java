package com.example.btl_mobileapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
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
import com.example.btl_mobileapp.R;
import com.example.btl_mobileapp.managers.DatabaseManager;
import com.example.btl_mobileapp.managers.ImageManager;
import com.example.btl_mobileapp.managers.NotificationManager;
import com.example.btl_mobileapp.models.Image;
import com.example.btl_mobileapp.models.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class showImageActivity extends BaseActivity{
    private static final String TAG = "ShowImageActivity";
    private ImageView ivImage;

    private TextView tvTimeImage;

    private FloatingActionButton btCamera;

    private ImageButton btDelete;

    private ImageButton btLib;

    private String imageId;

    private String coupleId;

    private String imageUrl;

    private User user;

    private ActivityResultLauncher<Intent> takePictureLauncher;
    private Uri photoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_image_scene);
        loadInfoUser();
        initViews();
        onCLick();
        initCameraLauncher();
        loadData();
    }

    public void initViews () {
        ivImage = findViewById(R.id.iv_image);
        tvTimeImage = findViewById(R.id.tv_timeImage);
        btCamera = findViewById(R.id.bt_camera);
        btDelete = findViewById(R.id.bt_delete);
        btLib = findViewById(R.id.bt_lib);
    }

    public void loadData() {
        Intent intent = getIntent();
        imageUrl = intent.getStringExtra("imageUrl");
        imageId = intent.getStringExtra("imageId");
        coupleId = intent.getStringExtra("coupleId");


        Glide.with(ivImage.getContext())
                .load(imageUrl)
                .into(ivImage);


        ImageManager.getInstance().getImageById(coupleId, imageId, new ImageManager.ImageSingleCallback() {
            @Override
            public void onSuccess(Image image) {
                tvTimeImage.setText(image.getTimeStamp());
            }

            @Override
            public void onError(String error) {
                tvTimeImage.setText(TAG + " Lỗi: " + error);
            }
        });
    }

    public void loadInfoUser() {
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
    }

    public void onCLick() {
        btCamera.setOnClickListener(v -> openCamera());

        // Sau khi click xóa ảnh thì quay về kho ảnh
        btDelete.setOnClickListener(v -> {
            ImageManager.getInstance().deleteImage(coupleId, imageId, new ImageManager.ImageCallback() {
                @Override
                public void onSuccess() {
                    finish();
                }

                @Override
                public void onError(String error) {
                    tvTimeImage.setText(TAG + " Lỗi: " + error);
                }
            });
        });

        // khi click vaò nút này thì quay về kho ảnh
        btLib.setOnClickListener(v -> {
            Intent intent1 = new Intent(this, ImageActivity.class);
            startActivity(intent1);
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
                                Toast.makeText(showImageActivity.this, "Lưu DB thành công!", Toast.LENGTH_SHORT).show();
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
                                finish();
                            });
                        }
                        @Override
                        public void onError(String error) {
                            runOnUiThread(() ->
                                    Toast.makeText(showImageActivity.this, "Lỗi DB: " + error, Toast.LENGTH_SHORT).show()
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

