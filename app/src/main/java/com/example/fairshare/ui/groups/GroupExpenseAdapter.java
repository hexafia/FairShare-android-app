package com.example.fairshare.ui.groups;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fairshare.GroupExpense;
import com.example.fairshare.R;

import java.util.Objects;

public class GroupExpenseAdapter extends ListAdapter<GroupExpense, GroupExpenseAdapter.ViewHolder> {

    public GroupExpenseAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<GroupExpense> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<GroupExpense>() {
                @Override
                public boolean areItemsTheSame(@NonNull GroupExpense oldItem, @NonNull GroupExpense newItem) {
                    return Objects.equals(oldItem.getId(), newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull GroupExpense oldItem, @NonNull GroupExpense newItem) {
                    return Objects.equals(oldItem.getTitle(), newItem.getTitle())
                            && oldItem.getAmount() == newItem.getAmount();
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group_expense, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvExpenseTitle, tvPaidBy, tvSplitInfo, tvAmount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvExpenseTitle = itemView.findViewById(R.id.tvExpenseTitle);
            tvPaidBy = itemView.findViewById(R.id.tvPaidBy);
            tvSplitInfo = itemView.findViewById(R.id.tvSplitInfo);
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }

        void bind(GroupExpense expense) {
            tvExpenseTitle.setText(expense.getTitle());
            tvPaidBy.setText("Paid by " + (expense.getPayerName() != null ? expense.getPayerName() : "Unknown"));
            tvSplitInfo.setText("Split equally among " + expense.getParticipantCount() + " people");
            tvAmount.setText(String.format("₱%,.0f", expense.getAmount()));
        }
    }
}
