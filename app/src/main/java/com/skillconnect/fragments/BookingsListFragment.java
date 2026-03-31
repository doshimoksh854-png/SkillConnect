package com.skillconnect.fragments;

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
        repo.updateBookingStatus(b.getDocumentId(), "completed", r -> {
            if (isAdded()) Toast.makeText(getContext(), "Completed!", Toast.LENGTH_SHORT).show();
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
                ReviewDialogFragment d = ReviewDialogFragment.newInstance(
                        b.getDocumentId(), b.getSkillId(), b.getSkillTitle());
                d.setOnReviewSubmittedListener(BookingsListFragment.this::loadBookings);
                d.show(getParentFragmentManager(), "review");
            }
            @Override public void onError(String e) {}
        });
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
