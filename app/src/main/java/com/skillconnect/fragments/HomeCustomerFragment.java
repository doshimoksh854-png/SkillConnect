package com.skillconnect.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.skillconnect.ProviderProfileActivity;
import com.skillconnect.R;
import com.skillconnect.SkillListActivity;
import com.skillconnect.adapters.CategoryAdapter;
import com.skillconnect.adapters.FeaturedProviderAdapter;
import com.skillconnect.data.FirebaseRepository;
import com.skillconnect.data.SessionManager;
import com.skillconnect.models.Category;
import com.skillconnect.models.Provider;
import java.util.ArrayList;

public class HomeCustomerFragment extends Fragment {

    private RecyclerView rvCategories, rvFeaturedProviders;
    private FirebaseRepository repo;
    private TextView tvCustomerWalletBalance;
    private SessionManager session;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home_customer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repo                = FirebaseRepository.getInstance();
        rvCategories        = view.findViewById(R.id.rvCategories);
        rvFeaturedProviders = view.findViewById(R.id.rvFeaturedProviders);

        session = new SessionManager(requireContext());
        TextView tvGreeting = view.findViewById(R.id.tvGreeting);
        if (tvGreeting != null) {
            String name = session.getUserName();
            tvGreeting.setText("Hello, " + (name.isEmpty() ? "there" : name) + "! 👋");
        }

        EditText etSearch = view.findViewById(R.id.etSearch);
        if (etSearch != null) {
            etSearch.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    Intent i = new Intent(getContext(), SkillListActivity.class);
                    i.putExtra("search_query", etSearch.getText().toString().trim());
                    startActivity(i);
                    return true;
                }
                return false;
            });
        }

        setupCategories();
        loadFeaturedProviders();

        View cardMyJobs = view.findViewById(R.id.cardMyJobs);
        if (cardMyJobs != null) {
            cardMyJobs.setOnClickListener(v -> 
                startActivity(new Intent(getContext(), com.skillconnect.CustomerJobsActivity.class))
            );
        }

        View cardMyWallet = view.findViewById(R.id.cardMyWallet);
        tvCustomerWalletBalance = view.findViewById(R.id.tvCustomerWalletBalance);
        if (cardMyWallet != null) {
            cardMyWallet.setOnClickListener(v -> 
                startActivity(new Intent(getContext(), com.skillconnect.WalletActivity.class))
            );
        }
        
        loadWalletBalance();
    }

    @Override public void onResume() { 
        super.onResume(); 
        loadFeaturedProviders(); 
        loadWalletBalance();
    }

    private void loadWalletBalance() {
        if (!isAdded() || session == null) return;
        String uid = session.getUserId();
        if (uid == null || uid.isEmpty()) return;
        repo.getOrCreateWallet(uid, new FirebaseRepository.Callback<com.skillconnect.models.Wallet>() {
            @Override public void onSuccess(com.skillconnect.models.Wallet w) {
                if (!isAdded() || tvCustomerWalletBalance == null) return;
                tvCustomerWalletBalance.setText(String.format(java.util.Locale.getDefault(), "Balance: ₹%.0f", w.getBalance()));
            }
            @Override public void onError(String e) {}
        });
    }

    private void setupCategories() {
        ArrayList<Category> cats = new ArrayList<>();
        cats.add(new Category(1, getString(R.string.category_software_dev), R.drawable.ic_category_software));
        cats.add(new Category(2, getString(R.string.category_tech_support), R.drawable.ic_category_tech));
        cats.add(new Category(3, getString(R.string.category_design),       R.drawable.ic_category_design));
        cats.add(new Category(4, getString(R.string.category_education),    R.drawable.ic_category_education));
        cats.add(new Category(5, getString(R.string.category_marketing),    R.drawable.ic_category_marketing));
        cats.add(new Category(6, getString(R.string.category_business),     R.drawable.ic_category_business));
        CategoryAdapter adapter = new CategoryAdapter(cats, cat -> {
            Intent i = new Intent(getContext(), SkillListActivity.class);
            i.putExtra("category_name", cat.getName());
            startActivity(i);
        });
        rvCategories.setLayoutManager(new GridLayoutManager(getContext(), 2));
        rvCategories.setAdapter(adapter);
    }

    private void loadFeaturedProviders() {
        if (!isAdded()) return;
        repo.getFeaturedProviders(new FirebaseRepository.Callback<java.util.List<Provider>>() {
            @Override public void onSuccess(java.util.List<Provider> providers) {
                if (!isAdded()) return;
                FeaturedProviderAdapter adapter = new FeaturedProviderAdapter(providers, provider -> {
                    Intent i = new Intent(getContext(), ProviderProfileActivity.class);
                    i.putExtra("provider_id",        provider.getStringId());
                    i.putExtra("provider_name",      provider.getName());
                    i.putExtra("provider_specialty", provider.getSpecialty());
                    i.putExtra("provider_rating",    provider.getRating());
                    startActivity(i);
                });
                rvFeaturedProviders.setLayoutManager(
                        new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
                rvFeaturedProviders.setAdapter(adapter);
            }
            @Override public void onError(String error) { /* show empty state */ }
        });
    }
}
