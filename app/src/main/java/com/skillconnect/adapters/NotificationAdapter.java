package com.skillconnect.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.skillconnect.R;
import com.skillconnect.models.Notification;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification n);
        void onNotificationDelete(Notification n, int position);
    }

    private final List<Notification> list;
    private final OnNotificationClickListener listener;

    public NotificationAdapter(List<Notification> list, OnNotificationClickListener listener) {
        this.list     = list;
        this.listener = listener;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                               .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        Notification n = list.get(pos);
        h.tvTitle.setText(n.getTitle());
        h.tvMessage.setText(n.getMessage());
        h.tvTime.setText(formatTime(n.getCreatedAt()));
        h.viewUnreadDot.setVisibility(n.isRead() ? View.INVISIBLE : View.VISIBLE);

        // Card background tint for unread
        int bgColor = n.isRead()
                ? android.graphics.Color.parseColor("#FFFFFF")
                : android.graphics.Color.parseColor("#F0F4FF");
        if (h.itemView instanceof com.google.android.material.card.MaterialCardView) {
            ((com.google.android.material.card.MaterialCardView) h.itemView).setCardBackgroundColor(bgColor);
        }

        // Icon based on type
        h.ivIcon.setImageResource(getIconForType(n.getType()));

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onNotificationClick(n);
        });
    }

    @Override public int getItemCount() { return list.size(); }

    public void removeItem(int pos) {
        if (pos >= 0 && pos < list.size()) {
            list.remove(pos);
            notifyItemRemoved(pos);
        }
    }

    private String formatTime(long millis) {
        long diff = System.currentTimeMillis() - millis;
        if (diff < 60_000)          return "Just now";
        if (diff < 3_600_000)       return (diff / 60_000) + " min ago";
        if (diff < 86_400_000)      return (diff / 3_600_000) + " hr ago";
        return new SimpleDateFormat("dd MMM", Locale.getDefault()).format(new Date(millis));
    }

    private int getIconForType(String type) {
        if (type == null) return R.drawable.ic_notification;
        switch (type) {
            case "bid_received":
            case "bid_accepted":
            case "bid_rejected":   return R.drawable.ic_jobs;
            case "new_message":    return R.drawable.ic_nav_chat;
            case "booking_confirmed":
            case "booking_cancelled": return R.drawable.ic_nav_bookings;
            case "payment_received":
            case "payment_success": return R.drawable.ic_payment;
            default:               return R.drawable.ic_notification;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView  tvTitle, tvMessage, tvTime;
        View      viewUnreadDot;

        ViewHolder(View v) {
            super(v);
            ivIcon        = v.findViewById(R.id.ivNotifIcon);
            tvTitle       = v.findViewById(R.id.tvNotifTitle);
            tvMessage     = v.findViewById(R.id.tvNotifMessage);
            tvTime        = v.findViewById(R.id.tvNotifTime);
            viewUnreadDot = v.findViewById(R.id.viewUnreadDot);
        }
    }
}
