package com.example.btl_mobileapp.activities;

// Import Android core libraries
import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

// Import androidx libraries
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

// Import Glide library
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

// Import Google Material library
import com.google.android.material.floatingactionbutton.FloatingActionButton;

// Import Firebase libraries
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

// Import project-specific classes
import com.example.btl_mobileapp.R;
import com.example.btl_mobileapp.managers.DatabaseManager;
import com.example.btl_mobileapp.models.User;
import com.example.btl_mobileapp.utils.AvatarCache;
import com.example.btl_mobileapp.utils.NameCache;
import com.example.btl_mobileapp.utils.DateUtils;
import com.example.btl_mobileapp.utils.FragmentUtils;
import com.example.btl_mobileapp.viewmodels.UserProfileData;

// Import Java utilities
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HomeMain1Fragment extends Fragment {

    private static final String TAG = "HomeMain1Fragment"; // Thêm TAG để debug
    private static final String PREFS_NAME = "AppSettings"; // Tên file SharedPreferences
    private static final String PREF_BACKGROUND_URL = "saved_background_url"; // Key lưu URL nền

    private static final ExecutorService IMAGE_EXECUTOR = Executors.newFixedThreadPool(2);
    private UserProfileData profileData;

    // Love Counter Variables
    private Runnable loveCounterRunnable;
    private Timestamp startLoveDate = null;
    private final Handler loveCounterHandler = new Handler(Looper.getMainLooper());
    private TextView txtYears, txtMonths, txtWeeks, txtDays, txtTimer, txtStartDate;

    // Background Change Variables
    private ActivityResultLauncher<String[]> requestPermissionLauncher;
    private ActivityResultLauncher<PickVisualMediaRequest> pickImageLauncher;
    private ConstraintLayout rootLayout;
    private FloatingActionButton fabChangeBackground;
    private FirebaseStorage storage;
    private ProgressDialog progressDialog;
    private SharedPreferences sharedPreferences;

    public HomeMain1Fragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
        storage = FirebaseStorage.getInstance();
        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        setupActivityLaunchers();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        View root = inflater.inflate(R.layout.homemain1, container, false);

        // Find Views
        rootLayout = root.findViewById(R.id.root_homemain1);
        fabChangeBackground = root.findViewById(R.id.fab_change_background);
        txtYears = root.findViewById(R.id.txtYears);
        txtMonths = root.findViewById(R.id.txtMonths);
        txtWeeks = root.findViewById(R.id.txtWeeks);
        txtDays = root.findViewById(R.id.txtDays);
        txtTimer = root.findViewById(R.id.txtTimer);
        txtStartDate = root.findViewById(R.id.txtStartDate);

        // Initialize ViewModel and ProgressDialog
        profileData = UserProfileData.getInstance();
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setTitle("Đang tải lên");
        progressDialog.setMessage("Vui lòng đợi trong giây lát...");
        progressDialog.setCancelable(false);

        // Load saved background immediately if available
        loadSavedBackground();

        // Bind user profile data (this will also load background from DB if needed)
        bindUserProfile(root);

        // Set listener for background change button
        fabChangeBackground.setOnClickListener(v -> checkPermissionAndPickImage());

        return root;
    }

    // --- Permission and Image Picking Logic ---

    private void setupActivityLaunchers() {
        Log.d(TAG, "setupActivityLaunchers");
        // 1. Launcher for picking image using Photo Picker
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(),
                (Uri uri) -> {
                    if (uri != null && isAdded()) {
                        Log.d(TAG, "Image selected: " + uri.toString());
                        uploadImageToFirebaseStorage(uri);
                    } else {
                        Log.d(TAG, "No media selected from picker.");
                    }
                });

        // 2. Launcher for requesting permissions
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    String readImagePermission = getReadImagePermissionString(); // Helper to get correct permission name

                    boolean imagesGranted = permissions.getOrDefault(readImagePermission, false);
                    boolean selectedAccessGranted = false;

                    // Only check selected access on Android 14+ IF full access was denied
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && !imagesGranted) {
                        selectedAccessGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                                == PackageManager.PERMISSION_GRANTED;
                    }

                    if (imagesGranted) {
                        Log.d(TAG, "Permission Result: Full media access granted.");
                        launchImagePicker();
                    } else if (selectedAccessGranted) {
                        Log.d(TAG, "Permission Result: Selected Photos Access granted.");
                        launchImagePicker();
                        Toast.makeText(getContext(), "Bạn chỉ cấp quyền cho ảnh đã chọn.", Toast.LENGTH_LONG).show();
                    } else {
                        Log.d(TAG, "Permission Result: Media permissions denied.");
                        Toast.makeText(getContext(), "Bạn cần cấp quyền truy cập ảnh để đổi nền", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void launchImagePicker() {
        Log.d(TAG, "Launching image picker.");
        pickImageLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void checkPermissionAndPickImage() {
        Log.d(TAG, "checkPermissionAndPickImage called");
        if (!isAdded()) {
            Log.w(TAG, "Fragment not added, cannot pick image.");
            return;
        }

        String readImagePermission = getReadImagePermissionString();
        boolean hasReadImages = ContextCompat.checkSelfPermission(requireContext(), readImagePermission) == PackageManager.PERMISSION_GRANTED;
        boolean hasSelectedAccess = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            hasSelectedAccess = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED;
        }

        if (hasReadImages || hasSelectedAccess) {
            Log.d(TAG, "Permission available (full or selected). Launching picker.");
            launchImagePicker();
        } else {
            Log.d(TAG, "Requesting read image permission.");
            requestPermissionLauncher.launch(new String[]{readImagePermission});
        }
    }

    // Helper to get the correct permission string based on Android version
    private String getReadImagePermissionString() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            return Manifest.permission.READ_MEDIA_IMAGES;
        } else { // Android 12-
            return Manifest.permission.READ_EXTERNAL_STORAGE;
        }
    }

    // --- Firebase Storage and Firestore Logic ---

    private void uploadImageToFirebaseStorage(Uri imageUri) {
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser == null) {
            Toast.makeText(getContext(), "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();
        Log.d(TAG, "Starting image upload to Storage.");

        StorageReference storageRef = storage.getReference();
        String fileName = "background_" + System.currentTimeMillis() + ".jpg";
        StorageReference backgroundRef = storageRef.child("backgrounds/" + fbUser.getUid() + "/" + fileName);
        Uri compressedUri = null;
        try {
            compressedUri = compressImageBeforeUpload(requireContext(), imageUri);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Lỗi khi nén ảnh!", Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
            return;
        }

        backgroundRef.putFile(compressedUri)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "Image upload successful. Getting download URL.");
                    backgroundRef.getDownloadUrl().addOnSuccessListener(downloadUrl -> {
                        String urlString = downloadUrl.toString();
                        Log.d(TAG, "Download URL obtained: " + urlString);
                        saveBackgroundUrlToDatabase(urlString); // Save to Firestore
                        loadBackgroundFromUrl(urlString);      // Set as background
                        saveBackgroundUrlLocally(urlString);   // Save to SharedPreferences
                        if (profileData != null) {
                            profileData.setCurrentUserBackgroundImageUrl(urlString);
                            Log.d(TAG, "Updated ViewModel with new background URL.");
                        }
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Đổi nền thành công!", Toast.LENGTH_SHORT).show();
                    }).addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Log.e(TAG, "Failed to get download URL.", e);
                        Toast.makeText(getContext(), "Lỗi lấy URL ảnh", Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.e(TAG, "Image upload failed.", e);
                    Toast.makeText(getContext(), "Tải ảnh lên thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveBackgroundUrlToDatabase(String downloadUrl) {
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser == null) return;
        String uid = fbUser.getUid();

        Log.d(TAG, "Saving background URL to Firestore for user: " + uid);
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .update("backgroundImageUrl", downloadUrl)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Firestore update successful."))
                .addOnFailureListener(e -> Log.e(TAG, "Firestore update failed.", e));
    }

    // --- Local Storage (SharedPreferences) Logic ---

    private void saveBackgroundUrlLocally(String url) {
        if (sharedPreferences != null) {
            sharedPreferences.edit().putString(PREF_BACKGROUND_URL, url).apply();
            Log.d(TAG, "Saved background URL to SharedPreferences.");
        }
    }

    private void loadSavedBackground() {
        if (sharedPreferences != null) {
            String savedUrl = sharedPreferences.getString(PREF_BACKGROUND_URL, null);
            if (savedUrl != null && !savedUrl.isEmpty()) {
                Log.d(TAG, "Loading background from SharedPreferences: " + savedUrl);
                loadBackgroundFromUrl(savedUrl);
            } else {
                Log.d(TAG, "No background URL found in SharedPreferences.");
                // Optionally set default background here if needed immediately
                // if (rootLayout != null) rootLayout.setBackgroundResource(R.drawable.background_home);
            }
        }
    }

    // --- Glide Background Loading ---

    private void loadBackgroundFromUrl(String url) {
        if (url == null || url.isEmpty() || !isAdded() || rootLayout == null) {
            Log.w(TAG, "Cannot load background: URL is null/empty, or fragment/layout not ready.");
            // Ensure default is set if URL is invalid and nothing else was loaded
            if (rootLayout != null && rootLayout.getBackground() == null) {
                Log.d(TAG, "Setting default background because URL was invalid.");
                rootLayout.setBackgroundResource(R.drawable.background_home);
            }
            return;
        }

        Log.d(TAG, "Loading background from URL using Glide: " + url);
        Glide.with(this)
                .load(url)
                .error(R.drawable.background_home)          // ảnh fallback nếu lỗi
                .diskCacheStrategy(DiskCacheStrategy.ALL)   // cache cả ảnh gốc & nén
                .skipMemoryCache(false)                     // dùng cache trong RAM
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        if (rootLayout != null && isAdded()) {
                            rootLayout.setBackground(resource);
                        }
                    }
                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        Log.d(TAG, "Glide load cleared.");
                    }
                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        Log.e(TAG, "Glide failed to load background from URL: " + url);
                        if (rootLayout != null && isAdded()) {
                            // Fallback to default background on failure
                            rootLayout.setBackgroundResource(R.drawable.background_home);
                            Log.d(TAG, "Set default background due to Glide load failure.");
                        }
                    }
                });
    }


    // --- User Profile and Love Counter Logic (Mostly unchanged, added logging and minor fixes) ---

    private void bindUserProfile(View root) {
        Log.d(TAG, "bindUserProfile called");
        View userInfo = root.findViewById(R.id.layoutUserInfo);
        if (userInfo == null) {
            Log.w(TAG, "layoutUserInfo not found.");
            return;
        }

        ImageView avtLeft = userInfo.findViewById(R.id.avtLeft);
        TextView txtNameLeft = userInfo.findViewById(R.id.txtNameLeft);
        TextView tvAgeLeft = userInfo.findViewById(R.id.tvAgeLeft);
        TextView tvZodiacLeft = userInfo.findViewById(R.id.tvZodiacLeft);
        ImageView avtRight = userInfo.findViewById(R.id.avtRight);
        TextView txtNameRight = userInfo.findViewById(R.id.txtNameRight);
        TextView tvAgeRight = userInfo.findViewById(R.id.tvAgeRight);
        TextView tvZodiacRight = userInfo.findViewById(R.id.tvZodiacRight);

        // --- Check ViewModel cache first ---
        if (profileData.isLoaded() && profileData.hasCurrentUserData()) {
            Log.d(TAG, "Using cached data from ViewModel.");
            displayCachedData(avtLeft, txtNameLeft, tvAgeLeft, tvZodiacLeft,
                    avtRight, txtNameRight, tvAgeRight, tvZodiacRight); // displayCachedData now handles background too

            // Load love date from ViewModel cache
            startLoveDate = profileData.getCurrentUserStartLoveDate();
            startLoveCounter(); // Will handle null date inside

            return; // Stop here, used cache
        }

        // --- If ViewModel cache is empty, load from Database ---
        Log.d(TAG, "ViewModel cache empty, loading from Database.");
        // Set defaults while loading
        FragmentUtils.safeSetText(this, txtNameLeft, "Loading...");
        FragmentUtils.safeSetText(this, txtNameRight, "");
        FragmentUtils.safeSetText(this, tvAgeLeft, "");
        FragmentUtils.safeSetText(this, tvZodiacLeft, "");
        FragmentUtils.safeSetText(this, tvAgeRight, "");
        FragmentUtils.safeSetText(this, tvZodiacRight, "");
        if(avtLeft != null) avtLeft.setImageResource(R.drawable.ic_default_avatar);
        if(avtRight != null) avtRight.setImageResource(R.drawable.ic_default_avatar);
        // Ensure default background is shown initially if nothing else loaded yet
        if (rootLayout != null && sharedPreferences.getString(PREF_BACKGROUND_URL, null) == null) {
            rootLayout.setBackgroundResource(R.drawable.background_home);
        }


        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser == null) {
            Log.w(TAG, "Firebase user is null, cannot load data.");
            handleUserLoadError(fbUser); // Show fallback UI
            return;
        }
        String uid = fbUser.getUid();

        // Load user data from Database
        DatabaseManager.getInstance().getUser(uid, new DatabaseManager.DatabaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (!isAdded()) {
                    Log.w(TAG, "Fragment detached during DB callback.");
                    return;
                }
                if (user == null) {
                    Log.e(TAG, "User data from DB is null for UID: " + uid);
                    handleUserLoadError(fbUser);
                    profileData.setLoaded(true); // Mark as loaded even on error
                    return;
                }

                Log.d(TAG, "User data loaded successfully from DB.");

                // 1. Load Background Image (Prioritize SharedPreferences -> DB -> Default)
                String backgroundUrlFromDb = user.getBackgroundImageUrl();
                String backgroundUrlFromPrefs = sharedPreferences.getString(PREF_BACKGROUND_URL, null);
                String urlToLoad = null;

                // Priority: Use Prefs if available (means user changed it recently)
                if (!TextUtils.isEmpty(backgroundUrlFromPrefs)) {
                    urlToLoad = backgroundUrlFromPrefs;
                    Log.d(TAG, "Using background URL from SharedPreferences: " + urlToLoad);
                }
                // Otherwise, use DB URL if available
                else if (!TextUtils.isEmpty(backgroundUrlFromDb)) {
                    urlToLoad = backgroundUrlFromDb;
                    Log.d(TAG, "Using background URL from DB: " + urlToLoad);
                    // Save the DB URL to Prefs for next time
                    saveBackgroundUrlLocally(urlToLoad);
                }

                // Load the determined URL (or default if null)
                if (urlToLoad != null) {
                    loadBackgroundFromUrl(urlToLoad);
                    // Update ViewModel cache
                    if (profileData != null) {
                        profileData.setCurrentUserProfilePicUrl(urlToLoad);
                    }
                } else {
                    Log.d(TAG, "No background URL found in Prefs or DB. Using default background resource.");
                    if (rootLayout != null) {
                        rootLayout.setBackgroundResource(R.drawable.background_home);
                    }
                    // Ensure ViewModel cache reflects no URL
                    if (profileData != null) {
                        profileData.setCurrentUserBackgroundImageUrl(null);
                    }
                }

                // --- 2. Load User Info (Name, Age, Zodiac, Avatar) ---
                // Name (Prioritize DB -> Auth DisplayName -> Auth Email)
                String displayName = user.getName();
                if (TextUtils.isEmpty(displayName)) displayName = fbUser.getDisplayName();
                if (TextUtils.isEmpty(displayName)) displayName = fbUser.getEmail();
                if (!TextUtils.isEmpty(displayName)) {
                    FragmentUtils.safeSetText(HomeMain1Fragment.this, txtNameLeft, displayName);
                    profileData.setCurrentUserName(displayName);
                    profileData.setCurrentUserId(uid);
                    NameCache.setCurrentName(requireContext(), displayName); // Keep if needed elsewhere
                } else {
                    FragmentUtils.safeSetText(HomeMain1Fragment.this, txtNameLeft, "User"); // Fallback name
                }

                // Age & Zodiac
                if (user.getDateOfBirth() != null) {
                    int age = DateUtils.calculateAge(user.getDateOfBirth());
                    String zodiac = DateUtils.getZodiacSign(user.getDateOfBirth());
                    profileData.setCurrentUserAge(age);
                    profileData.setCurrentUserZodiac(zodiac);
                    profileData.setCurrentUserDateOfBirth(user.getDateOfBirth());
                    FragmentUtils.safeSetText(HomeMain1Fragment.this, tvAgeLeft, age > 0 ? String.valueOf(age) : "");
                    FragmentUtils.safeSetText(HomeMain1Fragment.this, tvZodiacLeft, zodiac);
                } else {
                    profileData.setCurrentUserAge(0);
                    profileData.setCurrentUserZodiac(null);
                    profileData.setCurrentUserDateOfBirth(null);
                    FragmentUtils.safeSetText(HomeMain1Fragment.this, tvAgeLeft, "");
                    FragmentUtils.safeSetText(HomeMain1Fragment.this, tvZodiacLeft, "");
                }

                // Avatar (Load from cache -> DB URL -> Auth URL -> Default)
                Bitmap cachedAvatar = AvatarCache.getCachedBitmap(requireContext());
                if (cachedAvatar != null && avtLeft != null) {
                    avtLeft.setImageBitmap(cachedAvatar);
                    profileData.setCurrentUserAvatar(cachedAvatar);
                    Log.d(TAG, "Loaded current user avatar from cache.");
                } else if (!TextUtils.isEmpty(user.getProfilePicUrl())) {
                    Log.d(TAG, "Loading current user avatar from DB URL.");
                    loadImageAsync(user.getProfilePicUrl(), bmp -> {
                        if (bmp != null && avtLeft != null && isAdded()) {
                            avtLeft.setImageBitmap(bmp);
                            profileData.setCurrentUserAvatar(bmp);
                            AvatarCache.saveBitmapToCache(requireContext(), bmp);
                            Log.d(TAG, "Loaded/cached current user avatar from DB URL.");
                        } else if (avtLeft != null && isAdded()) {
                            avtLeft.setImageResource(R.drawable.ic_default_avatar); // Default on load fail
                        }
                    });
                } else if (fbUser.getPhotoUrl() != null) {
                    Log.d(TAG, "Loading current user avatar from Auth URL.");
                    loadImageAsync(fbUser.getPhotoUrl().toString(), bmp -> {
                        if (bmp != null && avtLeft != null && isAdded()) {
                            avtLeft.setImageBitmap(bmp);
                            profileData.setCurrentUserAvatar(bmp);
                            AvatarCache.saveBitmapToCache(requireContext(), bmp);
                            Log.d(TAG, "Loaded/cached current user avatar from Auth URL.");
                        } else if (avtLeft != null && isAdded()) {
                            avtLeft.setImageResource(R.drawable.ic_default_avatar); // Default on load fail
                        }
                    });
                } else if (avtLeft != null) {
                    Log.d(TAG, "No avatar source found, setting default.");
                    avtLeft.setImageResource(R.drawable.ic_default_avatar); // Default if no URL/Cache
                    profileData.setCurrentUserAvatar(null); // Clear ViewModel cache
                }


                // --- 3. Load Partner Info ---
                String partnerId = user.getPartnerId();
                if (!TextUtils.isEmpty(partnerId)) {
                    Log.d(TAG, "Partner ID found: " + partnerId + ". Loading partner data.");
                    profileData.setPartnerId(partnerId);
                    NameCache.setPartnerId(requireContext(), partnerId); // Keep if needed elsewhere
                    loadPartnerData(partnerId, avtRight, txtNameRight, tvAgeRight, tvZodiacRight);
                } else {
                    Log.d(TAG, "No partner ID found.");
                    clearPartnerUI(avtRight, txtNameRight, tvAgeRight, tvZodiacRight);
                    NameCache.clearPartner(requireContext()); // Keep if needed elsewhere
                    profileData.clearPartnerData(); // Clear ViewModel partner cache
                    profileData.setLoaded(true); // Mark data loading as complete (no partner to wait for)
                }

                // --- 4. Load Start Love Date ---
                startLoveDate = user.getStartLoveDate();
                profileData.setCurrentUserStartLoveDate(startLoveDate); // Update ViewModel
                startLoveCounter(); // Start or stop counter based on the loaded date

                // Mark loading complete ONLY IF there's no partner to wait for
                if (TextUtils.isEmpty(partnerId)) {
                    profileData.setLoaded(true);
                    Log.d(TAG, "ViewModel marked as loaded (no partner).");
                } // Otherwise loadPartnerData will mark it when done

            } // End onSuccess (User)

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to load user data from DB: " + error);
                handleUserLoadError(fbUser); // Use fallback logic
                profileData.setLoaded(true); // Mark as loaded even on error
            }
        }); // End getUser callback
    } // End bindUserProfile
    // ✅ HÀM MỚI: Tải dữ liệu Partner (tách ra cho gọn)
    private void loadPartnerData(String partnerId, ImageView avtRight, TextView txtNameRight, TextView tvAgeRight, TextView tvZodiacRight) {
        // Load from cache first
        Bitmap cachedPartnerAvatar = AvatarCache.getPartnerCachedBitmap(requireContext());
        if (cachedPartnerAvatar != null && avtRight != null) {
            avtRight.setImageBitmap(cachedPartnerAvatar);
            profileData.setPartnerAvatar(cachedPartnerAvatar);
            Log.d(TAG, "Loaded partner avatar from cache.");
        }

        // Fetch partner data from DB
        DatabaseManager.getInstance().getUser(partnerId, new DatabaseManager.DatabaseCallback<User>() {
            @Override
            public void onSuccess(User partner) {
                if (!isAdded() || partner == null) {
                    Log.w(TAG, "Fragment detached or partner data is null during partner load.");
                    if (partner == null) { // If partner deleted/not found
                        clearPartnerUI(avtRight, txtNameRight, tvAgeRight, tvZodiacRight);
                        profileData.clearPartnerData();
                    }
                    profileData.setLoaded(true); // Mark loading complete
                    return;
                }
                Log.d(TAG, "Partner data loaded from DB for ID: " + partnerId);

                // Partner Name
                String partnerName = partner.getName();
                if (TextUtils.isEmpty(partnerName)) partnerName = "Partner"; // Default if name missing
                FragmentUtils.safeSetText(HomeMain1Fragment.this, txtNameRight, partnerName);
                profileData.setPartnerName(partnerName);
                NameCache.setPartnerName(requireContext(), partnerName); // Keep if needed

                // Partner Age & Zodiac
                if (partner.getDateOfBirth() != null) {
                    int partnerAge = DateUtils.calculateAge(partner.getDateOfBirth());
                    String partnerZodiac = DateUtils.getZodiacSign(partner.getDateOfBirth());
                    profileData.setPartnerAge(partnerAge);
                    profileData.setPartnerZodiac(partnerZodiac);
                    profileData.setPartnerDateOfBirth(partner.getDateOfBirth());
                    FragmentUtils.safeSetText(HomeMain1Fragment.this, tvAgeRight, partnerAge > 0 ? String.valueOf(partnerAge) : "");
                    FragmentUtils.safeSetText(HomeMain1Fragment.this, tvZodiacRight, partnerZodiac);
                } else {
                    FragmentUtils.safeSetText(HomeMain1Fragment.this, tvAgeRight, "");
                    FragmentUtils.safeSetText(HomeMain1Fragment.this, tvZodiacRight, "");
                }

                // Partner Avatar (Load only if not already loaded from cache)
                if (cachedPartnerAvatar == null && !TextUtils.isEmpty(partner.getProfilePicUrl())) {
                    Log.d(TAG, "Loading partner avatar from DB URL.");
                    loadImageAsync(partner.getProfilePicUrl(), bmp -> {
                        if (bmp != null && avtRight != null && isAdded()) {
                            avtRight.setImageBitmap(bmp);
                            profileData.setPartnerAvatar(bmp);
                            AvatarCache.savePartnerBitmapToCache(requireContext(), bmp);
                            Log.d(TAG, "Loaded and cached partner avatar from DB URL.");
                        } else if (avtRight != null && isAdded()) {
                            avtRight.setImageResource(R.drawable.ic_default_avatar); // Default on load fail
                        }
                    });
                } else if (cachedPartnerAvatar == null && avtRight != null) {
                    Log.d(TAG, "No partner avatar URL found, setting default.");
                    avtRight.setImageResource(R.drawable.ic_default_avatar); // Default if no URL
                }
                profileData.setLoaded(true); // Mark loading complete
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to load partner data from DB: " + error);
                if (!isAdded()) return;
                // Clear partner UI on error
                clearPartnerUI(avtRight, txtNameRight, tvAgeRight, tvZodiacRight);
                profileData.clearPartnerData();
                profileData.setLoaded(true); // Mark loading complete
            }
        });
    }

    // ✅ HÀM MỚI: Xóa thông tin Partner trên UI
    private void clearPartnerUI(ImageView avtRight, TextView txtNameRight, TextView tvAgeRight, TextView tvZodiacRight) {
        if (!isAdded()) return;
        FragmentUtils.safeSetText(this, txtNameRight, "");
        FragmentUtils.safeSetText(this, tvAgeRight, "");
        FragmentUtils.safeSetText(this, tvZodiacRight, "");
        if (avtRight != null) avtRight.setImageResource(R.drawable.ic_default_avatar);
    }


    // --- Love Counter Methods ---

    private void startLoveCounter() {
        if (startLoveDate == null) {
            Log.d(TAG, "startLoveDate is null, counter not starting.");
            stopLoveCounter(); // Ensure counter is stopped
            clearLoveCounterUI(); // Clear old values
            return;
        }

        stopLoveCounter(); // Stop any existing counter before starting new one
        Log.d(TAG, "Starting love counter.");

        loveCounterRunnable = new Runnable() {
            @Override
            public void run() {
                if (startLoveDate == null || !isAdded()) {
                    stopLoveCounter(); // Stop if date becomes null or fragment detached
                    return;
                }

                Date startDate = startLoveDate.toDate();
                Date currentDate = new Date();
                long diffInMillis = currentDate.getTime() - startDate.getTime();

                // Check for negative difference (start date in future?)
                if (diffInMillis < 0) {
                    Log.w(TAG, "Start date is in the future, stopping counter.");
                    stopLoveCounter();
                    clearLoveCounterUI();
                    safeSetText(txtStartDate, "Ngày bắt đầu không hợp lệ");
                    return;
                }

                // Breakdown Calculation (Keep your original logic)
                Calendar startCal = Calendar.getInstance(); startCal.setTime(startDate);
                Calendar currentCal = Calendar.getInstance(); currentCal.setTime(currentDate);
                int years = currentCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR);
                int months = currentCal.get(Calendar.MONTH) - startCal.get(Calendar.MONTH);
                int days = currentCal.get(Calendar.DAY_OF_MONTH) - startCal.get(Calendar.DAY_OF_MONTH);
                if (days < 0) { months--; Calendar temp = (Calendar) currentCal.clone(); temp.add(Calendar.MONTH, -1); days += temp.getActualMaximum(Calendar.DAY_OF_MONTH); }
                if (months < 0) { years--; months += 12; }
                int weeks = days / 7;
                int remainingDays = days % 7;

                // Update UI
                safeSetText(txtYears, String.valueOf(years));
                safeSetText(txtMonths, String.valueOf(months));
                safeSetText(txtWeeks, String.valueOf(weeks));
                safeSetText(txtDays, String.valueOf(remainingDays));
                long hours = TimeUnit.MILLISECONDS.toHours(diffInMillis) % 24;
                long minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis) % 60;
                long seconds = TimeUnit.MILLISECONDS.toSeconds(diffInMillis) % 60;
                safeSetText(txtTimer, String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
                SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
                String formattedDate = sdf.format(startDate);
                safeSetText(txtStartDate, formattedDate);

                // Schedule next run
                loveCounterHandler.postDelayed(this, 1000);
            }
        };
        loveCounterHandler.post(loveCounterRunnable); // Start the first run
    }

    private void stopLoveCounter() {
        if (loveCounterRunnable != null) {
            loveCounterHandler.removeCallbacks(loveCounterRunnable);
            loveCounterRunnable = null;
            Log.d(TAG, "Love counter stopped.");
        }
    }

    private void clearLoveCounterUI() {
        Log.d(TAG, "Clearing love counter UI.");
        safeSetText(txtYears, "-");
        safeSetText(txtMonths, "-");
        safeSetText(txtWeeks, "-");
        safeSetText(txtDays, "-");
        safeSetText(txtTimer, "--:--:--");
        safeSetText(txtStartDate, "..."); // Placeholder
    }


    // --- Helper Methods ---

    private void safeSetText(@Nullable TextView tv, @Nullable String text) {
        if (tv != null && isAdded()) {
            // Ensure running on UI thread
            if (Looper.myLooper() == Looper.getMainLooper()) {
                tv.setText(text != null ? text : "");
            } else {
                requireActivity().runOnUiThread(() -> tv.setText(text != null ? text : ""));
            }
        }
    }

    private void displayCachedData(ImageView avtLeft, TextView txtNameLeft, TextView tvAgeLeft, TextView tvZodiacLeft,
                                   ImageView avtRight, TextView txtNameRight, TextView tvAgeRight, TextView tvZodiacRight) {
        Log.d(TAG, "displayCachedData called");
        // Current User
        if (profileData.getCurrentUserAvatar() != null && avtLeft != null) {
            avtLeft.setImageBitmap(profileData.getCurrentUserAvatar());
        } else if (avtLeft != null) {
            avtLeft.setImageResource(R.drawable.ic_default_avatar);
        }
        FragmentUtils.safeSetText(this, txtNameLeft, profileData.getCurrentUserName());
        FragmentUtils.safeSetText(this, tvAgeLeft, profileData.getCurrentUserAge() > 0 ? String.valueOf(profileData.getCurrentUserAge()) : "");
        FragmentUtils.safeSetText(this, tvZodiacLeft, profileData.getCurrentUserZodiac());

        // Partner
        if (profileData.getPartnerAvatar() != null && avtRight != null) {
            avtRight.setImageBitmap(profileData.getPartnerAvatar());
        } else if (avtRight != null) {
            avtRight.setImageResource(R.drawable.ic_default_avatar);
        }
        FragmentUtils.safeSetText(this, txtNameRight, profileData.getPartnerName());
        FragmentUtils.safeSetText(this, tvAgeRight, profileData.getPartnerAge() > 0 ? String.valueOf(profileData.getPartnerAge()) : "");
        FragmentUtils.safeSetText(this, tvZodiacRight, profileData.getPartnerZodiac());
    }

    private interface BitmapCallback { void onBitmap(Bitmap bmp); }

    // --- Lifecycle Methods ---

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView called");
        stopLoveCounter(); // Stop counter
        // Release views to prevent leaks
        rootLayout = null;
        fabChangeBackground = null;
        txtYears = txtMonths = txtWeeks = txtDays = txtTimer = txtStartDate = null;
        // Dismiss dialog if showing
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        progressDialog = null;
    }

    // --- Image Loading (Keep using your existing method) ---
    private void loadImageAsync(String urlStr, BitmapCallback callback) {
        if (TextUtils.isEmpty(urlStr)) { if (callback != null) callback.onBitmap(null); return; }
        IMAGE_EXECUTOR.execute(() -> {
            Bitmap bmp = null;
            HttpURLConnection conn = null;
            try {
                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(15000); // 15 seconds
                conn.setReadTimeout(20000);    // 20 seconds
                conn.setInstanceFollowRedirects(true);
                conn.connect();
                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) { // Check for successful response
                    try (InputStream is = conn.getInputStream()) {
                        // Use BitmapFactory.Options for better memory management (optional but good practice)
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        // options.inSampleSize = 2; // Example: reduce size by factor of 2 if needed
                        bmp = BitmapFactory.decodeStream(is, null, options);
                    }
                } else {
                    Log.w(TAG, "loadImageAsync HTTP error code: " + code + " for URL: " + urlStr);
                }
            } catch (Exception e) {
                Log.e(TAG, "loadImageAsync error for URL: " + urlStr, e);
            } finally {
                if (conn != null) conn.disconnect();
            }

            // Ensure fragment is still attached before posting to UI thread
            if (!isAdded()) {
                Log.w(TAG, "Fragment detached after image load, discarding bitmap.");
                return;
            }
            final Bitmap result = bmp; // Final variable for lambda
            // Use Handler associated with the main looper for UI updates
            new Handler(Looper.getMainLooper()).post(() -> {
                if (callback != null) {
                    callback.onBitmap(result); // Pass bitmap (or null) back
                }
            });
        });
    }

    // ✅ HÀM MỚI: Xử lý khi không tải được User từ DB
    private void handleUserLoadError(FirebaseUser fbUser) {
        Log.e(TAG, "Error loading User from DB, using fallback from Auth.");
        if (!isAdded() || getView() == null) { // Check if fragment view is available
            Log.w(TAG, "Fragment detached or view is null in handleUserLoadError.");
            return;
        }

        // Find views again safely within this method
        TextView txtNameLeft = getView().findViewById(R.id.txtNameLeft);
        TextView tvAgeLeft = getView().findViewById(R.id.tvAgeLeft);
        TextView tvZodiacLeft = getView().findViewById(R.id.tvZodiacLeft);
        ImageView avtLeft = getView().findViewById(R.id.avtLeft);
        ImageView avtRight = getView().findViewById(R.id.avtRight);
        TextView txtNameRight = getView().findViewById(R.id.txtNameRight);
        TextView tvAgeRight = getView().findViewById(R.id.tvAgeRight);
        TextView tvZodiacRight = getView().findViewById(R.id.tvZodiacRight);

        // Use fallback name from Firebase Auth if fbUser is not null
        String fallbackName = "User"; // Default fallback
        if (fbUser != null) {
            fallbackName = fbUser.getDisplayName();
            if (TextUtils.isEmpty(fallbackName)) fallbackName = fbUser.getEmail();
            if (TextUtils.isEmpty(fallbackName)) fallbackName = "User"; // Final fallback
        }

        FragmentUtils.safeSetText(this, txtNameLeft, fallbackName);
        if (profileData != null) { // Update ViewModel too
            profileData.setCurrentUserName(fallbackName);
        }

        // Clear other current user fields
        FragmentUtils.safeSetText(this, tvAgeLeft, "");
        FragmentUtils.safeSetText(this, tvZodiacLeft, "");
        if (avtLeft != null) avtLeft.setImageResource(R.drawable.ic_default_avatar);
        if (profileData != null) {
            profileData.setCurrentUserAge(0);
            profileData.setCurrentUserZodiac(null);
            profileData.setCurrentUserAvatar(null);
            profileData.setCurrentUserDateOfBirth(null);
        }


        // Clear partner fields as well
        clearPartnerUI(avtRight, txtNameRight, tvAgeRight, tvZodiacRight);
        if (profileData != null) {
            profileData.clearPartnerData();
        }


        // Stop counter and clear UI
        stopLoveCounter();
        clearLoveCounterUI();

        // Load default background if nothing saved in SharedPreferences
        if (sharedPreferences != null && sharedPreferences.getString(PREF_BACKGROUND_URL, null) == null && rootLayout != null) {
            Log.d(TAG, "Setting default background in handleUserLoadError.");
            rootLayout.setBackgroundResource(R.drawable.background_home);
        }
        // Ensure ViewModel background is also cleared if applicable
        if (profileData != null) {
            profileData.setCurrentUserBackgroundImageUrl(null);
        }
    }

    private Uri compressImageBeforeUpload(Context context, Uri imageUri) throws IOException {
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), imageUri);

        // Lấy kích thước màn hình
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int maxWidth = metrics.widthPixels;
        int maxHeight = metrics.heightPixels;

        // Giảm kích thước ảnh về bằng kích thước màn hình (nếu ảnh quá to)
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, maxWidth, maxHeight, true);

        // Ghi ra file tạm đã nén
        File outputDir = context.getCacheDir();
        File outputFile = new File(outputDir, "compressed_" + System.currentTimeMillis() + ".jpg");

        FileOutputStream out = new FileOutputStream(outputFile);
        resized.compress(Bitmap.CompressFormat.JPEG, 70, out); // Giảm chất lượng xuống 70%
        out.flush();
        out.close();

        return Uri.fromFile(outputFile);
    }


} // End of HomeMain1Fragment class