package com.example.fairshare.ui.groups;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.MemberViewHolder> {

    private List<String> memberUids = new ArrayList<>();
    private Map<String, String> memberNames;
    private String groupCreatorId;
    private OnMemberClickListener listener;

    public interface OnMemberClickListener {
        void onMemberClick(String memberUid);
    }

    public void setOnMemberClickListener(OnMemberClickListener listener) {
        this.listener = listener;
    }

    public MembersAdapter(Map<String, String> memberNames, String groupCreatorId) {
        this.memberNames = memberNames;
        this.groupCreatorId = groupCreatorId;
    }

    public void updateMembers(List<String> memberUids) {
        this.memberUids = new ArrayList<>(memberUids);
        // Sort: creator first, then alphabetically
        this.memberUids.sort((uid1, uid2) -> {
            if (uid1.equals(groupCreatorId)) return -1;
            if (uid2.equals(groupCreatorId)) return 1;
            return memberNames.get(uid1).compareToIgnoreCase(memberNames.get(uid2));
        });
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        String uid = memberUids.get(position);
        String name = memberNames.get(uid);
        
        if (uid.equals(groupCreatorId)) {
            holder.textView.setText(name + " (Creator)");
        } else {
            holder.textView.setText(name);
        }
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMemberClick(uid);
            }
        });
    }

    @Override
    public int getItemCount() {
        return memberUids.size();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}
