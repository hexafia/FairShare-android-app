package com.example.fairshare.ui.ledger;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fairshare.CurrencyHelper;
import com.example.fairshare.ExpenseAdapter;
import com.example.fairshare.ExpenseViewModel;
import com.example.fairshare.R;
import com.example.fairshare.Transaction;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LedgerFragment extends Fragment {

    private ExpenseViewModel viewModel;
    private ExpenseAdapter adapter;

    private List<Transaction> allExpenses = new ArrayList<>();
    
    private TextInputEditText etSearch;
    private Spinner spinnerFilterCategory;
    private MaterialButton btnFilterDate;
    private TextView tvTotalExpense;
    private TextView tvEmptyState;
    private RecyclerView rvPersonalExpenses;

    private String currentSearch = "";
    private String currentCategory = "All";
    private Long selectedDateStart = null; // Storing date as midnight timestamp

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ledger, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etSearch = view.findViewById(R.id.etSearch);
        spinnerFilterCategory = view.findViewById(R.id.spinnerFilterCategory);
        btnFilterDate = view.findViewById(R.id.btnFilterDate);
        tvTotalExpense = view.findViewById(R.id.tvTotalExpense);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        rvPersonalExpenses = view.findViewById(R.id.rvPersonalExpenses);

        setupRecyclerView();
        setupFilters();
        setupViewModel();
    }

    private void setupRecyclerView() {
        adapter = new ExpenseAdapter(transaction -> {
            // No-op or provide delete capability
            if (transaction.getId() != null) {
                viewModel.deleteExpense(transaction.getId());
            }
        });
        rvPersonalExpenses.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPersonalExpenses.setAdapter(adapter);
    }

    private void setupFilters() {
        // Setup Category Spinner
        List<String> categories = new ArrayList<>();
        categories.add("All");
        // Add predefined categories from resources
        String[] arr = getResources().getStringArray(R.array.categories);
        for(String s : arr) categories.add(s);

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterCategory.setAdapter(catAdapter);

        spinnerFilterCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentCategory = categories.get(position);
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Setup Search
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearch = s.toString().trim().toLowerCase();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Setup Date Picker
        btnFilterDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Date")
                    .setSelection(selectedDateStart != null ? selectedDateStart : MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                selectedDateStart = selection;
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                btnFilterDate.setText(sdf.format(new Date(selection)));
                applyFilters();
            });

            datePicker.show(getParentFragmentManager(), "DATE_PICKER");
        });
        
        // Add a long click listener to clear the date filter
        btnFilterDate.setOnLongClickListener(v -> {
            selectedDateStart = null;
            btnFilterDate.setText("All Dates");
            applyFilters();
            return true;
        });
    }

    private void setupViewModel() {
        // Shared ViewModel tied to Activity
        viewModel = new ViewModelProvider(requireActivity()).get(ExpenseViewModel.class);
        viewModel.getExpenses().observe(getViewLifecycleOwner(), transactions -> {
            allExpenses = transactions != null ? transactions : new ArrayList<>();
            applyFilters();
        });
    }

    private void applyFilters() {
        List<Transaction> filtered = new ArrayList<>();
        double totalFilteredExpense = 0;

        for (Transaction t : allExpenses) {
            boolean matchesSearch = currentSearch.isEmpty() || 
                    (t.getTitle() != null && t.getTitle().toLowerCase().contains(currentSearch));
            
            boolean matchesCategory = "All".equals(currentCategory) || 
                    (t.getCategory() != null && t.getCategory().equals(currentCategory));
            
            boolean matchesDate = true;
            if (selectedDateStart != null && t.getDate() != null) {
                Calendar tCal = Calendar.getInstance();
                tCal.setTime(t.getDate());
                
                Calendar sCal = Calendar.getInstance();
                sCal.setTimeInMillis(selectedDateStart);
                
                matchesDate = tCal.get(Calendar.YEAR) == sCal.get(Calendar.YEAR) &&
                              tCal.get(Calendar.DAY_OF_YEAR) == sCal.get(Calendar.DAY_OF_YEAR);
            }

            if (matchesSearch && matchesCategory && matchesDate) {
                filtered.add(t);
                // All transactions are now expenses (income tracking removed)
                totalFilteredExpense += t.getAmount();
            }
        }

        adapter.submitList(filtered);
        tvTotalExpense.setText(CurrencyHelper.format(totalFilteredExpense));

        if (filtered.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvPersonalExpenses.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvPersonalExpenses.setVisibility(View.VISIBLE);
        }
    }
}
