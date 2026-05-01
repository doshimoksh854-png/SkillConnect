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
import com.skillconnect.models.Wallet;
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
    private String bookingId, providerId, providerName, jobTitle, skillId;
    private double amount, fullAmount;
    private String selectedMethod = "upi";
    private String generatedTxId;
    private String paymentType; // "booking_fee" or "final_payment"
    // Review data (only for final_payment)
    private float reviewRating;
    private String reviewComment;

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
        skillId      = getIntent().getStringExtra("skill_id");
        providerId   = getIntent().getStringExtra("provider_id");
        providerName = getIntent().getStringExtra("provider_name");
        jobTitle     = getIntent().getStringExtra("job_title");
        amount       = getIntent().getDoubleExtra("amount", 0);
        fullAmount   = getIntent().getDoubleExtra("full_amount", amount);
        paymentType  = getIntent().getStringExtra("payment_type");
        if (paymentType == null) paymentType = "final_payment";
        reviewRating = getIntent().getFloatExtra("review_rating", 0f);
        reviewComment= getIntent().getStringExtra("review_comment");

        if (tvJobTitle    != null) tvJobTitle.setText(jobTitle != null ? jobTitle : "Service");
        if (tvProviderName!= null) tvProviderName.setText(providerName != null ? providerName : "Provider");

        // Set amount + context label based on payment type
        if ("booking_fee".equals(paymentType)) {
            if (tvAmount != null) tvAmount.setText(String.format(Locale.getDefault(),
                    "₹%.0f  (10%% Booking Deposit)", amount));
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Pay Booking Fee");
        } else {
            // final_payment
            double remainingAmt = fullAmount - (fullAmount * 0.10);
            amount = Math.ceil(remainingAmt); // recalculate to be safe
            if (tvAmount != null) tvAmount.setText(String.format(Locale.getDefault(),
                    "₹%.0f  (Remaining 90%%)", amount));
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Final Payment");
        }
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
                if (btnViewReceipt != null) btnViewReceipt.setVisibility(View.VISIBLE);
                savePaymentSuccess();
                break;
            case "pending":
                if (tvResultIcon  != null) tvResultIcon.setText("🕐");
                if (tvResultTitle != null) tvResultTitle.setText("Payment Pending");
                if (tvResultMsg   != null) tvResultMsg.setText("Your Cash on Service payment is pending.\nPay the provider upon service completion.");
                if (btnViewReceipt != null) btnViewReceipt.setVisibility(View.GONE);
                savePaymentPending();
                break;
            case "failed":
                if (tvResultIcon  != null) tvResultIcon.setText("❌");
                if (tvResultTitle != null) tvResultTitle.setText("Payment Failed");
                if (tvResultMsg   != null) tvResultMsg.setText("Transaction could not be completed.\nPlease try again or use a different method.");
                if (btnViewReceipt != null) btnViewReceipt.setVisibility(View.GONE);
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

        // If paying from wallet, deduct from customer wallet first
        if ("wallet".equals(selectedMethod)) {
            repo.getOrCreateWallet(uid, new FirebaseRepository.Callback<Wallet>() {
                @Override
                public void onSuccess(Wallet wallet) {
                    if (wallet.getBalance() >= amount) {
                        double newBalance = wallet.getBalance() - amount;
                        repo.updateWalletBalance(uid, newBalance, success -> {
                            if (success) {
                                // Continue with payment processing after wallet deduction
                                processPaymentAfterWalletDeduction(uid);
                            } else {
                                showPaymentError("Failed to deduct from wallet");
                            }
                        });
                    } else {
                        showPaymentError("Insufficient wallet balance");
                    }
                }
                @Override
                public void onError(String error) {
                    showPaymentError("Failed to check wallet balance: " + error);
                }
            });
        } else {
            // For other payment methods, proceed directly
            processPaymentAfterWalletDeduction(uid);
        }
    }

    private void processPaymentAfterWalletDeduction(String uid) {
        // Transaction record for customer
        Transaction tx = new Transaction(uid, bookingId, "payment", amount, "success",
                selectedMethod, "Payment for: " + jobTitle);
        repo.createTransaction(tx, null);

        // Transaction record for provider (funds held in escrow)
        Transaction provTx = new Transaction(providerId, bookingId, "received", amount,
                "held", selectedMethod, "Payment received for: " + jobTitle + " (Held in escrow)");
        repo.createTransaction(provTx, null);

        // ── Success: handle each payment type differently ──
        if ("booking_fee".equals(paymentType)) {
            // 10% deposit paid — update booking to 'accepted' (work can start)
            repo.updateBookingStatusWithTimestamp(bookingId, "accepted", null);

            // Credit provider wallet with 10% booking fee
            repo.holdEscrowForProvider(providerId, amount, success -> {
                // wallet credited (or failed silently)
            });

            // Schedule 24-hour payment-deadline reminder (in case customer forgets remaining payment)
            schedulePaymentDeadlineCheck(bookingId, uid);

            // Notify provider
            repo.createNotification(new Notification(providerId, "provider", "booking_confirmed",
                    "Job Confirmed ✅",
                    "Customer has paid the ₹" + String.format(Locale.getDefault(), "%.0f", amount) + " booking fee for '" + jobTitle + "'. You can start work!",
                    bookingId), null);

            // Notify customer
            repo.createNotification(new Notification(uid, "customer", "booking_fee_paid",
                    "Booking Confirmed!",
                    "₹" + String.format(Locale.getDefault(), "%.0f", amount) + " booking fee paid. Provider will start work soon.",
                    bookingId), null);

        } else {
            // final_payment (90%) — update booking to 'paid'
            if (bookingId != null) repo.updateBookingStatus(bookingId, "paid", null);

            // Auto-submit review if review data was passed
            if (reviewRating > 0 && bookingId != null) {
                com.skillconnect.models.Review review = new com.skillconnect.models.Review();
                review.setBookingId(bookingId);
                review.setSkillId(skillId != null ? skillId : "");
                review.setUserId(uid);
                review.setUserName(session.getUserName());
                review.setRating(reviewRating);
                review.setComment(reviewComment != null ? reviewComment : "");
                review.setCreatedAt(System.currentTimeMillis());
                repo.addReview(review, docId -> {
                    repo.releaseEscrowFunds(bookingId, uid, providerId, amount, released ->
                        repo.updateBookingStatus(bookingId, "completed", null));
                });
            } else if (bookingId != null) {
                repo.releaseEscrowFunds(bookingId, uid, providerId, amount, released ->
                    repo.updateBookingStatus(bookingId, "completed", null));
            }

            // Notify customer
            repo.createNotification(new Notification(uid, "customer", "payment_success",
                    "Payment Complete ✅",
                    "₹" + String.format(Locale.getDefault(), "%.0f", amount) + " final payment done for '" + jobTitle + "'.",
                    bookingId), null);

            // Notify provider
            repo.createNotification(new Notification(providerId, "provider", "payment_received",
                    "Payment Received 💰",
                    "₹" + String.format(Locale.getDefault(), "%.0f", amount) + " received for '" + jobTitle + "'. Escrow released!",
                    bookingId), null);
        }

        // Save payment record to Firestore
        Map<String, Object> payDoc = new HashMap<>();
        payDoc.put("bookingId", bookingId);
        payDoc.put("customerId", uid);
        payDoc.put("providerId", providerId);
        payDoc.put("amount", amount);
        payDoc.put("status", "held"); // Funds are held in escrow
        payDoc.put("method", selectedMethod);
        payDoc.put("provider", "demo");
        payDoc.put("transactionId", generatedTxId);
        payDoc.put("createdAt", System.currentTimeMillis());
        repo.createPaymentRecord(payDoc, null);
    }

    private void showPaymentError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        // Reset UI to allow retry
        btnPay.setEnabled(true);
        btnPay.setText("Retry Payment");
        if (layoutPaymentForm != null) {
            layoutPaymentResult.setVisibility(View.GONE);
            layoutPaymentForm.setVisibility(View.VISIBLE);
        }
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

    private void schedulePaymentDeadlineCheck(String bookingId, String customerId) {
        if (bookingId == null) return;
        androidx.work.Data inputData = new androidx.work.Data.Builder()
                .putString(PaymentDeadlineWorker.KEY_BOOKING_ID,  bookingId)
                .putString(PaymentDeadlineWorker.KEY_CUSTOMER_ID, customerId)
                .putString(PaymentDeadlineWorker.KEY_JOB_TITLE,   jobTitle != null ? jobTitle : "job")
                .build();
        androidx.work.OneTimeWorkRequest req = new androidx.work.OneTimeWorkRequest.Builder(PaymentDeadlineWorker.class)
                .setInitialDelay(24, java.util.concurrent.TimeUnit.HOURS)
                .setInputData(inputData)
                .addTag("payment_deadline_" + bookingId)
                .build();
        androidx.work.WorkManager.getInstance(this).enqueue(req);
    }
}
