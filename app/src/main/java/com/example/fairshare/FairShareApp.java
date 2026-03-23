package com.example.fairshare;

import android.app.Application;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

public class FairShareApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

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
