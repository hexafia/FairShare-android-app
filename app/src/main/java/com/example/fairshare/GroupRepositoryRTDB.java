package com.example.fairshare;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository handling all Firebase Realtime Database operations for groups
 * and group expenses.
 */
public class GroupRepositoryRTDB {

    private static final String TAG = "GroupRepository";
    private static final String GROUPS_REF = "groups";
    private static final String GROUP_EXPENSES_REF = "group_expenses";
    private static final String SHARE_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final DatabaseReference dbRef;
    private final MutableLiveData<List<Group>> groupsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<GroupExpense>> expensesLiveData = new MutableLiveData<>(new ArrayList<>());

    private ValueEventListener groupsListener;
    private ValueEventListener expensesListener;
    private DatabaseReference currentGroupsQuery;
    private DatabaseReference currentExpensesRef;

    public GroupRepositoryRTDB() {
        dbRef = FirebaseDatabase.getInstance().getReference();
    }

    // ========================
    // GROUP OPERATIONS
    // ========================

    /**
     * Returns LiveData of all groups the current user is a member of.
     */
    public LiveData<List<Group>> getGroups() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return groupsLiveData;

        String uid = user.getUid();
        DatabaseReference groupsRef = dbRef.child(GROUPS_REF);

        if (groupsListener == null) {
            groupsListener = groupsRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<Group> groups = new ArrayList<>();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        // Only include groups where the current user is a member
                        DataSnapshot membersSnap = child.child("members").child(uid);
                        if (membersSnap.exists() && Boolean.TRUE.equals(membersSnap.getValue(Boolean.class))) {
                            Group group = child.getValue(Group.class);
                            if (group != null) {
                                group.setId(child.getKey());
                                groups.add(group);
                            }
                        }
                    }
                    groupsLiveData.setValue(groups);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.w(TAG, "Groups listen cancelled", error.toException());
                }
            });
            currentGroupsQuery = groupsRef;
        }
        return groupsLiveData;
    }

    /**
     * Creates a new group with a random 6-character share code.
     * The creating user is automatically added as a member.
     */
    public void createGroup(String name, OnCompleteCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onError("Not authenticated");
            return;
        }

        String shareCode = generateShareCode();
        Group group = new Group(name, shareCode, user.getUid());
        group.getMembers().add(user.getUid());

        String groupId = dbRef.child(GROUPS_REF).push().getKey();
        if (groupId == null) {
            callback.onError("Failed to generate group ID");
            return;
        }

        dbRef.child(GROUPS_REF).child(groupId).setValue(group)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Group created: " + groupId + " code: " + shareCode);
                    callback.onSuccess(shareCode);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error creating group", e);
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Joins a group by its 6-character share code.
     * Searches all groups for one matching the code, then adds the current user.
     */
    public void joinGroup(String shareCode, OnCompleteCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onError("Not authenticated");
            return;
        }

        String codeUpper = shareCode.trim().toUpperCase();
        dbRef.child(GROUPS_REF).orderByChild("shareCode").equalTo(codeUpper)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            callback.onError("No group found with that code");
                            return;
                        }
                        for (DataSnapshot child : snapshot.getChildren()) {
                            String groupId = child.getKey();
                            // Add user to group members
                            dbRef.child(GROUPS_REF).child(groupId).child("members")
                                    .child(user.getUid()).setValue(true)
                                    .addOnSuccessListener(aVoid -> {
                                        Group g = child.getValue(Group.class);
                                        String gName = g != null ? g.getName() : "Group";
                                        callback.onSuccess("Joined: " + gName);
                                    })
                                    .addOnFailureListener(e -> callback.onError(e.getMessage()));
                            break; // take the first match
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError(error.getMessage());
                    }
                });
    }

    // ========================
    // GROUP EXPENSE OPERATIONS
    // ========================

    /**
     * Returns LiveData of all expenses for a specific group, ordered by timestamp.
     */
    public LiveData<List<GroupExpense>> getGroupExpenses(String groupId) {
        DatabaseReference ref = dbRef.child(GROUP_EXPENSES_REF).child(groupId);

        if (expensesListener != null && currentExpensesRef != null) {
            currentExpensesRef.removeEventListener(expensesListener);
        }

        expensesListener = ref.orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<GroupExpense> expenses = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    GroupExpense expense = child.getValue(GroupExpense.class);
                    if (expense != null) {
                        expense.setId(child.getKey());
                        expenses.add(0, expense); // reverse for newest first
                    }
                }
                expensesLiveData.setValue(expenses);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Expenses listen cancelled", error.toException());
            }
        });
        currentExpensesRef = ref;
        return expensesLiveData;
    }

    /**
     * Adds a new shared expense to a group.
     * All current group members are added as participants (Equal Split).
     */
    public void addGroupExpense(String groupId, GroupExpense expense) {
        String expenseId = dbRef.child(GROUP_EXPENSES_REF).child(groupId).push().getKey();
        if (expenseId != null) {
            dbRef.child(GROUP_EXPENSES_REF).child(groupId).child(expenseId).setValue(expense)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Expense added: " + expenseId))
                    .addOnFailureListener(e -> Log.w(TAG, "Error adding expense", e));
        }
    }

    /**
     * Fetches the members map for a specific group (one-shot).
     */
    public void getGroupMembers(String groupId, OnMembersCallback callback) {
        dbRef.child(GROUPS_REF).child(groupId).child("members")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<String> uids = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            uids.add(child.getKey());
                        }
                        callback.onMembers(uids);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onMembers(new ArrayList<>());
                    }
                });
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
        if (groupsListener != null && currentGroupsQuery != null) {
            currentGroupsQuery.removeEventListener(groupsListener);
            groupsListener = null;
        }
        if (expensesListener != null && currentExpensesRef != null) {
            currentExpensesRef.removeEventListener(expensesListener);
            expensesListener = null;
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
