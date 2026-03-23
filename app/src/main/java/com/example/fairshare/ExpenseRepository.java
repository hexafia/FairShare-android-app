package com.example.fairshare;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

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

    private static final String TAG = "ExpenseRepository";
    private static final String COLLECTION = "expenses";

    private final CollectionReference expensesRef;
    private final MutableLiveData<List<Transaction>> expensesLiveData = new MutableLiveData<>(new ArrayList<>());
    private ListenerRegistration listenerRegistration;

    public ExpenseRepository() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        expensesRef = db.collection(COLLECTION);
    }

    /**
     * Returns a LiveData list of all transactions, ordered by date descending.
     * Calling this attaches a real-time snapshot listener.
     */
    public LiveData<List<Transaction>> getExpenses() {
        if (listenerRegistration == null) {
            listenerRegistration = expensesRef
                    .orderBy("date", Query.Direction.DESCENDING)
                    .addSnapshotListener((snapshots, error) -> {
                        if (error != null) {
                            Log.w(TAG, "Listen failed.", error);
                            return;
                        }
                        if (snapshots != null) {
                            List<Transaction> list = new ArrayList<>();
                            for (QueryDocumentSnapshot doc : snapshots) {
                                Transaction t = doc.toObject(Transaction.class);
                                list.add(t);
                            }
                            expensesLiveData.setValue(list);
                        }
                    });
        }
        return expensesLiveData;
    }

    /**
     * Add a new transaction. Firestore handles the id auto-generation.
     * Works offline thanks to persistent cache.
     */
    public void addExpense(Transaction transaction) {
        expensesRef.add(transaction)
                .addOnSuccessListener(docRef -> Log.d(TAG, "Added with ID: " + docRef.getId()))
                .addOnFailureListener(e -> Log.w(TAG, "Error adding document", e));
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
