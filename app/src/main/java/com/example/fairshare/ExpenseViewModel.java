package com.example.fairshare;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

/**
 * ViewModel layer that exposes LiveData from the ExpenseRepository.
 */
public class ExpenseViewModel extends ViewModel {

    private final ExpenseRepository repository;
    private final LiveData<List<Transaction>> expenses;

    public ExpenseViewModel() {
        repository = new ExpenseRepository();
        expenses = repository.getExpenses();
    }

    public LiveData<List<Transaction>> getExpenses() {
        return expenses;
    }

    public LiveData<Double> getExpenseTotal() {
        return repository.getExpenseTotal();
    }

    public void addExpense(Transaction transaction) {
        repository.addExpense(transaction);
    }

    public void addExpense(Transaction transaction, ExpenseRepository.ExpenseWriteCallback callback) {
        repository.addExpense(transaction, callback);
    }

    public void deleteExpense(String id) {
        repository.deleteExpense(id);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.removeListener();
    }
}
