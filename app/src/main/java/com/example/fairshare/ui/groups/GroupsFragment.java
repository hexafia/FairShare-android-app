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
import com.example.fairshare.GroupRepository;
import com.example.fairshare.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class GroupsFragment extends Fragment implements FastActionHandler {

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

    @Override
    public void onFastAction() {
        showGroupOptions();
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
