package com.example.fairshare.ui.groups;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
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
    private ImageButton btnSettledGroupsMenu;
    private List<Group> allSettledGroups = new ArrayList<>();
    private int settledGroupsDisplayMode = 0; // 0=Show All, 1=Hide All, 2=Show Recent (5)

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
        btnSettledGroupsMenu = view.findViewById(R.id.btnSettledGroupsMenu);
        
        // Setup settled groups menu
        setupSettledGroupsMenu();

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
        fabGlobalAction.setOnClickListener(v -> showGroupOptions());

        // Observe groups
        groupRepository = new GroupRepository();
        groupRepository.getGroups().observe(getViewLifecycleOwner(), groups -> {
            if (groups != null) {
                List<Group> activeGroups = new ArrayList<>();
                allSettledGroups = new ArrayList<>();

                for (Group group : groups) {
                    if (group.isActive()) {
                        activeGroups.add(group);
                    } else if (group.isSettled()) {
                        allSettledGroups.add(group);
                    }
                }

                activeAdapter.submitList(activeGroups);
                
                // Apply settled groups display mode
                updateSettledGroupsDisplay();
                
                // Update empty states
                if (activeGroups.isEmpty()) {
                    layoutEmptyActive.setVisibility(View.VISIBLE);
                    rvActiveGroups.setVisibility(View.GONE);
                } else {
                    layoutEmptyActive.setVisibility(View.GONE);
                    rvActiveGroups.setVisibility(View.VISIBLE);
                }

                // Control FAB visibility - only show when Active Groups section is active
                if (fabGlobalAction != null) {
                    fabGlobalAction.setVisibility(View.VISIBLE); // Always show in groups fragment for creating new groups
                }
            }
        });
    }

    private void setupSettledGroupsMenu() {
        btnSettledGroupsMenu.setOnClickListener(v -> showSettledGroupsPopupMenu());
    }

    private void showSettledGroupsPopupMenu() {
        PopupMenu popup = new PopupMenu(requireContext(), btnSettledGroupsMenu);
        popup.getMenuInflater().inflate(R.menu.settled_groups_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_hide_all_settled) {
                settledGroupsDisplayMode = 1; // Hide All
                updateSettledGroupsDisplay();
                return true;
            } else if (itemId == R.id.action_show_recent_5) {
                settledGroupsDisplayMode = 2; // Show Recent (5)
                updateSettledGroupsDisplay();
                return true;
            } else if (itemId == R.id.action_show_all) {
                settledGroupsDisplayMode = 0; // Show All
                updateSettledGroupsDisplay();
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void updateSettledGroupsDisplay() {
        List<Group> displaySettledGroups = new ArrayList<>();
        
        switch (settledGroupsDisplayMode) {
            case 0: // Show All
                displaySettledGroups = new ArrayList<>(allSettledGroups);
                break;
            case 1: // Hide All
                displaySettledGroups = new ArrayList<>();
                break;
            case 2: // Show Recent (5)
                // Sort by timestamp (newest first) and take first 5
                List<Group> sortedGroups = new ArrayList<>(allSettledGroups);
                sortedGroups.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                int limit = Math.min(5, sortedGroups.size());
                displaySettledGroups = sortedGroups.subList(0, limit);
                break;
        }
        
        settledAdapter.submitList(displaySettledGroups);
        
        // Update empty states
        if (displaySettledGroups.isEmpty()) {
            if (settledGroupsDisplayMode == 1) {
                // When hiding all, don't show empty state
                layoutEmptySettled.setVisibility(View.GONE);
                rvSettledGroups.setVisibility(View.GONE);
            } else {
                layoutEmptySettled.setVisibility(View.VISIBLE);
                rvSettledGroups.setVisibility(View.GONE);
            }
        } else {
            layoutEmptySettled.setVisibility(View.GONE);
            rvSettledGroups.setVisibility(View.VISIBLE);
        }
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
