package com.example.btl_mobileapp.activities;

import android.os.Bundle;
import android.widget.TextView;

import com.example.btl_mobileapp.R;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class SettingAboutActivity extends BaseActivity {

    private final String ABOUT_FILE = "about.txt"; // Tạo và định nghĩa nó trong assets

    private TextView tvFileContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_scene);

        tvFileContent.setText(readTextFromAssets(ABOUT_FILE));
    }

    private String readTextFromAssets(String fileName) {
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(getAssets().open(fileName)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line).append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return text.toString();
    }



}
