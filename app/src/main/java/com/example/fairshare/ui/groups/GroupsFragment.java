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

import com.example.fairshare.FastActionHandler;
import com.example.fairshare.Group;
import com.example.fairshare.GroupRepository;
import com.example.fairshare.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class GroupsFragment extends Fragment implements FastActionHandler {

    private GroupRepository groupRepository;
    private GroupAdapter activeAdapter;
    private GroupAdapter settledAdapter;
    private View layoutEmptyActive;
    private View layoutEmptySettled;
    private RecyclerView rvActiveGroups;
    private RecyclerView rvSettledGroups;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabGlobalAction;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_groups, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize UI elements
        layoutEmptyActive = view.findViewById(R.id.layoutEmptyActive);
        layoutEmptySettled = view.findViewById(R.id.layoutEmptySettled);
        rvActiveGroups = view.findViewById(R.id.rvActiveGroups);
        rvSettledGroups = view.findViewById(R.id.rvSettledGroups);

        // Setup button listeners
        MaterialButton btnCreateNewGroup = view.findViewById(R.id.btnCreateNewGroup);
        MaterialButton btnJoinViaCode = view.findViewById(R.id.btnJoinViaCode);

        btnCreateNewGroup.setOnClickListener(v -> showCreateGroupDialog());
        btnJoinViaCode.setOnClickListener(v -> showJoinGroupDialog());

        // Setup RecyclerViews
        activeAdapter = new GroupAdapter(group -> {
            Intent intent = new Intent(requireContext(), GroupLobbyActivity.class);
            intent.putExtra("GROUP_ID", group.getId());
            intent.putExtra("GROUP_NAME", group.getName());
            intent.putExtra("SHARE_CODE", group.getShareCode());
            startActivity(intent);
        });

        settledAdapter = new GroupAdapter(group -> {
            Intent intent = new Intent(requireContext(), GroupLobbyActivity.class);
            intent.putExtra("GROUP_ID", group.getId());
            intent.putExtra("GROUP_NAME", group.getName());
            intent.putExtra("SHARE_CODE", group.getShareCode());
            startActivity(intent);
        });

        rvActiveGroups.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvActiveGroups.setAdapter(activeAdapter);

        rvSettledGroups.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSettledGroups.setAdapter(settledAdapter);

        // Initialize FAB
        fabGlobalAction = view.findViewById(R.id.fabGlobalAction);
        fabGlobalAction.setOnClickListener(v -> showCreateGroupDialog());

        // Observe groups
        groupRepository = new GroupRepository();
        groupRepository.getGroups().observe(getViewLifecycleOwner(), groups -> {
            if (groups != null) {
                List<Group> activeGroups = new ArrayList<>();
                List<Group> settledGroups = new ArrayList<>();

                for (Group group : groups) {
                    if (group.isActive()) {
                        activeGroups.add(group);
                    } else if (group.isSettled()) {
                        settledGroups.add(group);
                    }
                }

                activeAdapter.submitList(activeGroups);
                settledAdapter.submitList(settledGroups);

                // Update empty states
                if (activeGroups.isEmpty()) {
                    layoutEmptyActive.setVisibility(View.VISIBLE);
                    rvActiveGroups.setVisibility(View.GONE);
                } else {
                    layoutEmptyActive.setVisibility(View.GONE);
                    rvActiveGroups.setVisibility(View.VISIBLE);
                }

                if (settledGroups.isEmpty()) {
                    layoutEmptySettled.setVisibility(View.VISIBLE);
                    rvSettledGroups.setVisibility(View.GONE);
                } else {
                    layoutEmptySettled.setVisibility(View.GONE);
                    rvSettledGroups.setVisibility(View.VISIBLE);
                }

                // Control FAB visibility - only show when Active Groups section is active
                if (fabGlobalAction != null) {
                    fabGlobalAction.setVisibility(View.VISIBLE); // Always show in groups fragment for creating new groups
                }
            }
        });
    }

    @Override
    public void onFastAction() {
        showCreateGroupDialog();
    }

    private void showGroupOptions() {
        Dialog dialog = new Dialog(requireContext(), R.style.Theme_FairShare_Dialog);
        dialog.setContentView(R.layout.dialog_group_options);

        MaterialButton btnCreate = dialog.findViewById(R.id.btnCreateOption);
        MaterialButton btnJoin = dialog.findViewById(R.id.btnJoinOption);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);

        btnCreate.setOnClickListener(v -> {
            dialog.dismiss();
            showCreateGroupDialog();
        });

        btnJoin.setOnClickListener(v -> {
            dialog.dismiss();
            showJoinGroupDialog();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        if (dialog.getWindow() != null) {
            int width = (int) (350 * getResources().getDisplayMetrics().density);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

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
