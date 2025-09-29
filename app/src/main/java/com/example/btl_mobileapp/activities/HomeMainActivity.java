package com.example.btl_mobileapp.activities;

import android.os.Bundle;
import androidx.viewpager2.widget.ViewPager2;
import androidx.fragment.app.Fragment;

import com.example.btl_mobileapp.R;

import java.util.ArrayList;
import java.util.List;

public class HomeMainActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homemain); // layout có ViewPager2

        ViewPager2 viewPager = findViewById(R.id.viewPager);
        List<Fragment> fragments = new ArrayList<>();
        fragments.add(new HomeMain1Fragment());
        fragments.add(new HomeMain2Fragment());

        HomePagerAdapter adapter = new HomePagerAdapter(this, fragments);
        viewPager.setAdapter(adapter);
    }
}
