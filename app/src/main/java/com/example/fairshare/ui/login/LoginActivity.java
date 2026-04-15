package com.example.fairshare.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fairshare.MainActivity;
import com.example.fairshare.R;
import com.example.fairshare.databinding.ActivityLoginBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private ActivityLoginBinding binding;
    private boolean isLoginMode = true;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        firebaseAuthWithGoogle(account.getIdToken());
                    } catch (ApiException e) {
                        Log.w(TAG, "Google sign in failed", e);
                        Toast.makeText(this, "Google sign in failed.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            startMainActivity();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        binding.btnGoogleLogin.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String pass = binding.etPassword.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isLoginMode) {
                mAuth.signInWithEmailAndPassword(email, pass)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                startMainActivity();
                            } else {
                                Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                mAuth.createUserWithEmailAndPassword(email, pass)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                startMainActivity();
                            } else {
                                Toast.makeText(LoginActivity.this, "Sign up failed.", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        binding.tvToggleSignup.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            if (isLoginMode) {
                binding.btnLogin.setText("Login");
                binding.tvToggleSignup.setText("Don't have an account? Sign Up");
            } else {
                binding.btnLogin.setText("Sign Up");
                binding.tvToggleSignup.setText("Already have an account? Login");
            }
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        startMainActivity();
                    } else {
                        Toast.makeText(LoginActivity.this, "Firebase Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startMainActivity() {
        // Retrieve and save FCM token
        retrieveAndSaveFcmToken();
        
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
    
    private void retrieveAndSaveFcmToken() {
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                    return;
                }

                // Get new FCM registration token
                String token = task.getResult();
                Log.d(TAG, "FCM Token: " + token);
                
                // Save token to Firestore
                saveFcmTokenToFirestore(token);
            });
    }
    
    private void saveFcmTokenToFirestore(String token) {
        if (mAuth.getCurrentUser() == null) {
            Log.w(TAG, "No authenticated user to save FCM token");
            return;
        }
        
        String userId = mAuth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Create or update user document with FCM token
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Update existing user document
                    db.collection("users").document(userId)
                        .update("fcmToken", token)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "FCM token updated successfully");
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to update FCM token", e);
                        });
                } else {
                    // Create new user document
                    java.util.Map<String, Object> userData = new java.util.HashMap<>();
                    userData.put("fcmToken", token);
                    userData.put("email", mAuth.getCurrentUser().getEmail());
                    userData.put("displayName", mAuth.getCurrentUser().getDisplayName());
                    
                    db.collection("users").document(userId)
                        .set(userData)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "User document created with FCM token");
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to create user document with FCM token", e);
                        });
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to check user document", e);
            });
    }
}
