package com.example.couple_app.managers;

import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;

/**
 * Manager class for handling Firebase Storage operations
 */
public class StorageManager {
    private static final String TAG = "StorageManager";
    private static StorageManager instance;
    private final FirebaseStorage storage;
    private final StorageReference storageRef;

    private StorageManager() {
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
    }

    public static synchronized StorageManager getInstance() {
        if (instance == null) {
            instance = new StorageManager();
        }
        return instance;
    }

    /**
     * Upload avatar image to Firebase Storage
     * @param bitmap The image bitmap to upload
     * @param userId The user ID (for organizing storage)
     * @param callback Callback for upload result
     */
    public void uploadAvatar(@NonNull Bitmap bitmap, @NonNull String userId, @NonNull UploadCallback callback) {
        try {
            // Compress bitmap to JPEG
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
            byte[] data = baos.toByteArray();

            // Create a unique filename
            String filename = "avatar_" + System.currentTimeMillis() + ".jpg";
            StorageReference avatarRef = storageRef.child("avatars/" + userId + "/" + filename);

            // Upload the file
            UploadTask uploadTask = avatarRef.putBytes(data);
            uploadTask.addOnProgressListener(taskSnapshot -> {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                callback.onProgress((int) progress);
            }).addOnSuccessListener(taskSnapshot -> {
                // Get download URL
                avatarRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    Log.d(TAG, "Avatar uploaded successfully: " + uri.toString());
                    callback.onSuccess(uri.toString());
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get download URL", e);
                    callback.onError("Failed to get download URL: " + e.getMessage());
                });
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Avatar upload failed", e);
                callback.onError("Upload failed: " + e.getMessage());
            });

        } catch (Exception e) {
            Log.e(TAG, "Error preparing upload", e);
            callback.onError("Error preparing upload: " + e.getMessage());
        }
    }

    /**
     * Upload image from URI to Firebase Storage (for couple images)
     * @param imageUri The image URI to upload
     * @param coupleId The couple ID (for organizing storage by couple)
     * @param callback Callback for upload result
     */
    public void uploadImageFromUri(@NonNull Uri imageUri, @NonNull String coupleId, @NonNull UploadCallback callback) {
        try {
            // Create a unique filename
            String filename = "image_" + System.currentTimeMillis() + ".jpg";
            // Store images in couples/{coupleId}/images/ folder
            StorageReference imageRef = storageRef.child("couples/" + coupleId + "/images/" + filename);

            // Upload the file
            UploadTask uploadTask = imageRef.putFile(imageUri);
            uploadTask.addOnProgressListener(taskSnapshot -> {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                callback.onProgress((int) progress);
            }).addOnSuccessListener(taskSnapshot -> {
                // Get download URL
                imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    Log.d(TAG, "Image uploaded successfully: " + uri.toString());
                    callback.onSuccess(uri.toString());
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get download URL", e);
                    callback.onError("Failed to get download URL: " + e.getMessage());
                });
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Image upload failed", e);
                callback.onError("Upload failed: " + e.getMessage());
            });

        } catch (Exception e) {
            Log.e(TAG, "Error preparing upload", e);
            callback.onError("Error preparing upload: " + e.getMessage());
        }
    }

    /**
     * Delete image from Firebase Storage
     * @param imageUrl The full URL of the image to delete
     * @param callback Callback for deletion result
     */
    public void deleteImage(@NonNull String imageUrl, @NonNull DeleteCallback callback) {
        try {
            StorageReference imageRef = storage.getReferenceFromUrl(imageUrl);
            imageRef.delete().addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Image deleted successfully");
                callback.onSuccess();
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to delete image", e);
                callback.onError("Delete failed: " + e.getMessage());
            });
        } catch (Exception e) {
            Log.e(TAG, "Error deleting image", e);
            callback.onError("Error deleting image: " + e.getMessage());
        }
    }

    /**
     * Delete all images of a couple from Firebase Storage
     * @param coupleId The couple ID
     * @param callback Callback for deletion result
     */
    public void deleteAllCoupleImages(@NonNull String coupleId, @NonNull DeleteCallback callback) {
        try {
            StorageReference coupleImagesRef = storageRef.child("couples/" + coupleId + "/images/");

            coupleImagesRef.listAll().addOnSuccessListener(listResult -> {
                if (listResult.getItems().isEmpty()) {
                    Log.d(TAG, "No images to delete for couple: " + coupleId);
                    callback.onSuccess();
                    return;
                }

                // Delete each file
                int totalFiles = listResult.getItems().size();
                final int[] deletedCount = {0};
                final boolean[] hasError = {false};

                for (StorageReference item : listResult.getItems()) {
                    item.delete().addOnSuccessListener(aVoid -> {
                        deletedCount[0]++;
                        Log.d(TAG, "Deleted image: " + item.getName() + " (" + deletedCount[0] + "/" + totalFiles + ")");

                        if (deletedCount[0] == totalFiles && !hasError[0]) {
                            Log.d(TAG, "All couple images deleted successfully");
                            callback.onSuccess();
                        }
                    }).addOnFailureListener(e -> {
                        if (!hasError[0]) {
                            hasError[0] = true;
                            Log.e(TAG, "Failed to delete image: " + item.getName(), e);
                            callback.onError("Failed to delete some images: " + e.getMessage());
                        }
                    });
                }
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to list images for couple: " + coupleId, e);
                callback.onError("Failed to list images: " + e.getMessage());
            });

        } catch (Exception e) {
            Log.e(TAG, "Error deleting couple images", e);
            callback.onError("Error deleting couple images: " + e.getMessage());
        }
    }

    /**
     * Callback interface for upload operations
     */
    public interface UploadCallback {
        void onSuccess(String downloadUrl);
        void onError(String error);
        void onProgress(int progress);
    }

    /**
     * Callback interface for delete operations
     */
    public interface DeleteCallback {
        void onSuccess();
        void onError(String error);
    }
}
