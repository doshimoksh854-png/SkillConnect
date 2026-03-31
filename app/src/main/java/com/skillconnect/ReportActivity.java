package com.skillconnect;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.skillconnect.data.FirebaseRepository;
import com.skillconnect.data.SessionManager;
import java.util.HashMap;
import java.util.Map;

public class ReportActivity extends AppCompatActivity {

    private static final String[] REASONS = {
        "Poor Quality of Service",
        "Payment Issue",
        "Inappropriate Behavior",
        "Service Not Delivered",
        "Fraudulent Activity",
        "Other"
    };

    private AutoCompleteTextView spinnerReason;
    private TextInputEditText etDescription;
    private MaterialButton btnSubmit;
    private TextView tvBookingRef;

    private FirebaseRepository repo;
    private SessionManager session;

    private String bookingId, againstId, againstName, serviceName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        repo = FirebaseRepository.getInstance();
        session = new SessionManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        spinnerReason = findViewById(R.id.spinnerReason);
        etDescription = findViewById(R.id.etDescription);
        btnSubmit     = findViewById(R.id.btnSubmitReport);
        tvBookingRef  = findViewById(R.id.tvBookingRef);

        // Populate reason dropdown
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_dropdown_item_1line, REASONS);
        spinnerReason.setAdapter(adapter);

        // Unpack intent
        bookingId   = getIntent().getStringExtra("booking_id");
        againstId   = getIntent().getStringExtra("against_id");
        againstName = getIntent().getStringExtra("against_name");
        serviceName = getIntent().getStringExtra("service_name");

        tvBookingRef.setText((serviceName != null ? serviceName : "Service") 
            + " — " + (againstName != null ? againstName : "User"));

        btnSubmit.setOnClickListener(v -> submitReport());
    }

    private void submitReport() {
        String reason = spinnerReason.getText().toString().trim();
        String desc = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";

        if (TextUtils.isEmpty(reason)) {
            spinnerReason.setError("Select a reason");
            return;
        }
        if (TextUtils.isEmpty(desc) || desc.length() < 20) {
            etDescription.setError("Please describe the issue (at least 20 characters)");
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Submitting...");

        Map<String, Object> dispute = new HashMap<>();
        dispute.put("bookingId",    bookingId);
        dispute.put("reporterId",   session.getUserId());
        dispute.put("reporterName", session.getUserName());
        dispute.put("reporterRole", session.getUserRole());
        dispute.put("againstId",    againstId);
        dispute.put("againstName",  againstName);
        dispute.put("reason",       reason);
        dispute.put("description",  desc);
        dispute.put("status",       "open");
        dispute.put("timestamp",    System.currentTimeMillis());

        repo.createDispute(dispute, new FirebaseRepository.Callback<String>() {
            @Override
            public void onSuccess(String docId) {
                Toast.makeText(ReportActivity.this, 
                    "Report submitted! Our team will review it within 24 hours.", 
                    Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onError(String error) {
                btnSubmit.setEnabled(true);
                btnSubmit.setText("Submit Report");
                Toast.makeText(ReportActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
