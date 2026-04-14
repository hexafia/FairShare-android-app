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
import com.example.fairshare.R;
import com.example.fairshare.SettlementCalculator;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Adapter for displaying settlement details based on the breakdown map from Firestore.
 * Shows who owes whom and how much for a specific expense.
 */
public class SettlementDetailAdapter extends ListAdapter<SettlementCalculator.SettlementDetail, SettlementDetailAdapter.ViewHolder> {

    private Map<String, String> memberNames = new HashMap<>();

    public SettlementDetailAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setMemberNames(Map<String, String> names) {
        this.memberNames = names;
    }

    private static final DiffUtil.ItemCallback<SettlementCalculator.SettlementDetail> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<SettlementCalculator.SettlementDetail>() {
                @Override
                public boolean areItemsTheSame(@NonNull SettlementCalculator.SettlementDetail old, 
                                              @NonNull SettlementCalculator.SettlementDetail newItem) {
                    return Objects.equals(old.expenseId, newItem.expenseId)
                            && Objects.equals(old.debtorUid, newItem.debtorUid)
                            && Objects.equals(old.creditorUid, newItem.creditorUid);
                }

                @Override
                public boolean areContentsTheSame(@NonNull SettlementCalculator.SettlementDetail old, 
                                                 @NonNull SettlementCalculator.SettlementDetail newItem) {
                    return old.settlementAmount == newItem.settlementAmount
                            && Objects.equals(old.expenseTitle, newItem.expenseTitle);
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_settlement_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDebtorInitial, tvCreditorInitial;
        private final TextView tvSettlementDescription, tvSettlementAmount;
        private final TextView tvExpenseTitle;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDebtorInitial = itemView.findViewById(R.id.tvDebtorInitial);
            tvCreditorInitial = itemView.findViewById(R.id.tvCreditorInitial);
            tvSettlementDescription = itemView.findViewById(R.id.tvSettlementDescription);
            tvSettlementAmount = itemView.findViewById(R.id.tvSettlementAmount);
            tvExpenseTitle = itemView.findViewById(R.id.tvExpenseTitle);
        }

        void bind(SettlementCalculator.SettlementDetail settlement) {
            String debtorName = memberNames.getOrDefault(settlement.debtorUid, shortenUid(settlement.debtorUid));
            String creditorName = memberNames.getOrDefault(settlement.creditorUid, shortenUid(settlement.creditorUid));

            tvDebtorInitial.setText(String.valueOf(debtorName.charAt(0)).toUpperCase());
            tvCreditorInitial.setText(String.valueOf(creditorName.charAt(0)).toUpperCase());
            
            // Enhanced "Who owes Whom" display format
            tvSettlementDescription.setText(debtorName + " owes " + creditorName);
            tvSettlementAmount.setText(CurrencyHelper.format(settlement.settlementAmount));
            
            // Show the expense title for context
            if (settlement.expenseTitle != null && !settlement.expenseTitle.isEmpty()) {
                tvExpenseTitle.setText("for: " + settlement.expenseTitle);
                tvExpenseTitle.setVisibility(View.VISIBLE);
            } else {
                tvExpenseTitle.setVisibility(View.GONE);
            }
        }

        private String shortenUid(String uid) {
            return uid != null && uid.length() > 6 ? uid.substring(0, 6) : (uid != null ? uid : "?");
        }
    }
}
