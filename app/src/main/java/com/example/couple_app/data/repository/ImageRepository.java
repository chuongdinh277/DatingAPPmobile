package com.example.couple_app.data.repository;

import com.example.couple_app.data.model.Image;
import com.example.couple_app.managers.ImageManager;

import java.util.List;

/**
 * Repository để tương tác với Image trong Firebase
 * Tách biệt logic data với UI
 */
public class ImageRepository {

    private final ImageManager imageManager;

    public ImageRepository() {
        this.imageManager = ImageManager.getInstance();
    }

    /**
     * Callback interface cho các thao tác image
     */
    public interface ImageCallback {
        void onSuccess();
        void onError(String error);
    }

    /**
     * Callback interface cho việc lấy danh sách image
     */
    public interface ImageListCallback {
        void onSuccess(List<Image> images);
        void onError(String error);
    }

    /**
     * Upload image lên Firebase
     */
    public void uploadImage(String imageUrl, String coupleId, String userId, ImageCallback callback) {
        imageManager.uploadImage(imageUrl, coupleId, userId, new ImageManager.ImageCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess();
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * Lấy danh sách image theo coupleId
     */
    public void getImagesByCoupleId(String coupleId, ImageListCallback callback) {
        imageManager.getListImages(coupleId, new ImageManager.ListImageCallback() {
            @Override
            public void onSuccess(List<Image> images) {
                callback.onSuccess(images);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * Xóa một image
     */
    public void deleteImage(String coupleId, String imageKey, ImageCallback callback) {
        imageManager.deleteImage(coupleId, imageKey, new ImageManager.ImageCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess();
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * Xóa tất cả image của couple
     */
    public void deleteAllImages(String coupleId, ImageCallback callback) {
        imageManager.deleteAllImages(coupleId, new ImageManager.ImageCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess();
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
}

