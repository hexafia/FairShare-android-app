package com.example.fairshare.ui.dashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.fairshare.ExpenseRepository;
import com.example.fairshare.GroupExpense;
import com.example.fairshare.GroupRepository;
import com.example.fairshare.Transaction;

import java.util.List;

public class DashboardViewModel extends ViewModel {

    private final ExpenseRepository expenseRepository;
    private final GroupRepository groupRepository;

    private final LiveData<List<Transaction>> personalExpenses;
    private final LiveData<List<GroupExpense>> groupExpenses;

    public DashboardViewModel() {
        expenseRepository = new ExpenseRepository();
        groupRepository = new GroupRepository();
        
        personalExpenses = expenseRepository.getExpenses();
        groupExpenses = groupRepository.getAllMyGroupExpenses();
    }

    public LiveData<List<Transaction>> getPersonalExpenses() {
        return personalExpenses;
    }

    public LiveData<List<GroupExpense>> getGroupExpenses() {
        return groupExpenses;
    }

    public void addPersonalExpense(Transaction transaction) {
        expenseRepository.addExpense(transaction);
    }

    public void deletePersonalExpense(String id) {
        expenseRepository.deleteExpense(id);
    }
    
    public void createGroup(String name, GroupRepository.OnCompleteCallback callback) {
        groupRepository.createGroup(name, callback);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        expenseRepository.removeListener();
        groupRepository.removeListeners();
    }
}
