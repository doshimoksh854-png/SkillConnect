package com.skillconnect.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.skillconnect.R;
import com.skillconnect.models.Skill;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for My Skills list (provider side).
 * Shows each skill with Edit and Delete action buttons.
 */
public class MySkillAdapter extends RecyclerView.Adapter<MySkillAdapter.ViewHolder> {

    public interface OnSkillActionListener {
        void onEdit(Skill skill);
        void onDelete(Skill skill);
    }

    private List<Skill> skills;
    private OnSkillActionListener listener;

    public MySkillAdapter(List<Skill> skills, OnSkillActionListener listener) {
        this.skills   = skills;
        this.listener = listener;
    }

    public void updateSkills(List<Skill> newSkills) {
        this.skills = newSkills;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_skill, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Skill skill = skills.get(position);
        holder.tvTitle.setText(skill.getTitle());
        holder.tvCategory.setText(skill.getCategoryName());
        holder.tvPrice.setText(String.format(Locale.getDefault(), "₹%.0f / hr", skill.getPrice()));
        holder.tvRating.setText(String.format(Locale.getDefault(), "⭐ %.1f (%d reviews)",
                skill.getRating(), skill.getReviewCount()));

        holder.btnEdit.setOnClickListener(v -> listener.onEdit(skill));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(skill));
    }

    @Override
    public int getItemCount() { return skills.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView tvTitle, tvCategory, tvPrice, tvRating;
        MaterialButton btnEdit, btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            card       = itemView.findViewById(R.id.cardMySkill);
            tvTitle    = itemView.findViewById(R.id.tvSkillTitle);
            tvCategory = itemView.findViewById(R.id.tvSkillCategory);
            tvPrice    = itemView.findViewById(R.id.tvSkillPrice);
            tvRating   = itemView.findViewById(R.id.tvSkillRating);
            btnEdit    = itemView.findViewById(R.id.btnEdit);
            btnDelete  = itemView.findViewById(R.id.btnDelete);
        }
    }
}
