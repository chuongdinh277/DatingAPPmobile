package com.example.couple_app.managers;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.couple_app.data.model.Image;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Manager quản lý ảnh trong Firebase Realtime Database
 *
 * Cấu trúc Database:
 * couples/
 *   {coupleId}/
 *     images/
 *       {pushKey1}/
 *         imageUrl: "https://..."
 *         uploadedBy: "userId"
 *         timestamp: 1234567890
 *       {pushKey2}/
 *         ...
 */
public class ImageManager {
    private static final String TAG = "ImageManager";
    private static ImageManager instance;
    private final DatabaseReference rootRef;

    private ImageManager() {
        String url = "https://couples-app-b83be-default-rtdb.asia-southeast1.firebasedatabase.app";
        rootRef = FirebaseDatabase.getInstance(url).getReference();
        Log.d(TAG, "ImageManager initialized with database URL: " + url);
    }

    public static synchronized ImageManager getInstance() {
        if (instance == null) {
            instance = new ImageManager();
        }
        return instance;
    }

    // ========== CALLBACKS ==========

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

    // ========== UPLOAD IMAGE ==========

    /**
     * Luồng 1: Upload ảnh
     * 1. Storage đã upload xong → có URL
     * 2. Lưu URL vào Realtime Database: couples/{coupleId}/images/{pushKey}
     */
    public void uploadImage(String imageUrl, String coupleId, String userId, ImageCallback callback) {
        Log.d(TAG, "═══════════════════════════════════");
        Log.d(TAG, "uploadImage called");
        Log.d(TAG, "URL: " + imageUrl);
        Log.d(TAG, "CoupleId: " + coupleId);
        Log.d(TAG, "UserId: " + userId);
        Log.d(TAG, "Database URL: " + rootRef.getDatabase().getApp().getOptions().getDatabaseUrl());
        Log.d(TAG, "═══════════════════════════════════");

        // Tạo reference đến couples/{coupleId}/images/
        DatabaseReference imagesRef = rootRef.child("couples").child(coupleId).child("images");
        Log.d(TAG, "Reference path: " + imagesRef.toString());

        // Tạo key tự động
        String pushKey = imagesRef.push().getKey();

        if (pushKey == null) {
            Log.e(TAG, "❌ Failed to generate push key");
            callback.onError("Không thể tạo ID cho ảnh");
            return;
        }

        Log.d(TAG, "✓ Generated pushKey: " + pushKey);
        Log.d(TAG, "Full path: couples/" + coupleId + "/images/" + pushKey);

        // Tạo object Image
        Image image = new Image(imageUrl, userId);
        Log.d(TAG, "Created Image object:");
        Log.d(TAG, "  - imageUrl: " + image.getImageUrl());
        Log.d(TAG, "  - uploadedBy: " + image.getUploadedBy());
        Log.d(TAG, "  - timestamp: " + image.getTimestamp());

        // Lưu vào database
        Log.d(TAG, "→ Calling setValue()...");
        imagesRef.child(pushKey).setValue(image)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "═══════════════════════════════════");
                    Log.d(TAG, "✓✓✓ setValue() SUCCESS! ✓✓✓");
                    Log.d(TAG, "Image saved to database successfully!");
                    Log.d(TAG, "Path: couples/" + coupleId + "/images/" + pushKey);
                    Log.d(TAG, "═══════════════════════════════════");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.d(TAG, "═══════════════════════════════════");
                    Log.e(TAG, "❌❌❌ setValue() FAILED! ❌❌❌");
                    Log.e(TAG, "Error message: " + e.getMessage());
                    Log.e(TAG, "Error class: " + e.getClass().getName());
                    Log.e(TAG, "Stack trace:", e);
                    Log.d(TAG, "═══════════════════════════════════");
                    callback.onError(e.getMessage());
                });

        Log.d(TAG, "setValue() call completed, waiting for callback...");
    }

    // ========== LOAD IMAGES ==========

    /**
     * Luồng 2: Load danh sách ảnh
     * 1. Đọc từ couples/{coupleId}/images/
     * 2. Trả về list các Image object (chứa URL)
     * 3. Client dùng Glide để load ảnh từ URL
     */
    public void getListImages(String coupleId, ListImageCallback callback) {
        Log.d(TAG, "═══════════════════════════════════");
        Log.d(TAG, "getListImages called");
        Log.d(TAG, "CoupleId: " + coupleId);
        Log.d(TAG, "Path: couples/" + coupleId + "/images/");
        Log.d(TAG, "Database instance: " + rootRef.getDatabase());
        Log.d(TAG, "═══════════════════════════════════");

        DatabaseReference imagesRef = rootRef.child("couples").child(coupleId).child("images");

        // Enable offline persistence and keep synced
        try {
            imagesRef.keepSynced(true);
            Log.d(TAG, "✓ keepSynced enabled");
        } catch (Exception e) {
            Log.w(TAG, "keepSynced failed (might be already enabled): " + e.getMessage());
        }

        Log.d(TAG, "Reference toString: " + imagesRef.toString());
        Log.d(TAG, "Reference key: " + imagesRef.getKey());
        Log.d(TAG, "→ Adding listener now...");

        // Thêm listener với logging chi tiết
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "═══════════════════════════════════");
                Log.d(TAG, "🔥🔥🔥 onDataChange CALLBACK! 🔥🔥🔥");
                Log.d(TAG, "═══════════════════════════════════");
                Log.d(TAG, "Snapshot exists: " + snapshot.exists());
                Log.d(TAG, "Snapshot key: " + snapshot.getKey());
                Log.d(TAG, "Children count: " + snapshot.getChildrenCount());
                Log.d(TAG, "Snapshot value: " + snapshot.getValue());

                List<Image> imageList = new ArrayList<>();

                if (snapshot.exists() && snapshot.hasChildren()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Log.d(TAG, "Processing child key: " + child.getKey());
                        try {
                            Image image = child.getValue(Image.class);
                            if (image != null) {
                                Log.d(TAG, "  ✓ imageUrl: " + image.getImageUrl());
                                Log.d(TAG, "  ✓ uploadedBy: " + image.getUploadedBy());
                                Log.d(TAG, "  ✓ timestamp: " + image.getTimestamp());

                                if (image.getImageUrl() != null && !image.getImageUrl().isEmpty()) {
                                    imageList.add(image);
                                } else {
                                    Log.w(TAG, "  ⚠ Image has null/empty URL at key: " + child.getKey());
                                }
                            } else {
                                Log.w(TAG, "  ⚠ Image is null at key: " + child.getKey());
                                Log.w(TAG, "  Raw value: " + child.getValue());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "  ❌ Error parsing image at key: " + child.getKey(), e);
                            Log.e(TAG, "  Raw value: " + child.getValue());
                        }
                    }
                } else {
                    Log.w(TAG, "⚠ Snapshot exists: " + snapshot.exists());
                    Log.w(TAG, "⚠ Snapshot hasChildren: " + snapshot.hasChildren());
                    Log.w(TAG, "⚠ No images found in database at path: couples/" + coupleId + "/images/");
                }

                Log.d(TAG, "═══════════════════════════════════");
                Log.d(TAG, "Total images loaded: " + imageList.size());
                Log.d(TAG, "Calling callback.onSuccess()...");
                Log.d(TAG, "═══════════════════════════════════");

                callback.onSuccess(imageList);

                Log.d(TAG, "✓ callback.onSuccess() completed");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(TAG, "═══════════════════════════════════");
                Log.e(TAG, "🔥🔥🔥 onCancelled CALLBACK! 🔥🔥🔥");
                Log.d(TAG, "═══════════════════════════════════");
                Log.e(TAG, "Error code: " + error.getCode());
                Log.e(TAG, "Error message: " + error.getMessage());
                Log.e(TAG, "Error details: " + error.getDetails());
                Log.d(TAG, "═══════════════════════════════════");

                callback.onError(error.getMessage());
            }
        };

        // Dùng addListenerForSingleValueEvent
        imagesRef.addListenerForSingleValueEvent(listener);

        Log.d(TAG, "✓ Listener object created and added");
        Log.d(TAG, "Listener class: " + listener.getClass().getName());
        Log.d(TAG, "Waiting for Firebase response...");
        Log.d(TAG, "If no callback in 5 seconds → Network or Firebase issue");
    }

    // ========== DELETE IMAGE ==========

    /**
     * Xóa ảnh khỏi database
     * Note: Cần xóa cả file trong Storage nếu muốn
     */
    public void deleteImage(String coupleId, String imageKey, ImageCallback callback) {
        Log.d(TAG, "deleteImage called for key: " + imageKey);

        DatabaseReference imageRef = rootRef.child("couples").child(coupleId).child("images").child(imageKey);

        imageRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✓ Image deleted from database");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to delete image", e);
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Xóa tất cả ảnh của couple
     */
    public void deleteAllImages(String coupleId, ImageCallback callback) {
        Log.d(TAG, "deleteAllImages called for coupleId: " + coupleId);

        DatabaseReference imagesRef = rootRef.child("couples").child(coupleId).child("images");

        imagesRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✓ All images deleted");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to delete all images", e);
                    callback.onError(e.getMessage());
                });
    }
}
