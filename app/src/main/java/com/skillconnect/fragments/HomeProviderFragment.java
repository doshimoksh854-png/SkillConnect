package com.skillconnect.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.skillconnect.AddSkillActivity;
import com.skillconnect.R;
import com.skillconnect.adapters.BookingAdapter;
import com.skillconnect.adapters.StatsAdapter;
import com.skillconnect.data.FirebaseRepository;
import com.skillconnect.data.SessionManager;
import com.skillconnect.models.Booking;
import com.skillconnect.models.Stat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeProviderFragment extends Fragment implements BookingAdapter.OnBookingActionListener {

    private RecyclerView rvStats, rvRecentBookings;
    private ExtendedFloatingActionButton fabAddSkill;
    private TextView tvEarnings, tvEmptyBookings, tvProviderWalletBalance;
    private FirebaseRepository repo;
    private SessionManager sessionManager;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home_provider, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo           = FirebaseRepository.getInstance();
        sessionManager = new SessionManager(requireContext());

        rvStats          = view.findViewById(R.id.rvStats);
        fabAddSkill      = view.findViewById(R.id.fabAddSkill);
        tvEarnings       = view.findViewById(R.id.tvEarnings);
        rvRecentBookings = view.findViewById(R.id.rvRecentBookings);
        tvEmptyBookings  = view.findViewById(R.id.tvEmptyBookings);

        if (fabAddSkill != null) {
            fabAddSkill.setOnClickListener(v ->
                    startActivity(new Intent(getContext(), AddSkillActivity.class)));
        }

        View cardFindJobs = view.findViewById(R.id.cardFindJobs);
        if (cardFindJobs != null) {
            cardFindJobs.setOnClickListener(v -> 
                startActivity(new Intent(getContext(), com.skillconnect.ProviderJobsActivity.class)));
        }

        View cardMyWallet = view.findViewById(R.id.cardMyWallet);
        tvProviderWalletBalance = view.findViewById(R.id.tvProviderWalletBalance);
        if (cardMyWallet != null) {
            cardMyWallet.setOnClickListener(v -> 
                startActivity(new Intent(getContext(), com.skillconnect.WalletActivity.class)));
        }

        loadDashboardData();
    }

    @Override public void onResume() { super.onResume(); loadDashboardData(); }

    private void loadDashboardData() {
        if (!isAdded()) return;
        String pid = sessionManager.getUserId();

        repo.getProviderStats(pid, new FirebaseRepository.Callback<int[]>() {
            @Override public void onSuccess(int[] s) {
                if (!isAdded()) return;
                List<Stat> list = new ArrayList<>();
                list.add(new Stat(getString(R.string.active_requests),  String.valueOf(s[0]), 0));
                list.add(new Stat(getString(R.string.completed_jobs),   String.valueOf(s[1]), 0));
                StatsAdapter sa = new StatsAdapter(list);
                int span = getResources().getConfiguration().screenWidthDp >= 600 ? 3 : 2;
                if (rvStats != null) {
                    rvStats.setLayoutManager(new GridLayoutManager(getContext(), span));
                    rvStats.setAdapter(sa);
                }
            }
            @Override public void onError(String e) {}
        });

        repo.getProviderEarnings(pid, new FirebaseRepository.Callback<Double>() {
            @Override public void onSuccess(Double earnings) {
                if (!isAdded() || tvEarnings == null) return;
                tvEarnings.setText(String.format(Locale.getDefault(), "₹%.0f", earnings));
            }
            @Override public void onError(String e) {}
        });

        repo.getOrCreateWallet(pid, new FirebaseRepository.Callback<com.skillconnect.models.Wallet>() {
            @Override public void onSuccess(com.skillconnect.models.Wallet w) {
                if (!isAdded() || tvProviderWalletBalance == null) return;
                tvProviderWalletBalance.setText(String.format(Locale.getDefault(), "Balance: ₹%.0f", w.getBalance()));
            }
            @Override public void onError(String e) {}
        });

        repo.getProviderBookings(pid, new FirebaseRepository.Callback<List<Booking>>() {
            @Override public void onSuccess(List<Booking> all) {
                if (!isAdded() || rvRecentBookings == null) return;
                List<Booking> recent = all.size() > 5 ? all.subList(0, 5) : all;
                if (recent.isEmpty()) {
                    rvRecentBookings.setVisibility(View.GONE);
                    if (tvEmptyBookings != null) tvEmptyBookings.setVisibility(View.VISIBLE);
                } else {
                    rvRecentBookings.setVisibility(View.VISIBLE);
                    if (tvEmptyBookings != null) tvEmptyBookings.setVisibility(View.GONE);
                    BookingAdapter ba = new BookingAdapter(new ArrayList<>(recent), "provider", HomeProviderFragment.this);
                    rvRecentBookings.setLayoutManager(new LinearLayoutManager(getContext()));
                    rvRecentBookings.setNestedScrollingEnabled(false);
                    rvRecentBookings.setAdapter(ba);
                }
            }
            @Override public void onError(String e) {}
        });
    }

    private void updateStatus(Booking b, String status) {
        repo.updateBookingStatus(b.getDocumentId(), status, result -> loadDashboardData());
    }

    @Override public void onAccept(Booking b)      { updateStatus(b, "accepted"); }
    @Override public void onReject(Booking b)      { updateStatus(b, "rejected"); }
    @Override public void onComplete(Booking b)    { updateStatus(b, "completed"); }
    @Override public void onCancel(Booking b)      { /* providers don't cancel */ }
    @Override public void onLeaveReview(Booking b) { /* providers don't review */ }
    @Override public void onChat(Booking b) {
        Intent ci = new Intent(getContext(), com.skillconnect.ChatActivity.class);
        ci.putExtra("chat_partner_id", b.getUserId());
        ci.putExtra("chat_partner_name", "Customer"); // Booking model doesn't store user name
        startActivity(ci);
    }
}
