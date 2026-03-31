package com.skillconnect;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.skillconnect.data.FirebaseRepository;
import com.skillconnect.data.SessionManager;
import com.skillconnect.models.Notification;
import com.skillconnect.models.Transaction;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class PaymentActivity extends AppCompatActivity {

    // ── PAYMENT PROVIDER FLAG — switch to RAZORPAY later ──
    private enum PaymentProvider { DEMO, RAZORPAY }
    private static final PaymentProvider ACTIVE_PROVIDER = PaymentProvider.DEMO;

    private TextView tvJobTitle, tvProviderName, tvAmount;
    private RadioGroup rgPaymentMethod;
    private MaterialButton btnPay;
    private ProgressBar progressBar;
    private LinearLayout layoutPaymentForm, layoutPaymentResult;
    private TextView tvResultIcon, tvResultTitle, tvResultMsg;
    private MaterialButton btnViewReceipt, btnGoHome;

    private FirebaseRepository repo;
    private SessionManager session;
    private String bookingId, providerId, providerName, jobTitle;
    private double amount;
    private String selectedMethod = "upi";
    private String generatedTxId;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        repo    = FirebaseRepository.getInstance();
        session = new SessionManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        initViews();
        unpackIntent();
    }

    private void initViews() {
        tvJobTitle        = findViewById(R.id.tvJobTitle);
        tvProviderName    = findViewById(R.id.tvProviderName);
        tvAmount          = findViewById(R.id.tvAmount);
        rgPaymentMethod   = findViewById(R.id.rgPaymentMethod);
        btnPay            = findViewById(R.id.btnPay);
        progressBar       = findViewById(R.id.progressBar);
        layoutPaymentForm = findViewById(R.id.layoutPaymentForm);
        layoutPaymentResult = findViewById(R.id.layoutPaymentResult);
        tvResultIcon      = findViewById(R.id.tvResultIcon);
        tvResultTitle     = findViewById(R.id.tvResultTitle);
        tvResultMsg       = findViewById(R.id.tvResultMsg);
        btnViewReceipt    = findViewById(R.id.btnViewReceipt);
        btnGoHome         = findViewById(R.id.btnGoHome);

        if (rgPaymentMethod != null) {
            rgPaymentMethod.setOnCheckedChangeListener((group, id) -> {
                if (id == R.id.rbWallet)  selectedMethod = "wallet";
                else if (id == R.id.rbUPI)  selectedMethod = "upi";
                else if (id == R.id.rbCard) selectedMethod = "card";
                else if (id == R.id.rbCash) selectedMethod = "cash";
            });
        }

        btnPay.setOnClickListener(v -> {
            if (ACTIVE_PROVIDER == PaymentProvider.DEMO) startDemoPayment();
            // else startRazorpayPayment();  // TODO: activate when ready
        });

        if (btnGoHome != null) {
            btnGoHome.setOnClickListener(v -> {
                startActivity(new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
            });
        }
    }

    private void unpackIntent() {
        bookingId    = getIntent().getStringExtra("booking_id");
        providerId   = getIntent().getStringExtra("provider_id");
        providerName = getIntent().getStringExtra("provider_name");
        jobTitle     = getIntent().getStringExtra("job_title");
        amount       = getIntent().getDoubleExtra("amount", 0);

        if (tvJobTitle    != null) tvJobTitle.setText(jobTitle != null ? jobTitle : "Service");
        if (tvProviderName!= null) tvProviderName.setText(providerName != null ? providerName : "Provider");
        if (tvAmount      != null) tvAmount.setText(String.format(Locale.getDefault(), "₹%.0f", amount));
    }

    // ────────────────────────────────────────────────────────
    // DEMO PAYMENT FLOW
    // ────────────────────────────────────────────────────────

    private void startDemoPayment() {
        btnPay.setEnabled(false);
        btnPay.setText("Processing...");
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        // Simulate network delay 2 sec
        new Handler().postDelayed(() -> {
            if (progressBar != null) progressBar.setVisibility(View.GONE);

            // Cash on service = always pending, else simulate success with 90% chance
            if ("cash".equals(selectedMethod)) {
                showResult("pending");
            } else {
                boolean success = new Random().nextInt(10) < 9; // 90% success
                showResult(success ? "success" : "failed");
            }
        }, 2000);
    }

    private void showResult(String status) {
        if (layoutPaymentForm   != null) layoutPaymentForm.setVisibility(View.GONE);
        if (layoutPaymentResult != null) layoutPaymentResult.setVisibility(View.VISIBLE);

        generatedTxId = "TXN" + System.currentTimeMillis();

        switch (status) {
            case "success":
                if (tvResultIcon  != null) tvResultIcon.setText("✅");
                if (tvResultTitle != null) tvResultTitle.setText("Payment Successful!");
                if (tvResultMsg   != null) tvResultMsg.setText("₹" + String.format(Locale.getDefault(), "%.0f", amount)
                        + " paid via " + selectedMethod.toUpperCase() + "\nTxn ID: " + generatedTxId);
                savePaymentSuccess();
                break;
            case "pending":
                if (tvResultIcon  != null) tvResultIcon.setText("🕐");
                if (tvResultTitle != null) tvResultTitle.setText("Payment Pending");
                if (tvResultMsg   != null) tvResultMsg.setText("Your Cash on Service payment is pending.\nPay the provider upon service completion.");
                savePaymentPending();
                break;
            case "failed":
                if (tvResultIcon  != null) tvResultIcon.setText("❌");
                if (tvResultTitle != null) tvResultTitle.setText("Payment Failed");
                if (tvResultMsg   != null) tvResultMsg.setText("Transaction could not be completed.\nPlease try again or use a different method.");
                btnPay.setEnabled(true);
                btnPay.setText("Retry Payment");
                if (layoutPaymentForm != null) {
                    new Handler().postDelayed(() -> {
                        if (layoutPaymentResult != null) layoutPaymentResult.setVisibility(View.GONE);
                        layoutPaymentForm.setVisibility(View.VISIBLE);
                    }, 3000);
                }
                break;
        }

        if (btnViewReceipt != null) {
            btnViewReceipt.setOnClickListener(v -> openReceipt(status));
        }
    }

    private void savePaymentSuccess() {
        String uid = session.getUserId();

        // Transaction record for customer
        Transaction tx = new Transaction(uid, bookingId, "payment", amount, "success",
                selectedMethod, "Payment for: " + jobTitle);
        repo.createTransaction(tx, null);

        // Transaction record for provider
        Transaction provTx = new Transaction(providerId, bookingId, "received", amount,
                "held", selectedMethod, "Payment received for: " + jobTitle);
        repo.createTransaction(provTx, null);

        // Update booking status
        if (bookingId != null) repo.updateBookingStatus(bookingId, "paid", null);

        // Save payment record to Firestore
        Map<String, Object> payDoc = new HashMap<>();
        payDoc.put("bookingId", bookingId);
        payDoc.put("customerId", uid);
        payDoc.put("providerId", providerId);
        payDoc.put("amount", amount);
        payDoc.put("status", "held");
        payDoc.put("method", selectedMethod);
        payDoc.put("provider", "demo");
        payDoc.put("transactionId", generatedTxId);
        payDoc.put("createdAt", System.currentTimeMillis());
        repo.createPaymentRecord(payDoc, null);

        // Customer notification
        repo.createNotification(new Notification(uid, "customer", "payment_success",
                "Payment Successful ✅",
                "₹" + String.format(Locale.getDefault(), "%.0f", amount) + " paid for " + jobTitle,
                bookingId), null);

        // Provider notification
        repo.createNotification(new Notification(providerId, "provider", "payment_received",
                "Payment Received 💰",
                "₹" + String.format(Locale.getDefault(), "%.0f", amount) + " received for " + jobTitle
                + ". Funds held until job completion.",
                bookingId), null);
    }

    private void savePaymentPending() {
        String uid = session.getUserId();
        Transaction tx = new Transaction(uid, bookingId, "payment", amount, "pending",
                "cash", "Cash on Service for: " + jobTitle);
        repo.createTransaction(tx, null);
        if (bookingId != null) repo.updateBookingStatus(bookingId, "accepted", null);
    }

    private void openReceipt(String status) {
        Intent i = new Intent(this, PaymentReceiptActivity.class);
        i.putExtra("job_title",     jobTitle);
        i.putExtra("provider_name", providerName);
        i.putExtra("customer_name", session.getUserName());
        i.putExtra("amount",        amount);
        i.putExtra("status",        status);
        i.putExtra("method",        selectedMethod);
        i.putExtra("tx_id",         generatedTxId);
        i.putExtra("booking_id",    bookingId);
        startActivity(i);
    }
}
