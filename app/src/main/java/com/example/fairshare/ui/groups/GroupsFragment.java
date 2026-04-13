package com.example.fairshare.ui.groups;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fairshare.GroupRepository;
import com.example.fairshare.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

public class GroupsFragment extends Fragment {

    private GroupRepository groupRepository;
    private GroupAdapter activeAdapter;
    private GroupAdapter settledAdapter;
    private View tvEmptyActive;
    private View tvEmptySettled;
    private RecyclerView rvActiveGroups;
    private RecyclerView rvSettledGroups;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_groups, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvEmptyActive = view.findViewById(R.id.tvEmptyActive);
        tvEmptySettled = view.findViewById(R.id.tvEmptySettled);
        rvActiveGroups = view.findViewById(R.id.rvActiveGroups);
        rvSettledGroups = view.findViewById(R.id.rvSettledGroups);
        
        MaterialButton btnCreateGroup = view.findViewById(R.id.btnCreateGroup);
        MaterialButton btnJoinGroup = view.findViewById(R.id.btnJoinGroup);

        // Setup RecyclerViews
        GroupAdapter.OnGroupClickListener listener = group -> {
            Intent intent = new Intent(requireContext(), GroupLobbyActivity.class);
            intent.putExtra("GROUP_ID", group.getId());
            intent.putExtra("GROUP_NAME", group.getName());
            intent.putExtra("SHARE_CODE", group.getShareCode());
            intent.putExtra("CREATED_BY", group.getCreatedBy());
            intent.putExtra("STATUS", group.getStatus());
            startActivity(intent);
        };

        activeAdapter = new GroupAdapter(listener);
        rvActiveGroups.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvActiveGroups.setAdapter(activeAdapter);

        settledAdapter = new GroupAdapter(listener);
        rvSettledGroups.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSettledGroups.setAdapter(settledAdapter);

        btnCreateGroup.setOnClickListener(v -> showCreateGroupDialog());
        btnJoinGroup.setOnClickListener(v -> showJoinGroupDialog());

        // Observe groups
        groupRepository = new GroupRepository();
        groupRepository.getGroups().observe(getViewLifecycleOwner(), groups -> {
            List<com.example.fairshare.Group> activeGroups = new java.util.ArrayList<>();
            List<com.example.fairshare.Group> settledGroups = new java.util.ArrayList<>();

            if (groups != null) {
                for (com.example.fairshare.Group group : groups) {
                    if ("settled".equalsIgnoreCase(group.getStatus())) {
                        settledGroups.add(group);
                    } else {
                        activeGroups.add(group);
                    }
                }
            }

            activeAdapter.submitList(activeGroups);
            settledAdapter.submitList(settledGroups);

            tvEmptyActive.setVisibility(activeGroups.isEmpty() ? View.VISIBLE : View.GONE);
            rvActiveGroups.setVisibility(activeGroups.isEmpty() ? View.GONE : View.VISIBLE);

            tvEmptySettled.setVisibility(settledGroups.isEmpty() ? View.VISIBLE : View.GONE);
            rvSettledGroups.setVisibility(settledGroups.isEmpty() ? View.GONE : View.VISIBLE);
        });
    }

    // Removed showGroupOptions method as we now use direct buttons

    private void showCreateGroupDialog() {
        Dialog dialog = new Dialog(requireContext(), R.style.Theme_FairShare_Dialog);
        dialog.setContentView(R.layout.dialog_create_group);

        TextInputEditText etGroupName = dialog.findViewById(R.id.etGroupName);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);
        MaterialButton btnCreate = dialog.findViewById(R.id.btnCreate);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnCreate.setOnClickListener(v -> {
            String name = etGroupName.getText() != null ? etGroupName.getText().toString().trim() : "";
            if (name.isEmpty()) {
                etGroupName.setError("Group name is required");
                return;
            }

            groupRepository.createGroup(name, new GroupRepository.OnCompleteCallback() {
                @Override
                public void onSuccess(String shareCode) {
                    Toast.makeText(requireContext(),
                            "Group created! Share code: " + shareCode, Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            int width = (int) (350 * getResources().getDisplayMetrics().density);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void showJoinGroupDialog() {
        Dialog dialog = new Dialog(requireContext(), R.style.Theme_FairShare_Dialog);
        dialog.setContentView(R.layout.dialog_join_group);

        TextInputEditText etShareCode = dialog.findViewById(R.id.etShareCode);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);
        MaterialButton btnJoin = dialog.findViewById(R.id.btnJoin);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnJoin.setOnClickListener(v -> {
            String code = etShareCode.getText() != null ? etShareCode.getText().toString().trim() : "";
            if (code.length() != 6) {
                etShareCode.setError("Code must be 6 characters");
                return;
            }

            groupRepository.joinGroup(code, new GroupRepository.OnCompleteCallback() {
                @Override
                public void onSuccess(String message) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                }
            });
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
        groupRepository.removeListeners();
    }
}
