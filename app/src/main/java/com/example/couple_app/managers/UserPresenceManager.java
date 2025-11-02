package com.example.couple_app.managers;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Manager for handling user online/offline presence status
 * Uses Firebase Realtime Database for real-time status updates
 */
public class UserPresenceManager {
    private static final String TAG = "UserPresenceManager";
    private static UserPresenceManager instance;

    private final DatabaseReference presenceRef;
    private String currentUserId;

    private UserPresenceManager() {
        // ✅ FIXED: Use correct database URL for Asia Southeast 1 region
        String databaseUrl = "https://couples-app-b83be-default-rtdb.asia-southeast1.firebasedatabase.app";
        FirebaseDatabase database = FirebaseDatabase.getInstance(databaseUrl);
        presenceRef = database.getReference("presence");

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // Monitor Firebase connection state
        DatabaseReference connectedRef = database.getReference(".info/connected");
        connectedRef.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class) != null && snapshot.getValue(Boolean.class);
                if (connected) {
                    Log.i(TAG, "🟢 Firebase Realtime Database CONNECTED");
                    // Refresh online status when reconnected
                    if (currentUserId != null && !currentUserId.isEmpty()) {
                        setUserOnline(currentUserId);
                    }
                } else {
                    Log.w(TAG, "🔴 Firebase Realtime Database DISCONNECTED");
                }
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                Log.e(TAG, "Connection monitoring error: " + error.getMessage());
            }
        });
    }

    public static synchronized UserPresenceManager getInstance() {
        if (instance == null) {
            instance = new UserPresenceManager();
        }
        return instance;
    }

    /**
     * Set user as online and setup automatic offline on disconnect
     */
    public void setUserOnline(String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Cannot set online: userId is null or empty");
            return;
        }

        Log.i(TAG, "========================================");
        Log.i(TAG, "Setting user ONLINE: " + userId);
        Log.i(TAG, "========================================");

        currentUserId = userId;
        DatabaseReference userPresenceRef = presenceRef.child(userId);

        // Data for online status
        Map<String, Object> onlineData = new HashMap<>();
        onlineData.put("isOnline", true);
        onlineData.put("lastSeen", ServerValue.TIMESTAMP);

        // Data for offline status (set on disconnect)
        Map<String, Object> offlineData = new HashMap<>();
        offlineData.put("isOnline", false);
        offlineData.put("lastSeen", ServerValue.TIMESTAMP);

        // Set offline status on disconnect
        userPresenceRef.onDisconnect().updateChildren(offlineData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ onDisconnect handler set successfully for user: " + userId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to set onDisconnect handler: " + e.getMessage());
                    e.printStackTrace();
                });

        // Set online status now
        userPresenceRef.updateChildren(onlineData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ User set to ONLINE successfully: " + userId);
                    Log.d(TAG, "Path: presence/" + userId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to set user online: " + e.getMessage());
                    e.printStackTrace();
                });
    }

    /**
     * Set user as offline manually
     */
    public void setUserOffline(String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Cannot set offline: userId is null or empty");
            return;
        }

        Log.i(TAG, "========================================");
        Log.i(TAG, "Setting user OFFLINE: " + userId);
        Log.i(TAG, "========================================");

        DatabaseReference userPresenceRef = presenceRef.child(userId);

        Map<String, Object> offlineData = new HashMap<>();
        offlineData.put("isOnline", false);
        offlineData.put("lastSeen", ServerValue.TIMESTAMP);

        userPresenceRef.updateChildren(offlineData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ User set to OFFLINE successfully: " + userId);
                    Log.d(TAG, "Path: presence/" + userId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to set user offline: " + e.getMessage());
                    e.printStackTrace();
                });
    }

    /**
     * Update last seen timestamp for current user
     */
    public void updateLastSeen() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            return;
        }

        DatabaseReference userPresenceRef = presenceRef.child(currentUserId);
        userPresenceRef.child("lastSeen").setValue(ServerValue.TIMESTAMP);
    }

    /**
     * Force refresh online status - useful when resuming app or opening chat
     */
    public void refreshOnlineStatus() {
        if (currentUserId != null && !currentUserId.isEmpty()) {
            Log.d(TAG, "Force refreshing online status for: " + currentUserId);
            setUserOnline(currentUserId);
        }
    }

    /**
     * Get reference to a user's presence data for listening
     */
    public DatabaseReference getUserPresenceRef(String userId) {
        return presenceRef.child(userId);
    }

    /**
     * Clean up when user logs out
     */
    public void cleanup() {
        if (currentUserId != null) {
            setUserOffline(currentUserId);
            currentUserId = null;
        }
    }
}

