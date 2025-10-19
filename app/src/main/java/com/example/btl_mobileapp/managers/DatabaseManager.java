package com.example.btl_mobileapp.managers;

import android.util.Log;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;
import com.example.btl_mobileapp.models.User;
import com.example.btl_mobileapp.models.Couple;
import com.example.btl_mobileapp.models.AISuggestion;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class DatabaseManager {
    private static final String TAG = "DatabaseManager";
    private FirebaseFirestore db;
    private static DatabaseManager instance;

    // Collection names
    private static final String USERS_COLLECTION = "users";
    private static final String COUPLES_COLLECTION = "couples";
    private static final String AI_SUGGESTIONS_COLLECTION = "ai_suggestions";
    private static final String MESSAGES_COLLECTION = "messages";

    private DatabaseManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public interface DatabaseCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }
    public interface PinCheckCallback {
        void onResult(boolean exists);
    }

    public interface DatabaseActionCallback {
        void onSuccess();
        void onError(String error);
    }

    // Generate random 6-digit PIN
    private String generatePinCode() {
        Random random = new Random();
        int pin = 100000 + random.nextInt(900000);
        return String.valueOf(pin);
    }

    // Create user document in Firestore (for email registration)
    public void createUserDocument(FirebaseUser firebaseUser, String name, AuthManager.AuthCallback callback) {
        String userId = firebaseUser.getUid();
        User user = new User(userId, name, firebaseUser.getEmail());

        db.collection(USERS_COLLECTION)
                .document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User document created successfully");
                    callback.onSuccess(firebaseUser);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error creating user document", e);
                    callback.onError("Failed to create user profile: " + e.getMessage());
                });
    }

    // Create user document in Firestore (for phone registration)
    public void createUserDocumentWithPhone(FirebaseUser firebaseUser, String name, String phoneNumber, AuthManager.AuthCallback callback) {
        String userId = firebaseUser.getUid();
        User user = new User(userId, name, phoneNumber, true); // true indicates phone registration

        db.collection(USERS_COLLECTION)
                .document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User document with phone created successfully");
                    callback.onSuccess(firebaseUser);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error creating user document with phone", e);
                    callback.onError("Failed to create user profile: " + e.getMessage());
                });
    }

    // Update user profile in Firestore using a Map for flexibility
    public void updateUserProfile(String userId, Map<String, Object> updates, AuthManager.AuthActionCallback callback) {
        if (updates == null || updates.isEmpty()) {
            callback.onSuccess(); // Nothing to update
            return;
        }

        db.collection(USERS_COLLECTION)
                .document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User profile updated successfully");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error updating user profile", e);
                    callback.onError("Failed to update profile: " + e.getMessage());
                });
    }

    // Update user email in Firestore (for Google account linking)
    public void updateUserEmail(String userId, String email, AuthManager.AuthActionCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("email", email);
        updates.put("updatedAt", Timestamp.now());

        db.collection(USERS_COLLECTION)
                .document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User email updated successfully to: " + email);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error updating user email", e);
                    callback.onError("Failed to update email: " + e.getMessage());
                });
    }

    // Generate and save PIN for user
    public void generateAndSavePinForUser(String userId, DatabaseCallback<String> callback) {
        generateAndSavePinRecursive(userId, callback, 5); // Thêm một giới hạn số lần thử, ví dụ 5 lần
    }

    // Modify checkPinExists to use the new interface
    private void checkPinExists(String pin, PinCheckCallback callback) {
        db.collection(USERS_COLLECTION)
                .whereEqualTo("pinCode", pin)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onResult(!task.getResult().isEmpty()); // Call the new method
                    } else {
                        Log.w(TAG, "Error checking PIN existence", task.getException());
                        // Decide how to handle errors here.
                        // You might want to pass an error callback separately
                        // or throw an exception, or call a generic error handler.
                        // For simplicity, I'm just logging here.
                        // If you need to propagate the error, the PinCheckCallback might need an onError method too,
                        // making it not a functional interface again, or you pass a separate error callback.
                    }
                });
    }

    private void generateAndSavePinRecursive(String userId, final DatabaseCallback<String> finalCallback, int retriesLeft) {
        if (retriesLeft <= 0) {
            finalCallback.onError("Failed to generate a unique PIN after several attempts.");
            return;
        }

        String newPin = generatePinCode();

        checkPinExists(newPin, exists -> {
            if (exists) {
                Log.d(TAG, "PIN " + newPin + " already exists. Retrying... Attempts left: " + (retriesLeft - 1));
                generateAndSavePinRecursive(userId, finalCallback, retriesLeft - 1);
            } else {
                // First check if user document exists
                db.collection(USERS_COLLECTION)
                        .document(userId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                // Document exists, update pinCode
                                db.collection(USERS_COLLECTION)
                                        .document(userId)
                                        .update("pinCode", newPin)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "PIN generated and updated: " + newPin);
                                            finalCallback.onSuccess(newPin);
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.w(TAG, "Error updating PIN", e);
                                            finalCallback.onError("Failed to update PIN: " + e.getMessage());
                                        });
                            } else {
                                // Document doesn't exist, create new user document with PIN
                                // Get current user info from Firebase Auth
                                com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                                if (currentUser != null) {
                                    User newUser;
                                    if (currentUser.getEmail() != null) {
                                        // Email user
                                        newUser = new User(userId,
                                                currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User",
                                                currentUser.getEmail());
                                    } else {
                                        // Phone user
                                        newUser = new User(userId,
                                                currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User",
                                                currentUser.getPhoneNumber(), true);
                                    }
                                    newUser.setPinCode(newPin);

                                    db.collection(USERS_COLLECTION)
                                            .document(userId)
                                            .set(newUser)
                                            .addOnSuccessListener(aVoid -> {
                                                Log.d(TAG, "User document created with PIN: " + newPin);
                                                finalCallback.onSuccess(newPin);
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.w(TAG, "Error creating user document with PIN", e);
                                                finalCallback.onError("Failed to create user with PIN: " + e.getMessage());
                                            });
                                } else {
                                    finalCallback.onError("No authenticated user found");
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.w(TAG, "Error checking user document existence", e);
                            finalCallback.onError("Failed to check user document: " + e.getMessage());
                        });
            }
        });
    }


    // Connect couple using PIN
    public void connectCoupleWithPin(String currentUserId, String pin, DatabaseCallback<String> callback) {
        // First, find user with this PIN
        db.collection(USERS_COLLECTION)
                .whereEqualTo("pinCode", pin)
                .whereEqualTo("partnerId", null) // Only unconnected users
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        QueryDocumentSnapshot partnerDoc = (QueryDocumentSnapshot) task.getResult().getDocuments().get(0);
                        String partnerId = partnerDoc.getId();

                        if (partnerId.equals(currentUserId)) {
                            callback.onError("Cannot connect to yourself");
                            return;
                        }

                        // Check if current user is already connected
                        db.collection(USERS_COLLECTION)
                                .document(currentUserId)
                                .get()
                                .addOnSuccessListener(currentUserDoc -> {
                                    if (currentUserDoc.exists()) {
                                        User currentUser = currentUserDoc.toObject(User.class);
                                        if (currentUser.getPartnerId() != null) {
                                            callback.onError("You are already connected to a partner");
                                            return;
                                        }

                                        // Create couple document
                                        createCoupleDocument(currentUserId, partnerId, callback);
                                    } else {
                                        callback.onError("Current user not found");
                                    }
                                })
                                .addOnFailureListener(e -> callback.onError("Error checking current user: " + e.getMessage()));

                    } else if (task.isSuccessful() && task.getResult().isEmpty()) {
                        callback.onError("Invalid PIN or user already connected");
                    } else {
                        callback.onError("Error finding user with PIN: " + task.getException().getMessage());
                    }
                });
    }

    // Create couple document and update both users
    private void createCoupleDocument(String user1Id, String user2Id, DatabaseCallback<String> callback) {
        // Create couple document
        String coupleId = db.collection(COUPLES_COLLECTION).document().getId();

        Timestamp now = Timestamp.now();

        Couple couple = new Couple(coupleId, user1Id, user2Id, now);
        couple.setUser1Id(user1Id);
        couple.setUser2Id(user2Id);
        couple.setStartDate(Timestamp.now());
        couple.setSharedStories(new ArrayList<>());

        db.collection(COUPLES_COLLECTION)
                .add(couple)
                .addOnSuccessListener(documentReference -> {
                    String coupleId_ = documentReference.getId();

                    // Update both users with partner info
                    updateUsersWithPartnerInfo(user1Id, user2Id, coupleId_, Timestamp.now(), callback);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error creating couple document", e);
                    callback.onError("Failed to create couple: " + e.getMessage());
                });
    }

    // Update both users with partner information
    private void updateUsersWithPartnerInfo(String user1Id, String user2Id, String coupleId, Timestamp startDate, DatabaseCallback<String> callback) {
        // Update user1
        Map<String, Object> user1Updates = new HashMap<>();
        user1Updates.put("partnerId", user2Id);
        user1Updates.put("startLoveDate", startDate);
        user1Updates.put("pinCode", null); // Clear PIN after connection

        // Update user2
        Map<String, Object> user2Updates = new HashMap<>();
        user2Updates.put("partnerId", user1Id);
        user2Updates.put("startLoveDate", startDate);
        user2Updates.put("pinCode", null); // Clear PIN after connection

        // Batch update both users
        db.collection(USERS_COLLECTION).document(user1Id).update(user1Updates)
                .addOnSuccessListener(aVoid -> {
                    db.collection(USERS_COLLECTION).document(user2Id).update(user2Updates)
                            .addOnSuccessListener(aVoid2 -> {
                                Log.d(TAG, "Couple connected successfully");
                                callback.onSuccess(coupleId);
                            })
                            .addOnFailureListener(e -> callback.onError("Failed to update partner: " + e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError("Failed to update user: " + e.getMessage()));
    }

    // Get user document
    public void getUser(String userId, DatabaseCallback<User> callback) {
        db.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        try {
                            User user = documentSnapshot.toObject(User.class);
                            if (user == null) {
                                callback.onError("User data is null");
                            } else {
                                callback.onSuccess(user);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to parse User document", e);
                            callback.onError("Failed to parse user: " + e.getMessage());
                        }
                    } else {
                        callback.onError("User not found");
                    }
                })
                .addOnFailureListener(e -> callback.onError("Error getting user: " + e.getMessage()));
    }

    // Get couple document by user ID
    public void getCoupleByUserId(String userId, DatabaseCallback<Couple> callback) {
        db.collection(COUPLES_COLLECTION)
                .whereEqualTo("user1Id", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        try {
                            QueryDocumentSnapshot doc = (QueryDocumentSnapshot) task.getResult().getDocuments().get(0);
                            Couple couple = doc.toObject(Couple.class);
                            if (couple == null) {
                                callback.onError("Couple data is null");
                                return;
                            }
                            couple.setCoupleId(doc.getId());
                            callback.onSuccess(couple);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to parse Couple document (user1Id)", e);
                            callback.onError("Failed to parse couple: " + e.getMessage());
                        }
                    } else {
                        // Try with user2Id
                        db.collection(COUPLES_COLLECTION)
                                .whereEqualTo("user2Id", userId)
                                .get()
                                .addOnCompleteListener(task2 -> {
                                    if (task2.isSuccessful() && !task2.getResult().isEmpty()) {
                                        try {
                                            QueryDocumentSnapshot doc = (QueryDocumentSnapshot) task2.getResult().getDocuments().get(0);
                                            Couple couple = doc.toObject(Couple.class);
                                            if (couple == null) {
                                                callback.onError("Couple data is null");
                                                return;
                                            }
                                            couple.setCoupleId(doc.getId());
                                            callback.onSuccess(couple);
                                        } catch (Exception e) {
                                            Log.e(TAG, "Failed to parse Couple document (user2Id)", e);
                                            callback.onError("Failed to parse couple: " + e.getMessage());
                                        }
                                    } else {
                                        callback.onError("Couple not found");
                                    }
                                });
                    }
                });
    }

    // Calculate love days
    public long calculateLoveDays(Timestamp startDate) {
        if (startDate == null) return 0;

        long startTimeMillis = startDate.toDate().getTime();
        long currentTimeMillis = System.currentTimeMillis();
        long diffMillis = currentTimeMillis - startTimeMillis;

        return diffMillis / (1000 * 60 * 60 * 24); // Convert to days
    }

    // Save AI suggestion
    public void saveAISuggestion(String coupleId, String type, String suggestionText, DatabaseActionCallback callback) {
        AISuggestion suggestion = new AISuggestion();
        suggestion.setCoupleId(coupleId);
        suggestion.setType(type);
        suggestion.setSuggestionText(suggestionText);
        suggestion.setTimestamp(Timestamp.now());

        db.collection(AI_SUGGESTIONS_COLLECTION)
                .add(suggestion)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "AI suggestion saved");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error saving AI suggestion", e);
                    callback.onError("Failed to save suggestion: " + e.getMessage());
                });
    }

    // Get AI suggestions for couple
    public void getAISuggestions(String coupleId, DatabaseCallback<List<AISuggestion>> callback) {
        db.collection(AI_SUGGESTIONS_COLLECTION)
                .whereEqualTo("coupleId", coupleId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<AISuggestion> suggestions = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        AISuggestion suggestion = doc.toObject(AISuggestion.class);
                        suggestion.setSuggestionId(doc.getId());
                        suggestions.add(suggestion);
                    }
                    callback.onSuccess(suggestions);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error getting AI suggestions", e);
                    callback.onError("Failed to get suggestions: " + e.getMessage());
                });
    }

    // Send a message
    public void sendMessage(String coupleId, String senderId, String senderName, String messageText, DatabaseActionCallback callback) {
        com.example.btl_mobileapp.models.Message message = new com.example.btl_mobileapp.models.Message(coupleId, senderId, senderName, messageText);

        db.collection(MESSAGES_COLLECTION)
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Message sent successfully");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error sending message", e);
                    callback.onError("Failed to send message: " + e.getMessage());
                });
    }

    // Get messages for a couple
    public void getMessages(String coupleId, DatabaseCallback<List<com.example.btl_mobileapp.models.Message>> callback) {
        db.collection(MESSAGES_COLLECTION)
                .whereEqualTo("coupleId", coupleId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<com.example.btl_mobileapp.models.Message> messages = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        com.example.btl_mobileapp.models.Message message = doc.toObject(com.example.btl_mobileapp.models.Message.class);
                        message.setMessageId(doc.getId());
                        messages.add(message);
                    }
                    callback.onSuccess(messages);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error getting messages", e);
                    callback.onError("Failed to get messages: " + e.getMessage());
                });
    }

    // Listen for real-time messages
    public com.google.firebase.firestore.ListenerRegistration listenForMessages(String coupleId, DatabaseCallback<List<com.example.btl_mobileapp.models.Message>> callback) {
        if (coupleId == null || coupleId.isEmpty()) {
            callback.onError("Couple ID is null or empty");
            return null;
        }

        return db.collection(MESSAGES_COLLECTION)
                .whereEqualTo("coupleId", coupleId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed: " + e.getMessage(), e);
                        callback.onError("Failed to listen for messages: " + e.getMessage());
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        List<com.example.btl_mobileapp.models.Message> messages = new ArrayList<>();
                        try {
                            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                com.example.btl_mobileapp.models.Message message = doc.toObject(com.example.btl_mobileapp.models.Message.class);
                                if (message != null) {
                                    message.setMessageId(doc.getId());
                                    messages.add(message);
                                }
                            }
                            callback.onSuccess(messages);
                        } catch (Exception ex) {
                            Log.e(TAG, "Error parsing messages: " + ex.getMessage(), ex);
                            callback.onError("Error parsing messages: " + ex.getMessage());
                        }
                    } else {
                        // No messages yet, return empty list
                        callback.onSuccess(new ArrayList<>());
                    }
                });
    }

    // Mark message as read
    public void markMessageAsRead(String messageId, DatabaseActionCallback callback) {
        db.collection(MESSAGES_COLLECTION)
                .document(messageId)
                .update("read", true)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Message marked as read");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error marking message as read", e);
                    callback.onError("Failed to mark message as read: " + e.getMessage());
                });
    }

    // Check if phone number exists in database
    public void checkPhoneNumberExists(String phoneNumber, DatabaseCallback<Boolean> callback) {
        db.collection(USERS_COLLECTION)
                .whereEqualTo("phoneNumber", phoneNumber)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean exists = !task.getResult().isEmpty();
                        callback.onSuccess(exists);
                    } else {
                        Log.w(TAG, "Error checking phone number existence", task.getException());
                        callback.onError("Failed to check phone number: " + task.getException().getMessage());
                    }
                });
    }

    // Check if email exists in database and return user info if exists
    public void checkEmailExists(String email, DatabaseCallback<User> callback) {
        db.collection(USERS_COLLECTION)
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            // Email exists, return the user
                            QueryDocumentSnapshot doc = (QueryDocumentSnapshot) task.getResult().getDocuments().get(0);
                            try {
                                User existingUser = doc.toObject(User.class);
                                existingUser.setUserId(doc.getId()); // Set the document ID as user ID
                                callback.onSuccess(existingUser);
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing existing user with email", e);
                                callback.onError("Error parsing user data: " + e.getMessage());
                            }
                        } else {
                            // Email doesn't exist
                            callback.onSuccess(null);
                        }
                    } else {
                        Log.w(TAG, "Error checking email existence", task.getException());
                        callback.onError("Failed to check email: " + task.getException().getMessage());
                    }
                });
    }
}

