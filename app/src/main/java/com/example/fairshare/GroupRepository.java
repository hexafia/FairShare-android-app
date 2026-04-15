package com.example.fairshare;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository handling all Firebase Firestore operations for groups
 * and group expenses.
 */
public class GroupRepository {

    private static final String TAG = "GroupRepository";
    private static final String GROUPS_COLLECTION = "groups";
    private static final String GROUP_EXPENSES_COLLECTION = "group_expenses";
    private static final String SHARE_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final FirebaseFirestore db;
    private final MutableLiveData<List<Group>> groupsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Group>> settledGroupsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<GroupExpense>> expensesLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<GroupExpense>> allMyExpensesLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<GroupExpense>> settleUpExpensesLiveData = new MutableLiveData<>(new ArrayList<>());

    private ListenerRegistration groupsListener;
    private ListenerRegistration settledGroupsListener;
    private ListenerRegistration expensesListener;
    private ListenerRegistration allMyExpensesListener;
    private ListenerRegistration settleUpListener;

    public GroupRepository() {
        db = FirebaseFirestore.getInstance();
    }

    // ========================
    // GROUP OPERATIONS
    // ========================

    public LiveData<List<Group>> getGroups() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return groupsLiveData;

        if (groupsListener == null) {
            groupsListener = db.collection(GROUPS_COLLECTION)
                    .whereArrayContains("members", user.getUid())
                    .addSnapshotListener((value, error) -> {
                        if (error != null) {
                            Log.w(TAG, "Groups listen failed.", error);
                            return;
                        }
                        List<Group> groups = new ArrayList<>();
                        if (value != null) {
                            for (DocumentSnapshot doc : value) {
                                Group group = doc.toObject(Group.class);
                                if (group != null) {
                                    // ID is hydrated automatically by @DocumentId
                                    groups.add(group);
                                }
                            }
                        }
                        groupsLiveData.setValue(groups);
                    });
        }
        return groupsLiveData;
    }
    
    // Separate method to get settled groups for GroupsFragment
    public void getSettledGroups(MutableLiveData<List<Group>> settledGroupsLiveData) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            settledGroupsLiveData.setValue(new ArrayList<>());
            return;
        }

        if (settledGroupsListener == null) {
            settledGroupsListener = db.collection(GROUPS_COLLECTION)
                    .whereArrayContains("members", user.getUid())
                    .whereEqualTo("status", "settled") // Fetch settled groups
                    .addSnapshotListener((value, error) -> {
                        if (error != null) {
                            Log.w(TAG, "Settled groups listen failed.", error);
                            return;
                        }
                        List<Group> settledGroups = new ArrayList<>();
                        if (value != null) {
                            for (DocumentSnapshot doc : value) {
                                Group group = doc.toObject(Group.class);
                                if (group != null) {
                                    settledGroups.add(group);
                                }
                            }
                        }
                        settledGroupsLiveData.setValue(settledGroups);
                    });
        }
    }
    
    // Update GroupsFragment to use both listeners for real-time updates
    public void getGroupsWithBothListeners(MutableLiveData<List<Group>> activeGroupsLiveData, 
                                            MutableLiveData<List<Group>> settledGroupsLiveData) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            activeGroupsLiveData.setValue(new ArrayList<>());
            settledGroupsLiveData.setValue(new ArrayList<>());
            return;
        }

        if (groupsListener == null) {
            groupsListener = db.collection(GROUPS_COLLECTION)
                    .whereArrayContains("members", user.getUid())
                    .addSnapshotListener((value, error) -> {
                        if (error != null) {
                            Log.w(TAG, "Groups listen failed.", error);
                            return;
                        }
                        List<Group> groups = new ArrayList<>();
                        if (value != null) {
                            for (DocumentSnapshot doc : value) {
                                Group group = doc.toObject(Group.class);
                                if (group != null) {
                                    if (group.isActive()) {
                                        List<Group> currentActive = activeGroupsLiveData.getValue();
                                        if (currentActive != null) {
                                            currentActive.add(group);
                                            activeGroupsLiveData.setValue(currentActive);
                                        }
                                    } else if (group.isSettled()) {
                                        List<Group> currentSettled = settledGroupsLiveData.getValue();
                                        if (currentSettled != null) {
                                            currentSettled.add(group);
                                            settledGroupsLiveData.setValue(currentSettled);
                                        }
                                    }
                                }
                            }
                        }
                        groupsLiveData.setValue(groups);
                    });
        }
        
        if (settledGroupsListener == null) {
            settledGroupsListener = db.collection(GROUPS_COLLECTION)
                    .whereArrayContains("members", user.getUid())
                    .whereEqualTo("status", "settled") // Fetch settled groups
                    .addSnapshotListener((value, error) -> {
                        if (error != null) {
                            Log.w(TAG, "Settled groups listen failed.", error);
                            return;
                        }
                        List<Group> settledGroups = new ArrayList<>();
                        if (value != null) {
                            for (DocumentSnapshot doc : value) {
                                Group group = doc.toObject(Group.class);
                                if (group != null) {
                                    List<Group> currentSettled = settledGroupsLiveData.getValue();
                                    if (currentSettled != null) {
                                        currentSettled.add(group);
                                        settledGroupsLiveData.setValue(currentSettled);
                                    }
                                }
                            }
                        }
                        settledGroupsLiveData.setValue(settledGroups);
                    });
        }
    }

    public void createGroup(String name, OnCompleteCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onError("Not authenticated");
            return;
        }

        String shareCode = generateShareCode();
        Group group = new Group(name, shareCode, user.getUid());
        group.getMembers().add(user.getUid());

        db.collection(GROUPS_COLLECTION).add(group)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Group created with ID: " + documentReference.getId());
                    callback.onSuccess(shareCode);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding group", e);
                    callback.onError(e.getMessage());
                });
    }

    public void joinGroup(String shareCode, OnCompleteCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onError("Not authenticated");
            return;
        }

        String codeUpper = shareCode.trim().toUpperCase();
        db.collection(GROUPS_COLLECTION)
                .whereEqualTo("shareCode", codeUpper)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        DocumentSnapshot doc = task.getResult().getDocuments().get(0);
                        String groupId = doc.getId();
                        
                        // Check group status before allowing join
                        String status = doc.getString("status");
                        if (status == null) {
                            status = "active"; // Default to active if status field is missing
                        }
                        
                        if ("settled".equals(status)) {
                            callback.onError("This group has been archived and is no longer accepting new members.");
                            return;
                        }
                        
                        db.collection(GROUPS_COLLECTION).document(groupId)
                                .update("members", FieldValue.arrayUnion(user.getUid()))
                                .addOnSuccessListener(aVoid -> {
                                    Group g = doc.toObject(Group.class);
                                    String gName = g != null ? g.getName() : "Group";
                                    callback.onSuccess("Joined: " + gName);
                                })
                                .addOnFailureListener(e -> callback.onError(e.getMessage()));
                    } else {
                        callback.onError("No group found with that code");
                    }
                });
    }

    public void updateGroupStatus(String groupId, String status, OnCompleteCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onError("Not authenticated");
            return;
        }

        db.collection(GROUPS_COLLECTION).document(groupId)
                .update("status", status)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Group status updated to: " + status);
                    callback.onSuccess("Group marked as " + status);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error updating group status", e);
                    callback.onError(e.getMessage());
                });
    }

    // ========================
    // GROUP EXPENSE OPERATIONS
    // ========================

    public LiveData<List<GroupExpense>> getGroupExpenses(String groupId) {
        if (expensesListener != null) {
            expensesListener.remove();
        }

        expensesListener = db.collection(GROUP_EXPENSES_COLLECTION)
                .whereEqualTo("groupId", groupId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Expenses listen failed.", error);
                        return;
                    }
                    List<GroupExpense> expenses = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot doc : value) {
                            GroupExpense expense = doc.toObject(GroupExpense.class);
                            if (expense != null) {
                                expenses.add(expense);
                            }
                        }
                    }
                    expensesLiveData.setValue(expenses);
                });
        return expensesLiveData;
    }
    
    /**
     * Extra method: fetches ALL group expenses for the current user 
     * where they are a participant. This avoids needing nested queries.
     */
    public LiveData<List<GroupExpense>> getAllMyGroupExpenses() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return allMyExpensesLiveData;

        if (allMyExpensesListener == null) {
            allMyExpensesListener = db.collection(GROUP_EXPENSES_COLLECTION)
                    .whereArrayContains("participants", user.getUid())
                    .addSnapshotListener((value, error) -> {
                        if (error != null) {
                            Log.w(TAG, "All My Expenses listen failed.", error);
                            return;
                        }
                        List<GroupExpense> expenses = new ArrayList<>();
                        if (value != null) {
                            for (DocumentSnapshot doc : value) {
                                GroupExpense expense = doc.toObject(GroupExpense.class);
                                if (expense != null) {
                                    expenses.add(expense);
                                }
                            }
                        }
                        // Reverse sort locally if needed, skipping orderBy to avoid compound index requirement
                        expenses.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                        allMyExpensesLiveData.setValue(expenses);
                    });
        }
        return allMyExpensesLiveData;
    }

    /**
     * Fetches all expenses where the current user is involved (payer or participant).
     * Uses the involvedUsers array for efficient querying.
     * This is used for the Settle Up tab to calculate settlement amounts.
     */
    public LiveData<List<GroupExpense>> getExpensesForSettleUp() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return settleUpExpensesLiveData;

        if (settleUpListener != null) {
            settleUpListener.remove();
        }

        settleUpListener = db.collection(GROUP_EXPENSES_COLLECTION)
                .whereArrayContains("involvedUsers", user.getUid())
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Settle Up Expenses listen failed.", error);
                        return;
                    }
                    List<GroupExpense> expenses = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot doc : value) {
                            GroupExpense expense = doc.toObject(GroupExpense.class);
                            if (expense != null) {
                                expenses.add(expense);
                            }
                        }
                    }
                    // Sort by timestamp descending
                    expenses.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                    settleUpExpensesLiveData.setValue(expenses);
                });
        return settleUpExpensesLiveData;
    }

    public void addGroupExpense(String groupId, GroupExpense expense) {
        expense.setGroupId(groupId);
        
        // Populate involvedUsers with payer + all participants
        java.util.Set<String> involvedUsers = new java.util.HashSet<>();
        involvedUsers.add(expense.getPayerUid());
        if (expense.getParticipants() != null) {
            involvedUsers.addAll(expense.getParticipants());
        }
        expense.setInvolvedUsers(new ArrayList<>(involvedUsers));
        
        db.collection(GROUP_EXPENSES_COLLECTION).add(expense)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Expense added with ID: " + documentReference.getId()))
                .addOnFailureListener(e -> Log.w(TAG, "Error adding expense", e));
    }

    public void markSettled(String expenseId, String debtorUid, OnCompleteCallback callback) {
        // Get current timestamp for settled date
        long settledTimestamp = System.currentTimeMillis();
        
        db.collection(GROUP_EXPENSES_COLLECTION).document(expenseId)
                .update("settledStatus." + debtorUid, true,
                        "settledDates." + debtorUid, settledTimestamp)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Marked settled for " + debtorUid + " on expense " + expenseId + " at " + settledTimestamp);
                    callback.onSuccess("Settled!");
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error marking settled", e);
                    callback.onError(e.getMessage());
                });
    }

    public void getGroupMembers(String groupId, OnMembersCallback callback) {
        db.collection(GROUPS_COLLECTION).document(groupId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Group group = documentSnapshot.toObject(Group.class);
                        if (group != null && group.getMembers() != null) {
                            callback.onMembers(group.getMembers());
                            return;
                        }
                    }
                    callback.onMembers(new ArrayList<>());
                })
                .addOnFailureListener(e -> callback.onMembers(new ArrayList<>()));
    }

    // ========================
    // UTILITIES
    // ========================

    private String generateShareCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(SHARE_CODE_CHARS.charAt(random.nextInt(SHARE_CODE_CHARS.length())));
        }
        return sb.toString();
    }

    public void removeListeners() {
        if (groupsListener != null) {
            groupsListener.remove();
            groupsListener = null;
        }
        if (expensesListener != null) {
            expensesListener.remove();
            expensesListener = null;
        }
        if (allMyExpensesListener != null) {
            allMyExpensesListener.remove();
            allMyExpensesListener = null;
        }
        if (settleUpListener != null) {
            settleUpListener.remove();
            settleUpListener = null;
        }
    }

    // ========================
    // CALLBACKS
    // ========================

    public interface OnCompleteCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public interface OnMembersCallback {
        void onMembers(List<String> memberUids);
    }
}
