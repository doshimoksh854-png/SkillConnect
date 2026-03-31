package com.skillconnect.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.imageview.ShapeableImageView;
import com.skillconnect.R;
import com.skillconnect.models.Provider;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for featured providers horizontal list
 */
public class FeaturedProviderAdapter extends RecyclerView.Adapter<FeaturedProviderAdapter.ProviderViewHolder> {

    private List<Provider> providers;
    private OnProviderClickListener listener;

    public interface OnProviderClickListener {
        void onProviderClick(Provider provider);
    }

    public FeaturedProviderAdapter(List<Provider> providers, OnProviderClickListener listener) {
        this.providers = providers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProviderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_provider_card, parent, false);
        return new ProviderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProviderViewHolder holder, int position) {
        Provider provider = providers.get(position);
        holder.bind(provider);
    }

    @Override
    public int getItemCount() {
        return providers != null ? providers.size() : 0;
    }

    class ProviderViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivProviderImage;
        TextView tvProviderName;
        TextView tvProviderSpecialty;
        TextView tvProviderRating;

        public ProviderViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProviderImage = itemView.findViewById(R.id.ivProviderImage);
            tvProviderName = itemView.findViewById(R.id.tvProviderName);
            tvProviderSpecialty = itemView.findViewById(R.id.tvProviderSpecialty);
            tvProviderRating = itemView.findViewById(R.id.tvProviderRating);

            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onProviderClick(providers.get(getAdapterPosition()));
                }
            });
        }

        public void bind(Provider provider) {
            tvProviderName.setText(provider.getName());
            tvProviderSpecialty.setText(provider.getSpecialty());
            tvProviderRating.setText(String.format(Locale.getDefault(), "%.1f", provider.getRating()));

            ivProviderImage.setImageResource(R.drawable.ic_profile_placeholder);
        }
    }
}
