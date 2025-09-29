package com.example.btl_mobileapp.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.btl_mobileapp.R;
import com.google.android.material.button.MaterialButton;

public class SettingProfileActivity extends BaseActivity {

    private TextView tvFirstname, tvLastname, tvUsername, tvGmail, tvPhone;
    private EditText etFirstname, etLastname, etUsername, etGmail, etPhone;
    private ImageButton btnEdit;
    private MaterialButton btnUpload;

    private boolean isEditing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_profile);

        // Ánh xạ TextView
        tvFirstname = findViewById(R.id.tv_firstname_value);
        tvLastname = findViewById(R.id.tv_lastname_value);
        tvUsername = findViewById(R.id.tv_username_value);
        tvGmail = findViewById(R.id.tv_gmail_value);
        tvPhone = findViewById(R.id.tv_phone_value);

        // Ánh xạ EditText
        etFirstname = findViewById(R.id.et_firstname_edit);
        etLastname = findViewById(R.id.et_lastname_edit);
        etUsername = findViewById(R.id.et_username_edit);
        etGmail = findViewById(R.id.et_gmail_edit);
        etPhone = findViewById(R.id.et_phone_edit);

        // Nút
        btnEdit = findViewById(R.id.btn_edit);
        btnUpload = findViewById(R.id.bt_upload);

        // Ban đầu: chỉ hiển thị TextView
        setEditMode(false);

        // Sự kiện nút Edit
        btnEdit.setOnClickListener(v -> {
            isEditing = !isEditing;
            setEditMode(isEditing);
        });

        // Sự kiện nút Upload
        btnUpload.setOnClickListener(v -> {
            // Lấy dữ liệu từ EditText -> cập nhật TextView
            tvFirstname.setText(etFirstname.getText().toString());
            tvLastname.setText(etLastname.getText().toString());
            tvUsername.setText(etUsername.getText().toString());
            tvGmail.setText(etGmail.getText().toString());
            tvPhone.setText(etPhone.getText().toString());

            // Tắt chế độ edit
            isEditing = false;
            setEditMode(false);
        });
    }

    private void setEditMode(boolean enable) {
        if (enable) {
            // Hiện EditText + ẩn TextView
            tvFirstname.setVisibility(View.GONE);
            etFirstname.setVisibility(View.VISIBLE);

            tvLastname.setVisibility(View.GONE);
            etLastname.setVisibility(View.VISIBLE);

            tvUsername.setVisibility(View.GONE);
            etUsername.setVisibility(View.VISIBLE);

            tvGmail.setVisibility(View.GONE);
            etGmail.setVisibility(View.VISIBLE);

            tvPhone.setVisibility(View.GONE);
            etPhone.setVisibility(View.VISIBLE);

            btnUpload.setVisibility(View.VISIBLE);

        } else {
            // Hiện TextView + ẩn EditText
            tvFirstname.setVisibility(View.VISIBLE);
            etFirstname.setVisibility(View.GONE);

            tvLastname.setVisibility(View.VISIBLE);
            etLastname.setVisibility(View.GONE);

            tvUsername.setVisibility(View.VISIBLE);
            etUsername.setVisibility(View.GONE);

            tvGmail.setVisibility(View.VISIBLE);
            etGmail.setVisibility(View.GONE);

            tvPhone.setVisibility(View.VISIBLE);
            etPhone.setVisibility(View.GONE);

            btnUpload.setVisibility(View.INVISIBLE);
        }
    }
}
