package com.skillconnect;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.skillconnect.adapters.PaymentAdapter;
import com.skillconnect.data.FirebaseRepository;
import com.skillconnect.data.SessionManager;
import java.util.List;
import java.util.Map;

public class PaymentHistoryActivity extends AppCompatActivity {

    private RecyclerView rvPayments;
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;
    private FirebaseRepository repo;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_history);

        repo = FirebaseRepository.getInstance();
        session = new SessionManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        rvPayments = findViewById(R.id.rvPayments);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        progressBar = findViewById(R.id.progressBar);

        rvPayments.setLayoutManager(new LinearLayoutManager(this));

        loadPayments();
    }

    private void loadPayments() {
        progressBar.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        rvPayments.setVisibility(View.GONE);

        repo.getPaymentHistory(session.getUserId(), new FirebaseRepository.Callback<List<Map<String, Object>>>() {
            @Override
            public void onSuccess(List<Map<String, Object>> result) {
                progressBar.setVisibility(View.GONE);
                if (result == null || result.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                } else {
                    rvPayments.setVisibility(View.VISIBLE);
                    boolean isCustomer = "customer".equalsIgnoreCase(session.getUserRole());
                    PaymentAdapter adapter = new PaymentAdapter(result, isCustomer);
                    rvPayments.setAdapter(adapter);
                }
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                layoutEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
