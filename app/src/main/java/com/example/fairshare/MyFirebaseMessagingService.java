package com.example.fairshare;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Firebase Cloud Messaging service for handling push notifications
 * Handles incoming messages even when app is in background
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "fairshare_notifications";
    private static final String CHANNEL_NAME = "FairShare Notifications";
    private static final String CHANNEL_DESCRIPTION = "Notifications for FairShare app";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "FCM Token refreshed: " + token);
        
        // Save token to current user's Firestore profile
        saveTokenToFirestore(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "Message received from: " + remoteMessage.getFrom());

        // Check if message contains a data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            handleDataMessage(remoteMessage.getData());
        }

        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message notification body: " + remoteMessage.getNotification().getBody());
            handleNotificationMessage(remoteMessage.getNotification());
        }
    }

    /**
     * Handle data payload messages (when app is in foreground/background)
     */
    private void handleDataMessage(android.os.Bundle data) {
        String title = data.getString("title", "FairShare");
        String body = data.getString("body", "You have a new notification");
        String type = data.getString("type", "general");
        String groupId = data.getString("groupId", "");
        String groupName = data.getString("groupName", "");
        String notificationId = data.getString("notificationId", "");

        // Create and show notification
        showNotification(title, body, type, groupId, groupName, notificationId);
    }

    /**
     * Handle notification payload messages (when app is in background)
     */
    private void handleNotificationMessage(RemoteMessage.Notification notification) {
        String title = notification.getTitle();
        String body = notification.getBody();
        
        showNotification(title != null ? title : "FairShare", 
                        body != null ? body : "You have a new notification", 
                        "general", "", "", "");
    }

    /**
     * Create and show a system notification
     */
    private void showNotification(String title, String body, String type, String groupId, String groupName, String notificationId) {
        // Create notification channel for Android 8.0+
        createNotificationChannel();

        // Create intent for when notification is tapped
        Intent intent = createNotificationIntent(type, groupId, groupName, notificationId);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(getNotificationIcon(type))
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        // Show notification
        NotificationManager notificationManager = 
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        // Use notification ID as unique identifier
        int notificationIdInt = notificationId.hashCode();
        notificationManager.notify(notificationIdInt, builder.build());
    }

    /**
     * Create appropriate intent based on notification type
     */
    private Intent createNotificationIntent(String type, String groupId, String groupName, String notificationId) {
        Intent intent;
        
        switch (type) {
            case "nudge":
                // Navigate to Group Lobby with Settle Up tab
                intent = new Intent(this, com.example.fairshare.ui.groups.GroupLobbyActivity.class);
                intent.putExtra("groupId", groupId);
                intent.putExtra("groupName", groupName);
                intent.putExtra("openSettleUpTab", true);
                break;
                
            case "payment_confirmed":
                // Navigate to Group Lobby with Ledger tab
                intent = new Intent(this, com.example.fairshare.ui.groups.GroupLobbyActivity.class);
                intent.putExtra("groupId", groupId);
                intent.putExtra("groupName", groupName);
                break;
                
            default:
                // Navigate to NotificationsFragment
                intent = new Intent(this, com.example.fairshare.MainActivity.class);
                intent.putExtra("openNotifications", true);
                break;
        }
        
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    /**
     * Get appropriate notification icon based on type
     */
    private int getNotificationIcon(String type) {
        switch (type) {
            case "nudge":
                return android.R.drawable.ic_dialog_email; // Bell icon for nudges
            case "payment_confirmed":
                return android.R.drawable.ic_menu_save; // Check icon for payments
            default:
                return android.R.drawable.ic_dialog_info; // Default info icon
        }
    }

    /**
     * Create notification channel for Android 8.0 and above
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableLights(true);
            channel.enableVibration(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Save FCM token to current user's Firestore profile
     */
    private void saveTokenToFirestore(String token) {
        // Check if user is logged in
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
            String userId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
            com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
            
            // Update user document with FCM token
            db.collection("users").document(userId)
                .update("fcmToken", token)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "FCM token saved to Firestore successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save FCM token to Firestore", e);
                });
        }
    }
}
