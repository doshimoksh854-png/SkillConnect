package com.skillconnect;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Displays a vertical stepper timeline showing the booking lifecycle stages.
 * Stages: Booked → Accepted → Work In Progress → Ready for Review → Payment → Completed
 */
public class BookingTimelineActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_timeline);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Unpack booking info
        String title = getIntent().getStringExtra("booking_title");
        String provider = getIntent().getStringExtra("booking_provider");
        double price = getIntent().getDoubleExtra("booking_price", 0);
        String status = getIntent().getStringExtra("booking_status");
        long bookingDate = getIntent().getLongExtra("booking_date", 0);

        TextView tvTitle = findViewById(R.id.tvBookingTitle);
        TextView tvProvider = findViewById(R.id.tvBookingProvider);
        TextView tvPrice = findViewById(R.id.tvBookingPrice);

        tvTitle.setText(title != null ? title : "Service");
        tvProvider.setText(provider != null ? "Provider: " + provider : "");
        tvPrice.setText(String.format(Locale.getDefault(), "₹%.0f", price));

        // Build timeline
        buildTimeline(status != null ? status.toLowerCase() : "pending", bookingDate);
    }

    private void buildTimeline(String currentStatus, long bookingDate) {
        LinearLayout container = findViewById(R.id.layoutTimeline);
        container.removeAllViews();

        // Define the ordered stages
        List<String[]> stages = new ArrayList<>();
        stages.add(new String[]{"pending",          "Booking Created",     "Your booking request has been submitted"});
        stages.add(new String[]{"accepted",         "Provider Accepted",   "The provider has accepted your booking"});
        stages.add(new String[]{"awarded",          "Booking Fee Paid",    "10% booking fee paid, work can begin"});
        stages.add(new String[]{"ready_for_review", "Work Completed",      "Provider has marked the work as done"});
        stages.add(new String[]{"paid",             "Final Payment Done",  "Remaining 90% payment processed"});
        stages.add(new String[]{"completed",        "Booking Completed",   "Review submitted, escrow funds released"});

        // Find current stage index
        int currentIdx = -1;
        for (int i = 0; i < stages.size(); i++) {
            if (stages.get(i)[0].equals(currentStatus)) {
                currentIdx = i;
                break;
            }
        }

        // Handle special statuses
        boolean isCancelled = "cancelled".equals(currentStatus) || "rejected".equals(currentStatus);

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

        for (int i = 0; i < stages.size(); i++) {
            String[] stage = stages.get(i);
            boolean isCompleted = i < currentIdx;
            boolean isCurrent = i == currentIdx;
            boolean isFuture = i > currentIdx;

            if (isCancelled && i > 0) {
                isFuture = true;
                isCompleted = false;
                isCurrent = false;
            }

            addTimelineStep(container, stage[1], stage[2],
                isCompleted, isCurrent, isFuture,
                i == 0 && bookingDate > 0 ? sdf.format(new Date(bookingDate)) : null,
                i < stages.size() - 1);
        }

        // Add cancelled step if applicable
        if (isCancelled) {
            addTimelineStep(container, 
                "cancelled".equals(currentStatus) ? "Booking Cancelled" : "Booking Rejected",
                "cancelled".equals(currentStatus) ? "This booking was cancelled" : "The provider rejected this booking",
                false, true, false, null, false);
        }
    }

    private void addTimelineStep(LinearLayout container, String title, String description,
                                  boolean isCompleted, boolean isCurrent, boolean isFuture,
                                  String dateText, boolean showConnector) {
        // Step container
        LinearLayout stepLayout = new LinearLayout(this);
        stepLayout.setOrientation(LinearLayout.HORIZONTAL);
        stepLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // Left column (indicator + connector line)
        LinearLayout leftCol = new LinearLayout(this);
        leftCol.setOrientation(LinearLayout.VERTICAL);
        leftCol.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
            dpToPx(40), LinearLayout.LayoutParams.WRAP_CONTENT);
        leftCol.setLayoutParams(leftParams);

        // Circle indicator
        View circle = new View(this);
        int circleSize = dpToPx(isCompleted || isCurrent ? 20 : 16);
        LinearLayout.LayoutParams circleParams = new LinearLayout.LayoutParams(circleSize, circleSize);
        circleParams.gravity = Gravity.CENTER_HORIZONTAL;
        circle.setLayoutParams(circleParams);

        GradientDrawable circleBg = new GradientDrawable();
        circleBg.setShape(GradientDrawable.OVAL);
        if (isCompleted) {
            circleBg.setColor(0xFF4CAF50); // green
        } else if (isCurrent) {
            circleBg.setColor(0xFF1976D2); // blue
        } else {
            circleBg.setColor(0xFFBDBDBD); // grey
        }
        circle.setBackground(circleBg);
        leftCol.addView(circle);

        // Connector line
        if (showConnector) {
            View line = new View(this);
            LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(
                dpToPx(2), dpToPx(50));
            lineParams.gravity = Gravity.CENTER_HORIZONTAL;
            lineParams.topMargin = dpToPx(4);
            line.setLayoutParams(lineParams);
            line.setBackgroundColor(isCompleted ? 0xFF4CAF50 : 0xFFE0E0E0);
            leftCol.addView(line);
        }

        stepLayout.addView(leftCol);

        // Right column (text content)
        LinearLayout rightCol = new LinearLayout(this);
        rightCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        rightParams.setMarginStart(dpToPx(12));
        rightCol.setLayoutParams(rightParams);

        // Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(15);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        if (isFuture) {
            tvTitle.setTextColor(0xFFBDBDBD);
        } else if (isCurrent) {
            tvTitle.setTextColor(0xFF1976D2);
        } else {
            tvTitle.setTextColor(0xFF212121);
        }
        rightCol.addView(tvTitle);

        // Description
        TextView tvDesc = new TextView(this);
        tvDesc.setText(description);
        tvDesc.setTextSize(13);
        tvDesc.setTextColor(isFuture ? 0xFFBDBDBD : 0xFF757575);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        descParams.topMargin = dpToPx(2);
        tvDesc.setLayoutParams(descParams);
        rightCol.addView(tvDesc);

        // Date if available
        if (dateText != null) {
            TextView tvDate = new TextView(this);
            tvDate.setText(dateText);
            tvDate.setTextSize(11);
            tvDate.setTextColor(0xFF9E9E9E);
            LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            dateParams.topMargin = dpToPx(2);
            tvDate.setLayoutParams(dateParams);
            rightCol.addView(tvDate);
        }

        // Current status indicator
        if (isCurrent) {
            TextView tvCurrent = new TextView(this);
            tvCurrent.setText("● Current Status");
            tvCurrent.setTextSize(11);
            tvCurrent.setTextColor(0xFF1976D2);
            tvCurrent.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams curParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            curParams.topMargin = dpToPx(4);
            tvCurrent.setLayoutParams(curParams);
            rightCol.addView(tvCurrent);
        }

        stepLayout.addView(rightCol);

        // Add padding
        int pad = dpToPx(4);
        stepLayout.setPadding(0, pad, 0, pad);

        container.addView(stepLayout);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
