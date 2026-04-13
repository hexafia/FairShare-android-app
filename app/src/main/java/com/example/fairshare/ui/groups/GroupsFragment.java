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
    private GroupAdapter adapter;
    private View layoutEmpty;
    private RecyclerView rvGroups;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_groups, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        rvGroups = view.findViewById(R.id.rvGroups);
        FloatingActionButton fab = view.findViewById(R.id.fabGroup);

        // Setup RecyclerView
        adapter = new GroupAdapter(group -> {
            Intent intent = new Intent(requireContext(), GroupLobbyActivity.class);
            intent.putExtra("GROUP_ID", group.getId());
            intent.putExtra("GROUP_NAME", group.getName());
            intent.putExtra("SHARE_CODE", group.getShareCode());
            startActivity(intent);
        });
        rvGroups.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvGroups.setAdapter(adapter);

        // FAB → show create/join options
        fab.setOnClickListener(v -> showGroupOptions());

        // Observe groups
        groupRepository = new GroupRepository();
        groupRepository.getGroups().observe(getViewLifecycleOwner(), groups -> {
            adapter.submitList(groups);
            if (groups == null || groups.isEmpty()) {
                layoutEmpty.setVisibility(View.VISIBLE);
                rvGroups.setVisibility(View.GONE);
            } else {
                layoutEmpty.setVisibility(View.GONE);
                rvGroups.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showGroupOptions() {
        String[] options = {"Create New Group", "Join via Code"};
        new AlertDialog.Builder(requireContext(), R.style.Theme_FairShare_Dialog)
                .setTitle("Groups")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showCreateGroupDialog();
                    } else {
                        showJoinGroupDialog();
                    }
                })
                .show();
    }

    private void showCreateGroupDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_create_group);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

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
    }

    private void showJoinGroupDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_join_group);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        groupRepository.removeListeners();
    }
}
