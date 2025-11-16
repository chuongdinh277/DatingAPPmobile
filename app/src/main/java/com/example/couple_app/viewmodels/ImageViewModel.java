package com.example.couple_app.viewmodels;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.couple_app.managers.ImageManager;
import com.example.couple_app.models.Image;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for managing image list data
 * Survives configuration changes and caches data to avoid redundant Firebase calls
 */
public class ImageViewModel extends ViewModel {
    private static final String TAG = "ImageViewModel";

    private final MutableLiveData<List<Image>> imagesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> imageCountLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    private String currentCoupleId;
    private boolean isDataLoaded = false;

    public ImageViewModel() {
        imagesLiveData.setValue(new ArrayList<>());
        imageCountLiveData.setValue(0);
        isLoadingLiveData.setValue(false);
    }

    /**
     * Get LiveData for image list
     */
    public LiveData<List<Image>> getImages() {
        return imagesLiveData;
    }

    /**
     * Get LiveData for image count
     */
    public LiveData<Integer> getImageCount() {
        return imageCountLiveData;
    }

    /**
     * Get LiveData for loading state
     */
    public LiveData<Boolean> getIsLoading() {
        return isLoadingLiveData;
    }

    /**
     * Get LiveData for error messages
     */
    public LiveData<String> getError() {
        return errorLiveData;
    }

    /**
     * Load images from Firebase (only if not already loaded or coupleId changed)
     */
    public void loadImages(String coupleId) {
        if (coupleId == null || coupleId.isEmpty()) {
            Log.w(TAG, "Cannot load images: coupleId is null or empty");
            errorLiveData.setValue("Không có thông tin cặp đôi");
            return;
        }

        // Only reload if coupleId changed or data not loaded yet
        if (isDataLoaded && coupleId.equals(currentCoupleId)) {
            Log.d(TAG, "Data already loaded for coupleId: " + coupleId + ". Skipping reload.");
            return;
        }

        currentCoupleId = coupleId;
        isLoadingLiveData.setValue(true);

        Log.d(TAG, "Loading images for coupleId: " + coupleId);

        ImageManager.getInstance().getListImages(coupleId, new ImageManager.ListImageCallback() {
            @Override
            public void onSuccess(List<Image> images) {
                Log.d(TAG, "✓ Successfully loaded " + images.size() + " images");

                imagesLiveData.setValue(images);
                imageCountLiveData.setValue(images.size());
                isLoadingLiveData.setValue(false);
                isDataLoaded = true;

                if (images.isEmpty()) {
                    Log.d(TAG, "No images found for couple");
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "❌ Failed to load images: " + errorMessage);

                errorLiveData.setValue(errorMessage);
                isLoadingLiveData.setValue(false);
                isDataLoaded = false;
            }
        });
    }

    /**
     * Force refresh images from Firebase
     */
    public void refreshImages() {
        isDataLoaded = false;
        if (currentCoupleId != null) {
            loadImages(currentCoupleId);
        }
    }

    /**
     * Add a new image to the list (called after upload)
     */
    public void addImage(Image image) {
        List<Image> currentList = imagesLiveData.getValue();
        if (currentList != null) {
            List<Image> newList = new ArrayList<>(currentList);
            newList.add(0, image); // Add to beginning
            imagesLiveData.setValue(newList);
            imageCountLiveData.setValue(newList.size());
            Log.d(TAG, "Image added to list. New count: " + newList.size());
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "ViewModel cleared");
    }
}

