package com.example.fairshare.utils;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

/**
 * Utility class for cleaning up old/malformed notification documents
 */
public class NotificationCleanup {
    
    private static final String TAG = "NotificationCleanup";
    
    /**
     * Clean up old FCM test notification documents and malformed notifications
     * This should be called once during app startup to clean up the database
     */
    public static void cleanupOldNotifications() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Delete notifications with old FCM types
        db.collection("notifications")
            .whereIn("type", java.util.Arrays.asList("NUDGE", "SETTLEMENT", "payment_confirmed"))
            .get()
            .addOnSuccessListener(querySnapshot -> {
                Log.d(TAG, "Found " + querySnapshot.size() + " old FCM notifications to delete");
                
                for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    doc.getReference().delete()
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Deleted old notification: " + doc.getId());
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to delete notification: " + doc.getId(), e);
                        });
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error querying old notifications", e);
            });
        
        // Delete notifications with missing required fields
        db.collection("notifications")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                int deletedCount = 0;
                
                for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    // Check for missing required fields
                    if (!doc.contains("recipientUid") || 
                        !doc.contains("senderUid") || 
                        !doc.contains("type") ||
                        !doc.contains("message")) {
                        
                        doc.getReference().delete()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Deleted malformed notification: " + doc.getId());
                                deletedCount++;
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to delete malformed notification: " + doc.getId(), e);
                            });
                    }
                }
                
                Log.d(TAG, "Deleted " + deletedCount + " malformed notifications");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error querying all notifications for cleanup", e);
            });
    }
    
    /**
     * Clean up notifications for a specific user that might be causing issues
     * @param userId The user ID to clean up notifications for
     */
    public static void cleanupUserNotifications(String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "Cannot cleanup notifications for null or empty user ID");
            return;
        }
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Delete old type notifications for this user
        db.collection("notifications")
            .whereEqualTo("recipientUid", userId)
            .whereIn("type", java.util.Arrays.asList("NUDGE", "SETTLEMENT", "payment_confirmed"))
            .get()
            .addOnSuccessListener(querySnapshot -> {
                Log.d(TAG, "Found " + querySnapshot.size() + " old notifications for user " + userId);
                
                for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    doc.getReference().delete()
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Deleted old user notification: " + doc.getId());
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to delete user notification: " + doc.getId(), e);
                        });
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error querying old user notifications", e);
            });
    }
}
