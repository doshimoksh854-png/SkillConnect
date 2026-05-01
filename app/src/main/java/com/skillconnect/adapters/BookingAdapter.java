package com.skillconnect.adapters;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.skillconnect.R;
import com.skillconnect.models.Booking;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for booking cards with status-coloured chips and role-based actions
 */
public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    private List<Booking> bookings;
    private final String userRole; // "customer" or "provider"
    private OnBookingActionListener listener;

    public interface OnBookingActionListener {
        void onAccept(Booking booking);
        void onReject(Booking booking);
        void onComplete(Booking booking);
        void onCancel(Booking booking);
        void onLeaveReview(Booking booking);
        void onReviewWork(Booking booking);
        void onChat(Booking booking);
        void onPayNow(Booking booking);
    }

    public BookingAdapter(List<Booking> bookings, String userRole, OnBookingActionListener listener) {
        this.bookings = bookings;
        this.userRole = userRole;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_booking_card, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookings.get(position);
        holder.bind(booking);
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    public void updateBookings(List<Booking> newBookings) {
        this.bookings = newBookings;
        notifyDataSetChanged();
    }

    class BookingViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvSkillTitle, tvStatus, tvSubtitle, tvPrice, tvDate;
        private final LinearLayout layoutActions;
        private final MaterialButton btnPrimary, btnSecondary, btnChat;

        BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSkillTitle = itemView.findViewById(R.id.tvSkillTitle);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvDate = itemView.findViewById(R.id.tvDate);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnPrimary = itemView.findViewById(R.id.btnPrimary);
            btnSecondary = itemView.findViewById(R.id.btnSecondary);
            btnChat = itemView.findViewById(R.id.btnChat);
        }

        void bind(Booking booking) {
            tvSkillTitle.setText(booking.getSkillTitle());
            tvPrice.setText(String.format(Locale.getDefault(), "₹%.0f", booking.getPrice()));

            // Format date
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            tvDate.setText(sdf.format(new Date(booking.getBookingDate())));

            // Subtitle depends on role
            if ("provider".equals(userRole)) {
                tvSubtitle.setText("Customer booking");
            } else {
                tvSubtitle.setText(booking.getProviderName());
            }

            // Status chip with colour
            String status = booking.getStatus();
            tvStatus.setText(status.toUpperCase());
            GradientDrawable statusBg = new GradientDrawable();
            statusBg.setCornerRadius(20f);

            int statusColor;
            switch (status.toLowerCase()) {
                case "accepted":
                    statusColor = ContextCompat.getColor(itemView.getContext(), R.color.status_accepted);
                    break;
                case "completed":
                case "paid":
                    statusColor = ContextCompat.getColor(itemView.getContext(), R.color.status_completed);
                    break;
                case "cancelled":
                case "rejected":
                    statusColor = ContextCompat.getColor(itemView.getContext(), R.color.status_cancelled);
                    break;
                case "ready_for_review":
                    statusColor = ContextCompat.getColor(itemView.getContext(), R.color.status_accepted);
                    break;
                case "awarded":   // 10% not yet paid
                    statusColor = 0xFFFF9800; // orange
                    break;
                default: // pending
                    statusColor = ContextCompat.getColor(itemView.getContext(), R.color.status_pending);
                    break;
            }
            // Enhance UI: 20% opacity background (51/255) with full color text
            int alphaColor = (statusColor & 0x00FFFFFF) | (51 << 24);
            statusBg.setColor(alphaColor);
            tvStatus.setBackground(statusBg);
            tvStatus.setTextColor(statusColor);

            // Action buttons based on role + status
            setupActions(booking);
        }

        private void setupActions(Booking booking) {
            String status = booking.getStatus().toLowerCase();
            layoutActions.setVisibility(View.GONE);
            btnPrimary.setVisibility(View.GONE);
            btnSecondary.setVisibility(View.GONE);
            btnChat.setVisibility(View.GONE);

            if ("provider".equals(userRole)) {
                // Show chat button for providers on all active bookings
                layoutActions.setVisibility(View.VISIBLE);
                btnChat.setVisibility(View.VISIBLE);
                btnChat.setOnClickListener(v -> listener.onChat(booking));

                if ("pending".equals(status)) {
                    btnPrimary.setVisibility(View.VISIBLE);
                    btnSecondary.setVisibility(View.VISIBLE);
                    btnPrimary.setText(R.string.accept);
                    btnSecondary.setText(R.string.reject);
                    btnPrimary.setOnClickListener(v -> listener.onAccept(booking));
                    btnSecondary.setOnClickListener(v -> listener.onReject(booking));
                } else if ("accepted".equals(status)) {
                    btnPrimary.setVisibility(View.VISIBLE);
                    btnPrimary.setText(R.string.mark_done);
                    btnPrimary.setOnClickListener(v -> listener.onComplete(booking));
                }
            } else {
                // Customer—also show chat on active bookings
                if ("pending".equals(status) || "accepted".equals(status) || "awarded".equals(status)) {
                    layoutActions.setVisibility(View.VISIBLE);
                    btnChat.setVisibility(View.VISIBLE);
                    btnChat.setOnClickListener(v -> listener.onChat(booking));
                }
                if ("pending".equals(status)) {
                    btnSecondary.setVisibility(View.VISIBLE);
                    btnSecondary.setText(R.string.cancel_booking);
                    btnSecondary.setOnClickListener(v -> listener.onCancel(booking));
                } else if ("awarded".equals(status)) {
                    // Booking accepted but 10% deposit not yet paid
                    layoutActions.setVisibility(View.VISIBLE);
                    btnPrimary.setVisibility(View.VISIBLE);
                    btnPrimary.setText("Pay Booking Fee (10%)");
                    btnPrimary.setOnClickListener(v -> listener.onPayNow(booking));
                } else if ("ready_for_review".equals(status)) {
                    // Provider marked task done — customer must review + pay remaining 90%
                    layoutActions.setVisibility(View.VISIBLE);
                    btnPrimary.setVisibility(View.VISIBLE);
                    btnPrimary.setText("Review & Pay Remaining (90%)");
                    btnPrimary.setOnClickListener(v -> listener.onPayNow(booking));
                    btnChat.setVisibility(View.VISIBLE);
                    btnChat.setOnClickListener(v -> listener.onChat(booking));
                } else if ("paid".equals(status)) {
                    // Payment done — customer can now leave a review
                    layoutActions.setVisibility(View.VISIBLE);
                    btnPrimary.setVisibility(View.VISIBLE);
                    btnPrimary.setText(R.string.leave_review);
                    btnPrimary.setOnClickListener(v -> listener.onLeaveReview(booking));
                } else if ("completed".equals(status)) {
                    layoutActions.setVisibility(View.VISIBLE);
                    btnPrimary.setVisibility(View.VISIBLE);
                    btnPrimary.setText(R.string.leave_review);
                    btnPrimary.setOnClickListener(v -> listener.onLeaveReview(booking));
                }
            }
        }
    }
}
