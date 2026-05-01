package com.skillconnect.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.skillconnect.R;
import com.skillconnect.adapters.BookingAdapter;
import com.skillconnect.data.FirebaseRepository;
import com.skillconnect.data.SessionManager;
import com.skillconnect.models.Booking;
import java.util.ArrayList;
import java.util.List;

public class BookingsListFragment extends Fragment implements BookingAdapter.OnBookingActionListener {

    private RecyclerView rvBookings;
    private LinearLayout layoutEmpty;
    private BookingAdapter adapter;
    private FirebaseRepository repo;
    private SessionManager sessionManager;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bookings_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo           = FirebaseRepository.getInstance();
        sessionManager = new SessionManager(requireContext());
        rvBookings  = view.findViewById(R.id.rvBookings);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        rvBookings.setLayoutManager(new LinearLayoutManager(getContext()));
        loadBookings();
    }

    @Override public void onResume() { super.onResume(); loadBookings(); }

    private void loadBookings() {
        if (!isAdded()) return;
        String role = sessionManager.getUserRole();
        String uid  = sessionManager.getUserId();

        FirebaseRepository.Callback<List<Booking>> cb = new FirebaseRepository.Callback<List<Booking>>() {
            @Override public void onSuccess(List<Booking> bookings) {
                if (!isAdded()) return;
                if (bookings == null) bookings = new ArrayList<>();
                if (bookings.isEmpty()) {
                    rvBookings.setVisibility(View.GONE);
                    layoutEmpty.setVisibility(View.VISIBLE);
                } else {
                    rvBookings.setVisibility(View.VISIBLE);
                    layoutEmpty.setVisibility(View.GONE);
                }
                if (adapter == null) {
                    adapter = new BookingAdapter(bookings, role, BookingsListFragment.this);
                    rvBookings.setAdapter(adapter);
                } else {
                    adapter.updateBookings(bookings);
                }
            }
            @Override public void onError(String e) {}
        };

        if ("provider".equals(role)) repo.getProviderBookings(uid, cb);
        else repo.getUserBookings(uid, cb);
    }

    @Override public void onAccept(Booking b) {
        repo.updateBookingStatus(b.getDocumentId(), "accepted", r -> {
            if (isAdded()) Toast.makeText(getContext(), "Accepted", Toast.LENGTH_SHORT).show();
            loadBookings();
        });
    }
    @Override public void onReject(Booking b) {
        repo.updateBookingStatus(b.getDocumentId(), "rejected", r -> {
            if (isAdded()) Toast.makeText(getContext(), "Rejected", Toast.LENGTH_SHORT).show();
            loadBookings();
        });
    }
    @Override public void onComplete(Booking b) {
        repo.updateBookingStatus(b.getDocumentId(), "ready_for_review", r -> {
            if (isAdded()) {
                Toast.makeText(getContext(), "Marked as done! Customer will review your work.", Toast.LENGTH_LONG).show();
                // Notify customer
                repo.createNotification(new com.skillconnect.models.Notification(
                    b.getUserId(), "customer", "work_ready",
                    "Work Ready for Review 🎯",
                    "Provider has completed the work for '" + b.getSkillTitle() + "'. Please review and approve payment.",
                    b.getDocumentId()), null);
            }
            loadBookings();
        });
    }
    @Override public void onCancel(Booking b) {
        repo.updateBookingStatus(b.getDocumentId(), "cancelled", r -> {
            if (isAdded()) Toast.makeText(getContext(), "Cancelled", Toast.LENGTH_SHORT).show();
            loadBookings();
        });
    }
    @Override public void onLeaveReview(Booking b) {
        repo.hasReviewForBooking(b.getDocumentId(), new FirebaseRepository.Callback<Boolean>() {
            @Override public void onSuccess(Boolean exists) {
                if (!isAdded()) return;
                if (exists) {
                    Toast.makeText(getContext(), "Already reviewed", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Payment already done — show plain review dialog (no payment required)
                ReviewDialogFragment d = ReviewDialogFragment.newInstance(
                        b.getDocumentId(), b.getSkillId(), b.getSkillTitle(),
                        b.getProviderId(),
                        b.getProviderName() != null ? b.getProviderName() : "Provider",
                        b.getPrice(),
                        false /* requirePayment */);
                d.setOnReviewSubmittedListener(BookingsListFragment.this::loadBookings);
                d.show(getParentFragmentManager(), "review");
            }
            @Override public void onError(String e) {}
        });
    }

    @Override public void onReviewWork(Booking b) {
        // 'Review Work' button is now merged into the 'Pay Now' flow — this is a no-op fallback
        onPayNow(b);
    }

    @Override
    public void onPayNow(Booking b) {
        String status = b.getStatus();

        if ("awarded".equals(status)) {
            // \u2500 Stage 1: Pay 10% booking deposit (bid just accepted, work not yet started) \u2500
            double bookingFee = Math.ceil(b.getPrice() * 0.10);
            android.content.Intent intent = new android.content.Intent(
                    getContext(), com.skillconnect.PaymentActivity.class);
            intent.putExtra("booking_id",    b.getDocumentId());
            intent.putExtra("skill_id",      b.getSkillId() != null ? b.getSkillId() : "");
            intent.putExtra("provider_id",   b.getProviderId());
            intent.putExtra("provider_name", b.getProviderName() != null ? b.getProviderName() : "Provider");
            intent.putExtra("job_title",     b.getSkillTitle());
            intent.putExtra("amount",        bookingFee);
            intent.putExtra("full_amount",   b.getPrice());
            intent.putExtra("payment_type",  "booking_fee");
            startActivity(intent);

        } else {
            // \u2500 Stage 2: Provider done \u2014 review required first, then pay remaining 90% \u2500
            repo.hasReviewForBooking(b.getDocumentId(), new FirebaseRepository.Callback<Boolean>() {
                @Override public void onSuccess(Boolean exists) {
                    if (!isAdded()) return;
                    double remaining = Math.ceil(b.getPrice() * 0.90);
                    if (exists) {
                        // Already reviewed \u2014 go straight to 90% payment
                        android.content.Intent intent = new android.content.Intent(
                                getContext(), com.skillconnect.PaymentActivity.class);
                        intent.putExtra("booking_id",    b.getDocumentId());
                        intent.putExtra("skill_id",      b.getSkillId() != null ? b.getSkillId() : "");
                        intent.putExtra("provider_id",   b.getProviderId());
                        intent.putExtra("provider_name", b.getProviderName() != null ? b.getProviderName() : "Provider");
                        intent.putExtra("job_title",     b.getSkillTitle());
                        intent.putExtra("amount",        remaining);
                        intent.putExtra("full_amount",   b.getPrice());
                        intent.putExtra("payment_type",  "final_payment");
                        startActivity(intent);
                    } else {
                        // Must review first \u2014 dialog will forward to PaymentActivity with review data
                        ReviewDialogFragment d = ReviewDialogFragment.newInstance(
                                b.getDocumentId(), b.getSkillId(), b.getSkillTitle(),
                                b.getProviderId(),
                                b.getProviderName() != null ? b.getProviderName() : "Provider",
                                remaining,
                                true /* requirePayment */);
                        d.setOnReviewSubmittedListener(BookingsListFragment.this::loadBookings);
                        d.show(getParentFragmentManager(), "review_then_pay");
                    }
                }
                @Override public void onError(String e) {
                    Toast.makeText(getContext(), "Could not load booking info. Try again.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override public void onChat(Booking b) {
        String role = sessionManager.getUserRole();
        // If current user is provider, chat partner is customer (b.getUserId())
        // If current user is customer, chat partner is provider (b.getProviderId())
        String partnerId;
        String partnerName;
        
        if ("provider".equals(role)) {
            partnerId = b.getUserId();
            partnerName = "Customer"; // Booking model doesn't store user name
        } else {
            partnerId = b.getProviderId();
            partnerName = b.getProviderName() != null ? b.getProviderName() : "Provider";
        }
        
        android.content.Intent ci = new android.content.Intent(getContext(), com.skillconnect.ChatActivity.class);
        ci.putExtra("chat_partner_id", partnerId);
        ci.putExtra("chat_partner_name", partnerName);
        startActivity(ci);
    }
}
