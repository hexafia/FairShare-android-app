package com.example.fairshare.ui.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fairshare.MainActivity;
import com.example.fairshare.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private boolean isLoginMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Facebook-style login persistence
        SharedPreferences prefs = getSharedPreferences("FairSharePrefs", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
        
        if (isLoggedIn) {
            startMainActivity();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String pass = binding.etPassword.getText().toString().trim();
            
            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Mock login action
            prefs.edit().putBoolean("isLoggedIn", true).apply();
            startMainActivity();
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

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
