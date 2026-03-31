package com.skillconnect.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.skillconnect.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PaymentAdapter extends RecyclerView.Adapter<PaymentAdapter.PaymentViewHolder> {

    private final List<Map<String, Object>> payments;
    private final boolean isCustomer;

    public PaymentAdapter(List<Map<String, Object>> payments, boolean isCustomer) {
        this.payments = payments;
        this.isCustomer = isCustomer;
    }

    @NonNull
    @Override
    public PaymentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_payment, parent, false);
        return new PaymentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PaymentViewHolder holder, int position) {
        Map<String, Object> payment = payments.get(position);

        String jobTitle = (String) payment.get("jobTitle");
        double amount = payment.containsKey("amount") ? ((Number) payment.get("amount")).doubleValue() : 0;
        String status = (String) payment.get("status");
        long timestamp = payment.containsKey("timestamp") ? ((Number) payment.get("timestamp")).longValue() : 0;

        holder.tvJobTitle.setText(jobTitle != null ? jobTitle : "Service");
        holder.tvAmount.setText(String.format(Locale.getDefault(), "₹%.0f", amount));

        if (status != null) {
            holder.tvStatus.setText(status.toUpperCase());
            if ("success".equalsIgnoreCase(status) || "paid".equalsIgnoreCase(status)) {
                holder.tvStatus.setTextColor(0xFF4CAF50); // Green
            } else if ("failed".equalsIgnoreCase(status)) {
                holder.tvStatus.setTextColor(0xFFF44336); // Red
            } else {
                holder.tvStatus.setTextColor(0xFFFF9800); // Orange
            }
        }

        if (timestamp > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            holder.tvDate.setText(sdf.format(new Date(timestamp)));
        } else {
            holder.tvDate.setText("-");
        }

        if (isCustomer) {
            String providerName = (String) payment.get("providerName");
            holder.tvProviderName.setText("Paid to: " + (providerName != null ? providerName : "Provider"));
        } else {
            String customerName = (String) payment.get("customerName");
            holder.tvProviderName.setText("Received from: " + (customerName != null ? customerName : "Customer"));
        }
    }

    @Override
    public int getItemCount() {
        return payments.size();
    }

    static class PaymentViewHolder extends RecyclerView.ViewHolder {
        TextView tvJobTitle, tvProviderName, tvAmount, tvStatus, tvDate;

        public PaymentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvJobTitle = itemView.findViewById(R.id.tvJobTitle);
            tvProviderName = itemView.findViewById(R.id.tvProviderName);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
    }
}
