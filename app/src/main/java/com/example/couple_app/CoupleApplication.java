package com.example.couple_app;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class CoupleApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
    }
}
