package com.skillconnect;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.skillconnect.adapters.TransactionAdapter;
import com.skillconnect.data.FirebaseRepository;
import com.skillconnect.data.SessionManager;
import com.skillconnect.models.Transaction;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TransactionHistoryActivity extends AppCompatActivity {

    private RecyclerView rvTransactions;
    private TextView tvEmpty, tvTotalIn, tvTotalOut, tvTxCount;
    private TransactionAdapter adapter;
    private List<Transaction> txList = new ArrayList<>();
    private List<Transaction> allTransactions = new ArrayList<>();
    private FirebaseRepository repo;
    private SessionManager sessionManager;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

        repo           = FirebaseRepository.getInstance();
        sessionManager = new SessionManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvTransactions = findViewById(R.id.rvTransactions);
        tvEmpty        = findViewById(R.id.tvEmpty);
        tvTotalIn      = findViewById(R.id.tvTotalIn);
        tvTotalOut     = findViewById(R.id.tvTotalOut);
        tvTxCount      = findViewById(R.id.tvTxCount);

        adapter = new TransactionAdapter(txList);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(adapter);

        // Filter chips
        ChipGroup chipGroup = findViewById(R.id.chipGroupFilter);
        if (chipGroup != null) {
            chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty()) { applyFilter("all"); return; }
                int id = checkedIds.get(0);
                if (id == R.id.chipCredit)  applyFilter("credit");
                else if (id == R.id.chipDebit)   applyFilter("debit");
                else if (id == R.id.chipPending) applyFilter("pending");
                else applyFilter("all");
            });
        }

        loadTransactions();
    }

    private void loadTransactions() {
        String uid = sessionManager.getUserId();
        repo.getTransactionsForUser(uid, new FirebaseRepository.Callback<List<Transaction>>() {
            @Override public void onSuccess(List<Transaction> result) {
                allTransactions.clear();
                allTransactions.addAll(result);
                applyFilter("all");
            }
            @Override public void onError(String e) {
                tvEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    private void applyFilter(String filter) {
        txList.clear();
        double totalIn = 0, totalOut = 0;

        for (Transaction t : allTransactions) {
            boolean isCredit = "received".equals(t.getType()) || "topup".equals(t.getType())
                    || "refund".equals(t.getType());
            boolean isPending = "pending".equals(t.getStatus()) || "held".equals(t.getStatus());

            if (isCredit) totalIn += t.getAmount();
            else totalOut += t.getAmount();

            switch (filter) {
                case "credit":
                    if (isCredit) txList.add(t);
                    break;
                case "debit":
                    if (!isCredit) txList.add(t);
                    break;
                case "pending":
                    if (isPending) txList.add(t);
                    break;
                default:
                    txList.add(t);
                    break;
            }
        }

        adapter.notifyDataSetChanged();
        boolean empty = txList.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvTransactions.setVisibility(empty ? View.GONE : View.VISIBLE);

        if (tvTotalIn  != null) tvTotalIn.setText(String.format(Locale.getDefault(), "₹%.0f", totalIn));
        if (tvTotalOut != null) tvTotalOut.setText(String.format(Locale.getDefault(), "₹%.0f", totalOut));
        if (tvTxCount  != null) tvTxCount.setText(String.valueOf(txList.size()));
    }
}
