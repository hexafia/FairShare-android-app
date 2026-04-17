package com.example.fairshare.ui.dashboard;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.fairshare.CurrencyHelper;
import com.example.fairshare.DebtSimplifier;
import com.example.fairshare.ExpenseAdapter;
import com.example.fairshare.GroupExpense;
import com.example.fairshare.GroupRepository;
import com.example.fairshare.R;
import com.example.fairshare.Transaction;
import com.example.fairshare.databinding.FragmentDashboardBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class DashboardFragment extends Fragment implements com.example.fairshare.FastActionHandler {

    private FragmentDashboardBinding binding;
    private DashboardViewModel viewModel;
    private ExpenseAdapter adapter;
    private com.example.fairshare.ui.groups.GroupAdapter activeGroupsAdapter;

    private List<Transaction> currentPersonalExpenses = new ArrayList<>();
    private List<GroupExpense> currentGroupExpenses = new ArrayList<>();
    private List<com.example.fairshare.Group> currentGroups = new ArrayList<>();
    
    // Master lists for stats calculation (decoupled from filtered display)
    private List<Transaction> masterPersonalExpenses = new ArrayList<>();
    private List<GroupExpense> masterGroupExpenses = new ArrayList<>();
    
    // Top card total - reflects all personal expenses loaded from Firestore
    private double grandTotal = 0.0;

    // OCR and Camera
    private ActivityResultLauncher<Intent> cameraLauncher;
    private Uri currentPhotoUri;
    private TextInputEditText activeAmountEditText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Register camera launcher
        cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && currentPhotoUri != null && activeAmountEditText != null) {
                    com.example.fairshare.OcrHelper.extractAmountFromReceipt(requireContext(), currentPhotoUri, new com.example.fairshare.OcrHelper.OcrCallback() {
                        @Override
                        public void onSuccess(String amount) {
                            if (amount != null && !amount.isEmpty()) {
                                activeAmountEditText.setText(amount);
                                Toast.makeText(requireContext(), "Scan successful", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(requireContext(), "Could not detect total amount", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(requireContext(), "Scan failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

        setupRecyclerView();
        setupViewModel();

        binding.btnDashboardAddExpense.setOnClickListener(v -> showTransactionTypeDialog());
        binding.btnDashboardNewGroup.setOnClickListener(v -> showCreateGroupDialog());
    }

    @Override
    public void onFastAction() {
        showTransactionTypeDialog();
    }

    private void setupRecyclerView() {
        adapter = new ExpenseAdapter(this::showDeleteConfirmation);
        binding.rvExpenses.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvExpenses.setAdapter(adapter);
        
        // Setup Active Groups RecyclerView
        activeGroupsAdapter = new com.example.fairshare.ui.groups.GroupAdapter(group -> {
            Intent intent = new Intent(requireContext(), com.example.fairshare.ui.groups.GroupLobbyActivity.class);
            intent.putExtra("GROUP_ID", group.getId());
            intent.putExtra("GROUP_NAME", group.getName());
            intent.putExtra("SHARE_CODE", group.getShareCode());
            startActivity(intent);
        });
        binding.rvActiveGroups.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvActiveGroups.setAdapter(activeGroupsAdapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);

        viewModel.getPersonalExpenses().observe(getViewLifecycleOwner(), transactions -> {
            currentPersonalExpenses = transactions != null ? transactions : new ArrayList<>();
            masterPersonalExpenses = transactions != null ? transactions : new ArrayList<>();
            
            android.util.Log.d("DASHBOARD", "Personal expenses observer called. Count: " + (transactions != null ? transactions.size() : 0));
            for (Transaction t : currentPersonalExpenses) {
                android.util.Log.d("DASHBOARD", "  - " + t.getTitle() + ": " + t.getAmount() + " on " + t.getDate());
            }
            
            adapter.submitList(currentPersonalExpenses);
            
            // Update grandTotal only from Firestore LiveData observer
            updateGrandTotal();
            // Set UI once during initial data load
            binding.tvBalance.setText(CurrencyHelper.format(grandTotal));
            // Restore balance calculations
            updateSummary();

            if (currentPersonalExpenses.isEmpty()) {
                binding.tvEmpty.setVisibility(View.VISIBLE);
                binding.rvExpenses.setVisibility(View.GONE);
            } else {
                binding.tvEmpty.setVisibility(View.GONE);
                binding.rvExpenses.setVisibility(View.VISIBLE);
            }
        });

        // Set up click listeners for View All links
        binding.tvViewAllGroups.setOnClickListener(v -> {
            // Navigate to GroupsFragment
            androidx.fragment.app.FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.nav_host_fragment, new com.example.fairshare.ui.groups.GroupsFragment());
            transaction.addToBackStack(null);
            transaction.commit();
        });

        // Set up Recent Transactions View All link
        binding.tvViewAllTransactions.setOnClickListener(v -> {
            // Navigate to MainActivity (Ledger)
            Intent intent = new Intent(requireContext(), com.example.fairshare.MainActivity.class);
            startActivity(intent);
        });

        viewModel.getGroupExpenses().observe(getViewLifecycleOwner(), expenses -> {
            currentGroupExpenses = expenses != null ? expenses : new ArrayList<>();
            masterGroupExpenses = expenses != null ? expenses : new ArrayList<>();
            
            // Update grandTotal only from Firestore LiveData observer
            updateGrandTotal();
            // Set UI once during initial data load
            binding.tvBalance.setText(CurrencyHelper.format(grandTotal));
            // Restore balance calculations
            updateSummary();
        });

        viewModel.getGroups().observe(getViewLifecycleOwner(), groups -> {
            currentGroups = groups != null ? groups : new ArrayList<>();
            
            // Update Active Groups section - only show active groups
            List<com.example.fairshare.Group> activeGroups = new ArrayList<>();
            for (com.example.fairshare.Group group : currentGroups) {
                if (group.isActive()) {
                    activeGroups.add(group);
                }
            }
            activeGroupsAdapter.submitList(activeGroups);
        });
    }

    private boolean isCurrentMonth(Date date) {
        if (date == null) return false;
        Calendar today = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.setTime(date);
        return today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
               today.get(Calendar.MONTH) == target.get(Calendar.MONTH);
    }

    private boolean isCurrentMonth(long timestamp) {
        Calendar today = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(timestamp);
        return today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
               today.get(Calendar.MONTH) == target.get(Calendar.MONTH);
    }

    private void updateGrandTotal() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        double personalSpent = 0;
        for (Transaction t : masterPersonalExpenses) {
            personalSpent += t.getAmount();
        }

        grandTotal = personalSpent;
    }

    private void updateSummary() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String myUid = user.getUid();

        double personalSpent = 0;
        for (Transaction t : masterPersonalExpenses) {
            personalSpent += t.getAmount();
        }
        
        android.util.Log.d("DASHBOARD", "updateSummary() - personalSpent: " + personalSpent + ", masterPersonalExpenses size: " + masterPersonalExpenses.size());

        List<GroupExpense> monthGroupExpenses = new ArrayList<>();
        double groupPaid = 0;
        for (GroupExpense ge : masterGroupExpenses) {
            if (isCurrentMonth(ge.getTimestamp())) {
                monthGroupExpenses.add(ge);
                if (myUid.equals(ge.getPayerUid())) {
                    groupPaid += ge.getAmount();
                }
            }
        }

        double remainingOwed = 0;
        List<DebtSimplifier.Debt> debts = DebtSimplifier.simplify(monthGroupExpenses);
        for (DebtSimplifier.Debt d : debts) {
            if (d.debtorUid.equals(myUid)) {
                remainingOwed += d.amount;
            }
        }

        // REMOVED: UI text setting - now handled only in LiveData observer
        binding.tvPersonal.setText(CurrencyHelper.format(personalSpent));
        binding.tvPaid.setText(CurrencyHelper.format(groupPaid));
        binding.tvRemaining.setText(CurrencyHelper.format(remainingOwed));
        
        android.util.Log.d("DASHBOARD", "updateSummary() - Updated UI: Personal=" + personalSpent + ", Paid=" + groupPaid + ", Remaining=" + remainingOwed);
    }

    private void showTransactionTypeDialog() {
        Dialog dialog = new Dialog(requireContext(), R.style.Theme_FairShare_Dialog);
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_select_list, null);
        dialog.setContentView(dialogView);

        android.widget.TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        android.widget.ListView listView = dialogView.findViewById(R.id.listView);

        tvTitle.setText("Select type of transaction");
        String[] options = {"Personal Expense", "Group Expense"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.item_select_dialog, options);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            dialog.dismiss();
            if (position == 0) {
                showAddDialog();
            } else {
                showAddGroupExpenseDialog();
            }
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            int width = (int) (350 * getResources().getDisplayMetrics().density);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    // This handles the regular personal transaction dialog
    private void showAddDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_expense, null);

        TextInputEditText etTitle = dialogView.findViewById(R.id.etTitle);
        TextInputEditText etAmount = dialogView.findViewById(R.id.etAmount);
        Spinner spinnerCategory = dialogView.findViewById(R.id.spinnerCategory);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.categories, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        Dialog dialog = new Dialog(requireContext(), R.style.Theme_FairShare_Dialog);
        dialog.setContentView(dialogView);
        dialog.setCancelable(true);

        com.google.android.material.button.MaterialButton btnScanReceipt = dialogView.findViewById(R.id.btnScanReceipt);
        if (btnScanReceipt != null) {
            btnScanReceipt.setOnClickListener(v -> {
                activeAmountEditText = etAmount;
                launchCamera();
            });
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
            String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";

            if (title.isEmpty()) {
                etTitle.setError("Title is required");
                return;
            }
            if (amountStr.isEmpty()) {
                etAmount.setError("Amount is required");
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                etAmount.setError("Invalid amount");
                return;
            }

            String category = spinnerCategory.getSelectedItem().toString();

            Transaction transaction = new Transaction(title, amount, category);
            viewModel.addPersonalExpense(transaction);

            Toast.makeText(requireContext(), "Transaction added!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            int width = (int) (350 * getResources().getDisplayMetrics().density);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void launchCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File storageDir = new File(requireContext().getCacheDir(), "images");
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }
            photoFile = File.createTempFile("JPEG_" + timeStamp + "_", ".jpg", storageDir);
        } catch (IOException ex) {
            Toast.makeText(requireContext(), "Error creating file for image", Toast.LENGTH_SHORT).show();
            return;
        }

        if (photoFile != null) {
            currentPhotoUri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
            cameraLauncher.launch(takePictureIntent);
        }
    }

    private void showAddGroupExpenseDialog() {
        if (currentGroups.isEmpty()) {
            Toast.makeText(requireContext(), "You are not part of any groups", Toast.LENGTH_SHORT).show();
            return;
        }

        GroupRepository groupRepository = new GroupRepository();
        AddGroupExpenseDialog dialogBuilder = new AddGroupExpenseDialog(
                requireContext(),
                currentGroups,
                viewModel,
                groupRepository,
                null);
        dialogBuilder.show();
    }

    private void showDeleteConfirmation(Transaction transaction) {
        new AlertDialog.Builder(requireContext(), R.style.Theme_FairShare_Dialog)
                .setTitle(R.string.delete_confirm_title)
                .setMessage(R.string.delete_confirm_msg)
                .setPositiveButton(R.string.delete, (d, which) -> {
                    if (transaction.getId() != null) {
                        viewModel.deletePersonalExpense(transaction.getId());
                        Toast.makeText(requireContext(), "Transaction deleted", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showCreateGroupDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_group, null);
        TextInputEditText etGroupName = dialogView.findViewById(R.id.etGroupName);
        MaterialButton btnCreate = dialogView.findViewById(R.id.btnCreate);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        Dialog dialog = new Dialog(requireContext(), R.style.Theme_FairShare_Dialog);
        dialog.setContentView(dialogView);
        dialog.setCancelable(true);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnCreate.setOnClickListener(v -> {
            String name = etGroupName.getText() != null ? etGroupName.getText().toString().trim() : "";
            if (name.isEmpty()) {
                etGroupName.setError("Group name is required");
                return;
            }

            viewModel.createGroup(name, new GroupRepository.OnCompleteCallback() {
                @Override
                public void onSuccess(String message) {
                    Toast.makeText(requireContext(), "Group created! Code: " + message, Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            int width = (int) (350 * getResources().getDisplayMetrics().density);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
