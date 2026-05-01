package com.skillconnect;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class PaymentReceiptActivity extends AppCompatActivity {

    private View receiptCard;
    private MaterialButton btnDownload;
    private ProgressBar downloadProgress;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_receipt);

        if (getIntent() == null || getIntent().getExtras() == null) {
            Toast.makeText(this, "Receipt data not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        String jobTitle     = getIntent().getStringExtra("job_title");
        String providerName = getIntent().getStringExtra("provider_name");
        String customerName = getIntent().getStringExtra("customer_name");
        double amount       = getIntent().getDoubleExtra("amount", 0);
        String status       = getIntent().getStringExtra("status");
        String method       = getIntent().getStringExtra("method");
        String txId         = getIntent().getStringExtra("tx_id");
        String bookingId    = getIntent().getStringExtra("booking_id");

        receiptCard     = findViewById(R.id.receiptCard);
        btnDownload     = findViewById(R.id.btnDownload);
        downloadProgress= findViewById(R.id.downloadProgress);

        TextView tvService   = findViewById(R.id.tvReceiptService);
        TextView tvProvider  = findViewById(R.id.tvReceiptProvider);
        TextView tvCustomer  = findViewById(R.id.tvReceiptCustomer);
        TextView tvAmount    = findViewById(R.id.tvReceiptAmount);
        TextView tvStatus    = findViewById(R.id.tvReceiptStatus);
        TextView tvMethod    = findViewById(R.id.tvReceiptMethod);
        TextView tvDate      = findViewById(R.id.tvReceiptDate);
        TextView tvTxId      = findViewById(R.id.tvReceiptTxId);
        TextView tvBookingId = findViewById(R.id.tvReceiptBookingId);

        if (tvService   != null) tvService.setText(jobTitle     != null ? jobTitle     : "—");
        if (tvProvider  != null) tvProvider.setText(providerName!= null ? providerName : "—");
        if (tvCustomer  != null) tvCustomer.setText(customerName!= null ? customerName : "—");
        if (tvAmount    != null) tvAmount.setText(String.format(Locale.getDefault(), "₹%.0f", amount));
        if (tvStatus    != null) tvStatus.setText(status != null ? capitalize(status)  : "—");
        if (tvMethod    != null) tvMethod.setText(method != null ? method.toUpperCase(Locale.getDefault()) : "—");
        if (tvDate      != null) tvDate.setText(new SimpleDateFormat("dd MMM yyyy, hh:mm a",
                                    Locale.getDefault()).format(new Date()));
        if (tvTxId      != null) tvTxId.setText(txId      != null ? txId      : "DEMO-" + System.currentTimeMillis());
        if (tvBookingId != null) tvBookingId.setText(bookingId != null ? bookingId : "—");

        MaterialButton btnDone = findViewById(R.id.btnDone);
        if (btnDone != null) btnDone.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        });

        if (btnDownload != null) {
            btnDownload.setOnClickListener(v -> downloadReceipt());
        }

        // Hide progress bar initially
        if (downloadProgress != null) downloadProgress.setVisibility(View.GONE);
    }

    private void downloadReceipt() {
        if (receiptCard == null) {
            Toast.makeText(this, "Receipt not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        if (btnDownload != null)    { btnDownload.setEnabled(false); btnDownload.setText("Preparing…"); }
        if (downloadProgress != null) downloadProgress.setVisibility(View.VISIBLE);

        // Render view → Bitmap off-thread, save on main
        Executors.newSingleThreadExecutor().execute(() -> {
            Bitmap bitmap = captureBitmap(receiptCard);
            new Handler(Looper.getMainLooper()).post(() -> {
                if (bitmap == null) {
                    resetDownloadButton();
                    Toast.makeText(this, "Failed to capture receipt image", Toast.LENGTH_SHORT).show();
                    return;
                }
                saveBitmapAndShare(bitmap);
            });
        });
    }

    /** Renders a View into a Bitmap (handles views not yet drawn) */
    private Bitmap captureBitmap(View view) {
        try {
            int w = view.getWidth();
            int h = view.getHeight();
            if (w == 0 || h == 0) return null;
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            view.draw(canvas);
            return bmp;
        } catch (Exception e) {
            return null;
        }
    }

    private void saveBitmapAndShare(Bitmap bitmap) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName  = "SkillConnect_Receipt_" + timeStamp + ".png";
            Uri savedUri;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ — use MediaStore (no WRITE_EXTERNAL_STORAGE needed)
                ContentValues cv = new ContentValues();
                cv.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                cv.put(MediaStore.Downloads.MIME_TYPE, "image/png");
                cv.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                ContentResolver resolver = getContentResolver();
                savedUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                if (savedUri == null) throw new Exception("MediaStore insert failed");
                try (OutputStream out = resolver.openOutputStream(savedUri)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                }
            } else {
                // Android < 10 — classic file approach
                File dir  = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!dir.exists()) dir.mkdirs();
                File file = new File(dir, fileName);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                }
                savedUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            }

            resetDownloadButton();
            if (downloadProgress != null) downloadProgress.setVisibility(View.GONE);
            Toast.makeText(this, "✅ Receipt saved to Downloads: " + fileName, Toast.LENGTH_LONG).show();

            // Offer to share
            shareReceipt(savedUri);

        } catch (Exception e) {
            resetDownloadButton();
            if (downloadProgress != null) downloadProgress.setVisibility(View.GONE);
            Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void shareReceipt(Uri fileUri) {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "SkillConnect Payment Receipt");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Here is my payment receipt from SkillConnect.");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share Receipt via"));
        } catch (Exception ignored) {}
    }

    private void resetDownloadButton() {
        if (btnDownload != null) {
            btnDownload.setEnabled(true);
            btnDownload.setText("⬇ Download Receipt");
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(Locale.getDefault()) + s.substring(1);
    }
}
