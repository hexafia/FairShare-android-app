package com.example.fairshare.ui.groups;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.fairshare.Group;
import com.example.fairshare.GroupRepository;
import com.example.fairshare.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.widget.TextView;

public class GroupDetailFragment extends Fragment {

    private GroupRepository groupRepository;
    private String groupId;
    private String currentUserId;
    private Group currentGroup;

    // UI elements
    private TextView tvGroupName;
    private TextView tvGroupCode;
    private TextView tvGroupStatus;
    private TextView tvMemberCount;
    private MaterialButton btnMarkAsAccomplished;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_group_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize repository and current user
        groupRepository = new GroupRepository();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = currentUser != null ? currentUser.getUid() : null;

        // Get group ID from arguments
        Bundle args = getArguments();
        if (args != null) {
            groupId = args.getString("GROUP_ID");
        }

        // Initialize UI elements
        initializeUI(view);
        setupViewPager();

        // Load group data
        if (groupId != null) {
            loadGroupData();
        }
        
        // CRITICAL: Check if group is settled and disable UI accordingly
        if (currentGroup != null && currentGroup.isSettled()) {
            disableAllInputElements();
        }
    }

    private void initializeUI(View view) {
        tvGroupName = view.findViewById(R.id.tvGroupName);
        tvGroupCode = view.findViewById(R.id.tvGroupCode);
        tvGroupStatus = view.findViewById(R.id.tvGroupStatus);
        tvMemberCount = view.findViewById(R.id.tvMemberCount);
        btnMarkAsAccomplished = view.findViewById(R.id.btnMarkAsAccomplished);
        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPager);

        // Setup back button
        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        // Setup Mark as Accomplished button
        btnMarkAsAccomplished.setOnClickListener(v -> markGroupAsAccomplished());
    }

    private void setupViewPager() {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            GroupDetailPagerAdapter adapter = new GroupDetailPagerAdapter(activity, groupId);
            viewPager.setAdapter(adapter);

            new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
                if (position == 0) {
                    tab.setText("Expenses");
                } else if (position == 1) {
                    tab.setText("Settle Up");
                } else {
                    tab.setText("Members");
                }
            }).attach();
        }
    }

    private void loadGroupData() {
        // For simplicity, we'll use a basic approach. In a real app, you'd want to 
        // observe the group data from Firestore
        groupRepository.getGroups().observe(getViewLifecycleOwner(), groups -> {
            if (groups != null) {
                for (Group group : groups) {
                    if (group.getId().equals(groupId)) {
                        currentGroup = group;
                        updateUI();
                        break;
                    }
                }
            }
        });
    }

    private void updateUI() {
        if (currentGroup == null) return;

        // Update group info
        tvGroupName.setText(currentGroup.getName());
        tvGroupCode.setText("Group Code: " + currentGroup.getShareCode());
        tvMemberCount.setText("Members: " + currentGroup.getMemberCount());

        // Update status
        if (currentGroup.isActive()) {
            tvGroupStatus.setText("Status: Active");
            tvGroupStatus.setTextColor(getResources().getColor(R.color.teal, null));
        } else if (currentGroup.isSettled()) {
            tvGroupStatus.setText("Status: Settled");
            tvGroupStatus.setTextColor(getResources().getColor(R.color.gray, null));
        }

        // Show/hide Mark as Accomplished button based on creator and status
        if (currentUserId != null && currentUserId.equals(currentGroup.getCreatedBy()) 
                && currentGroup.isActive()) {
            btnMarkAsAccomplished.setVisibility(View.VISIBLE);
        } else {
            btnMarkAsAccomplished.setVisibility(View.GONE);
        }

        // Disable add expense functionality for settled groups
        if (currentGroup.isSettled()) {
            // FAB was removed from XML, so no need to hide it
            // This would be communicated to the expenses fragment
        }
    }

    private void markGroupAsAccomplished() {
        if (groupId == null || currentGroup == null) return;

        groupRepository.updateGroupStatus(groupId, "settled", new GroupRepository.OnCompleteCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(requireContext(), "Group marked as accomplished!", Toast.LENGTH_SHORT).show();
                // The UI will update automatically when the group data changes
            }

            @Override
            public void onError(String error) {
                Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void disableAllInputElements() {
        // Disable Mark as Accomplished button
        if (btnMarkAsAccomplished != null) {
            btnMarkAsAccomplished.setEnabled(false);
            btnMarkAsAccomplished.setVisibility(View.GONE);
        }

        // FAB was removed from XML, so no need to disable it

        // Disable tab interaction for settled groups
        if (tabLayout != null) {
            tabLayout.setEnabled(false);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (groupRepository != null) {
            groupRepository.removeListeners();
        }
    }

    // ViewPager Adapter for tabs
    private static class GroupDetailPagerAdapter extends FragmentStateAdapter {
        private final String groupId;

        public GroupDetailPagerAdapter(@NonNull FragmentActivity fragmentActivity, String groupId) {
            super(fragmentActivity);
            this.groupId = groupId;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Fragment fragment;
            Bundle args = new Bundle();
            args.putString("GROUP_ID", groupId);

            if (position == 0) {
                fragment = new GroupExpensesFragment();
            } else if (position == 1) {
                fragment = new GroupSettleUpFragment();
            } else {
                fragment = new GroupMembersFragment();
            }

            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getItemCount() {
            return 3; // Expenses, Settle Up, and Members tabs
        }
    }
}
