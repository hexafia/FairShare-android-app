package com.example.fairshare.ui.profile;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.fairshare.CurrencyHelper;
import com.example.fairshare.ExpenseRepository;
import com.example.fairshare.R;
import com.example.fairshare.Transaction;
import com.example.fairshare.UserProfile;
import com.example.fairshare.UserRepository;
import com.example.fairshare.ui.settings.SettingsActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileFragment extends Fragment implements com.example.fairshare.FastActionHandler {

    private UserRepository userRepository;
    private ExpenseRepository expenseRepository;
    private UserProfile currentProfile;

    // Views
    private TextView tvDisplayName, tvTagline, tvUserEmail, tvUserPhone, tvUserLocation;
    private TextView tvAvatarInitial;
    private View flAvatarFallback;
    private ImageView ivAvatar;
    private TextView tvTotalExpenses;

    private final ExecutorService imageExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind views
        tvDisplayName = view.findViewById(R.id.tvDisplayName);
        tvTagline = view.findViewById(R.id.tvTagline);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvUserPhone = view.findViewById(R.id.tvUserPhone);
        tvUserLocation = view.findViewById(R.id.tvUserLocation);
        tvAvatarInitial = view.findViewById(R.id.tvAvatarInitial);
        flAvatarFallback = view.findViewById(R.id.flAvatarFallback);
        ivAvatar = view.findViewById(R.id.ivAvatar);
        tvTotalExpenses = view.findViewById(R.id.tvTotalExpenses);

        // Settings gear button → opens SettingsActivity
        view.findViewById(R.id.btnSettings).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SettingsActivity.class);
            startActivity(intent);
        });

        // Edit Profile button
        view.findViewById(R.id.btnEditProfile).setOnClickListener(v -> showEditProfileDialog());

        // Initialize repositories
        userRepository = new UserRepository();
        expenseRepository = new ExpenseRepository();

        // Observe user profile
        userRepository.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                currentProfile = profile;
                bindProfileData(profile);
            }
        });

        // Logout button
        view.findViewById(R.id.btnLogout).setOnClickListener(v -> {
            // Sign out from Firebase
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut();

            // Sign out from Google Client
            com.google.android.gms.auth.api.signin.GoogleSignInOptions gso = new com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN).build();
            com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(requireActivity(), gso).signOut().addOnCompleteListener(task -> {
                Intent intent = new Intent(requireContext(), com.example.fairshare.ui.login.LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                requireActivity().finish();
            });
        });

        // Observe expenses for live stats
        expenseRepository.getExpenses().observe(getViewLifecycleOwner(), this::calculateStats);
    }

    private void bindProfileData(UserProfile profile) {
        String name = profile.getDisplayName() != null ? profile.getDisplayName() : "User";
        String tag = profile.getTagline() != null ? profile.getTagline() : "0000";

        tvDisplayName.setText(name);
        tvTagline.setText("#" + tag);

        tvUserEmail.setText(profile.getEmail() != null ? profile.getEmail() : "Not set");
        tvUserPhone.setText(profile.getPhoneNumber() != null && !profile.getPhoneNumber().isEmpty()
                ? profile.getPhoneNumber() : "Not set");
        tvUserLocation.setText(profile.getLocation() != null && !profile.getLocation().isEmpty()
                ? profile.getLocation() : "Not set");

        // Avatar: show Google photo if available, otherwise show initial letter
        if (profile.getPhotoUrl() != null && !profile.getPhotoUrl().isEmpty()) {
            loadAvatarFromUrl(profile.getPhotoUrl());
        } else {
            ivAvatar.setVisibility(View.GONE);
            flAvatarFallback.setVisibility(View.VISIBLE);
            tvAvatarInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
        }
    }

    private void loadAvatarFromUrl(String urlString) {
        imageExecutor.execute(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                input.close();

                mainHandler.post(() -> {
                    if (isAdded() && ivAvatar != null) {
                        ivAvatar.setImageBitmap(bitmap);
                        ivAvatar.setVisibility(View.VISIBLE);
                        flAvatarFallback.setVisibility(View.GONE);
                    }
                });
            } catch (Exception e) {
                // Fallback to initial letter on error
                mainHandler.post(() -> {
                    if (isAdded()) {
                        ivAvatar.setVisibility(View.GONE);
                        flAvatarFallback.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    private void calculateStats(List<Transaction> transactions) {
        if (transactions == null) return;

        double totalExpenses = 0;

        for (Transaction t : transactions) {
            // All transactions are now expenses (income tracking removed)
            totalExpenses += t.getAmount();
        }

        tvTotalExpenses.setText(CurrencyHelper.format(totalExpenses));
    }

    @Override
    public void onFastAction() {
        showEditProfileDialog();
    }

    private void showEditProfileDialog() {
        Dialog dialog = new Dialog(requireContext(), R.style.Theme_FairShare_Dialog);
        dialog.setContentView(R.layout.dialog_edit_profile);

        TextInputEditText etDisplayName = dialog.findViewById(R.id.etDisplayName);
        TextInputEditText etTagline = dialog.findViewById(R.id.etTagline);
        TextInputLayout tilTagline = dialog.findViewById(R.id.tilTagline);
        TextInputEditText etPhone = dialog.findViewById(R.id.etPhone);
        TextInputEditText etLocation = dialog.findViewById(R.id.etLocation);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);
        MaterialButton btnSave = dialog.findViewById(R.id.btnSave);

        // Pre-fill fields with current data
        if (currentProfile != null) {
            etDisplayName.setText(currentProfile.getDisplayName());
            etTagline.setText(currentProfile.getTagline());
            etPhone.setText(currentProfile.getPhoneNumber());
            etLocation.setText(currentProfile.getLocation());
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = etDisplayName.getText().toString().trim();
            String tagline = etTagline.getText().toString().trim().toUpperCase();
            String phone = etPhone.getText().toString().trim();
            String location = etLocation.getText().toString().trim();

            // Validate display name
            if (name.isEmpty()) {
                etDisplayName.setError("Display name is required");
                return;
            }

            // Validate tagline: exactly 4 alphanumeric characters
            if (tagline.length() != 4 || !tagline.matches("[A-Z0-9]{4}")) {
                tilTagline.setError("Must be exactly 4 alphanumeric characters");
                return;
            } else {
                tilTagline.setError(null);
            }

            // Update the profile
            if (currentProfile != null) {
                currentProfile.setDisplayName(name);
                currentProfile.setTagline(tagline);
                currentProfile.setPhoneNumber(phone);
                currentProfile.setLocation(location);
                userRepository.saveUserProfile(currentProfile);
                Toast.makeText(requireContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            int width = (int) (350 * getResources().getDisplayMetrics().density);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        userRepository.removeListener();
        expenseRepository.removeListener();
    }
}
