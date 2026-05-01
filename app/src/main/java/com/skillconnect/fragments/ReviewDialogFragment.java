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
    private static final String ARG_PROVIDER_ID = "provider_id";
    private static final String ARG_PROVIDER_NAME = "provider_name";
    private static final String ARG_AMOUNT      = "amount";
    private static final String ARG_REQUIRE_PAY = "require_pay";

    private OnReviewSubmittedListener listener;

    public interface OnReviewSubmittedListener {
        void onReviewSubmitted();
    }

    public static ReviewDialogFragment newInstance(String bookingId, String skillId, String skillTitle,
                                                   String providerId, String providerName, double amount,
                                                   boolean requirePayment) {
        ReviewDialogFragment fragment = new ReviewDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BOOKING_ID,  bookingId);
        args.putString(ARG_SKILL_ID,    skillId);
        args.putString(ARG_SKILL_TITLE, skillTitle);
        args.putString(ARG_PROVIDER_ID, providerId);
        args.putString(ARG_PROVIDER_NAME, providerName);
        args.putDouble(ARG_AMOUNT,      amount);
        args.putBoolean(ARG_REQUIRE_PAY, requirePayment);
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

        String providerId = getArguments().getString(ARG_PROVIDER_ID);
        String providerName = getArguments().getString(ARG_PROVIDER_NAME);
        double amount = getArguments().getDouble(ARG_AMOUNT);
        boolean requirePay = getArguments().getBoolean(ARG_REQUIRE_PAY);

        if (requirePay) {
            btnSubmit.setText("Proceed to Payment");
        }

        btnSubmit.setOnClickListener(v -> {
            float rating  = ratingBar.getRating();
            String comment= etComment.getText() != null ? etComment.getText().toString().trim() : "";

            if (rating == 0) {
                Toast.makeText(v.getContext(), "Please select a rating", Toast.LENGTH_SHORT).show();
                return;
            }

            SessionManager session = new SessionManager(v.getContext());
            String customerId = session.getUserId();

            if (requirePay) {
                btnSubmit.setEnabled(false);
                btnSubmit.setText("Opening Payment...");
                try {
                    // Pre-payment flow: Launch PaymentActivity with review data + final_payment type
                    android.content.Intent intent = new android.content.Intent(v.getContext(), com.skillconnect.PaymentActivity.class);
                    intent.putExtra("booking_id",    bookingId);
                    intent.putExtra("skill_id",      skillId);
                    intent.putExtra("provider_id",   providerId);
                    intent.putExtra("provider_name", providerName != null ? providerName : "Provider");
                    intent.putExtra("job_title",     skillTitle);
                    intent.putExtra("amount",        amount);         // 90% remaining
                    intent.putExtra("full_amount",   amount / 0.90);  // back-calculate full price
                    intent.putExtra("payment_type",  "final_payment");
                    intent.putExtra("review_rating", rating);
                    intent.putExtra("review_comment",comment);
                    v.getContext().startActivity(intent);
                    dismiss();
                } catch (Exception e) {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Proceed to Payment");
                    Toast.makeText(v.getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                // Post-payment / Legacy flow: directly submit review
                submitReview(bookingId, skillId, customerId, session.getUserName(), rating, comment, btnSubmit, v.getContext());
            }
        });
    }

    private void submitReview(String bookingId, String skillId, String userId, String userName,
                            float rating, String comment, MaterialButton btnSubmit, android.content.Context ctx) {
        Review review = new Review();
        review.setBookingId(bookingId);
        review.setSkillId(skillId);
        review.setUserId(userId);
        review.setUserName(userName);
        review.setRating(rating);
        review.setComment(comment);
        review.setCreatedAt(System.currentTimeMillis());

        btnSubmit.setEnabled(false);
        FirebaseRepository.getInstance().addReview(review, docId -> {
            if (!isAdded()) return;
            // Update booking status to completed after review
            FirebaseRepository.getInstance().updateBookingStatus(bookingId, "completed", statusUpdated -> {
                Toast.makeText(ctx, "Review submitted and escrow funds released! ✅", Toast.LENGTH_LONG).show();
                if (listener != null) listener.onReviewSubmitted();
                dismiss();
            });
        });
    }
}
