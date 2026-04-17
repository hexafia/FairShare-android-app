package com.example.fairshare.ui.groups;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fairshare.CurrencyHelper;
import com.example.fairshare.R;
import com.example.fairshare.SettlementCalculator;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Adapter for displaying settlement details based on the breakdown map from Firestore.
 * Shows who owes whom and how much for a specific expense.
 */
public class SettlementDetailAdapter extends ListAdapter<SettlementCalculator.SettlementDetail, SettlementDetailAdapter.ViewHolder> {

    private Map<String, String> memberNames = new HashMap<>();
    private boolean isGroupAccomplished = false;
    private String currentUserId;

    public SettlementDetailAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setMemberNames(Map<String, String> names) {
        this.memberNames = names;
    }

    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
    }

    public void setGroupAccomplished(boolean accomplished) {
        this.isGroupAccomplished = accomplished;
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
                            && Objects.equals(old.expenseTitle, newItem.expenseTitle)
                            && old.settled == newItem.settled;
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
        private final TextView tvExpenseName, tvDateAdded, tvSettledDate;
        private final MaterialButton btnSettle;
        private final ImageButton btnNudge;
        private final TextView tvSettledLabel;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDebtorInitial = itemView.findViewById(R.id.tvDebtorInitial);
            tvCreditorInitial = itemView.findViewById(R.id.tvCreditorInitial);
            tvSettlementDescription = itemView.findViewById(R.id.tvSettlementDescription);
            tvSettlementAmount = itemView.findViewById(R.id.tvSettlementAmount);
            tvExpenseName = itemView.findViewById(R.id.tvExpenseName);
            tvDateAdded = itemView.findViewById(R.id.tvDateAdded);
            tvSettledDate = itemView.findViewById(R.id.tvSettledDate);
            btnSettle = itemView.findViewById(R.id.btnSettle);
            btnNudge = itemView.findViewById(R.id.btnNudge);
            tvSettledLabel = itemView.findViewById(R.id.tvSettledLabel);
        }

        void bind(SettlementCalculator.SettlementDetail settlement) {
            String debtorName = memberNames.getOrDefault(settlement.debtorUid, shortenUid(settlement.debtorUid));
            String creditorName = memberNames.getOrDefault(settlement.creditorUid, shortenUid(settlement.creditorUid));

            tvDebtorInitial.setText(String.valueOf(debtorName.charAt(0)).toUpperCase());
            tvCreditorInitial.setText(String.valueOf(creditorName.charAt(0)).toUpperCase());
            
            // Settlement Description: Use perspective text from SettlementDetail
            tvSettlementDescription.setText(settlement.perspectiveText + " " + CurrencyHelper.format(settlement.settlementAmount));
            tvSettlementAmount.setText(CurrencyHelper.format(settlement.settlementAmount));
            
            // Map data to specific views for itemized transactions
            if (settlement.expenseTitle != null && !settlement.expenseTitle.isEmpty()) {
                tvExpenseName.setText(settlement.expenseTitle);
                tvExpenseName.setVisibility(View.VISIBLE);
            } else {
                tvExpenseName.setVisibility(View.GONE);
            }
            
            // Date Added: Format timestamp
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String dateAddedText = "Added: " + dateFormat.format(new Date(settlement.dateAdded));
            tvDateAdded.setText(dateAddedText);
            tvDateAdded.setVisibility(View.VISIBLE);
            
            // Settled Date: Initially hidden, only show if payment is marked as successful
            if (settlement.settled && settlement.settledDate != null) {
                tvSettledDate.setText("Settled: " + settlement.settledDate);
                tvSettledDate.setVisibility(View.VISIBLE);
            } else {
                tvSettledDate.setVisibility(View.GONE);
            }

            // Handle settled state and group accomplished status
            if (settlement.settled || isGroupAccomplished) {
                btnSettle.setVisibility(View.GONE);
                btnNudge.setVisibility(View.GONE);
                tvSettledLabel.setVisibility(View.VISIBLE);
                itemView.setAlpha(0.5f);
                
                // Show appropriate text for accomplished groups
                if (isGroupAccomplished && !settlement.settled) {
                    tvSettledLabel.setText("Settled");
                    tvSettledLabel.setTextColor(itemView.getContext().getResources().getColor(android.R.color.darker_gray));
                }
            } else {
                btnSettle.setVisibility(View.GONE);
                btnNudge.setVisibility(View.GONE);
                tvSettledLabel.setVisibility(View.VISIBLE);
                tvSettledLabel.setText("Actions unavailable");
                tvSettledLabel.setTextColor(itemView.getContext().getResources().getColor(android.R.color.darker_gray));
                itemView.setAlpha(0.85f);
            }
        }

        private String shortenUid(String uid) {
            return uid != null && uid.length() > 6 ? uid.substring(0, 6) : (uid != null ? uid : "?");
        }
    }
}
