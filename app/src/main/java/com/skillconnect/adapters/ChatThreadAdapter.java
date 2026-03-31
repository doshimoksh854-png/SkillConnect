package com.skillconnect.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.skillconnect.R;
import com.skillconnect.models.ChatThread;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatThreadAdapter extends RecyclerView.Adapter<ChatThreadAdapter.ViewHolder> {

    public interface OnChatClickListener {
        void onChatClick(ChatThread thread);
    }

    private final List<ChatThread> threads;
    private final OnChatClickListener listener;

    public ChatThreadAdapter(List<ChatThread> threads, OnChatClickListener listener) {
        this.threads = threads;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_thread, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatThread thread = threads.get(position);
        holder.tvName.setText(thread.getPartnerName());
        holder.tvLastMsg.setText(thread.getLastMessage());
        
        String time = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
                .format(new Date(thread.getLastTimestamp()));
        holder.tvTime.setText(time);

        holder.itemView.setOnClickListener(v -> listener.onChatClick(thread));
    }

    @Override
    public int getItemCount() {
        return threads.size();
    }

    public void updateThreads(List<ChatThread> newThreads) {
        this.threads.clear();
        this.threads.addAll(newThreads);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvLastMsg, tvTime;
        ViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvPartnerName);
            tvLastMsg = v.findViewById(R.id.tvLastMessage);
            tvTime = v.findViewById(R.id.tvTime);
        }
    }
}
