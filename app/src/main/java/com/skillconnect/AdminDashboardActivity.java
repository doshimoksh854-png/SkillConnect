package com.skillconnect;

import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.TextView;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.appbar.MaterialToolbar;
import com.skillconnect.data.FirebaseRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvTotalUsers, tvProviders, tvRevenue, tvCompletionRate, tvCompletionDetail;
    private BarChart chartUserGrowth;
    private PieChart chartBookingStatus;
    private FirebaseRepository repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        repo = FirebaseRepository.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvTotalUsers       = findViewById(R.id.tvTotalUsers);
        tvProviders        = findViewById(R.id.tvProviders);
        tvRevenue          = findViewById(R.id.tvRevenue);
        tvCompletionRate   = findViewById(R.id.tvCompletionRate);
        tvCompletionDetail = findViewById(R.id.tvCompletionDetail);
        chartUserGrowth    = findViewById(R.id.chartUserGrowth);
        chartBookingStatus = findViewById(R.id.chartBookingStatus);

        loadStats();
    }

    private void loadStats() {
        // User counts
        repo.getAdminUserCount(new FirebaseRepository.Callback<int[]>() {
            @Override public void onSuccess(int[] counts) {
                tvTotalUsers.setText(String.valueOf(counts[0]));
                tvProviders.setText(String.valueOf(counts[1]));
            }
            @Override public void onError(String e) {}
        });

        // Revenue
        repo.getAdminRevenue(new FirebaseRepository.Callback<Double>() {
            @Override public void onSuccess(Double total) {
                tvRevenue.setText(String.format(Locale.getDefault(), "₹%.0f", total));
            }
            @Override public void onError(String e) {}
        });

        // Completion rate
        repo.getAdminJobCompletionRate(new FirebaseRepository.Callback<double[]>() {
            @Override public void onSuccess(double[] data) {
                tvCompletionRate.setText(String.format(Locale.getDefault(), "%.0f%%", data[0]));
                tvCompletionDetail.setText(String.format(Locale.getDefault(),
                        "%.0f of %.0f jobs completed", data[1], data[2]));
            }
            @Override public void onError(String e) {}
        });

        // User growth chart
        repo.getAdminMonthlyGrowth(new FirebaseRepository.Callback<Map<String, Integer>>() {
            @Override public void onSuccess(Map<String, Integer> monthly) {
                setupBarChart(monthly);
            }
            @Override public void onError(String e) {}
        });

        // Booking status pie chart
        repo.getAdminBookingStats(new FirebaseRepository.Callback<Map<String, Integer>>() {
            @Override public void onSuccess(Map<String, Integer> stats) {
                setupPieChart(stats);
            }
            @Override public void onError(String e) {}
        });
    }

    private void setupBarChart(Map<String, Integer> monthly) {
        if (chartUserGrowth == null || monthly.isEmpty()) return;

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Integer> e : monthly.entrySet()) {
            entries.add(new BarEntry(i, e.getValue()));
            labels.add(e.getKey());
            i++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "New Users");
        dataSet.setColors(new int[]{
            Color.parseColor("#5C6BC0"),
            Color.parseColor("#7986CB"),
            Color.parseColor("#9FA8DA"),
            Color.parseColor("#3F51B5"),
            Color.parseColor("#303F9F"),
            Color.parseColor("#1A237E")
        });
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.DKGRAY);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);

        chartUserGrowth.setData(barData);
        chartUserGrowth.getDescription().setEnabled(false);
        chartUserGrowth.getLegend().setEnabled(false);
        chartUserGrowth.setFitBars(true);
        chartUserGrowth.animateY(800);

        XAxis xAxis = chartUserGrowth.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        chartUserGrowth.getAxisLeft().setDrawGridLines(false);
        chartUserGrowth.getAxisRight().setEnabled(false);
        chartUserGrowth.invalidate();
    }

    private void setupPieChart(Map<String, Integer> stats) {
        if (chartBookingStatus == null || stats.isEmpty()) return;

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> e : stats.entrySet()) {
            String label = capitalize(e.getKey());
            entries.add(new PieEntry(e.getValue(), label));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(new int[]{
            Color.parseColor("#4CAF50"),  // completed - green
            Color.parseColor("#FF9800"),  // pending - orange
            Color.parseColor("#2196F3"),  // accepted - blue
            Color.parseColor("#F44336"),  // cancelled - red
            Color.parseColor("#9C27B0"),  // paid - purple
            Color.parseColor("#795548"),  // other - brown
        });
        dataSet.setSliceSpace(2f);
        dataSet.setValueTextSize(11f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData pieData = new PieData(dataSet);

        chartBookingStatus.setData(pieData);
        chartBookingStatus.getDescription().setEnabled(false);
        chartBookingStatus.setUsePercentValues(true);
        chartBookingStatus.setEntryLabelTextSize(11f);
        chartBookingStatus.setEntryLabelColor(Color.DKGRAY);
        chartBookingStatus.setCenterText("Bookings");
        chartBookingStatus.setCenterTextSize(14f);
        chartBookingStatus.setHoleRadius(45f);
        chartBookingStatus.setTransparentCircleRadius(50f);
        chartBookingStatus.animateY(800);
        chartBookingStatus.invalidate();
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
