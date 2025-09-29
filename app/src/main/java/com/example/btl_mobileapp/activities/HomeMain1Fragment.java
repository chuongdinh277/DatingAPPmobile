package com.example.btl_mobileapp.activities;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.btl_mobileapp.R;

public class HomeMain1Fragment extends Fragment {
    public HomeMain1Fragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Nạp layout homemain1.xml
        return inflater.inflate(R.layout.homemain1, container, false);
    }
}
