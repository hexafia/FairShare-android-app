package com.example.fairshare;

import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.security.SecureRandom;

/**
 * Repository that abstracts Firestore CRUD operations for the "users" collection.
 * Each document is keyed by the Firebase Auth UID.
 */
public class UserRepository {

    private static final String TAG = "UserRepository";
    private static final String COLLECTION = "users";
    private static final String TAGLINE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final FirebaseFirestore db;
    private final MutableLiveData<UserProfile> profileLiveData = new MutableLiveData<>();
    private ListenerRegistration listenerRegistration;

    public UserRepository() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Returns a LiveData that emits the current user's profile.
     * Attaches a real-time snapshot listener on the user's document.
     * If the document doesn't exist yet, creates a default profile.
     */
    public LiveData<UserProfile> getUserProfile() {
        if (SessionManager.isLocalAdmin(FairShareApp.getAppContext())) {
            profileLiveData.setValue(SessionManager.getLocalAdminProfile());
            return profileLiveData;
        }

        FirebaseUser firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            profileLiveData.setValue(null);
            return profileLiveData;
        }

        String uid = firebaseUser.getUid();
        DocumentReference docRef = db.collection(COLLECTION).document(uid);

        if (listenerRegistration == null) {
            listenerRegistration = docRef.addSnapshotListener((snapshot, error) -> {
                if (error != null) {
                    Log.w(TAG, "Listen failed.", error);
                    return;
                }
                if (snapshot != null && snapshot.exists()) {
                    UserProfile profile = snapshot.toObject(UserProfile.class);
                    profileLiveData.setValue(profile);
                } else {
                    // Document doesn't exist yet — create a default profile
                    createDefaultProfile(firebaseUser);
                }
            });
        }
        return profileLiveData;
    }

    /**
     * Saves/updates the user profile document in Firestore.
     */
    public void saveUserProfile(UserProfile profile) {
        if (SessionManager.isLocalAdmin(FairShareApp.getAppContext())) {
            profileLiveData.setValue(profile);
            return;
        }

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;

        String uid = firebaseUser.getUid();
        db.collection(COLLECTION).document(uid)
                .set(profile)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Profile saved for: " + uid))
                .addOnFailureListener(e -> Log.w(TAG, "Error saving profile", e));
    }

    /**
     * Creates a default profile using data from the Firebase Auth provider.
     * Auto-generates a random 4-character alphanumeric tagline.
     */
    private void createDefaultProfile(FirebaseUser firebaseUser) {
        String name = firebaseUser.getDisplayName();
        if (name == null || name.isEmpty()) {
            // Fallback: use the part before @ in the email
            String email = firebaseUser.getEmail();
            if (email != null && email.contains("@")) {
                name = email.substring(0, email.indexOf("@"));
            } else {
                name = "User";
            }
        }

        String email = firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "";
        Uri photoUri = firebaseUser.getPhotoUrl();
        String photoUrl = photoUri != null ? photoUri.toString() : null;

        String tagline = generateRandomTagline();

        UserProfile profile = new UserProfile(name, tagline, email);
        profile.setPhotoUrl(photoUrl);

        saveUserProfile(profile);
    }

    /**
     * Generates a random 4-character alphanumeric tagline (e.g. "A7X2").
     */
    private String generateRandomTagline() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            sb.append(TAGLINE_CHARS.charAt(random.nextInt(TAGLINE_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * Remove the snapshot listener when it's no longer needed.
     */
    public void removeListener() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }
}
