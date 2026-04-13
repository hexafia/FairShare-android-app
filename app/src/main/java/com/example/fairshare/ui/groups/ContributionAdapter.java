package com.example.fairshare.ui.groups;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fairshare.ContributionSummary;
import com.example.fairshare.R;

import java.util.Locale;
import java.util.Objects;

public class ContributionAdapter extends ListAdapter<ContributionSummary, ContributionAdapter.ViewHolder> {

    public ContributionAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<ContributionSummary> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ContributionSummary>() {
                @Override
                public boolean areItemsTheSame(@NonNull ContributionSummary oldItem, @NonNull ContributionSummary newItem) {
                    return Objects.equals(oldItem.getUid(), newItem.getUid());
                }

                @Override
                public boolean areContentsTheSame(@NonNull ContributionSummary oldItem, @NonNull ContributionSummary newItem) {
                    return oldItem.getAmount() == newItem.getAmount()
                            && oldItem.getPercentage() == newItem.getPercentage()
                            && Objects.equals(oldItem.getName(), newItem.getName());
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contribution, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvInitial;
        private final TextView tvName;
        private final TextView tvAmount;
        private final TextView tvShare;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInitial = itemView.findViewById(R.id.tvContributionInitial);
            tvName = itemView.findViewById(R.id.tvContributionName);
            tvAmount = itemView.findViewById(R.id.tvContributionAmount);
            tvShare = itemView.findViewById(R.id.tvContributionShare);
        }

        void bind(ContributionSummary summary) {
            String name = summary.getName();
            if (name == null || name.isEmpty()) {
                name = "Member";
            }

            tvInitial.setText(String.valueOf(name.charAt(0)).toUpperCase(Locale.getDefault()));
            tvName.setText(name);
            tvAmount.setText(String.format(Locale.US, "₱%,.2f", summary.getAmount()));
            tvShare.setText(String.format(Locale.US, "%.1f%% of total", summary.getPercentage()));
        }
    }
}
