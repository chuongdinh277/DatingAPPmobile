package com.example.btl_mobileapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.btl_mobileapp.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CreatePasswordActivity extends AppCompatActivity {

    private EditText etPassword;
    private Button btnCreatePassword;
    private String phone, name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_password);

        etPassword = findViewById(R.id.etPassword);
        btnCreatePassword = findViewById(R.id.btnCreatePassword);

        phone = getIntent().getStringExtra("phone");
        name = getIntent().getStringExtra("name");

        btnCreatePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String password = etPassword.getText().toString().trim();
                if(TextUtils.isEmpty(password) || password.length() < 6){
                    Toast.makeText(CreatePasswordActivity.this, "Password ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
                    return;
                }

                saveUserToFirestore(phone, name, password);
            }
        });
    }

    private void saveUserToFirestore(String phone, String name, String password){
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> user = new HashMap<>();
        user.put("phone", phone);
        user.put("name", name);
        user.put("password", password);

        db.collection("users").document(phone)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(CreatePasswordActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(CreatePasswordActivity.this, LoginByPhoneActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(CreatePasswordActivity.this, "Lỗi: "+e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
