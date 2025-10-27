package com.example.btl_mobileapp.activities;

// Import Android core libraries
import android.Manifest; // Needed for permissions if you add them later
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent; // If needed for navigation later
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
// Removed: import android.widget.ImageButton; // Not used directly
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
// Removed: import android.widget.TextView; // Not used directly
import android.widget.Toast;

// Import androidx libraries
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;

// Import Firebase libraries
import com.example.btl_mobileapp.utils.DateUtils;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

// Import Google Material library
import com.google.android.material.button.MaterialButton;

// Import project-specific classes
import com.example.btl_mobileapp.R;
import com.example.btl_mobileapp.managers.AuthManager;
import com.example.btl_mobileapp.managers.DatabaseManager;
import com.example.btl_mobileapp.models.User;
import com.example.btl_mobileapp.utils.AvatarCache;
import com.example.btl_mobileapp.viewmodels.UserProfileData;

// Import JSON library
import org.json.JSONObject;

// Import Java utilities
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Import CircleImageView library
import de.hdodenhof.circleimageview.CircleImageView;

public class SettingProfileActivity extends BaseActivity {
    private static final String TAG = "SettingProfileActivity";

    // Views
    private CircleImageView ivUserImage;
    private EditText etUsername, etDob, etStartLoveDate, etEmail, etPhone;
    private ImageView ivBack, ivEdit, ivEditAvatar;
    private MaterialButton btSaveChanges;
    private RadioGroup rgGender;
    private RadioButton rbMale, rbFemale, rbOther;

    // Image Picking
    private ActivityResultLauncher<String> pickImageLauncher;
    private Bitmap newAvatarBitmap = null; // Stores the newly selected bitmap for upload

