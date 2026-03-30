package com.example.fairshare.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.fairshare.R;
import com.example.fairshare.ui.settings.SettingsActivity;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Settings gear button → opens SettingsActivity
        view.findViewById(R.id.btnSettings).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SettingsActivity.class);
            startActivity(intent);
        });

        // Edit Profile button (placeholder)
        view.findViewById(R.id.btnEditProfile).setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Edit Profile — Coming soon!", Toast.LENGTH_SHORT).show();
        });
    }
}
