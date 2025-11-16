package com.example.couple_app.ui.viewmodels;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.couple_app.data.local.DatabaseManager;
import com.example.couple_app.data.model.User;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel for managing avatar data
 * Caches avatars in memory to avoid redundant network calls
 * Survives configuration changes
 */
public class AvatarViewModel extends ViewModel {
    private static final String TAG = "AvatarViewModel";

    // Cache for avatars - key is userId
    private final Map<String, MutableLiveData<Bitmap>> avatarCache = new HashMap<>();
    private final Map<String, MutableLiveData<String>> avatarUrlCache = new HashMap<>();

    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public AvatarViewModel() {
        isLoadingLiveData.setValue(false);
    }

    /**
     * Get LiveData for a user's avatar bitmap
     */
    public LiveData<Bitmap> getAvatar(String userId) {
        if (!avatarCache.containsKey(userId)) {
            avatarCache.put(userId, new MutableLiveData<>());
        }
        return avatarCache.get(userId);
    }

    /**
     * Get LiveData for a user's avatar URL
     */
    public LiveData<String> getAvatarUrl(String userId) {
        if (!avatarUrlCache.containsKey(userId)) {
            avatarUrlCache.put(userId, new MutableLiveData<>());
        }
        return avatarUrlCache.get(userId);
    }

    /**
     * Get loading state
     */
    public LiveData<Boolean> getIsLoading() {
        return isLoadingLiveData;
    }

    /**
     * Get error messages
     */
    public LiveData<String> getError() {
        return errorLiveData;
    }

    /**
     * Load avatar for a user (from cache or Firebase)
     * Only loads if not already cached
     */
    public void loadAvatar(String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "Cannot load avatar: userId is null or empty");
            return;
        }

        // Check if already loaded in cache
        MutableLiveData<Bitmap> cachedBitmap = avatarCache.get(userId);
        if (cachedBitmap != null && cachedBitmap.getValue() != null) {
            Log.d(TAG, "Avatar for user " + userId + " already cached. Skipping reload.");
            return;
        }

        isLoadingLiveData.setValue(true);
        Log.d(TAG, "Loading avatar for userId: " + userId);

        DatabaseManager.getInstance().getUser(userId, new DatabaseManager.DatabaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null && user.getProfilePicUrl() != null && !user.getProfilePicUrl().isEmpty()) {
                    String avatarUrl = user.getProfilePicUrl();

                    // Cache the URL
                    if (!avatarUrlCache.containsKey(userId)) {
                        avatarUrlCache.put(userId, new MutableLiveData<>());
                    }
                    avatarUrlCache.get(userId).postValue(avatarUrl);

                    // Load the bitmap
                    loadAvatarFromUrl(userId, avatarUrl);
                } else {
                    Log.w(TAG, "No avatar URL found for user " + userId);
                    isLoadingLiveData.postValue(false);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to load user data for avatar: " + error);
                errorLiveData.postValue(error);
                isLoadingLiveData.postValue(false);
            }
        });
    }

    /**
     * Load multiple avatars at once
     */
    public void loadAvatars(String... userIds) {
        for (String userId : userIds) {
            if (userId != null && !userId.isEmpty()) {
                loadAvatar(userId);
            }
        }
    }

    /**
     * Force reload avatar (clears cache and reloads)
     */
    public void refreshAvatar(String userId) {
        if (userId == null || userId.isEmpty()) {
            return;
        }

        // Clear cache
        if (avatarCache.containsKey(userId)) {
            avatarCache.get(userId).setValue(null);
        }
        if (avatarUrlCache.containsKey(userId)) {
            avatarUrlCache.get(userId).setValue(null);
        }

        // Reload
        loadAvatar(userId);
    }

    /**
     * Update avatar in cache (called after upload)
     */
    public void updateAvatar(String userId, Bitmap newAvatar, String newAvatarUrl) {
        if (userId == null || userId.isEmpty()) {
            return;
        }

        Log.d(TAG, "Updating avatar cache for user: " + userId);

        // Update bitmap cache
        if (!avatarCache.containsKey(userId)) {
            avatarCache.put(userId, new MutableLiveData<>());
        }
        avatarCache.get(userId).postValue(newAvatar);

        // Update URL cache
        if (newAvatarUrl != null && !newAvatarUrl.isEmpty()) {
            if (!avatarUrlCache.containsKey(userId)) {
                avatarUrlCache.put(userId, new MutableLiveData<>());
            }
            avatarUrlCache.get(userId).postValue(newAvatarUrl);
        }
    }

    /**
     * Load bitmap from URL in background thread
     */
    private void loadAvatarFromUrl(String userId, String urlString) {
        executorService.execute(() -> {
            try {
                Log.d(TAG, "Downloading avatar from: " + urlString);

                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.connect();

                InputStream input = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);

                if (bitmap != null) {
                    Log.d(TAG, "✓ Avatar loaded successfully for user: " + userId);

                    // Cache the bitmap
                    if (!avatarCache.containsKey(userId)) {
                        avatarCache.put(userId, new MutableLiveData<>());
                    }
                    avatarCache.get(userId).postValue(bitmap);
                } else {
                    Log.w(TAG, "Failed to decode bitmap from URL");
                    errorLiveData.postValue("Failed to decode avatar image");
                }

                isLoadingLiveData.postValue(false);

            } catch (Exception e) {
                Log.e(TAG, "Error loading avatar from URL: " + e.getMessage(), e);
                errorLiveData.postValue("Failed to load avatar: " + e.getMessage());
                isLoadingLiveData.postValue(false);
            }
        });
    }

    /**
     * Clear all cached avatars (use when user logs out)
     */
    public void clearCache() {
        Log.d(TAG, "Clearing all avatar cache");
        avatarCache.clear();
        avatarUrlCache.clear();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "ViewModel cleared");

        // Shutdown executor
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}

