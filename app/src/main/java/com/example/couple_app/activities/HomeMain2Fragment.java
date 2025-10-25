package com.example.couple_app.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.couple_app.R;
import com.example.couple_app.utils.FragmentUtils;
import com.example.couple_app.viewmodels.AvatarViewModel;
import com.example.couple_app.viewmodels.UserProfileData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeMain2Fragment extends Fragment {
    private static final String TAG = "HomeMain2Fragment";
    private UserProfileData profileData;
    private AvatarViewModel avatarViewModel;

    public HomeMain2Fragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.homemain2, container, false);

        // Get shared profile data instance
        profileData = UserProfileData.getInstance();

        // Initialize AvatarViewModel (shared across Activity)
        avatarViewModel = new ViewModelProvider(requireActivity()).get(AvatarViewModel.class);

        // Bind user profile information
        bindUserProfile(root);

        return root;
    }

    private void bindUserProfile(View root) {
        View userInfo = root.findViewById(R.id.layoutUserInfo);
        if (userInfo == null) return;

        // Views for left user (current user)
        ImageView avtLeft = userInfo.findViewById(R.id.avtLeft);
        TextView txtNameLeft = userInfo.findViewById(R.id.txtNameLeft);
        TextView tvAgeLeft = userInfo.findViewById(R.id.tvAgeLeft);
        TextView tvZodiacLeft = userInfo.findViewById(R.id.tvZodiacLeft);

        // Views for right user (partner)
        ImageView avtRight = userInfo.findViewById(R.id.avtRight);
        TextView txtNameRight = userInfo.findViewById(R.id.txtNameRight);
        TextView tvAgeRight = userInfo.findViewById(R.id.tvAgeRight);
        TextView tvZodiacRight = userInfo.findViewById(R.id.tvZodiacRight);

        // Display text data from cache
        displayTextData(txtNameLeft, tvAgeLeft, tvZodiacLeft, txtNameRight, tvAgeRight, tvZodiacRight);

        // Setup avatar observers
        setupAvatarObservers(avtLeft, avtRight);
    }

    private void displayTextData(TextView txtNameLeft, TextView tvAgeLeft, TextView tvZodiacLeft,
                                 TextView txtNameRight, TextView tvAgeRight, TextView tvZodiacRight) {
        // Display current user text data from cache
        FragmentUtils.safeSetText(this, txtNameLeft, profileData.getCurrentUserName());
        if (profileData.getCurrentUserAge() > 0) {
            FragmentUtils.safeSetText(this, tvAgeLeft, String.valueOf(profileData.getCurrentUserAge()));
        }
        if (!TextUtils.isEmpty(profileData.getCurrentUserZodiac())) {
            FragmentUtils.safeSetText(this, tvZodiacLeft, profileData.getCurrentUserZodiac());
        }

        // Display partner text data from cache
        if (!TextUtils.isEmpty(profileData.getPartnerName())) {
            FragmentUtils.safeSetText(this, txtNameRight, profileData.getPartnerName());
        }
        if (profileData.getPartnerAge() > 0) {
            FragmentUtils.safeSetText(this, tvAgeRight, String.valueOf(profileData.getPartnerAge()));
        }
        if (!TextUtils.isEmpty(profileData.getPartnerZodiac())) {
            FragmentUtils.safeSetText(this, tvZodiacRight, profileData.getPartnerZodiac());
        }
    }

    private void setupAvatarObservers(ImageView avtLeft, ImageView avtRight) {
        // ⭐ CRITICAL FIX: Get current user ID from Firebase Auth, NOT from cached UserProfileData
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "No user logged in, cannot load avatars");
            return;
        }

        String currentUserId = currentUser.getUid();

        // Get partner ID from profileData (this is OK as it's loaded fresh by HomeMain1Fragment)
        String partnerId = profileData.getPartnerId();

        Log.d(TAG, "Setting up avatar observers - currentUserId: " + currentUserId + ", partnerId: " + partnerId);

        // Observe current user avatar
        if (currentUserId != null && !currentUserId.isEmpty()) {
            avatarViewModel.getAvatar(currentUserId).observe(getViewLifecycleOwner(), bitmap -> {
                if (bitmap != null && avtLeft != null) {
                    Log.d(TAG, "Current user avatar loaded from ViewModel");
                    avtLeft.setImageBitmap(bitmap);
                } else {
                    Log.d(TAG, "Current user avatar is null");
                }
            });

            // Load avatar if not cached
            Log.d(TAG, "Loading current user avatar for userId: " + currentUserId);
            avatarViewModel.loadAvatar(currentUserId);
        } else {
            Log.w(TAG, "Current user ID is empty");
            // Fallback to old cache if userId not available yet
            if (profileData.getCurrentUserAvatar() != null && avtLeft != null) {
                avtLeft.setImageBitmap(profileData.getCurrentUserAvatar());
            }
        }

        // Observe partner avatar
        if (partnerId != null && !partnerId.isEmpty()) {
            avatarViewModel.getAvatar(partnerId).observe(getViewLifecycleOwner(), bitmap -> {
                if (bitmap != null && avtRight != null) {
                    Log.d(TAG, "Partner avatar loaded from ViewModel");
                    avtRight.setImageBitmap(bitmap);
                } else {
                    Log.d(TAG, "Partner avatar is null");
                }
            });

            // Load avatar if not cached
            Log.d(TAG, "Loading partner avatar for partnerId: " + partnerId);
            avatarViewModel.loadAvatar(partnerId);
        } else {
            Log.w(TAG, "Partner ID is empty or null");
            // Fallback to old cache if partnerId not available yet
            if (profileData.getPartnerAvatar() != null && avtRight != null) {
                avtRight.setImageBitmap(profileData.getPartnerAvatar());
            }
        }
    }
}
