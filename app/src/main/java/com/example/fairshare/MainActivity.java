package com.example.fairshare;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.fairshare.database.DatabaseHelper;
import com.example.fairshare.models.Expense;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private LinearLayout dashboardContainer;
    private TextView tvTotalSpent;
    private Button btnAddExpense;
    private Button btnRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        // Initialize views
        tvTotalSpent = findViewById(R.id.tvTotalSpent);
        btnAddExpense = findViewById(R.id.btnAddExpense);
        btnRefresh = findViewById(R.id.btnRefresh);
        dashboardContainer = findViewById(R.id.dashboardContainer);

        btnAddExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddExpenseActivity.class);
                startActivity(intent);
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadDashboard();
            }
        });

        loadDashboard();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboard();
    }

    private void loadDashboard() {
        dashboardContainer.removeAllViews();
        List<Expense> expenses = dbHelper.getAllExpenses();

        double totalSpent = 0;
        for (Expense expense : expenses) {
            totalSpent += expense.getAmount();
        }

        tvTotalSpent.setText(String.format("Total Spent: $%.2f", totalSpent));

        // Group expenses by category
        String[] categories = {"Groceries", "Takeouts", "Shopping", "Transport", "Entertainment", "Other"};

        for (String category : categories) {
            List<Expense> categoryExpenses = dbHelper.getExpensesByCategory(category);
            if (!categoryExpenses.isEmpty()) {
                addCategorySection(category, categoryExpenses);
            }
        }

        // Show recent expenses
        addRecentExpensesSection(expenses);
    }

    private void addCategorySection(String category, List<Expense> expenses) {
        CardView cardView = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(16, 8, 16, 8);
        cardView.setLayoutParams(cardParams);
        cardView.setRadius(8);
        cardView.setCardElevation(4);
        cardView.setUseCompatPadding(true);

        LinearLayout contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding(24, 16, 24, 16);

        // Category header
        TextView tvCategory = new TextView(this);
        tvCategory.setText(category);
        tvCategory.setTextSize(20);
        tvCategory.setPadding(0, 0, 0, 8);

        double categoryTotal = 0;
        for (Expense expense : expenses) {
            categoryTotal += expense.getAmount();
        }

        TextView tvCategoryTotal = new TextView(this);
        tvCategoryTotal.setText(String.format("Total: $%.2f (%d items)", categoryTotal, expenses.size()));
        tvCategoryTotal.setTextSize(16);
        tvCategoryTotal.setPadding(0, 0, 0, 16);

        contentLayout.addView(tvCategory);
        contentLayout.addView(tvCategoryTotal);

        // Add recent items in this category
        int count = 0;
        for (Expense expense : expenses) {
            if (count < 3) { // Show only last 3 items
                TextView tvExpense = new TextView(this);
                tvExpense.setText(String.format("• %s - $%.2f (%s)",
                        expense.getDescription(),
                        expense.getAmount(),
                        expense.getFormattedDate()));
                tvExpense.setTextSize(14);
                tvExpense.setPadding(16, 4, 0, 4);
                contentLayout.addView(tvExpense);
                count++;
            }
        }

        cardView.addView(contentLayout);
        dashboardContainer.addView(cardView);
    }

    private void addRecentExpensesSection(List<Expense> expenses) {
        if (expenses.isEmpty()) return;

        CardView cardView = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(16, 8, 16, 8);
        cardView.setLayoutParams(cardParams);
        cardView.setRadius(8);
        cardView.setCardElevation(4);
        cardView.setUseCompatPadding(true);

        LinearLayout contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding(24, 16, 24, 16);

        TextView tvRecentHeader = new TextView(this);
        tvRecentHeader.setText("Recent All Activities");
        tvRecentHeader.setTextSize(20);
        tvRecentHeader.setPadding(0, 0, 0, 16);
        contentLayout.addView(tvRecentHeader);

        int count = 0;
        for (Expense expense : expenses) {
            if (count < 5) { // Show last 5 expenses
                TextView tvExpense = new TextView(this);
                tvExpense.setText(String.format("• %s - %s: $%.2f (%s)",
                        expense.getCategory(),
                        expense.getDescription(),
                        expense.getAmount(),
                        expense.getFormattedDate()));
                tvExpense.setTextSize(14);
                tvExpense.setPadding(16, 4, 0, 4);
                contentLayout.addView(tvExpense);
                count++;
            }
        }

        cardView.addView(contentLayout);
        dashboardContainer.addView(cardView);
    }
}