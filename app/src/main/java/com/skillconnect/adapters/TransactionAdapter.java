package com.skillconnect.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.skillconnect.R;
import com.skillconnect.models.Transaction;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private final List<Transaction> list;

    public TransactionAdapter(List<Transaction> list) {
        this.list = list;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                               .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        Transaction t = list.get(pos);

        // Description
        String desc = t.getDescription();
        if (desc == null || desc.isEmpty()) desc = typeLabel(t.getType());
        h.tvDescription.setText(desc);

        // Method
        String method = t.getMethod() != null ? capitalize(t.getMethod()) : "—";
        h.tvMethod.setText(method);

        // Date
        h.tvDate.setText(new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                         .format(new Date(t.getCreatedAt())));

        // Determine credit/debit
        boolean isCredit = "received".equals(t.getType()) || "topup".equals(t.getType())
                           || "refund".equals(t.getType());

        // Amount — green for received/topup, red for payment/withdrawal
        String prefix = isCredit ? "+₹" : "-₹";
        int amtColor   = isCredit ? Color.parseColor("#2E7D32") : Color.parseColor("#C62828");
        h.tvAmount.setText(String.format(Locale.getDefault(), "%s%.0f", prefix, t.getAmount()));
        h.tvAmount.setTextColor(amtColor);

        // Icon background + tint based on type
        int iconBg, iconTint, iconRes;
        switch (t.getType() != null ? t.getType() : "") {
            case "topup":
                iconBg = R.drawable.bg_circle_green;
                iconTint = Color.parseColor("#2E7D32");
                iconRes = R.drawable.ic_add;
                break;
            case "received":
                iconBg = R.drawable.bg_circle_green;
                iconTint = Color.parseColor("#2E7D32");
                iconRes = R.drawable.ic_payment;
                break;
            case "payment":
                iconBg = R.drawable.bg_circle_red;
                iconTint = Color.parseColor("#C62828");
                iconRes = R.drawable.ic_payment;
                break;
            case "withdrawal":
                iconBg = R.drawable.bg_circle_orange;
                iconTint = Color.parseColor("#E65100");
                iconRes = R.drawable.ic_withdraw;
                break;
            case "refund":
                iconBg = R.drawable.bg_circle_blue;
                iconTint = Color.parseColor("#1565C0");
                iconRes = R.drawable.ic_payment;
                break;
            default:
                iconBg = R.drawable.bg_circle_primary;
                iconTint = Color.parseColor("#5C6BC0");
                iconRes = R.drawable.ic_payment;
                break;
        }

        // Apply icon styling
        View iconParent = (View) h.ivIcon.getParent();
        if (iconParent != null) iconParent.setBackgroundResource(iconBg);
        h.ivIcon.setImageResource(iconRes);
        h.ivIcon.setColorFilter(iconTint);

        // Status chip
        h.chipStatus.setText(capitalize(t.getStatus()));
        int chipColor = statusColor(t.getStatus());
        h.chipStatus.setChipBackgroundColor(
            android.content.res.ColorStateList.valueOf(chipColor));
        h.chipStatus.setTextColor(Color.WHITE);
    }

    @Override public int getItemCount() { return list.size(); }

    private String typeLabel(String type) {
        if (type == null) return "Transaction";
        switch (type) {
            case "topup":      return "Wallet Top-up";
            case "payment":    return "Booking Payment";
            case "received":   return "Payment Received";
            case "refund":     return "Refund";
            case "withdrawal": return "Withdrawal";
            default:           return capitalize(type);
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(Locale.getDefault()) + s.substring(1);
    }

    private int statusColor(String status) {
        if (status == null) return Color.GRAY;
        switch (status) {
            case "success":  case "released": return Color.parseColor("#2E7D32");
            case "pending":  case "held":     return Color.parseColor("#E65100");
            case "failed":   case "cancelled": return Color.parseColor("#C62828");
            case "refunded":                  return Color.parseColor("#1565C0");
            default:                          return Color.GRAY;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView  tvDescription, tvMethod, tvDate, tvAmount;
        Chip      chipStatus;

        ViewHolder(View v) {
            super(v);
            ivIcon        = v.findViewById(R.id.ivTxIcon);
            tvDescription = v.findViewById(R.id.tvTxDescription);
            tvMethod      = v.findViewById(R.id.tvTxMethod);
            tvDate        = v.findViewById(R.id.tvTxDate);
            tvAmount      = v.findViewById(R.id.tvTxAmount);
            chipStatus    = v.findViewById(R.id.chipTxStatus);
        }
    }
}