    // Threading
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    // Managers and Firebase
    private DatabaseManager dbManager;
    private AuthManager authManager;
    private FirebaseUser currentUser;
    private User currentUserData; // Stores the loaded User object from Firestore

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_profile);

        initViews();
        initManagers();

        currentUser = authManager.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            // Redirect to login or finish
            // Example: startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setupImagePicker();
        setupClickListeners();
        loadUserProfile();
        setEditingState(false); // Start in view-only mode initially
    }

    private void initViews() {
        ivBack = findViewById(R.id.iv_back);
        ivEdit = findViewById(R.id.iv_edit);
        ivUserImage = findViewById(R.id.iv_user_image);
        ivEditAvatar = findViewById(R.id.iv_edit_avatar);
        btSaveChanges = findViewById(R.id.bt_save_changes);

        etUsername = findViewById(R.id.et_username);
        etDob = findViewById(R.id.et_dob);
        etStartLoveDate = findViewById(R.id.et_start_love_date);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);

        rgGender = findViewById(R.id.rg_gender);
        rbMale = findViewById(R.id.rb_male);
        rbFemale = findViewById(R.id.rb_female);
        rbOther = findViewById(R.id.rb_other);

        // Disable email and phone editing (as they usually require verification)
        etEmail.setEnabled(false);
        etPhone.setEnabled(false);
    }

    private void initManagers() {
        dbManager = DatabaseManager.getInstance();
        authManager = AuthManager.getInstance();
    }

    // Toggles the editability of profile fields and button visibility
    private void setEditingState(boolean isEditing) {
        Log.d(TAG, "Setting editing state to: " + isEditing);
        etUsername.setEnabled(isEditing);
        etDob.setEnabled(isEditing);
        etStartLoveDate.setEnabled(isEditing);

        // Enable/disable radio buttons
        for (int i = 0; i < rgGender.getChildCount(); i++) {
            rgGender.getChildAt(i).setEnabled(isEditing);
        }

        // Show/hide Edit vs Save buttons
        ivEdit.setVisibility(isEditing ? View.GONE : View.VISIBLE);
        btSaveChanges.setVisibility(isEditing ? View.VISIBLE : View.GONE);
        ivEditAvatar.setVisibility(isEditing ? View.VISIBLE : View.INVISIBLE);

        // Reset temporary bitmap if canceling edit mode
        if (!isEditing) {
            newAvatarBitmap = null;
            // Optionally reload original avatar if user selected a new one but cancelled
            loadAvatar();
        }
    }

    // Initializes the ActivityResultLauncher for picking images
    private void setupImagePicker() {
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                try {
                    // Load bitmap from URI and store temporarily
                    newAvatarBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                    // Display the selected image immediately for preview
                    ivUserImage.setImageBitmap(newAvatarBitmap);
                    Log.d(TAG, "New avatar selected and previewed.");
                } catch (Exception e) {
                    Log.e(TAG, "Error loading image from URI", e);
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    newAvatarBitmap = null; // Reset on error
                }
            } else {
                Log.d(TAG, "No image selected from picker.");
            }
        });
    }

    // Sets up OnClickListeners for interactive elements
    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> onBackPressed()); // Standard back behavior
        ivEdit.setOnClickListener(v -> setEditingState(true)); // Switch to edit mode
        ivEditAvatar.setOnClickListener(v -> {
            Log.d(TAG, "Edit avatar clicked, launching image picker.");
            pickImageLauncher.launch("image/*"); // Launch gallery picker
        });
        btSaveChanges.setOnClickListener(v -> {
            Log.d(TAG, "Save changes clicked.");
            saveProfileChanges(); // Initiate save process
        });

        // Date Picker listeners for DOB and Start Love Date fields
        etDob.setOnClickListener(v -> {
            if (etDob.isEnabled()) showDatePickerDialog(etDob);
        });
        etStartLoveDate.setOnClickListener(v -> {
            if (etStartLoveDate.isEnabled()) showDatePickerDialog(etStartLoveDate);
        });
    }

    // Displays a DatePickerDialog, pre-filled with the date currently in the EditText
    private void showDatePickerDialog(final EditText editText) {
        Calendar calendar = Calendar.getInstance();
        String currentDateStr = editText.getText().toString();
        // Try to parse existing date to set initial picker date
        if (!currentDateStr.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date date = sdf.parse(currentDateStr);
                if (date != null) {
                    calendar.setTime(date);
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing date: " + currentDateStr, e);
                // Use current date if parsing fails
            }
        }

        // Create and show DatePickerDialog
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            // Format selected date and set it to the EditText
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            editText.setText(sdf.format(calendar.getTime()));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    // Loads user profile from Auth and Firestore, populating the UI fields
    private void loadUserProfile() {
        Log.d(TAG, "Loading user profile...");
        // Set basic info from Auth (can be overwritten by Firestore later)
        if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
            etUsername.setText(currentUser.getDisplayName());
        }
        etEmail.setText(currentUser.getEmail());
        etPhone.setText(currentUser.getPhoneNumber()); // Might be null

        // Load avatar (handles cache/Firestore/Auth)
        loadAvatar();

        // Fetch detailed profile from Firestore
        dbManager.getUser(currentUser.getUid(), new DatabaseManager.DatabaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    Log.d(TAG, "Firestore user data loaded.");
                    currentUserData = user; // Store the full User object

                    // Use Firestore name if available
                    if (user.getName() != null && !user.getName().isEmpty()) {
                        etUsername.setText(user.getName());
                    }
                    etDob.setText(user.getDateOfBirth());
                    etStartLoveDate.setText(formatTimestamp(user.getStartLoveDate()));

                    // Set gender radio button
                    String gender = user.getGender();
                    if (gender != null) {
                        if ("Male".equals(gender)) rbMale.setChecked(true);
                        else if ("Female".equals(gender)) rbFemale.setChecked(true);
                        else if ("Other".equals(gender)) rbOther.setChecked(true);
                        else rgGender.clearCheck();
                    } else {
                        rgGender.clearCheck();
                    }
                } else {
                    Log.w(TAG, "Firestore user data is null for UID: " + currentUser.getUid());
                }
            }
            @Override
            public void onError(String error) {
                Log.w(TAG, "Could not load additional user details from Firestore: " + error);
                Toast.makeText(SettingProfileActivity.this, "Could not load profile details.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Safely formats a Firebase Timestamp to "dd/MM/yyyy" string
    private String formatTimestamp(@Nullable Timestamp timestamp) {
        if (timestamp == null) return "";
        try {
            return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(timestamp.toDate());
        } catch (Exception e) {
            Log.e(TAG, "Error formatting timestamp", e);
            return ""; // Return empty string on formatting error
        }
    }

    // Loads the avatar image, prioritizing cache -> Firestore URL -> Auth URL -> Default
    private void loadAvatar() {
        Log.d(TAG, "Loading avatar...");
        Bitmap cachedAvatar = AvatarCache.getCachedBitmap(this);
        if (cachedAvatar != null) {
            ivUserImage.setImageBitmap(cachedAvatar);
            Log.d(TAG, "Avatar loaded from file cache.");
        } else {
            Log.d(TAG, "Avatar cache miss, checking Firestore/Auth...");
            // Need to fetch user data to get the Firestore URL
            dbManager.getUser(currentUser.getUid(), new DatabaseManager.DatabaseCallback<User>() {
                @Override
                public void onSuccess(User user) {
                    String urlToLoad = null;
                    if (user != null && !TextUtils.isEmpty(user.getProfilePicUrl())) {
                        urlToLoad = user.getProfilePicUrl(); // Priority 1: Firestore URL
                        Log.d(TAG, "Found avatar URL in Firestore.");
                    } else if (currentUser.getPhotoUrl() != null) {
                        urlToLoad = currentUser.getPhotoUrl().toString(); // Priority 2: Auth URL
                        Log.d(TAG, "Found avatar URL in Firebase Auth.");
                    }

                    if (urlToLoad != null) {
                        loadImageFromUrl(urlToLoad); // Load using the found URL
                    } else {
                        Log.d(TAG, "No avatar URL found, using default.");
                        if (ivUserImage != null) ivUserImage.setImageResource(R.drawable.ic_default_avatar);
                    }
                }
                @Override
                public void onError(String error) {
                    // Try Auth URL even if Firestore fails
                    Log.w(TAG, "Failed to get user data for avatar URL: " + error + ". Trying Auth URL.");
                    if (currentUser.getPhotoUrl() != null) {
                        loadImageFromUrl(currentUser.getPhotoUrl().toString());
                    } else {
                        Log.d(TAG, "No avatar URL in Auth either, using default.");
                        if (ivUserImage != null) ivUserImage.setImageResource(R.drawable.ic_default_avatar);
                    }
                }
            });
        }
    }

    // Loads image from URL using background thread, updates ImageView and saves to cache
    private void loadImageFromUrl(String urlStr) {
        if (TextUtils.isEmpty(urlStr)) {
            Log.w(TAG, "loadImageFromUrl called with empty URL.");
            if (ivUserImage != null) ivUserImage.setImageResource(R.drawable.ic_default_avatar);
            return;
        }
        Log.d(TAG, "Attempting to load image from URL: " + urlStr);
        ioExecutor.execute(() -> {
            Bitmap bmp = null;
            // Simplified loading logic (consider using Glide for robustness)
            try (InputStream in = new java.net.URL(urlStr).openStream()) {
                bmp = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e(TAG, "Error loading image from URL: " + urlStr, e);
            }

            // Update UI and Cache on Main Thread
            if (bmp != null) {
                final Bitmap finalBmp = bmp;
                Log.d(TAG, "Image loaded successfully from URL.");
                AvatarCache.saveBitmapToCache(getApplicationContext(), finalBmp);
                mainHandler.post(() -> {
                    if (ivUserImage != null) ivUserImage.setImageBitmap(finalBmp);
                });
            } else {
                Log.w(TAG, "Failed to decode bitmap from URL: " + urlStr);
                // Optionally set default on failure
                // mainHandler.post(() -> { if (ivUserImage != null) ivUserImage.setImageResource(R.drawable.ic_default_avatar); });
            }
        });
    }

    // --- Saving Profile Changes Logic ---

    private void saveProfileChanges() {
        // Validate Username
        String newUsername = etUsername.getText().toString().trim();
        if (TextUtils.isEmpty(newUsername)) {
            etUsername.setError("Username cannot be empty");
            return;
        }

        Log.d(TAG, "Initiating save profile changes...");
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Gather updated data from UI
        String newDobStr = etDob.getText().toString().trim();
        String newStartLoveDateStr = etStartLoveDate.getText().toString().trim();
        int selectedGenderId = rgGender.getCheckedRadioButtonId();
        String newGender = "";
        if (selectedGenderId == R.id.rb_male) newGender = "Male";
        else if (selectedGenderId == R.id.rb_female) newGender = "Female";
        else if (selectedGenderId == R.id.rb_other) newGender = "Other";

        // Prepare Firestore update map (startLoveDate converted here)
        Map<String, Object> firestoreUpdates = new HashMap<>();
        firestoreUpdates.put("name", newUsername);
        firestoreUpdates.put("dateOfBirth", newDobStr);
        firestoreUpdates.put("startLoveDate", convertStringToTimestamp(newStartLoveDateStr)); // Convert now
        firestoreUpdates.put("gender", newGender);

        // Decide flow based on whether a new avatar was selected
        if (newAvatarBitmap != null) {
            Log.d(TAG, "New avatar selected, starting upload...");
            // Upload avatar first, then add URL to updates and save all
            uploadAvatarAndThenUpdateAllData(progressDialog, firestoreUpdates);
        } else {
            Log.d(TAG, "No new avatar, updating data directly.");
            // No avatar change, proceed to update Auth (without URL) and then Firestore
            updateAuthProfile(progressDialog, firestoreUpdates, null);
        }
    }

    // Uploads avatar to ImgBB, adds URL to firestoreUpdates map, then calls updateAuthProfile
    private void uploadAvatarAndThenUpdateAllData(ProgressDialog dialog, Map<String, Object> firestoreUpdates) {
        String apiKey = getString(R.string.imgbb_api_key);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        newAvatarBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
        String base64Image = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

        Log.d(TAG, "Uploading image to ImgBB...");
        ioExecutor.execute(() -> {
            HttpURLConnection conn = null;
            String responseStr = null;
            int responseCode = -1;
            try {
                // ImgBB Upload Request
                URL url = new URL("https://api.imgbb.com/1/upload?key=" + apiKey);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(20000); conn.setReadTimeout(30000);
                String body = "image=" + URLEncoder.encode(base64Image, "UTF-8");
                try (OutputStream os = conn.getOutputStream()) { os.write(body.getBytes()); }

                // Read Response
                responseCode = conn.getResponseCode();
                InputStream is = (responseCode == HttpURLConnection.HTTP_OK) ? conn.getInputStream() : conn.getErrorStream();
                ByteArrayOutputStream result = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024]; int length;
                while ((length = is.read(buffer)) != -1) result.write(buffer, 0, length);
                responseStr = result.toString("UTF-8");
                if (is != null) is.close();

                // Process Response
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "ImgBB upload successful.");
                    String imageUrl = new JSONObject(responseStr).getJSONObject("data").getString("url");
                    Log.d(TAG, "ImgBB Image URL: " + imageUrl);
                    firestoreUpdates.put("profilePicUrl", imageUrl); // Add URL to the map
                    mainHandler.post(() -> updateAuthProfile(dialog, firestoreUpdates, imageUrl)); // Proceed on main thread
                } else {
                    throw new Exception("ImgBB Upload failed (" + responseCode + "): " + responseStr);
                }
            } catch (Exception e) {
                Log.e(TAG, "ImgBB Avatar upload failed", e);
                mainHandler.post(() -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Avatar upload failed.", Toast.LENGTH_LONG).show();
                    // Keep editing enabled for user retry
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    // Updates Firebase Authentication profile (DisplayName, PhotoUrl)
    private void updateAuthProfile(ProgressDialog dialog, Map<String, Object> firestoreUpdates, @Nullable String avatarUrl) {
        Log.d(TAG, "Updating Firebase Auth profile...");
        String username = (String) firestoreUpdates.get("name");

        authManager.updateProfile(username, avatarUrl, new AuthManager.AuthActionCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Firebase Auth profile updated.");
                updateFirestoreDatabase(dialog, firestoreUpdates); // Proceed to Firestore update
            }
            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to update Firebase Auth profile: " + error);
                dialog.dismiss();
                Toast.makeText(SettingProfileActivity.this, "Failed to update auth profile: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    // Converts "dd/MM/yyyy" string to Firebase Timestamp
    @Nullable // Indicate that null can be returned
    private Timestamp convertStringToTimestamp(String dateStr) {
        if (TextUtils.isEmpty(dateStr)) return null;
        try {
            Date parsedDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateStr);
            return new Timestamp(parsedDate); // Convert java.util.Date to Firebase Timestamp
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date string '" + dateStr + "' to Timestamp", e);
            mainHandler.post(() -> Toast.makeText(this, "Định dạng ngày không hợp lệ: " + dateStr, Toast.LENGTH_SHORT).show());
            return null; // Return null on failure
        }
    }

    // Updates the user document in Firestore
    private void updateFirestoreDatabase(ProgressDialog dialog, Map<String, Object> updates) {
        Log.d(TAG, "Updating Firestore database for user: " + currentUser.getUid());
        dbManager.updateUserProfile(currentUser.getUid(), updates, new AuthManager.AuthActionCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Firestore database updated.");
                // Update local currentUserData object AFTER successful DB update
                if (currentUserData != null) {
                    // Manually update fields based on the 'updates' map
                    currentUserData.setName((String) updates.get("name"));
                    currentUserData.setDateOfBirth((String) updates.get("dateOfBirth"));
                    currentUserData.setStartLoveDate((Timestamp) updates.get("startLoveDate"));
                    currentUserData.setGender((String) updates.get("gender"));
                    if (updates.containsKey("profilePicUrl")) {
                        currentUserData.setProfilePicUrl((String) updates.get("profilePicUrl"));
                    }
                }
                updatePartnerProfile(dialog, updates); // Proceed to update partner if needed
            }
            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to update Firestore database: " + error);
                dialog.dismiss();
                Toast.makeText(SettingProfileActivity.this, "Failed to update database: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    // Updates partner's startLoveDate in Firestore if applicable
    private void updatePartnerProfile(ProgressDialog dialog, Map<String, Object> originalUpdates) {
        // Check if currentUserData is loaded, has a partner, and startLoveDate was changed
        if (currentUserData != null && !TextUtils.isEmpty(currentUserData.getPartnerId()) && originalUpdates.containsKey("startLoveDate")) {
            Log.d(TAG, "Updating partner's startLoveDate for partner ID: " + currentUserData.getPartnerId());
            Map<String, Object> partnerUpdates = new HashMap<>();
            partnerUpdates.put("startLoveDate", originalUpdates.get("startLoveDate")); // Only sync start date

            dbManager.updateUserProfile(currentUserData.getPartnerId(), partnerUpdates, new AuthManager.AuthActionCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Partner's startLoveDate updated successfully.");
                    handleSuccessfulUpdate(dialog, originalUpdates); // Both successful
                }
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Failed to update partner's startLoveDate: " + error);
                    mainHandler.post(() -> Toast.makeText(SettingProfileActivity.this, "Updated your profile, but failed to sync start date with partner.", Toast.LENGTH_LONG).show());
                    handleSuccessfulUpdate(dialog, originalUpdates); // Still handle success for main user
                }
            });
        } else {
            Log.d(TAG, "No partner update needed (no partner or start date unchanged).");
            handleSuccessfulUpdate(dialog, originalUpdates); // Proceed directly
        }
    }

    // Handles final steps after successful updates (updates ViewModel, cache, UI)
    private void handleSuccessfulUpdate(ProgressDialog dialog, Map<String, Object> originalUpdates) {
        Log.d(TAG, "Handling successful profile update completion.");

        // --- Update UserProfileData ViewModel ---
        UserProfileData profileData = UserProfileData.getInstance();

        // 1. Update Avatar Bitmap and URL in ViewModel and File Cache
        String finalAvatarUrl = null;
        if (originalUpdates.containsKey("profilePicUrl")) { // If a new URL was uploaded
            finalAvatarUrl = (String) originalUpdates.get("profilePicUrl");
        } else if (currentUserData != null) { // Otherwise, use the existing URL (might be null)
            finalAvatarUrl = currentUserData.getProfilePicUrl();
        }

        // Update Bitmap cache (ViewModel and File) only if a new bitmap was selected
        if (newAvatarBitmap != null) {
            profileData.setCurrentUserAvatar(newAvatarBitmap); // Update ViewModel Bitmap
            AvatarCache.saveBitmapToCache(getApplicationContext(), newAvatarBitmap); // Update file cache
            Log.d(TAG, "Updated ViewModel and file cache with new avatar bitmap.");
        }
        // Always update the URL in the ViewModel to reflect the latest state
        profileData.setCurrentUserProfilePicUrl(finalAvatarUrl);
        Log.d(TAG, "Updated ViewModel with avatar URL: " + finalAvatarUrl);


        // 2. Update Other Info in ViewModel
        String updatedName = (String) originalUpdates.get("name");
        String updatedDob = (String) originalUpdates.get("dateOfBirth");
        Timestamp updatedStartLoveDate = (Timestamp) originalUpdates.get("startLoveDate"); // Already Timestamp

        profileData.setCurrentUserName(updatedName);
        profileData.setCurrentUserDateOfBirth(updatedDob);
        profileData.setCurrentUserStartLoveDate(updatedStartLoveDate);
        // Maybe update Age/Zodiac here too if needed, based on updatedDob
        profileData.setCurrentUserAge(DateUtils.calculateAge(updatedDob));
        profileData.setCurrentUserZodiac(DateUtils.getZodiacSign(updatedDob));
        Log.d(TAG, "Updated ViewModel with name, DOB, startLoveDate, Age, Zodiac.");


        // --- UI Updates ---
        dialog.dismiss();
        Toast.makeText(SettingProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
        setEditingState(false); // Go back to view-only mode

        // Reset the temporary bitmap holder
        newAvatarBitmap = null;
        Log.d(TAG, "Profile update complete, editing disabled.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shutdown background executor
        if (ioExecutor != null && !ioExecutor.isShutdown()) {
            ioExecutor.shutdown();
            Log.d(TAG, "IO Executor shutdown.");
        }
    }
} // End of SettingProfileActivity class