package com.skillconnect;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PaymentReceiptActivity extends AppCompatActivity {

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_receipt);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        String jobTitle      = getIntent().getStringExtra("job_title");
        String providerName  = getIntent().getStringExtra("provider_name");
        String customerName  = getIntent().getStringExtra("customer_name");
        double amount        = getIntent().getDoubleExtra("amount", 0);
        String status        = getIntent().getStringExtra("status");
        String method        = getIntent().getStringExtra("method");
        String txId          = getIntent().getStringExtra("tx_id");
        String bookingId     = getIntent().getStringExtra("booking_id");

        TextView tvService      = findViewById(R.id.tvReceiptService);
        TextView tvProvider     = findViewById(R.id.tvReceiptProvider);
        TextView tvCustomer     = findViewById(R.id.tvReceiptCustomer);
        TextView tvAmount       = findViewById(R.id.tvReceiptAmount);
        TextView tvStatus       = findViewById(R.id.tvReceiptStatus);
        TextView tvMethod       = findViewById(R.id.tvReceiptMethod);
        TextView tvDate         = findViewById(R.id.tvReceiptDate);
        TextView tvTxId         = findViewById(R.id.tvReceiptTxId);
        TextView tvBookingId    = findViewById(R.id.tvReceiptBookingId);

        if (tvService   != null) tvService.setText(jobTitle != null ? jobTitle : "—");
        if (tvProvider  != null) tvProvider.setText(providerName != null ? providerName : "—");
        if (tvCustomer  != null) tvCustomer.setText(customerName != null ? customerName : "—");
        if (tvAmount    != null) tvAmount.setText(String.format(Locale.getDefault(), "₹%.0f", amount));
        if (tvStatus    != null) tvStatus.setText(status != null ? capitalize(status) : "—");
        if (tvMethod    != null) tvMethod.setText(method != null ? method.toUpperCase(Locale.getDefault()) : "—");
        if (tvDate      != null) tvDate.setText(new SimpleDateFormat("dd MMM yyyy, hh:mm a",
                                    Locale.getDefault()).format(new Date()));
        if (tvTxId      != null) tvTxId.setText(txId != null ? txId : "DEMO-" + System.currentTimeMillis());
        if (tvBookingId != null) tvBookingId.setText(bookingId != null ? bookingId : "—");

        MaterialButton btnDone = findViewById(R.id.btnDone);
        if (btnDone != null) btnDone.setOnClickListener(v -> finish());
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(Locale.getDefault()) + s.substring(1);
    }
}
