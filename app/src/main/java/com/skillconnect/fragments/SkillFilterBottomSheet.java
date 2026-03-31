package com.skillconnect.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.skillconnect.R;
import java.util.Locale;

public class SkillFilterBottomSheet extends BottomSheetDialogFragment {

    public interface OnFilterListener {
        void onFilterApplied(String sortBy, float minPrice, float maxPrice, float minRating);
    }

    private OnFilterListener listener;
    private String currentSort;
    private float  savedMaxPrice = 5000, savedMinRating = 0;

    public static SkillFilterBottomSheet newInstance(String currentSort,
            float minPrice, float maxPrice, float minRating) {
        SkillFilterBottomSheet sheet = new SkillFilterBottomSheet();
        sheet.currentSort   = currentSort;
        sheet.savedMaxPrice = maxPrice;
        sheet.savedMinRating= minRating;
        return sheet;
    }

    public static SkillFilterBottomSheet newInstance(String currentSort) {
        return newInstance(currentSort, 0, 5000, 0);
    }

    public void setOnFilterListener(OnFilterListener listener) {
        this.listener = listener;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RadioGroup     radioSort    = view.findViewById(R.id.radioSort);
        SeekBar        seekPrice    = view.findViewById(R.id.seekBarPrice);
        SeekBar        seekRating   = view.findViewById(R.id.seekBarRating);
        TextView       tvPriceRange = view.findViewById(R.id.tvPriceRange);
        TextView       tvMinRating  = view.findViewById(R.id.tvMinRating);
        MaterialButton btnApply     = view.findViewById(R.id.btnApplyFilter);
        MaterialButton btnReset     = view.findViewById(R.id.btnResetFilter);

        // Pre-select sort
        if ("price_asc".equals(currentSort))       radioSort.check(R.id.rbPriceAsc);
        else if ("price_desc".equals(currentSort)) radioSort.check(R.id.rbPriceDesc);
        else                                        radioSort.check(R.id.rbRating);

        // Restore saved values: seekPrice max=50 → multiply by 100 = ₹0–₹5000
        int priceProgress = (savedMaxPrice >= 5000) ? 50 : (int)(savedMaxPrice / 100);
        seekPrice.setProgress(priceProgress);
        seekRating.setProgress((int)(savedMinRating * 2)); // 0-10 → 0-5 stars

        updatePriceLabel(tvPriceRange, priceProgress);
        updateRatingLabel(tvMinRating, seekRating.getProgress());

        seekPrice.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) { updatePriceLabel(tvPriceRange, p); }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });

        seekRating.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) { updateRatingLabel(tvMinRating, p); }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });

        btnApply.setOnClickListener(v -> {
            int checkedId = radioSort.getCheckedRadioButtonId();
            String sort  = checkedId == R.id.rbPriceAsc  ? "price_asc"
                         : checkedId == R.id.rbPriceDesc ? "price_desc"
                         : "rating_desc";
            float maxP = seekPrice.getProgress() >= 50 ? 5000f : seekPrice.getProgress() * 100f;
            float minR = seekRating.getProgress() / 2f;
            if (listener != null) listener.onFilterApplied(sort, 0, maxP, minR);
            dismiss();
        });

        btnReset.setOnClickListener(v -> {
            radioSort.check(R.id.rbRating);
            seekPrice.setProgress(50);
            seekRating.setProgress(0);
            updatePriceLabel(tvPriceRange, 50);
            updateRatingLabel(tvMinRating, 0);
        });
    }

    private void updatePriceLabel(TextView tv, int progress) {
        if (tv == null) return;
        if (progress >= 50) { tv.setText("Up to ₹5000+"); return; }
        tv.setText(String.format(Locale.getDefault(), "Up to ₹%d", progress * 100));
    }

    private void updateRatingLabel(TextView tv, int progress) {
        if (tv == null) return;
        float stars = progress / 2f;
        tv.setText(stars == 0 ? "Any rating"
                : String.format(Locale.getDefault(), "%.1f ⭐ and above", stars));
    }
}
