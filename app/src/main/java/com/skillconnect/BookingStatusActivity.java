package com.skillconnect;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.Locale;

/**
 * Activity displaying booking status with progress tracker
 */
public class BookingStatusActivity extends AppCompatActivity {

    private TextView tvSkillTitle, tvProviderName, tvBookingId, tvPrice;
    private String skillTitle, providerName;
    private double price;
    private String bookingId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_status);

        // Initialize views
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        tvSkillTitle = findViewById(R.id.tvSkillTitle);
        tvProviderName = findViewById(R.id.tvProviderName);
        tvBookingId = findViewById(R.id.tvBookingId);
        tvPrice = findViewById(R.id.tvPrice);

        // Setup toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Get booking data
        loadBookingData();

        // Display data
        displayBookingInfo();
    }

    private void loadBookingData() {
        if (getIntent() != null) {
            skillTitle = getIntent().getStringExtra("skill_title");
            providerName = getIntent().getStringExtra("provider_name");
            price = getIntent().getDoubleExtra("price", 0);
        }

        // Generate random booking ID
        bookingId = "#BK" + (10000 + (int) (Math.random() * 90000));
    }

    private void displayBookingInfo() {
        tvSkillTitle.setText(skillTitle);
        tvProviderName.setText(providerName);
        tvBookingId.setText(bookingId);
        tvPrice.setText(String.format(Locale.getDefault(), "$%.0f", price));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
