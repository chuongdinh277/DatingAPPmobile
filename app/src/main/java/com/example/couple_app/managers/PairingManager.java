package com.example.couple_app.managers;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.example.couple_app.models.User;
import com.example.couple_app.models.Couple;
import java.util.Random;

public class PairingManager {
    private static final String TAG = "PairingManager";
    private DatabaseReference database;
    private static PairingManager instance;

    // Database paths
    private static final String USERS_PATH = "users";
    private static final String COUPLES_PATH = "couples";

    private PairingManager() {
        database = FirebaseDatabase.getInstance().getReference();
    }

    public static synchronized PairingManager getInstance() {
        if (instance == null) {
            instance = new PairingManager();
        }
        return instance;
    }

    public interface PairingCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public interface PinGeneratedCallback {
        void onPinGenerated(String pin);
        void onError(String error);
    }

    public interface PartnerFoundCallback {
        void onPartnerFound(User partner, String coupleId);
        void onError(String error);
    }

    // Generate a unique 6-digit PIN for the user
    public void generatePinForUser(String userId, PinGeneratedCallback callback) {
        if (userId == null) {
            callback.onError("Invalid user ID");
            return;
        }

        // Generate a random 6-digit PIN
        String pin = generateRandomPin();

        // Check if PIN already exists and generate new one if needed
        checkPinAvailability(pin, userId, callback, 0);
    }

    private void checkPinAvailability(String pin, String userId, PinGeneratedCallback callback, int attemptCount) {
        if (attemptCount >= 10) {
            callback.onError("Unable to generate unique PIN after multiple attempts");
            return;
        }

        Query query = database.child(USERS_PATH).orderByChild("pinCode").equalTo(pin);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // PIN already exists, generate a new one
                    String newPin = generateRandomPin();
                    checkPinAvailability(newPin, userId, callback, attemptCount + 1);
                } else {
                    // PIN is unique, save it to the user
                    savePinToUser(userId, pin, callback);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.w(TAG, "Error checking PIN availability", error.toException());
                callback.onError("Failed to check PIN availability: " + error.getMessage());
            }
        });
    }

    private void savePinToUser(String userId, String pin, PinGeneratedCallback callback) {
        database.child(USERS_PATH).child(userId).child("pinCode").setValue(pin)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "PIN saved successfully for user: " + userId);
                callback.onPinGenerated(pin);
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "Error saving PIN", e);
                callback.onError("Failed to save PIN: " + e.getMessage());
            });
    }

    private String generateRandomPin() {
        Random random = new Random();
        int pin = 100000 + random.nextInt(900000); // Generates 6-digit number
        return String.valueOf(pin);
    }

    // Find partner by PIN and create couple
    public void pairWithPartner(String currentUserId, String partnerPin, PairingCallback callback) {
        if (currentUserId == null || partnerPin == null || partnerPin.length() != 6) {
            callback.onError("Invalid user ID or PIN format");
            return;
        }

        // First check if current user already has a partner
        database.child(USERS_PATH).child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot currentUserSnapshot) {
                User currentUser = currentUserSnapshot.getValue(User.class);
                if (currentUser == null) {
                    callback.onError("Current user not found");
                    return;
                }

                if (currentUser.getPartnerId() != null && !currentUser.getPartnerId().isEmpty()) {
                    callback.onError("You are already paired with someone");
                    return;
                }

                // Find partner by PIN
                Query partnerQuery = database.child(USERS_PATH).orderByChild("pinCode").equalTo(partnerPin);
                partnerQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot partnerSnapshot) {
                        if (!partnerSnapshot.exists()) {
                            callback.onError("No user found with this PIN");
                            return;
                        }

                        // Get the partner user
                        User partner = null;
                        String partnerId = null;
                        for (DataSnapshot child : partnerSnapshot.getChildren()) {
                            partner = child.getValue(User.class);
                            partnerId = child.getKey();
                            break;
                        }

                        if (partner == null || partnerId == null) {
                            callback.onError("Partner data not found");
                            return;
                        }

                        if (partnerId.equals(currentUserId)) {
                            callback.onError("You cannot pair with yourself");
                            return;
                        }

                        if (partner.getPartnerId() != null && !partner.getPartnerId().isEmpty()) {
                            callback.onError("This user is already paired with someone");
                            return;
                        }

                        // Create the couple
                        createCouple(currentUserId, partnerId, currentUser, partner, callback);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.w(TAG, "Error finding partner", error.toException());
                        callback.onError("Failed to find partner: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.w(TAG, "Error getting current user", error.toException());
                callback.onError("Failed to get current user: " + error.getMessage());
            }
        });
    }

    private void createCouple(String user1Id, String user2Id, User user1, User user2, PairingCallback callback) {
        String coupleId = generateCoupleId(user1Id, user2Id);
        Timestamp now = Timestamp.now();

        // Create couple object
        Couple couple = new Couple(coupleId, user1Id, user2Id, now);

        // Update both users with partner IDs and start love date
        database.child(USERS_PATH).child(user1Id).child("partnerId").setValue(user2Id);
        database.child(USERS_PATH).child(user1Id).child("startLoveDate").setValue(now);
        database.child(USERS_PATH).child(user2Id).child("partnerId").setValue(user1Id);
        database.child(USERS_PATH).child(user2Id).child("startLoveDate").setValue(now);

        // Save couple to database
        database.child(COUPLES_PATH).child(coupleId).setValue(couple)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Couple created successfully: " + coupleId);
                callback.onSuccess("Successfully paired with " + user2.getName() + "!");
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "Error creating couple", e);
                // Rollback partner IDs if couple creation failed
                database.child(USERS_PATH).child(user1Id).child("partnerId").setValue(null);
                database.child(USERS_PATH).child(user1Id).child("startLoveDate").setValue(0);
                database.child(USERS_PATH).child(user2Id).child("partnerId").setValue(null);
                database.child(USERS_PATH).child(user2Id).child("startLoveDate").setValue(0);
                callback.onError("Failed to create couple: " + e.getMessage());
            });
    }

    private String generateCoupleId(String user1Id, String user2Id) {
        // Create consistent couple ID regardless of order
        String combined = user1Id.compareTo(user2Id) < 0 ? user1Id + "_" + user2Id : user2Id + "_" + user1Id;
        return combined;
    }

    // Check if user is already paired
    public void checkPairingStatus(String userId, PartnerFoundCallback callback) {
        if (userId == null) {
            callback.onError("Invalid user ID");
            return;
        }

        database.child(USERS_PATH).child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot userSnapshot) {
                User user = userSnapshot.getValue(User.class);
                if (user == null) {
                    callback.onError("User not found");
                    return;
                }

                String partnerId = user.getPartnerId();
                if (partnerId == null || partnerId.isEmpty()) {
                    callback.onError("No partner found");
                    return;
                }

                // Get partner details
                database.child(USERS_PATH).child(partnerId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot partnerSnapshot) {
                        User partner = partnerSnapshot.getValue(User.class);
                        if (partner == null) {
                            callback.onError("Partner not found");
                            return;
                        }

                        String coupleId = generateCoupleId(userId, partnerId);
                        callback.onPartnerFound(partner, coupleId);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.w(TAG, "Error getting partner details", error.toException());
                        callback.onError("Failed to get partner details: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.w(TAG, "Error checking pairing status", error.toException());
                callback.onError("Failed to check pairing status: " + error.getMessage());
            }
        });
    }
}
