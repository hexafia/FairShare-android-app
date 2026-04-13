package com.example.fairshare.ui.groups;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fairshare.Group;
import com.example.fairshare.GroupRepository;
import com.example.fairshare.R;
import com.google.android.material.button.MaterialButton;

public class GroupMembersFragment extends Fragment {

    private GroupRepository groupRepository;
    private String groupId;
    private MemberAdapter memberAdapter;
    private Group currentGroup;

    // UI elements
    private RecyclerView rvMembers;
    private View layoutEmptyMembers;
    private MaterialButton btnAddMember;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_group_members, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        groupRepository = new GroupRepository();
        
        // Get group ID from arguments
        Bundle args = getArguments();
        if (args != null) {
            groupId = args.getString("GROUP_ID");
        }

        // Setup UI
        btnAddMember = view.findViewById(R.id.btnAddMember);
        rvMembers = view.findViewById(R.id.rvMembers);
        layoutEmptyMembers = view.findViewById(R.id.layoutEmptyMembers);

        // Setup RecyclerView
        rvMembers.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Setup add member button
        btnAddMember.setOnClickListener(v -> {
            // TODO: Show add member dialog
            // For now, just show a toast
            android.widget.Toast.makeText(requireContext(), "Add member feature coming soon", android.widget.Toast.LENGTH_SHORT).show();
        });

        // Load group data
        if (groupId != null) {
            loadGroupData();
        }
    }

    private void loadGroupData() {
        groupRepository.getGroups().observe(getViewLifecycleOwner(), groups -> {
            if (groups != null) {
                for (Group group : groups) {
                    if (group.getId().equals(groupId)) {
                        currentGroup = group;
                        updateMembersList();
                        updateAddMemberButton();
                        break;
                    }
                }
            }
        });
    }

    private void updateMembersList() {
        if (currentGroup == null) return;

        // Create and set adapter with group creator ID
        memberAdapter = new MemberAdapter(currentGroup.getCreatedBy());
        rvMembers.setAdapter(memberAdapter);

        // Submit member list
        memberAdapter.submitList(currentGroup.getMembers());

        // Update empty state
        if (currentGroup.getMembers().isEmpty()) {
            layoutEmptyMembers.setVisibility(View.VISIBLE);
            rvMembers.setVisibility(View.GONE);
        } else {
            layoutEmptyMembers.setVisibility(View.GONE);
            rvMembers.setVisibility(View.VISIBLE);
        }
    }

    private void updateAddMemberButton() {
        if (currentGroup == null) return;

        // Disable add member button for settled groups
        if (currentGroup.isSettled()) {
            btnAddMember.setEnabled(false);
            btnAddMember.setAlpha(0.5f);
            btnAddMember.setText("Group Settled - Cannot Add Members");
        } else {
            btnAddMember.setEnabled(true);
            btnAddMember.setAlpha(1.0f);
            btnAddMember.setText("Add Member");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (groupRepository != null) {
            groupRepository.removeListeners();
        }
    }
}
