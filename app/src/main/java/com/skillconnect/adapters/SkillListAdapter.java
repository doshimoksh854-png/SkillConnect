package com.skillconnect.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.skillconnect.R;
import com.skillconnect.models.Skill;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for skill listing screen
 */
public class SkillListAdapter extends RecyclerView.Adapter<SkillListAdapter.SkillViewHolder> {

    private List<Skill> skills;
    private OnSkillClickListener listener;

    public interface OnSkillClickListener {
        void onSkillClick(Skill skill);
    }

    public SkillListAdapter(List<Skill> skills, OnSkillClickListener listener) {
        this.skills = skills;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SkillViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_skill_card, parent, false);
        return new SkillViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SkillViewHolder holder, int position) {
        Skill skill = skills.get(position);
        holder.bind(skill);
    }

    @Override
    public int getItemCount() {
        return skills != null ? skills.size() : 0;
    }

    public void updateSkills(List<Skill> newSkills) {
        this.skills = newSkills;
        notifyDataSetChanged();
    }

    class SkillViewHolder extends RecyclerView.ViewHolder {
        TextView tvSkillTitle;
        TextView tvProviderName;
        TextView tvRating;
        TextView tvReviewCount;
        TextView tvPrice;
        MaterialButton btnViewDetails;

        public SkillViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSkillTitle = itemView.findViewById(R.id.tvSkillTitle);
            tvProviderName = itemView.findViewById(R.id.tvProviderName);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvReviewCount = itemView.findViewById(R.id.tvReviewCount);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);

            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onSkillClick(skills.get(getAdapterPosition()));
                }
            });

            btnViewDetails.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onSkillClick(skills.get(getAdapterPosition()));
                }
            });
        }

        public void bind(Skill skill) {
            tvSkillTitle.setText(skill.getTitle());
            tvProviderName.setText(skill.getProviderName());
            tvRating.setText(String.format(Locale.getDefault(), "%.1f", skill.getRating()));
            tvPrice.setText(skill.getFormattedPrice());

            if (skill.getReviewCount() > 0) {
                tvReviewCount.setText(String.format(Locale.getDefault(), "(%d)", skill.getReviewCount()));
                tvReviewCount.setVisibility(View.VISIBLE);
            } else {
                tvReviewCount.setVisibility(View.GONE);
            }
        }
    }
}
