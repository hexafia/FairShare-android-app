package com.example.fairshare;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository that abstracts Firestore CRUD operations for the "expenses" collection.
 * Firestore's offline persistence is configured in FairShareApp so writes will be
 * queued locally if the device is offline and synced when connectivity returns.
 */
public class ExpenseRepository {

    public interface ExpenseWriteCallback {
        void onSuccess(Transaction transaction);
        void onFailure(Exception error);
    }

    private static final String TAG = "ExpenseRepository";
    private static final String COLLECTION = "expenses";

    private static final CollectionReference expensesRef = FirebaseFirestore.getInstance().collection(COLLECTION);
    private static final MutableLiveData<List<Transaction>> expensesLiveData = new MutableLiveData<>(new ArrayList<>());
    private static final MutableLiveData<Double> expenseTotalLiveData = new MutableLiveData<>(0.0);
    private static ListenerRegistration listenerRegistration;

    public ExpenseRepository() {
    }

    /**
     * Returns a LiveData list of all transactions, ordered by date descending.
     * Calling this attaches a real-time snapshot listener.
     */
    public LiveData<List<Transaction>> getExpenses() {
        if (listenerRegistration == null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            Log.d(TAG, "Setting up listener for expenses with uid: " + uid);
            listenerRegistration = expensesRef
                    .whereEqualTo("uid", uid)
                    .orderBy("date", Query.Direction.DESCENDING)
                    .addSnapshotListener((snapshots, error) -> {
                        if (error != null) {
                            Log.w(TAG, "Listen failed.", error);
                            return;
                        }
                        if (snapshots != null) {
                            List<Transaction> list = new ArrayList<>();
                            double total = 0.0;
                            for (QueryDocumentSnapshot doc : snapshots) {
                                Transaction t = doc.toObject(Transaction.class);
                                list.add(t);
                                total += t.getAmount();
                                Log.d(TAG, "Loaded expense: " + t.getTitle() + " - " + t.getAmount());
                            }
                            Log.d(TAG, "Expense listener update: " + list.size() + " transactions");
                            expensesLiveData.setValue(list);
                            expenseTotalLiveData.setValue(total);
                        }
                    });
        }
        return expensesLiveData;
    }

    public LiveData<Double> getExpenseTotal() {
        return expenseTotalLiveData;
    }

    /**
     * Add a new transaction. Firestore handles the id auto-generation.
     * Works offline thanks to persistent cache.
     */
    public void addExpense(Transaction transaction) {
        addExpense(transaction, null);
    }

    public void addExpense(Transaction transaction, ExpenseWriteCallback callback) {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            transaction.setUid(FirebaseAuth.getInstance().getCurrentUser().getUid());
        }
        expensesRef.add(transaction)
                .addOnSuccessListener(docRef -> {
                    transaction.setId(docRef.getId());
                    Log.d(TAG, "Added with ID: " + docRef.getId());

                    if (callback != null) {
                        callback.onSuccess(transaction);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding document", e);
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }

    /**
     * Delete a transaction by its Firestore document id.
     */
    public void deleteExpense(String id) {
        expensesRef.document(id).delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Deleted: " + id))
                .addOnFailureListener(e -> Log.w(TAG, "Error deleting", e));
    }

    /**
     * Remove the snapshot listener when it's no longer needed.
     */
    public void removeListener() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }
}
