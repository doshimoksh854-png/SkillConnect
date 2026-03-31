package com.skillconnect;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.skillconnect.adapters.SkillListAdapter;
import com.skillconnect.data.FirebaseRepository;
import com.skillconnect.models.Skill;
import java.util.ArrayList;
import java.util.List;

public class ProviderProfileActivity extends AppCompatActivity {

    private FirebaseRepository repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_profile);

        repo = FirebaseRepository.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String providerId   = getIntent().getStringExtra("provider_id");
        String providerName = getIntent().getStringExtra("provider_name");
        String specialty    = getIntent().getStringExtra("provider_specialty");
        float  rating       = getIntent().getFloatExtra("provider_rating", 0f);

        if (getSupportActionBar() != null) getSupportActionBar().setTitle(providerName);

        TextView tvName      = findViewById(R.id.tvProviderName);
        TextView tvSpecialty = findViewById(R.id.tvSpecialty);
        TextView tvRating    = findViewById(R.id.tvRating);

        if (tvName      != null) tvName.setText(providerName);
        if (tvSpecialty != null) tvSpecialty.setText(specialty != null ? specialty : "Professional");
        if (tvRating    != null) tvRating.setText(String.format("%.1f ★", rating));

        RecyclerView rvSkills = findViewById(R.id.rvProviderSkills);
        if (rvSkills != null) {
            rvSkills.setLayoutManager(new LinearLayoutManager(this));
            if (providerId != null) {
                repo.getSkillsByProviderId(providerId,
                        new FirebaseRepository.Callback<List<Skill>>() {
                            @Override public void onSuccess(List<Skill> skills) {
                                SkillListAdapter adp = new SkillListAdapter(
                                        skills != null ? skills : new ArrayList<>(),
                                        ProviderProfileActivity.this::openSkillDetail);
                                rvSkills.setAdapter(adp);
                            }
                            @Override public void onError(String e) {}
                        });
            }
        }
    }

    private void openSkillDetail(Skill skill) {
        Intent i = new Intent(this, SkillDetailActivity.class);
        i.putExtra("skill_doc_id",      skill.getDocumentId());
        i.putExtra("skill_provider_id", skill.getProviderId());
        i.putExtra("skill_title",       skill.getTitle());
        i.putExtra("skill_description", skill.getDescription());
        i.putExtra("skill_price",       skill.getPrice());
        i.putExtra("skill_rating",      skill.getRating());
        i.putExtra("skill_provider",    skill.getProviderName());
        i.putExtra("skill_review_count",skill.getReviewCount());
        startActivity(i);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
