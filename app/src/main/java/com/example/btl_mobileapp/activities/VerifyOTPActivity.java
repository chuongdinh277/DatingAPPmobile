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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

public class VerifyOTPActivity extends AppCompatActivity {

    private EditText etOTP;
    private Button btnVerify;
    private String verificationId, phone, name;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otp);

        etOTP = findViewById(R.id.etOTP);
        btnVerify = findViewById(R.id.btnVerifyOTP);

        phone = getIntent().getStringExtra("phone");
        name = getIntent().getStringExtra("name");
        verificationId = getIntent().getStringExtra("verificationId");

        mAuth = FirebaseAuth.getInstance();

        btnVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String code = etOTP.getText().toString().trim();
                if(TextUtils.isEmpty(code)){
                    Toast.makeText(VerifyOTPActivity.this, "Nhập OTP", Toast.LENGTH_SHORT).show();
                    return;
                }
                verifyCode(code);
            }
        });
    }

    private void verifyCode(String code){
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        // OTP đúng → chuyển sang tạo password
                        Intent intent = new Intent(VerifyOTPActivity.this, CreatePasswordActivity.class);
                        intent.putExtra("phone", phone);
                        intent.putExtra("name", name);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(VerifyOTPActivity.this, "OTP không đúng", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
