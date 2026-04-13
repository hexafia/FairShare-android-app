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
import com.example.fairshare.DebtSimplifier;
import com.example.fairshare.R;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DebtAdapter extends ListAdapter<DebtSimplifier.Debt, DebtAdapter.ViewHolder> {

    private Map<String, String> memberNames = new HashMap<>();

    public DebtAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setMemberNames(Map<String, String> names) {
        this.memberNames = names;
    }

    private static final DiffUtil.ItemCallback<DebtSimplifier.Debt> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<DebtSimplifier.Debt>() {
                @Override
                public boolean areItemsTheSame(@NonNull DebtSimplifier.Debt old, @NonNull DebtSimplifier.Debt newItem) {
                    return Objects.equals(old.debtorUid, newItem.debtorUid)
                            && Objects.equals(old.creditorUid, newItem.creditorUid);
                }

                @Override
                public boolean areContentsTheSame(@NonNull DebtSimplifier.Debt old, @NonNull DebtSimplifier.Debt newItem) {
                    return old.amount == newItem.amount;
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_debt, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDebtorInitial, tvCreditorInitial;
        private final TextView tvDebtDescription, tvDebtAmount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDebtorInitial = itemView.findViewById(R.id.tvDebtorInitial);
            tvCreditorInitial = itemView.findViewById(R.id.tvCreditorInitial);
            tvDebtDescription = itemView.findViewById(R.id.tvDebtDescription);
            tvDebtAmount = itemView.findViewById(R.id.tvDebtAmount);
        }

        void bind(DebtSimplifier.Debt debt) {
            String debtorName = memberNames.getOrDefault(debt.debtorUid, shortenUid(debt.debtorUid));
            String creditorName = memberNames.getOrDefault(debt.creditorUid, shortenUid(debt.creditorUid));

            tvDebtorInitial.setText(String.valueOf(debtorName.charAt(0)).toUpperCase());
            tvCreditorInitial.setText(String.valueOf(creditorName.charAt(0)).toUpperCase());
            tvDebtDescription.setText(debtorName + " → " + creditorName);
            tvDebtAmount.setText(CurrencyHelper.format(debt.amount));
        }

        private String shortenUid(String uid) {
            return uid != null && uid.length() > 6 ? uid.substring(0, 6) : (uid != null ? uid : "?");
        }
    }
}
