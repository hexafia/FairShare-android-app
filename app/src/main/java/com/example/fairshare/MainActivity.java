package com.example.fairshare;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.fairshare.databinding.ActivityMainBinding;
import com.example.fairshare.utils.NotificationCleanup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;

    // Notification observer
    private ListenerRegistration notificationListener;
    private int unreadNotificationCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(binding.bottomNav, navController);
        }

        // Clean up old FCM test notifications
        NotificationCleanup.cleanupOldNotifications();

        // Setup notification observer
        setupNotificationObserver();
    }

    private void setupNotificationObserver() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "No authenticated user, skipping notification observer");
            return;
        }

        String currentUserId = currentUser.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Query notificationQuery = db.collection("notifications")
                .whereEqualTo("recipientUid", currentUserId)
                .whereEqualTo("isRead", false)
                .orderBy("timestamp", Query.Direction.DESCENDING);

        notificationListener = notificationQuery.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e(TAG, "Notification listener error", error);
                return;
            }

            if (value != null) {
                unreadNotificationCount = value.size();
                updateNotificationBadge();

                // Show toast for new notifications
                for (DocumentChange doc : value.getDocumentChanges()) {
                    if (doc.getType() == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        String type = doc.getDocument().getString("type");
                        String senderName = doc.getDocument().getString("senderName");

                        // Toast reminders removed - notifications should only appear in NotificationsFragment
                    }
                }
            }
        });
    }

    private void updateNotificationBadge() {
        // Update bottom navigation badge
        Menu menu = binding.bottomNav.getMenu();
        MenuItem notificationsItem = menu.findItem(R.id.nav_notifications);

        if (notificationsItem != null) {
            // Show badge if there are unread notifications
            if (unreadNotificationCount > 0) {
                // You can customize this to show a badge
                // For now, we'll use the title to indicate count
                notificationsItem.setTitle("Notifications (" + unreadNotificationCount + ")");
            } else {
                notificationsItem.setTitle("Notifications");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove notification listener to prevent memory leaks
        if (notificationListener != null) {
            notificationListener.remove();
            notificationListener = null;
        }
    }
}