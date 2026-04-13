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

public class GroupExpensesFragment extends Fragment {

    private GroupRepository groupRepository;
    private String groupId;
    private Group currentGroup;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_group_expenses, container, false);
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
        MaterialButton btnAddExpense = view.findViewById(R.id.btnAddExpense);
        RecyclerView rvExpenses = view.findViewById(R.id.rvExpenses);
        View layoutEmptyExpenses = view.findViewById(R.id.layoutEmptyExpenses);

        // Setup RecyclerView
        rvExpenses.setLayoutManager(new LinearLayoutManager(requireContext()));
        // TODO: Create and set adapter for expenses

        // Setup add expense button
        btnAddExpense.setOnClickListener(v -> {
            // TODO: Navigate to add expense activity/fragment
        });

        // Load group data to check if settled
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
                        updateUIForGroupStatus();
                        break;
                    }
                }
            }
        });
    }

    private void updateUIForGroupStatus() {
        if (currentGroup == null) return;

        MaterialButton btnAddExpense = getView().findViewById(R.id.btnAddExpense);
        
        // Disable add expense button for settled groups
        if (currentGroup.isSettled()) {
            btnAddExpense.setEnabled(false);
            btnAddExpense.setAlpha(0.5f);
            btnAddExpense.setText("Group Settled - Cannot Add Expenses");
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
