package com.skillconnect.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.skillconnect.R;
import com.skillconnect.models.Message;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_SENT     = 1;
    private static final int VIEW_RECEIVED = 2;

    private final List<Message> messages;
    private final String        currentUserId;

    public ChatAdapter(List<Message> messages, String currentUserId) {
        this.messages      = messages;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        return currentUserId.equals(messages.get(position).getSenderId())
                ? VIEW_SENT : VIEW_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_SENT) {
            return new SentViewHolder(inf.inflate(R.layout.item_message_sent, parent, false));
        } else {
            return new ReceivedViewHolder(inf.inflate(R.layout.item_message_received, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message msg = messages.get(position);
        String  time = new SimpleDateFormat("hh:mm a", Locale.getDefault())
                .format(new Date(msg.getTimestamp()));
        if (holder instanceof SentViewHolder) {
            SentViewHolder h = (SentViewHolder) holder;
            h.tvTime.setText(time);
            
            // Handle text
            if (msg.getText() != null && !msg.getText().isEmpty()) {
                h.tvMessage.setVisibility(View.VISIBLE);
                h.tvMessage.setText(msg.getText());
            } else {
                h.tvMessage.setVisibility(View.GONE);
            }
            
            // Handle image/file
            if (msg.getImageUrl() != null && !msg.getImageUrl().isEmpty()) {
                h.ivAttachment.setVisibility(View.VISIBLE);
                
                // Show image, or a fallback icon if it's a document (PDF, etc)
                Glide.with(h.itemView.getContext())
                        .load(msg.getImageUrl())
                        .centerCrop()
                        .error(android.R.drawable.ic_menu_report_image) // fallback for files
                        .into(h.ivAttachment);
                        
                // Make it clickable to open in browser/viewer
                h.ivAttachment.setOnClickListener(v -> {
                    android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                    i.setData(android.net.Uri.parse(msg.getImageUrl()));
                    h.itemView.getContext().startActivity(i);
                });
            } else {
                h.ivAttachment.setVisibility(View.GONE);
                h.ivAttachment.setOnClickListener(null);
            }
            
        } else {
            ReceivedViewHolder h = (ReceivedViewHolder) holder;
            h.tvSenderName.setText(msg.getSenderName());
            h.tvTime.setText(time);
            
            // Handle text
            if (msg.getText() != null && !msg.getText().isEmpty()) {
                h.tvMessage.setVisibility(View.VISIBLE);
                h.tvMessage.setText(msg.getText());
            } else {
                h.tvMessage.setVisibility(View.GONE);
            }
            
            // Handle image/file
            if (msg.getImageUrl() != null && !msg.getImageUrl().isEmpty()) {
                h.ivAttachment.setVisibility(View.VISIBLE);
                
                // Show image, or a fallback icon if it's a document (PDF, etc)
                Glide.with(h.itemView.getContext())
                        .load(msg.getImageUrl())
                        .centerCrop()
                        .error(android.R.drawable.ic_menu_report_image) // fallback for files
                        .into(h.ivAttachment);
                        
                // Make it clickable to open in browser/viewer
                h.ivAttachment.setOnClickListener(v -> {
                    android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                    i.setData(android.net.Uri.parse(msg.getImageUrl()));
                    h.itemView.getContext().startActivity(i);
                });
            } else {
                h.ivAttachment.setVisibility(View.GONE);
                h.ivAttachment.setOnClickListener(null);
            }
        }
    }

    @Override public int getItemCount() { return messages.size(); }

    // —— ViewHolders ——
    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        ImageView ivAttachment;
        SentViewHolder(View v) {
            super(v);
            tvMessage    = v.findViewById(R.id.tvMessage);
            tvTime       = v.findViewById(R.id.tvTime);
            ivAttachment = v.findViewById(R.id.ivAttachment);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView tvSenderName, tvMessage, tvTime;
        ImageView ivAttachment;
        ReceivedViewHolder(View v) {
            super(v);
            tvSenderName = v.findViewById(R.id.tvSenderName);
            tvMessage    = v.findViewById(R.id.tvMessage);
            tvTime       = v.findViewById(R.id.tvTime);
            ivAttachment = v.findViewById(R.id.ivAttachment);
        }
    }
}
