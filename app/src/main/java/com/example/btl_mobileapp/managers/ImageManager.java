package com.example.btl_mobileapp.managers;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.btl_mobileapp.models.Image;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ImageManager {
    private static final String TAG = "ImageManager";

    private static ImageManager instance;
    private final DatabaseReference rootRef;

    private static final String IMAGE_PATH = "images";

    private ImageManager() {
        String url = "https://couples-app-b83be-default-rtdb.firebaseio.com/";
        rootRef = FirebaseDatabase.getInstance(url).getReference();
    }

    public static synchronized ImageManager getInstance() {
        if (instance == null) {
            instance = new ImageManager();
        }
        return instance;
    }

    public interface ImageCallback {
        void onSuccess();

        void onError(String error);
    }

    public interface ListImageCallback {
        void onSuccess(List<Image> images);

        void onError(String error);
    }

    public interface ImageSingleCallback {
        void onSuccess(Image image);

        void onError(String error);
    }

    public void uploadImage(String imageUrl, String coupleId, ImageCallback callback) {
        DatabaseReference libraryRef = rootRef.child(IMAGE_PATH).child(coupleId);
        String imageId = libraryRef.push().getKey();

        if (imageId == null) {
            callback.onError("Không thể tạo ID cho ảnh");
            return;
        }

        Image image = new Image(imageUrl, coupleId);

        libraryRef.child(imageId)
                .setValue(image)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Tải ảnh thành công");
                    libraryRef.child(imageId).child("imageId")
                            .setValue(imageId)
                            .addOnSuccessListener(unused -> Log.d(TAG, "Cập nhật imageId thành công"))
                            .addOnFailureListener(e -> Log.e(TAG, "Lỗi khi cập nhật imageId", e));
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi upload image", e);
                    callback.onError(e.getMessage());
                });
    }

    public void getImageById(String coupleId, String imageId, ImageSingleCallback callback) {
        DatabaseReference imageRef = rootRef.child(IMAGE_PATH).child(coupleId).child(imageId);

        imageRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Image image = snapshot.getValue(Image.class);
                    callback.onSuccess(image);
                } else {
                    callback.onError("Không tìm thấy ảnh với ID: " + imageId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }


    public void getListImages(String coupleId, ListImageCallback callback) {
        DatabaseReference libraryRef = rootRef.child(IMAGE_PATH).child(coupleId);
        libraryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Image> list = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Image i = child.getValue(Image.class);
                    if (i != null) {
                        list.add(i);
                    }
                }
                callback.onSuccess(list);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    public void deleteImage(String coupleId, String imageId, ImageManager.ImageCallback callback) {
        DatabaseReference libraryRef = rootRef.child(IMAGE_PATH).child(coupleId).child(imageId);
        libraryRef.removeValue()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void deleteALlImages(String coupleId, NotificationManager.NotificationCallback callback) {
        DatabaseReference libraryRef = rootRef.child(IMAGE_PATH).child(coupleId);
        libraryRef.removeValue()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}
