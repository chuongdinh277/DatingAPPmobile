package com.example.couple_app.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;

import com.example.couple_app.R;
import com.example.couple_app.managers.AuthManager;
import com.example.couple_app.managers.DatabaseManager;
import com.example.couple_app.models.User;
import com.example.couple_app.utils.AvatarCache;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

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

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingProfileActivity extends BaseActivity {
    private static final String TAG = "SettingProfileActivity";

    private CircleImageView ivUserImage;
    private EditText etUsername, etDob, etStartLoveDate, etEmail, etPhone;
    private ImageView ivBack, ivEdit, ivEditAvatar;
    private MaterialButton btSaveChanges;
    private RadioGroup rgGender;
    private RadioButton rbMale, rbFemale, rbOther;

    private ActivityResultLauncher<String> pickImageLauncher;
    private Bitmap newAvatarBitmap = null;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private DatabaseManager dbManager;
    private AuthManager authManager;
    private FirebaseUser currentUser;
    private User currentUserData;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_profile);

        initViews();
        initManagers();

        currentUser = authManager.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupImagePicker();
        setupClickListeners();
        loadUserProfile();
        setEditingState(false); // Start in view-only mode
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
    }

    private void initManagers() {
        dbManager = DatabaseManager.getInstance();
        authManager = AuthManager.getInstance();
    }

    private void setEditingState(boolean isEditing) {
        etUsername.setEnabled(isEditing);
        etDob.setEnabled(isEditing);
        etStartLoveDate.setEnabled(isEditing);

        for (int i = 0; i < rgGender.getChildCount(); i++) {
            rgGender.getChildAt(i).setEnabled(isEditing);
        }

        ivEdit.setVisibility(isEditing ? View.GONE : View.VISIBLE);
        btSaveChanges.setVisibility(isEditing ? View.VISIBLE : View.GONE);
        ivEditAvatar.setVisibility(isEditing ? View.VISIBLE : View.INVISIBLE);
    }

    private void setupImagePicker() {
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                try {
                    newAvatarBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                    ivUserImage.setImageBitmap(newAvatarBitmap);
                } catch (Exception e) {
                    Log.e(TAG, "Error loading image from URI", e);
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> onBackPressed());
        ivEdit.setOnClickListener(v -> setEditingState(true));
        ivEditAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        btSaveChanges.setOnClickListener(v -> saveProfileChanges());

        etDob.setOnClickListener(v -> {
            if (etDob.isEnabled()) showDatePickerDialog(etDob);
        });
        etStartLoveDate.setOnClickListener(v -> {
            if (etStartLoveDate.isEnabled()) showDatePickerDialog(etStartLoveDate);
        });
    }

    private void showDatePickerDialog(final EditText editText) {
        Calendar calendar = Calendar.getInstance();
        String currentDateStr = editText.getText().toString();
        if (!currentDateStr.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date date = sdf.parse(currentDateStr);
                if (date != null) {
                    calendar.setTime(date);
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing date: ", e);
            }
        }

        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            editText.setText(sdf.format(calendar.getTime()));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadUserProfile() {
        if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
            etUsername.setText(currentUser.getDisplayName());
        }
        etEmail.setText(currentUser.getEmail());
        etPhone.setText(currentUser.getPhoneNumber());
        loadAvatar();

        dbManager.getUser(currentUser.getUid(), new DatabaseManager.DatabaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    currentUserData = user;
                    if (user.getName() != null) {
                        etUsername.setText(user.getName());
                    }
                    etDob.setText(user.getDateOfBirth());
                    etStartLoveDate.setText(formatTimestamp(user.getStartLoveDate()));
                    if (user.getGender() != null) {
                        switch (user.getGender()) {
                            case "Male":
                                rbMale.setChecked(true);
                                break;
                            case "Female":
                                rbFemale.setChecked(true);
                                break;
                            case "Other":
                                rbOther.setChecked(true);
                                break;
                        }
                    }
                }
            }
            @Override
            public void onError(String error) {
                Log.w(TAG, "Could not load additional user details: " + error);
            }
        });
    }

    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) return "";
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(timestamp.toDate());
    }

    private void loadAvatar() {
        Bitmap cachedAvatar = AvatarCache.getCachedBitmap(this);
        if (cachedAvatar != null) {
            ivUserImage.setImageBitmap(cachedAvatar);
        } else {
            dbManager.getUser(currentUser.getUid(), new DatabaseManager.DatabaseCallback<User>() {
                @Override
                public void onSuccess(User user) {
                    if (user != null && !TextUtils.isEmpty(user.getProfilePicUrl())) {
                        loadImageFromUrl(user.getProfilePicUrl());
                    } else if (currentUser.getPhotoUrl() != null) {
                        loadImageFromUrl(currentUser.getPhotoUrl().toString());
                    }
                }
                @Override
                public void onError(String error) {
                    Log.w(TAG, "Failed to get user data for avatar: " + error);
                }
            });
        }
    }

    private void loadImageFromUrl(String urlStr) {
        ioExecutor.execute(() -> {
            try {
                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap bmp = BitmapFactory.decodeStream(input);
                if (bmp != null) {
                    AvatarCache.saveBitmapToCache(getApplicationContext(), bmp);
                    mainHandler.post(() -> ivUserImage.setImageBitmap(bmp));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading image from URL", e);
            }
        });
    }

    private void saveProfileChanges() {
        String newUsername = etUsername.getText().toString().trim();
        if (TextUtils.isEmpty(newUsername)) {
            etUsername.setError("Username cannot be empty");
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        String newDobStr = etDob.getText().toString().trim();
        String newStartLoveDateStr = etStartLoveDate.getText().toString().trim();
        int selectedGenderId = rgGender.getCheckedRadioButtonId();
        String newGender = "";
        if (selectedGenderId == R.id.rb_male) {
            newGender = "Male";
        } else if (selectedGenderId == R.id.rb_female) {
            newGender = "Female";
        } else if (selectedGenderId == R.id.rb_other) {
            newGender = "Other";
        }

        if (newAvatarBitmap != null) {
            uploadAvatarAndThenUpdateAllData(progressDialog, newUsername, newDobStr, newStartLoveDateStr, newGender);
        } else {
            updateAllData(progressDialog, newUsername, newDobStr, newStartLoveDateStr, newGender, null);
        }
    }

    private void uploadAvatarAndThenUpdateAllData(ProgressDialog dialog, String username, String dob, String startLoveDate, String gender) {
        String apiKey = getString(R.string.imgbb_api_key);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        newAvatarBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
        String base64Image = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

        ioExecutor.execute(() -> {
            try {
                URL url = new URL("https://api.imgbb.com/1/upload?key=" + apiKey);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                String body = "image=" + URLEncoder.encode(base64Image, "UTF-8");
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.getBytes());
                }

                InputStream is = conn.getResponseCode() == 200 ? conn.getInputStream() : conn.getErrorStream();
                ByteArrayOutputStream result = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) != -1) result.write(buffer, 0, length);
                String responseStr = result.toString("UTF-8");

                if (conn.getResponseCode() == 200) {
                    String imageUrl = new JSONObject(responseStr).getJSONObject("data").getString("url");
                    updateAllData(dialog, username, dob, startLoveDate, gender, imageUrl);
                } else {
                    throw new Exception("Upload failed: " + responseStr);
                }
            } catch (Exception e) {
                Log.e(TAG, "Avatar upload failed", e);
                mainHandler.post(() -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Avatar upload failed.", Toast.LENGTH_LONG).show();
                    setEditingState(true);
                });
            }
        });
    }

    private void updateAllData(ProgressDialog dialog, String username, String dobStr, String startLoveDateStr, String gender, @Nullable String avatarUrl) {
        authManager.updateProfile(username, avatarUrl, new AuthManager.AuthActionCallback() {
            @Override
            public void onSuccess() {
                updateFirestoreDatabase(dialog, username, dobStr, startLoveDateStr, gender, avatarUrl);
            }
            @Override
            public void onError(String error) {
                dialog.dismiss();
                Toast.makeText(SettingProfileActivity.this, "Failed to update auth profile: " + error, Toast.LENGTH_LONG).show();
                setEditingState(true);
            }
        });
    }

    private Timestamp convertStringToTimestamp(String dateStr) {
        if (TextUtils.isEmpty(dateStr)) return null;
        try {
            return new Timestamp(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateStr));
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date string to Timestamp", e);
            return null;
        }
    }

    private void updateFirestoreDatabase(ProgressDialog dialog, String username, String dobStr, String startLoveDateStr, String gender, @Nullable String avatarUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", username);
        updates.put("dateOfBirth", dobStr);
        updates.put("startLoveDate", convertStringToTimestamp(startLoveDateStr));
        updates.put("gender", gender);
        if (avatarUrl != null) updates.put("profilePicUrl", avatarUrl);

        dbManager.updateUserProfile(currentUser.getUid(), updates, new AuthManager.AuthActionCallback() {
            @Override
            public void onSuccess() {
                updatePartnerProfile(dialog, updates);
            }
            @Override
            public void onError(String error) {
                dialog.dismiss();
                Toast.makeText(SettingProfileActivity.this, "Failed to update database: " + error, Toast.LENGTH_LONG).show();
                setEditingState(true);
            }
        });
    }

    private void updatePartnerProfile(ProgressDialog dialog, Map<String, Object> originalUpdates) {
        if (currentUserData != null && !TextUtils.isEmpty(currentUserData.getPartnerId()) && originalUpdates.containsKey("startLoveDate")) {
            Map<String, Object> partnerUpdates = new HashMap<>();
            partnerUpdates.put("startLoveDate", originalUpdates.get("startLoveDate"));

            dbManager.updateUserProfile(currentUserData.getPartnerId(), partnerUpdates, new AuthManager.AuthActionCallback() {
                @Override
                public void onSuccess() {
                    handleSuccessfulUpdate(dialog);
                }
                @Override
                public void onError(String error) {
                    dialog.dismiss();
                    Toast.makeText(SettingProfileActivity.this, "Updated your profile, but failed to update partner's profile: " + error, Toast.LENGTH_LONG).show();
                    setEditingState(true);
                }
            });
        } else {
            handleSuccessfulUpdate(dialog);
        }
    }

    private void handleSuccessfulUpdate(ProgressDialog dialog) {
        // Clear old avatar cache before saving new one
        AvatarCache.clearCache(getApplicationContext());

        if (newAvatarBitmap != null) {
            AvatarCache.saveBitmapToCache(getApplicationContext(), newAvatarBitmap);
        }
        dialog.dismiss();
        Toast.makeText(SettingProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
        setEditingState(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!ioExecutor.isShutdown()) {
            ioExecutor.shutdown();
        }
    }
}