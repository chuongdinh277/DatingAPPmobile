package com.example.btl_mobileapp.managers;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthSettings;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.UserInfo;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AuthManager {
    private static final String TAG = "AuthManager";
    private FirebaseAuth mAuth;
    private static AuthManager instance;

    private AuthManager() {
        mAuth = FirebaseAuth.getInstance();
        mAuth.getFirebaseAuthSettings().setAppVerificationDisabledForTesting(true);
    }

    public static synchronized AuthManager getInstance() {
        if (instance == null) {
            instance = new AuthManager();
        }
        return instance;
    }

    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onError(String error);
    }

    public interface AuthActionCallback {
        void onSuccess();
        void onError(String error);
    }


    // Sign up with email and password
    public void signUp(String email, String password, String name, AuthCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        if (profileTask.isSuccessful()) {
                                            Log.d(TAG, "User profile updated.");
                                            DatabaseManager.getInstance().createUserDocument(user, name, callback);
                                        } else {
                                            callback.onError("Failed to update profile: " + profileTask.getException().getMessage());
                                        }
                                    });
                        }
                    } else {
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Sign up failed";
                        callback.onError(errorMessage);
                    }
                });
    }

    // Sign in with email and password
    public void signIn(String email, String password, AuthCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        callback.onSuccess(user);
                    } else {
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Sign in failed";
                        callback.onError(errorMessage);
                    }
                });
    }

    public void signInWithCredential(AuthCredential credential, AuthCallback callback) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Log.d(TAG, "Google sign in successful for: " + (user != null ? user.getEmail() : "null user"));
                        if (user != null) {
                            if (task.getResult().getAdditionalUserInfo() != null &&
                                    task.getResult().getAdditionalUserInfo().isNewUser()) {
                                Log.d(TAG, "New user signed up with Google (direct credential): " + user.getEmail());
                                String name = user.getDisplayName();
                                if (name == null || name.isEmpty()) {
                                    name = user.getEmail() != null ? user.getEmail().split("@")[0] : "Google User";
                                }
                                DatabaseManager.getInstance().createUserDocument(user, name, callback);
                            } else {
                                callback.onSuccess(user);
                            }
                        } else {
                            callback.onError("Google sign in succeeded but user is null.");
                        }
                    } else {
                        Exception exception = task.getException();
                        Log.w(TAG, "Google sign in failed (direct credential)", exception);
                        String errorMessage = exception != null ?
                                exception.getMessage() : "Google sign in failed";
                        callback.onError(errorMessage);
                    }
                });
    }

    public void signUpWithCredential(AuthCredential credential, String displayName, AuthCallback callback) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            if (task.getResult().getAdditionalUserInfo() != null &&
                                    task.getResult().getAdditionalUserInfo().isNewUser()) {
                                String nameToUse = displayName;
                                if (nameToUse == null || nameToUse.isEmpty()) {
                                    nameToUse = user.getDisplayName();
                                }
                                if (nameToUse == null || nameToUse.isEmpty()) {
                                    nameToUse = user.getEmail() != null ? user.getEmail().split("@")[0] : "New User";
                                }
                                if (displayName != null && !displayName.isEmpty() && !displayName.equals(user.getDisplayName())){
                                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                            .setDisplayName(displayName)
                                            .build();
                                    String finalNameToUse = nameToUse;
                                    user.updateProfile(profileUpdates).addOnCompleteListener(profileUpdateTask -> {
                                        if(profileUpdateTask.isSuccessful()){
                                            Log.d(TAG, "Firebase Auth profile updated with displayName: " + displayName);
                                        }
                                        DatabaseManager.getInstance().createUserDocument(user, finalNameToUse, callback);
                                    });
                                } else {
                                    DatabaseManager.getInstance().createUserDocument(user, nameToUse, callback);
                                }
                            } else {
                                Log.w(TAG, "signUpWithCredential called for existing user: " + user.getEmail());
                                callback.onSuccess(user);
                            }
                        }
                    } else {
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Google sign up failed";
                        callback.onError(errorMessage);
                    }
                });
    }

    public void linkGoogleAccount(AuthCredential googleCredential, AuthActionCallback callback) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.linkWithCredential(googleCredential)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Google account linked successfully");
                            callback.onSuccess();
                        } else {
                            String errorMessage = task.getException() != null ?
                                    task.getException().getMessage() : "Failed to link Google account";
                            Log.w(TAG, "Failed to link Google account", task.getException());
                            callback.onError(errorMessage);
                        }
                    });
        } else {
            callback.onError("No user signed in to link Google account");
        }
    }

    public void unlinkGoogleAccount(AuthActionCallback callback) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.unlink(GoogleAuthProvider.PROVIDER_ID)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Google account unlinked successfully");
                            callback.onSuccess();
                        } else {
                            String errorMessage = task.getException() != null ?
                                    task.getException().getMessage() : "Failed to unlink Google account";
                            callback.onError(errorMessage);
                        }
                    });
        } else {
            callback.onError("No user signed in");
        }
    }

    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    public boolean isSignedIn() {
        return mAuth.getCurrentUser() != null;
    }

    public void updateProfile(String name, String photoUrl, AuthActionCallback callback) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            UserProfileChangeRequest.Builder profileBuilder = new UserProfileChangeRequest.Builder();

            if (name != null && !name.isEmpty()) {
                profileBuilder.setDisplayName(name);
            }

            if (photoUrl != null) { // Allow empty string to clear photo
                profileBuilder.setPhotoUri(android.net.Uri.parse(photoUrl));
            }

            UserProfileChangeRequest profileUpdates = profileBuilder.build();

            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Create a Map to hold updates for Firestore
                            Map<String, Object> dbUpdates = new HashMap<>();
                            if (name != null) {
                                dbUpdates.put("name", name);
                            }
                            if (photoUrl != null) {
                                dbUpdates.put("profilePicUrl", photoUrl);
                            }
                            // Call the updated method in DatabaseManager
                            DatabaseManager.getInstance().updateUserProfile(user.getUid(), dbUpdates, callback);
                        } else {
                            String errorMessage = task.getException() != null ?
                                    task.getException().getMessage() : "Profile update failed";
                            callback.onError(errorMessage);
                        }
                    });
        } else {
            callback.onError("No user signed in");
        }
    }


    public void changePassword(String newPassword, AuthActionCallback callback) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.updatePassword(newPassword)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            callback.onSuccess();
                        } else {
                            String errorMessage = task.getException() != null ?
                                    task.getException().getMessage() : "Password change failed";
                            callback.onError(errorMessage);
                        }
                    });
        } else {
            callback.onError("No user signed in");
        }
    }

    public void sendPasswordResetEmail(String email, AuthActionCallback callback) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Failed to send reset email";
                        callback.onError(errorMessage);
                    }
                });
    }

    public void verifyCurrentPassword(String currentPassword, AuthActionCallback callback) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            AuthCredential credential = com.google.firebase.auth.EmailAuthProvider
                    .getCredential(user.getEmail(), currentPassword);

            user.reauthenticate(credential)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User re-authenticated successfully");
                            callback.onSuccess();
                        } else {
                            String errorMessage = task.getException() != null ?
                                    task.getException().getMessage() : "Current password is incorrect";
                            callback.onError(errorMessage);
                        }
                    });
        } else {
            callback.onError("No user signed in or user email not available");
        }
    }

    public void changePasswordWithVerification(String currentPassword, String newPassword, AuthActionCallback callback) {
        verifyCurrentPassword(currentPassword, new AuthActionCallback() {
            @Override
            public void onSuccess() {
                changePassword(newPassword, callback);
            }

            @Override
            public void onError(String error) {
                callback.onError("Verification failed: " + error);
            }
        });
    }

    public void signOut() {
        mAuth.signOut();
        Log.d(TAG, "User signed out successfully");
    }

    public void signInWithGoogleAndCheckLinking(AuthCredential credential, String emailFromGoogle, AuthCallback callback) {
        Log.d(TAG, "Starting Google authentication for email: " + emailFromGoogle);

        mAuth.fetchSignInMethodsForEmail(emailFromGoogle)
                .addOnCompleteListener(fetchTask -> {
                    if (fetchTask.isSuccessful()) {
                        List<String> providers = fetchTask.getResult().getSignInMethods();

                        if (providers == null || providers.isEmpty()) {
                            Log.d(TAG, "No providers found for " + emailFromGoogle + ". Proceeding with Google sign-in.");
                            proceedWithGoogleSignIn(credential, callback);
                        } else if (providers.contains(GoogleAuthProvider.PROVIDER_ID)) {
                            Log.d(TAG, "Email " + emailFromGoogle + " already has Google provider. Proceeding with Google sign-in.");
                            proceedWithGoogleSignIn(credential, callback);
                        } else if (providers.contains(EmailAuthProvider.PROVIDER_ID)) {
                            Log.d(TAG, "Email " + emailFromGoogle + " has Password provider but no Google provider. Needs linking.");
                            callback.onError("NEEDS_LINKING:" + emailFromGoogle);
                        } else {
                            Log.w(TAG, "Email " + emailFromGoogle + " exists with other providers: " + providers.toString());
                            callback.onError("Tài khoản tồn tại với phương thức đăng nhập khác. Vui lòng sử dụng phương thức đó.");
                        }
                    } else {
                        Log.e(TAG, "Error fetching sign-in methods for " + emailFromGoogle, fetchTask.getException());
                        callback.onError("Không thể kiểm tra phương thức đăng nhập: " + (fetchTask.getException() != null ? fetchTask.getException().getMessage() : "Lỗi không xác định"));
                    }
                });
    }

    private void proceedWithGoogleSignIn(AuthCredential credential, AuthCallback callback) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Log.d(TAG, "Google sign in successful for: " + user.getEmail());
                            if (task.getResult().getAdditionalUserInfo() != null &&
                                    task.getResult().getAdditionalUserInfo().isNewUser()) {
                                Log.d(TAG, "New user signed up with Google: " + user.getEmail());
                                String name = user.getDisplayName();
                                if (name == null || name.isEmpty()) {
                                    name = user.getEmail() != null ? user.getEmail().split("@")[0] : "Google User";
                                }
                                DatabaseManager.getInstance().createUserDocument(user, name, callback);
                            } else {
                                callback.onSuccess(user);
                            }
                        } else {
                            callback.onError("Google sign in succeeded but user is null.");
                        }
                    } else {
                        Exception exception = task.getException();
                        String errorMessage = exception != null ?
                                exception.getMessage() : "Google sign in failed";
                        Log.e(TAG, "Google sign in failed: " + errorMessage, exception);
                        callback.onError(errorMessage);
                    }
                });
    }

    public void linkGoogleAccountWithPassword(String email, String password, AuthCredential googleCredential, AuthCallback callback) {
        Log.d(TAG, "Attempting to link Google account with password for: " + email);

        signIn(email, password, new AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                Log.d(TAG, "Email/password authentication successful, now linking Google account to user: " + user.getEmail());
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser == null) {
                    callback.onError("Không tìm thấy người dùng hiện tại để liên kết.");
                    return;
                }

                currentUser.linkWithCredential(googleCredential)
                        .addOnCompleteListener(linkTask -> {
                            if (linkTask.isSuccessful()) {
                                Log.d(TAG, "Google account linked successfully to " + currentUser.getEmail());
                                callback.onSuccess(mAuth.getCurrentUser());
                            } else {
                                Log.e(TAG, "Failed to link Google account: " + linkTask.getException().getMessage(), linkTask.getException());
                                callback.onError("Không thể liên kết tài khoản Google: " + linkTask.getException().getMessage());
                            }
                        });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Email/password authentication failed during linking process: " + error);
                callback.onError("Mật khẩu không đúng hoặc lỗi xác thực: " + error);
            }
        });
    }

    // Link Phone number to existing Google account
    public void linkPhoneAccount(PhoneAuthCredential phoneCredential, AuthActionCallback callback) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.linkWithCredential(phoneCredential)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Phone number linked successfully");
                            callback.onSuccess();
                        } else {
                            String errorMessage = task.getException() != null ?
                                    task.getException().getMessage() : "Failed to link phone number";
                            Log.w(TAG, "Failed to link phone number", task.getException());
                            callback.onError(errorMessage);
                        }
                    });
        } else {
            callback.onError("No user signed in to link phone number");
        }
    }

    // Check if user has multiple providers linked
    public boolean hasMultipleProviders() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            List<? extends UserInfo> providers = user.getProviderData();
            // Filter out firebase provider (it's always there)
            return providers.stream()
                    .filter(provider -> !provider.getProviderId().equals("firebase"))
                    .count() > 1;
        }
        return false;
    }

    // Get list of linked providers
    public List<String> getLinkedProviders() {
        FirebaseUser user = mAuth.getCurrentUser();
        List<String> providers = new ArrayList<>();
        if (user != null) {
            for (UserInfo userInfo : user.getProviderData()) {
                if (!userInfo.getProviderId().equals("firebase")) {
                    providers.add(userInfo.getProviderId());
                }
            }
        }
        return providers;
    }

    // Unlink phone number
    public void unlinkPhoneAccount(AuthActionCallback callback) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.unlink("phone")
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Phone number unlinked successfully");
                            callback.onSuccess();
                        } else {
                            String errorMessage = task.getException() != null ?
                                    task.getException().getMessage() : "Failed to unlink phone number";
                            callback.onError(errorMessage);
                        }
                    });
        } else {
            callback.onError("No user signed in");
        }
    }
}
