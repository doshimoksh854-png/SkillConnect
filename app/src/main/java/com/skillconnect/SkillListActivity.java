package com.skillconnect;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.skillconnect.adapters.SkillListAdapter;
import com.skillconnect.data.FirebaseRepository;
import com.skillconnect.fragments.SkillFilterBottomSheet;
import com.skillconnect.models.Skill;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SkillListActivity extends AppCompatActivity
        implements SkillFilterBottomSheet.OnFilterListener {

    private RecyclerView    rvSkills;
    private SkillListAdapter adapter;
    private FirebaseRepository repo;

    private String currentCategory, currentSortBy, currentSearch;
    private float  filterMinPrice = 0, filterMaxPrice = 5000, filterMinRating = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skill_list);

        repo = FirebaseRepository.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        rvSkills = findViewById(R.id.rvSkills);
        rvSkills.setLayoutManager(new LinearLayoutManager(this));

        Intent intent = getIntent();
        if (intent != null) {
            currentCategory = intent.getStringExtra("category_name");
            currentSearch   = intent.getStringExtra("search_query");
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(currentCategory != null ? currentCategory : "All Skills");
        }

        loadSkills();
    }

    private void loadSkills() {
        FirebaseRepository.Callback<List<Skill>> cb = new FirebaseRepository.Callback<List<Skill>>() {
            @Override public void onSuccess(List<Skill> skills) {
                if (skills == null) skills = new ArrayList<>();
                setAdapter(applyClientFilters(skills));
            }
            @Override public void onError(String e) {
                Toast.makeText(SkillListActivity.this, "Error loading skills", Toast.LENGTH_SHORT).show();
                setAdapter(new ArrayList<>());
            }
        };

        if (currentSearch != null && !currentSearch.isEmpty()) {
            repo.searchSkills(currentSearch, cb);
        } else if (currentCategory != null && !currentCategory.isEmpty()) {
            repo.getSkillsByCategory(currentCategory, currentSortBy, cb);
        } else {
            repo.getAllSkills(currentSortBy, cb);
        }
    }

    /** Apply price range and minimum rating filters client-side */
    private List<Skill> applyClientFilters(List<Skill> skills) {
        float maxP = (filterMaxPrice >= 5000) ? Float.MAX_VALUE : filterMaxPrice;
        return skills.stream()
                .filter(s -> s.getPrice() >= filterMinPrice && s.getPrice() <= maxP)
                .filter(s -> s.getRating() >= filterMinRating)
                .collect(Collectors.toList());
    }

    private void setAdapter(List<Skill> skills) {
        if (adapter == null) {
            adapter = new SkillListAdapter(skills, this::openSkillDetail);
            rvSkills.setAdapter(adapter);
        } else {
            adapter.updateSkills(skills);
        }
    }

    private void openSkillDetail(Skill skill) {
        Intent i = new Intent(this, SkillDetailActivity.class);
        i.putExtra("skill_doc_id",       skill.getDocumentId());
        i.putExtra("skill_provider_id",  skill.getProviderId());
        i.putExtra("skill_title",        skill.getTitle());
        i.putExtra("skill_description",  skill.getDescription());
        i.putExtra("skill_price",        skill.getPrice());
        i.putExtra("skill_rating",       skill.getRating());
        i.putExtra("skill_provider",     skill.getProviderName());
        i.putExtra("skill_review_count", skill.getReviewCount());
        startActivity(i);
    }

    @Override
    public void onFilterApplied(String sortBy, float minPrice, float maxPrice, float minRating) {
        currentSortBy    = sortBy;
        filterMinPrice   = minPrice;
        filterMaxPrice   = maxPrice;
        filterMinRating  = minRating;
        loadSkills();
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_skill_list, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        if (item.getItemId() == R.id.action_filter) {
            SkillFilterBottomSheet sheet = SkillFilterBottomSheet.newInstance(
                    currentSortBy, filterMinPrice, filterMaxPrice, filterMinRating);
            sheet.setOnFilterListener(this);
            sheet.show(getSupportFragmentManager(), "filter");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
