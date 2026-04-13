package com.example.fairshare;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

public class FairShareApp extends Application {

    private static FairShareApp instance;

    public static FairShareApp getInstance() {
        return instance;
    }

    public static Application getAppContext() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Apply saved theme preference
        SharedPreferences prefs = getSharedPreferences("FairSharePrefs", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("darkMode", false);
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        // FirebaseApp is auto-initialized via google-services.json.
        // We configure Firestore to use persistent offline caching so
        // data is available even without a network connection.
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build();
        db.setFirestoreSettings(settings);
    }
}
