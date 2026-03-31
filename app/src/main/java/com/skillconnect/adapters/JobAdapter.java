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
import com.skillconnect.models.JobPost;
import java.util.List;
import java.util.Locale;

public class JobAdapter extends RecyclerView.Adapter<JobAdapter.JobViewHolder> {

    private List<JobPost> jobs;
    private final OnJobClickListener listener;

    public interface OnJobClickListener {
        void onJobClick(JobPost job);
    }

    public JobAdapter(List<JobPost> jobs, OnJobClickListener listener) {
        this.jobs = jobs;
        this.listener = listener;
    }

    public void updateJobs(List<JobPost> newJobs) {
        this.jobs = newJobs;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public JobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_job_post, parent, false);
        return new JobViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull JobViewHolder holder, int position) {
        JobPost job = jobs.get(position);

        holder.tvTitle.setText(job.getTitle());
        holder.tvBudget.setText(String.format(Locale.getDefault(), "₹%.0f", job.getBudget()));
        holder.tvCategory.setText(job.getCategory());
        holder.tvDescription.setText(job.getDescription());
        holder.tvCustomerName.setText(job.getCustomerName());

        // Status badge color
        String status = job.getStatus() != null ? job.getStatus().toUpperCase() : "OPEN";
        holder.tvStatus.setText(status);
        switch (status) {
            case "OPEN":
                holder.tvStatus.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#2ECC71")));
                break;
            case "AWARDED":
                holder.tvStatus.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#1A73E8")));
                break;
            default:
                holder.tvStatus.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#78909C")));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onJobClick(job);
        });
        if (holder.btnViewBid != null) {
            holder.btnViewBid.setOnClickListener(v -> {
                if (listener != null) listener.onJobClick(job);
            });
        }
    }

    @Override
    public int getItemCount() {
        return jobs != null ? jobs.size() : 0;
    }

    static class JobViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvBudget, tvCategory, tvDescription, tvCustomerName, tvStatus;
        MaterialButton btnViewBid;

        JobViewHolder(@NonNull View v) {
            super(v);
            tvTitle        = v.findViewById(R.id.tvJobTitle);
            tvBudget       = v.findViewById(R.id.tvBudget);
            tvCategory     = v.findViewById(R.id.tvCategory);
            tvDescription  = v.findViewById(R.id.tvDescription);
            tvCustomerName = v.findViewById(R.id.tvCustomerName);
            tvStatus       = v.findViewById(R.id.tvStatus);
            btnViewBid     = v.findViewById(R.id.tvBidNow);
        }
    }
}
