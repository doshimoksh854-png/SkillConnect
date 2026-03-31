package com.skillconnect;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.skillconnect.adapters.TransactionAdapter;
import com.skillconnect.data.FirebaseRepository;
import com.skillconnect.data.SessionManager;
import com.skillconnect.models.Transaction;
import com.skillconnect.models.Wallet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WalletActivity extends AppCompatActivity {

    private TextView tvBalance, tvPending, tvTotalEarned, tvTotalSpent, tvNoTransactions;
    private ImageView ivToggleBalance;
    private RecyclerView rvRecentTransactions;
    private TransactionAdapter adapter;
    private List<Transaction> txList = new ArrayList<>();
    private FirebaseRepository repo;
    private SessionManager sessionManager;
    private Wallet currentWallet;
    private boolean balanceVisible = true;
    private String actualBalanceText = "₹0.00";
    private String actualPendingText = "₹0.00";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);

        repo           = FirebaseRepository.getInstance();
        sessionManager = new SessionManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvBalance           = findViewById(R.id.tvBalance);
        tvPending           = findViewById(R.id.tvPending);
        tvTotalEarned       = findViewById(R.id.tvTotalEarned);
        tvTotalSpent        = findViewById(R.id.tvTotalSpent);
        tvNoTransactions    = findViewById(R.id.tvNoTransactions);
        rvRecentTransactions= findViewById(R.id.rvRecentTransactions);
        ivToggleBalance     = findViewById(R.id.ivToggleBalance);

        // Action tiles
        MaterialCardView tileAddMoney = findViewById(R.id.tileAddMoney);
        MaterialCardView tileWithdraw = findViewById(R.id.tileWithdraw);
        MaterialCardView tileHistory  = findViewById(R.id.tileHistory);

        // View Transactions button
        View btnViewTx = findViewById(R.id.btnViewTransactions);

        adapter = new TransactionAdapter(txList);
        rvRecentTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvRecentTransactions.setNestedScrollingEnabled(false);
        rvRecentTransactions.setAdapter(adapter);

        // Action tile listeners
        if (tileAddMoney != null) tileAddMoney.setOnClickListener(v -> showAddMoneyDialog());
        if (tileWithdraw != null) tileWithdraw.setOnClickListener(v -> showWithdrawDialog());
        if (tileHistory  != null) tileHistory.setOnClickListener(v ->
                startActivity(new Intent(this, TransactionHistoryActivity.class)));
        if (btnViewTx    != null) btnViewTx.setOnClickListener(v ->
                startActivity(new Intent(this, TransactionHistoryActivity.class)));

        // Quick add chips
        setupQuickAddChips();

        // Balance visibility toggle
        if (ivToggleBalance != null) {
            ivToggleBalance.setOnClickListener(v -> toggleBalanceVisibility());
        }

        loadWallet();
        loadTransactions();
    }

    @Override protected void onResume() {
        super.onResume();
        loadWallet();
        loadTransactions();
    }

    private void setupQuickAddChips() {
        int[] chipIds   = {R.id.chipAdd100, R.id.chipAdd200, R.id.chipAdd500, R.id.chipAdd1000, R.id.chipAdd2000};
        double[] values = {100, 200, 500, 1000, 2000};
        String[] labels = {"₹100", "₹200", "₹500", "₹1,000", "₹2,000"};

        for (int i = 0; i < chipIds.length; i++) {
            Chip chip = findViewById(chipIds[i]);
            if (chip == null) continue;
            final double amount = values[i];
            final String label = labels[i];
            chip.setOnClickListener(v -> quickTopUp(amount, label));
        }
    }

    private void quickTopUp(double amount, String label) {
        String uid = sessionManager.getUserId();
        repo.topUpWallet(uid, amount, result -> {
            Transaction t = new Transaction(uid, "", "topup", amount, "success", "wallet",
                    "Quick Top-up +" + label);
            repo.createTransaction(t, null);
            Toast.makeText(this, label + " added to wallet! ✅", Toast.LENGTH_SHORT).show();
            loadWallet();
            loadTransactions();
        });
    }

    private void toggleBalanceVisibility() {
        balanceVisible = !balanceVisible;
        if (balanceVisible) {
            tvBalance.setText(actualBalanceText);
            tvPending.setText(actualPendingText);
            ivToggleBalance.setImageResource(R.drawable.ic_visibility);
        } else {
            tvBalance.setText("₹ ••••");
            tvPending.setText("₹ ••••");
            ivToggleBalance.setImageResource(R.drawable.ic_visibility_off);
        }
    }

    private void loadWallet() {
        String uid = sessionManager.getUserId();
        repo.getOrCreateWallet(uid, new FirebaseRepository.Callback<Wallet>() {
            @Override public void onSuccess(Wallet w) {
                currentWallet = w;
                actualBalanceText = String.format(Locale.getDefault(), "₹%.2f", w.getBalance());
                actualPendingText = String.format(Locale.getDefault(), "₹%.2f", w.getPendingBalance());
                if (balanceVisible) {
                    tvBalance.setText(actualBalanceText);
                    tvPending.setText(actualPendingText);
                }
            }
            @Override public void onError(String e) {}
        });
    }

    private void loadTransactions() {
        String uid = sessionManager.getUserId();
        repo.getTransactionsForUser(uid, new FirebaseRepository.Callback<List<Transaction>>() {
            @Override public void onSuccess(List<Transaction> result) {
                txList.clear();
                // Show only last 5 on wallet screen
                List<Transaction> recent = result.size() > 5 ? result.subList(0, 5) : result;
                txList.addAll(recent);
                adapter.notifyDataSetChanged();

                // Compute stats from ALL transactions
                double earned = 0, spent = 0;
                for (Transaction t : result) {
                    if ("received".equals(t.getType()) || "topup".equals(t.getType()))
                        earned += t.getAmount();
                    else if ("payment".equals(t.getType()) || "withdrawal".equals(t.getType()))
                        spent += t.getAmount();
                }
                tvTotalEarned.setText(String.format(Locale.getDefault(), "₹%.0f", earned));
                tvTotalSpent.setText(String.format(Locale.getDefault(), "₹%.0f", spent));

                tvNoTransactions.setVisibility(txList.isEmpty() ? View.VISIBLE : View.GONE);
                rvRecentTransactions.setVisibility(txList.isEmpty() ? View.GONE : View.VISIBLE);
            }
            @Override public void onError(String e) {
                tvNoTransactions.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showAddMoneyDialog() {
        String[] amounts = {"₹100", "₹200", "₹500", "₹1,000", "₹2,000", "Custom Amount"};
        double[] values  = {100, 200, 500, 1000, 2000, -1};

        new MaterialAlertDialogBuilder(this)
            .setTitle("Add Money to Wallet")
            .setItems(amounts, (dialog, which) -> {
                if (values[which] == -1) {
                    showCustomAmountDialog(true);
                } else {
                    double amount = values[which];
                    String uid = sessionManager.getUserId();
                    repo.topUpWallet(uid, amount, result -> {
                        Transaction t = new Transaction(uid, "", "topup", amount, "success", "wallet",
                                "Wallet Top-up +" + amounts[which]);
                        repo.createTransaction(t, null);
                        Toast.makeText(this, amounts[which] + " added to wallet! ✅", Toast.LENGTH_SHORT).show();
                        loadWallet();
                        loadTransactions();
                    });
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showWithdrawDialog() {
        if (currentWallet == null || currentWallet.getBalance() <= 0) {
            Toast.makeText(this, "No balance to withdraw", Toast.LENGTH_SHORT).show();
            return;
        }
        showCustomAmountDialog(false);
    }

    /** Custom amount input dialog — used for both add money and withdraw */
    private void showCustomAmountDialog(boolean isTopUp) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint(isTopUp ? "Enter amount to add" : "Enter amount to withdraw");
        input.setPadding(48, 32, 48, 16);

        double maxWithdraw = currentWallet != null ? currentWallet.getBalance() : 0;
        String title = isTopUp ? "Add Custom Amount" : "Withdraw Funds";
        String subtitle = isTopUp ? "" :
                String.format(Locale.getDefault(), "Available: ₹%.2f", maxWithdraw);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(input)
            .setPositiveButton(isTopUp ? "Add" : "Withdraw", (d, w) -> {
                String text = input.getText().toString().trim();
                if (text.isEmpty()) {
                    Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
                    return;
                }
                double amount;
                try { amount = Double.parseDouble(text); } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (amount <= 0) {
                    Toast.makeText(this, "Amount must be positive", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (isTopUp) {
                    String uid = sessionManager.getUserId();
                    repo.topUpWallet(uid, amount, result -> {
                        Transaction t = new Transaction(uid, "", "topup", amount, "success", "wallet",
                                String.format(Locale.getDefault(), "Wallet Top-up +₹%.0f", amount));
                        repo.createTransaction(t, null);
                        Toast.makeText(this, String.format(Locale.getDefault(),
                                "₹%.0f added!", amount), Toast.LENGTH_SHORT).show();
                        loadWallet();
                        loadTransactions();
                    });
                } else {
                    if (amount > maxWithdraw) {
                        Toast.makeText(this,
                                String.format(Locale.getDefault(), "Cannot withdraw more than ₹%.2f", maxWithdraw),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String uid = sessionManager.getUserId();
                    double newBalance = maxWithdraw - amount;
                    Transaction t = new Transaction(uid, "", "withdrawal", amount, "pending", "bank",
                            String.format(Locale.getDefault(), "Withdrawal -₹%.0f", amount));
                    repo.createTransaction(t, null);
                    repo.updateWalletBalance(uid, newBalance, null);
                    Toast.makeText(this, String.format(Locale.getDefault(),
                            "₹%.0f withdrawal submitted!", amount), Toast.LENGTH_SHORT).show();
                    loadWallet();
                    loadTransactions();
                }
            })
            .setNegativeButton("Cancel", null);

        if (!subtitle.isEmpty()) builder.setMessage(subtitle);
        builder.show();
    }
}
