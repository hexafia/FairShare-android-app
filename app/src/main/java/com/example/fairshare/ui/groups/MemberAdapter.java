package com.example.fairshare.ui.groups;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fairshare.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class MemberAdapter extends ListAdapter<String, MemberAdapter.MemberViewHolder> {

    private final String groupCreatorId;
    private final String currentUserId;

    public MemberAdapter(String groupCreatorId) {
        super(new DiffUtil.ItemCallback<String>() {
            @Override
            public boolean areItemsTheSame(@NonNull String oldItem, @NonNull String newItem) {
                return oldItem.equals(newItem);
            }

            @Override
            public boolean areContentsTheSame(@NonNull String oldItem, @NonNull String newItem) {
                return oldItem.equals(newItem);
            }
        });
        
        this.groupCreatorId = groupCreatorId;
        
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        this.currentUserId = currentUser != null ? currentUser.getUid() : null;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        String memberUid = getItem(position);
        
        // For now, we'll display the UID. In a real app, you'd fetch user profiles
        // to display names, emails, avatars, etc.
        holder.bind(memberUid, memberUid.equals(groupCreatorId));
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvMemberName;
        private final TextView tvMemberEmail;
        private final TextView tvMemberInitial;
        private final TextView tvMemberRole;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
            tvMemberEmail = itemView.findViewById(R.id.tvMemberEmail);
            tvMemberInitial = itemView.findViewById(R.id.tvMemberInitial);
            tvMemberRole = itemView.findViewById(R.id.tvMemberRole);
        }

        public void bind(String memberUid, boolean isCreator) {
            // Display member information
            // In a real implementation, you'd fetch user profile data
            tvMemberName.setText("User " + memberUid.substring(0, 6));
            tvMemberEmail.setText(memberUid + "@example.com");
            
            // Set avatar initial
            if (memberUid.length() > 0) {
                tvMemberInitial.setText(String.valueOf(Character.toUpperCase(memberUid.charAt(0))));
            }
            
            // Show creator role badge
            if (isCreator) {
                tvMemberRole.setVisibility(View.VISIBLE);
                tvMemberRole.setText("Creator");
            } else {
                tvMemberRole.setVisibility(View.GONE);
            }
        }
    }
}
