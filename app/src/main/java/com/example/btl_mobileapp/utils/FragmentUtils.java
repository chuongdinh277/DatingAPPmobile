package com.example.btl_mobileapp.utils;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.widget.TextView;

/**
 * Utility class for Fragment operations
 */
public class FragmentUtils {

    /**
     * Safely set text to TextView with null checks and fragment state verification
     * @param fragment The fragment to check if it's added
     * @param tv The TextView to set text
     * @param text The text to set
     */
    public static void safeSetText(Fragment fragment, @Nullable TextView tv, @Nullable String text) {
        if (tv == null) return;
        if (!fragment.isAdded()) return;
        fragment.requireActivity().runOnUiThread(() -> tv.setText(text != null ? text : ""));
    }
}