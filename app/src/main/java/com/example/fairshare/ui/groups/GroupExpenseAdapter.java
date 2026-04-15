package com.example.fairshare.ui.groups;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fairshare.CurrencyHelper;
import com.example.fairshare.GroupExpense;
import com.example.fairshare.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GroupExpenseAdapter extends ListAdapter<GroupExpense, GroupExpenseAdapter.ViewHolder> {

    private Map<String, String> memberNames = new HashMap<>();

    public GroupExpenseAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setMemberNames(Map<String, String> names) {
        this.memberNames = names;
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
        return new ViewHolder(view, memberNames);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvExpenseTitle, tvPaidBy, tvSplitInfo, tvAmount;
        private final Map<String, String> memberNames;

        ViewHolder(@NonNull View itemView, Map<String, String> memberNames) {
            super(itemView);
            this.memberNames = memberNames;
            tvExpenseTitle = itemView.findViewById(R.id.tvExpenseTitle);
            tvPaidBy = itemView.findViewById(R.id.tvPaidBy);
            tvSplitInfo = itemView.findViewById(R.id.tvSplitInfo);
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }

        void bind(GroupExpense expense) {
            tvExpenseTitle.setText(expense.getTitle());
            tvPaidBy.setText("Paid by " + (expense.getPayerName() != null ? expense.getPayerName() : "Unknown"));
            
            // Build dynamic split text with participant names (SECURITY: Map UIDs to DisplayNames)
            String splitText;
            Map<String, Double> splitAmounts = expense.getSplitAmounts();
            List<String> participants = expense.getParticipants();
            if (splitAmounts != null && !splitAmounts.isEmpty() && participants != null && !participants.isEmpty()) {
                StringBuilder participantsBuilder = new StringBuilder();
                int count = 0;
                for (String participantUid : splitAmounts.keySet()) {
                    if (count > 0) {
                        participantsBuilder.append(", ");
                    }
                    // SECURITY: Map UID to displayName using memberNames map
                    String displayName = memberNames.get(participantUid);
                    if (displayName != null && !displayName.isEmpty()) {
                        participantsBuilder.append(displayName);
                    } else {
                        participantsBuilder.append("Unknown User");
                    }
                    count++;
                }
                splitText = "Split to [" + count + "] people: " + participantsBuilder.toString();
            } else {
                splitText = "Split equally among " + expense.getParticipantCount() + " people";
            }
            
            tvSplitInfo.setText(splitText);
            tvAmount.setText(CurrencyHelper.format(expense.getAmount()));
        }
    }
}
