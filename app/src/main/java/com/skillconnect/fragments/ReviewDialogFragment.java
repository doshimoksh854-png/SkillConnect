package com.skillconnect.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.skillconnect.R;
import com.skillconnect.data.FirebaseRepository;
import com.skillconnect.data.SessionManager;
import com.skillconnect.models.Review;

/**
 * Bottom sheet for submitting a review — now uses FirebaseRepository (Firestore).
 * bookingId and skillId are now Strings (Firestore document IDs).
 */
public class ReviewDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_BOOKING_ID  = "booking_id";
    private static final String ARG_SKILL_ID    = "skill_id";
    private static final String ARG_SKILL_TITLE = "skill_title";

    private OnReviewSubmittedListener listener;

    public interface OnReviewSubmittedListener {
        void onReviewSubmitted();
    }

    /** Create with String document IDs (Firestore) */
    public static ReviewDialogFragment newInstance(String bookingId, String skillId, String skillTitle) {
        ReviewDialogFragment fragment = new ReviewDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BOOKING_ID,  bookingId);
        args.putString(ARG_SKILL_ID,    skillId);
        args.putString(ARG_SKILL_TITLE, skillTitle);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnReviewSubmittedListener(OnReviewSubmittedListener listener) {
        this.listener = listener;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_review, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String bookingId  = getArguments().getString(ARG_BOOKING_ID);
        String skillId    = getArguments().getString(ARG_SKILL_ID);
        String skillTitle = getArguments().getString(ARG_SKILL_TITLE);

        TextView tvSkillName       = view.findViewById(R.id.tvSkillName);
        RatingBar ratingBar        = view.findViewById(R.id.ratingBar);
        TextInputEditText etComment= view.findViewById(R.id.etComment);
        MaterialButton btnSubmit   = view.findViewById(R.id.btnSubmitReview);

        if (tvSkillName != null) tvSkillName.setText(skillTitle);

        btnSubmit.setOnClickListener(v -> {
            float rating  = ratingBar.getRating();
            String comment= etComment.getText() != null ? etComment.getText().toString().trim() : "";

            if (rating == 0) {
                Toast.makeText(getContext(), "Please select a rating", Toast.LENGTH_SHORT).show();
                return;
            }

            SessionManager session = new SessionManager(requireContext());
            Review review = new Review();
            review.setBookingId(bookingId);
            review.setSkillId(skillId);
            review.setUserId(session.getUserId());
            review.setUserName(session.getUserName());
            review.setRating(rating);
            review.setComment(comment);
            review.setCreatedAt(System.currentTimeMillis());

            btnSubmit.setEnabled(false);
            FirebaseRepository.getInstance().addReview(review, docId -> {
                if (!isAdded()) return;
                Toast.makeText(getContext(), R.string.review_submitted, Toast.LENGTH_SHORT).show();
                if (listener != null) listener.onReviewSubmitted();
                dismiss();
            });
        });
    }
}
