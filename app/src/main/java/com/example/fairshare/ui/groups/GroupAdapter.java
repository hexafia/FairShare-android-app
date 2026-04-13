package com.example.fairshare.ui.groups;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fairshare.Group;
import com.example.fairshare.R;

import java.util.Objects;

public class GroupAdapter extends ListAdapter<Group, GroupAdapter.GroupViewHolder> {

    public interface OnGroupClickListener {
        void onGroupClick(Group group);
    }

    private final OnGroupClickListener clickListener;

    public GroupAdapter(OnGroupClickListener clickListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
    }

    private static final DiffUtil.ItemCallback<Group> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Group>() {
                @Override
                public boolean areItemsTheSame(@NonNull Group oldItem, @NonNull Group newItem) {
                    return Objects.equals(oldItem.getId(), newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Group oldItem, @NonNull Group newItem) {
                    return Objects.equals(oldItem.getName(), newItem.getName())
                            && oldItem.getMemberCount() == newItem.getMemberCount();
                }
            };

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class GroupViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvGroupInitial, tvGroupName, tvMemberCount, tvShareCode;

        GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGroupInitial = itemView.findViewById(R.id.tvGroupInitial);
            tvGroupName = itemView.findViewById(R.id.tvGroupName);
            tvMemberCount = itemView.findViewById(R.id.tvMemberCount);
            tvShareCode = itemView.findViewById(R.id.tvShareCode);
        }

        void bind(Group group) {
            String name = group.getName() != null ? group.getName() : "Group";
            tvGroupInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
            tvGroupName.setText(name);
            tvMemberCount.setText(group.getMemberCount() + " members");
            tvShareCode.setText(group.getShareCode());

            itemView.setOnClickListener(v -> clickListener.onGroupClick(group));
        }
    }
}
