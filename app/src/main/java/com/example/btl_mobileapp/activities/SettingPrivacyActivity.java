package com.example.btl_mobileapp.activities;

import android.os.Bundle;
import android.widget.TextView;

import com.example.btl_mobileapp.R;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class SettingPrivacyActivity extends BaseActivity {
    private static final String PRIVACY_POLICY_FILE = "privacy_policy.txt"; // Tạo và định nghĩa nó trong assets

    private TextView tvFileContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_privacy);

        tvFileContent.setText(readTextFromAssets(PRIVACY_POLICY_FILE));
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
