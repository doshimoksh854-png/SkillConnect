package com.skillconnect.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.skillconnect.R;
import com.skillconnect.models.Category;
import java.util.List;

/**
 * Adapter for category grid in customer home screen
 * Displays categories with vector icons and colored backgrounds
 */
public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private List<Category> categories;
    private OnCategoryClickListener listener;

    // Background colors for each category position
    private static final int[] CATEGORY_COLORS = {
        R.color.category_software,
        R.color.category_tech,
        R.color.category_design,
        R.color.category_education,
        R.color.category_marketing,
        R.color.category_business
    };

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    public CategoryAdapter(List<Category> categories, OnCategoryClickListener listener) {
        this.categories = categories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_card, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.bind(category, position);
    }

    @Override
    public int getItemCount() {
        return categories != null ? categories.size() : 0;
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCategoryIcon;
        TextView tvCategoryName;
        LinearLayout categoryBackground;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            categoryBackground = itemView.findViewById(R.id.categoryBackground);

            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onCategoryClick(categories.get(getAdapterPosition()));
                }
            });
        }

        public void bind(Category category, int position) {
            tvCategoryName.setText(category.getName());
            
            // Set vector icon
            if (category.getIconResource() != 0) {
                ivCategoryIcon.setImageResource(category.getIconResource());
            }
            
            // Set category-specific background color
            int colorIndex = position % CATEGORY_COLORS.length;
            categoryBackground.setBackgroundResource(CATEGORY_COLORS[colorIndex]);
        }
    }
}
