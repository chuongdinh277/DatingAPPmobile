package com.example.couple_app.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.couple_app.R;
import com.example.couple_app.utils.FragmentUtils;
import com.example.couple_app.viewmodels.UserProfileData;

public class HomeMain2Fragment extends Fragment {
    private UserProfileData profileData;

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

        // Check if data is already loaded - use cached data
        if (profileData.isLoaded() && profileData.hasCurrentUserData()) {
            displayCachedData(avtLeft, txtNameLeft, tvAgeLeft, tvZodiacLeft,
                            avtRight, txtNameRight, tvAgeRight, tvZodiacRight);
            return;
        }

        // If not loaded, HomeMain1Fragment will load it
        // Just display what we have from cache for now
        displayCachedData(avtLeft, txtNameLeft, tvAgeLeft, tvZodiacLeft,
                        avtRight, txtNameRight, tvAgeRight, tvZodiacRight);
    }

    private void displayCachedData(ImageView avtLeft, TextView txtNameLeft, TextView tvAgeLeft, TextView tvZodiacLeft,
                                   ImageView avtRight, TextView txtNameRight, TextView tvAgeRight, TextView tvZodiacRight) {
        // Display current user data from cache
        if (profileData.getCurrentUserAvatar() != null) {
            avtLeft.setImageBitmap(profileData.getCurrentUserAvatar());
        }
        FragmentUtils.safeSetText(this, txtNameLeft, profileData.getCurrentUserName());
        if (profileData.getCurrentUserAge() > 0) {
            FragmentUtils.safeSetText(this, tvAgeLeft, String.valueOf(profileData.getCurrentUserAge()));
        }
        if (!TextUtils.isEmpty(profileData.getCurrentUserZodiac())) {
            FragmentUtils.safeSetText(this, tvZodiacLeft, profileData.getCurrentUserZodiac());
        }

        // Display partner data from cache
        if (profileData.getPartnerAvatar() != null) {
            avtRight.setImageBitmap(profileData.getPartnerAvatar());
        }
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
}
