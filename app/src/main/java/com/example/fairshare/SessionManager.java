package com.example.fairshare;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Small session helper that supports a local admin mode alongside Firebase Auth.
 */
public final class SessionManager {

    private static final String PREFS_NAME = "FairShareSession";
    private static final String KEY_LOCAL_ADMIN = "localAdminLoggedIn";
    private static final String LOCAL_ADMIN_UID = "local-admin";

    private SessionManager() {
    }

    public static boolean isLoggedIn(Context context) {
        return getFirebaseUser() != null || isLocalAdmin(context);
    }

    public static String getCurrentUid(Context context) {
        FirebaseUser firebaseUser = getFirebaseUser();
        if (firebaseUser != null) {
            return firebaseUser.getUid();
        }
        if (isLocalAdmin(context)) {
            return LOCAL_ADMIN_UID;
        }
        return null;
    }

    public static boolean isLocalAdmin(Context context) {
        return isDebuggable(context) && getPrefs(context).getBoolean(KEY_LOCAL_ADMIN, false);
    }

    public static void loginLocalAdmin(Context context) {
        if (isDebuggable(context)) {
            getPrefs(context).edit().putBoolean(KEY_LOCAL_ADMIN, true).apply();
        }
    }

    public static void logoutLocalAdmin(Context context) {
        getPrefs(context).edit().putBoolean(KEY_LOCAL_ADMIN, false).apply();
    }

    public static UserProfile getLocalAdminProfile() {
        UserProfile profile = new UserProfile("admin", "ADMN", "admin");
        profile.setLocation("Local Session");
        profile.setPhoneNumber("Not set");
        return profile;
    }

    private static FirebaseUser getFirebaseUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isDebuggable(Context context) {
        ApplicationInfo appInfo = context.getApplicationContext().getApplicationInfo();
        return (appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }
}
