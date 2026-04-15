package com.example.fairshare;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;

public class ExpenseAdapter extends ListAdapter<Transaction, ExpenseAdapter.ExpenseViewHolder> {

    public interface OnDeleteClickListener {
        void onDeleteClick(Transaction transaction);
    }

    private final OnDeleteClickListener deleteListener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public ExpenseAdapter(OnDeleteClickListener deleteListener) {
        super(DIFF_CALLBACK);
        this.deleteListener = deleteListener;
    }

    private static final DiffUtil.ItemCallback<Transaction> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Transaction>() {
                @Override
                public boolean areItemsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
                    return Objects.equals(oldItem.getId(), newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
                    return Objects.equals(oldItem.getTitle(), newItem.getTitle())
                            && oldItem.getAmount() == newItem.getAmount()
                            && Objects.equals(oldItem.getCategory(), newItem.getCategory());
                }
            };

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Transaction transaction = getItem(position);
        holder.bind(transaction);
    }

    class ExpenseViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle, tvCategory, tvAmount, tvDate;

        ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvItemTitle);
            tvCategory = itemView.findViewById(R.id.tvItemCategory);
            tvAmount = itemView.findViewById(R.id.tvItemAmount);
            tvDate = itemView.findViewById(R.id.tvItemDate);
        }

        void bind(Transaction transaction) {
            tvTitle.setText(transaction.getTitle());
            tvCategory.setText(transaction.getCategory() != null ? transaction.getCategory() : "Uncategorized");

            String amountText = "- " + CurrencyHelper.format(transaction.getAmount());
            tvAmount.setText(amountText);
            tvAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.expense_red));

            if (transaction.getDate() != null) {
                tvDate.setText(dateFormat.format(transaction.getDate()));
            } else {
                tvDate.setText("Just now");
            }

            // Category indicator color
            int indicatorColor;
            String category = transaction.getCategory() != null ? transaction.getCategory().toLowerCase() : "";
            switch (category) {
                case "food":
                    indicatorColor = R.color.cat_food;
                    break;
                case "transport":
                    indicatorColor = R.color.cat_transport;
                    break;
                case "shopping":
                    indicatorColor = R.color.cat_shopping;
                    break;
                case "bills":
                    indicatorColor = R.color.cat_bills;
                    break;
                case "salary":
                    indicatorColor = R.color.cat_salary;
                    break;
                default:
                    indicatorColor = R.color.cat_other;
                    break;
            }
        }
    }
}
