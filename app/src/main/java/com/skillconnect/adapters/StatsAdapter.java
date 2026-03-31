package com.skillconnect.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.skillconnect.R;
import com.skillconnect.models.Stat;
import java.util.List;

/**
 * Adapter for provider dashboard stats grid
 */
public class StatsAdapter extends RecyclerView.Adapter<StatsAdapter.StatViewHolder> {

    private List<Stat> stats;

    public StatsAdapter(List<Stat> stats) {
        this.stats = stats;
    }

    @NonNull
    @Override
    public StatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_stats_card, parent, false);
        return new StatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StatViewHolder holder, int position) {
        Stat stat = stats.get(position);
        holder.bind(stat);
    }

    @Override
    public int getItemCount() {
        return stats != null ? stats.size() : 0;
    }

    public void updateStats(List<Stat> newStats) {
        this.stats = newStats;
        notifyDataSetChanged();
    }

    static class StatViewHolder extends RecyclerView.ViewHolder {
        TextView tvStatIcon;
        TextView tvStatValue;
        TextView tvStatLabel;

        public StatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStatIcon = itemView.findViewById(R.id.tvStatIcon);
            tvStatValue = itemView.findViewById(R.id.tvStatValue);
            tvStatLabel = itemView.findViewById(R.id.tvStatLabel);
        }

        public void bind(Stat stat) {
            tvStatValue.setText(stat.getValue());
            tvStatLabel.setText(stat.getLabel());

            // Set icon based on label (simple emoji mapping)
            String icon = "📊";
            if (stat.getLabel().toLowerCase().contains("request") || stat.getLabel().toLowerCase().contains("active")) {
                icon = "📋";
            } else if (stat.getLabel().toLowerCase().contains("completed")
                    || stat.getLabel().toLowerCase().contains("job")) {
                icon = "✅";
            } else if (stat.getLabel().toLowerCase().contains("rating")) {
                icon = "⭐";
            } else if (stat.getLabel().toLowerCase().contains("earning")) {
                icon = "💰";
            }
            tvStatIcon.setText(icon);
        }
    }
}
