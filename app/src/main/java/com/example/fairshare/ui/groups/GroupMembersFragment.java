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
        MaterialButton btnAddMember = view.findViewById(R.id.btnAddMember);
        RecyclerView rvMembers = view.findViewById(R.id.rvMembers);
        View layoutEmptyMembers = view.findViewById(R.id.layoutEmptyMembers);

        // Setup RecyclerView
        rvMembers.setLayoutManager(new LinearLayoutManager(requireContext()));
        // TODO: Create and set adapter for members

        // Setup add member button
        btnAddMember.setOnClickListener(v -> {
            // TODO: Show add member dialog
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
                        // TODO: Load and display members
                        break;
                    }
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (groupRepository != null) {
            groupRepository.removeListeners();
        }
    }
}
