package com.skillconnect.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.skillconnect.R;
import com.skillconnect.models.Bid;
import java.util.List;
import java.util.Locale;

public class BidAdapter extends RecyclerView.Adapter<BidAdapter.BidViewHolder> {

    private List<Bid> bids;
    private final boolean isCustomer;
    private boolean isJobOpen;
    private final String currentUserId;
    private final OnBidActionListener listener;

    // Distinct colors for avatar backgrounds
    private static final int[] AVATAR_COLORS = {
        0xFF1A73E8, 0xFFE53935, 0xFF43A047, 0xFFFB8C00,
        0xFF8E24AA, 0xFF00ACC1, 0xFFD81B60, 0xFF00897B
    };

    public interface OnBidActionListener {
        void onAcceptBid(Bid bid);
        void onChatBid(Bid bid);
    }

    public BidAdapter(List<Bid> bids, boolean isCustomer, boolean isJobOpen,
                      String currentUserId, OnBidActionListener listener) {
        this.bids = bids;
        this.isCustomer = isCustomer;
        this.isJobOpen = isJobOpen;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void updateBids(List<Bid> newBids, boolean isJobOpen) {
        this.bids = newBids;
        this.isJobOpen = isJobOpen;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BidViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bid, parent, false);
        return new BidViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull BidViewHolder holder, int position) {
        Bid bid = bids.get(position);

        // Avatar: first letter of provider name + a unique color
        String name = bid.getProviderName() != null ? bid.getProviderName() : "?";
        String initial = name.substring(0, 1).toUpperCase();
        holder.tvAvatar.setText(initial);
        int color = AVATAR_COLORS[Math.abs(name.hashCode()) % AVATAR_COLORS.length];
        holder.tvAvatar.getBackground().setTint(color);

        holder.tvProviderName.setText(name);
        holder.tvBidAmount.setText(String.format(Locale.getDefault(), "₹%.0f", bid.getBidAmount()));
        holder.tvProposal.setText(bid.getProposal());

        // Status badge
        String status = bid.getStatus() != null ? bid.getStatus() : "pending";
        holder.tvStatus.setText(status.toUpperCase());
        if ("accepted".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(Color.parseColor("#2ECC71"));
        } else if ("rejected".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(Color.parseColor("#E53935"));
        } else {
            holder.tvStatus.setTextColor(Color.parseColor("#FB8C00"));
        }

        // --- Customer sees Accept + Chat on open pending bids ---
        if (isCustomer) {
            boolean canAccept = isJobOpen && "pending".equalsIgnoreCase(bid.getStatus());
            holder.btnAccept.setVisibility(canAccept ? View.VISIBLE : View.GONE);
            if (canAccept) {
                holder.btnAccept.setOnClickListener(v -> {
                    if (listener != null) listener.onAcceptBid(bid);
                });
            }
            // Customer can chat with any bidder
            holder.btnChat.setVisibility(View.VISIBLE);
            holder.btnChat.setOnClickListener(v -> {
                if (listener != null) listener.onChatBid(bid);
            });
        } else {
            // Provider: hide Accept, show Chat only on own bid
            holder.btnAccept.setVisibility(View.GONE);
            boolean isOwnBid = currentUserId != null && currentUserId.equals(bid.getProviderId());
            holder.btnChat.setVisibility(isOwnBid ? View.VISIBLE : View.GONE);
            if (isOwnBid) {
                holder.btnChat.setText("💬 Chat with Customer");
                holder.btnChat.setOnClickListener(v -> {
                    if (listener != null) listener.onChatBid(bid);
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return bids != null ? bids.size() : 0;
    }

    static class BidViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvProviderName, tvStatus, tvBidAmount, tvProposal;
        MaterialButton btnAccept, btnChat;

        BidViewHolder(@NonNull View v) {
            super(v);
            tvAvatar       = v.findViewById(R.id.tvAvatar);
            tvProviderName = v.findViewById(R.id.tvProviderName);
            tvStatus       = v.findViewById(R.id.tvStatus);
            tvBidAmount    = v.findViewById(R.id.tvBidAmount);
            tvProposal     = v.findViewById(R.id.tvProposal);
            btnAccept      = v.findViewById(R.id.btnAcceptBid);
            btnChat        = v.findViewById(R.id.btnChatBid);
        }
    }
}
