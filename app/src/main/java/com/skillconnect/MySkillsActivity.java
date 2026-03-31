package com.skillconnect;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.skillconnect.adapters.MySkillAdapter;
import com.skillconnect.data.FirebaseRepository;
import com.skillconnect.data.SessionManager;
import com.skillconnect.models.Skill;
import java.util.ArrayList;
import java.util.List;

public class MySkillsActivity extends AppCompatActivity implements MySkillAdapter.OnSkillActionListener {

    private RecyclerView rvMySkills;
    private TextView tvEmpty;
    private MySkillAdapter adapter;
    private FirebaseRepository repo;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_skills);

        repo           = FirebaseRepository.getInstance();
        sessionManager = new SessionManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("My Skills");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        rvMySkills = findViewById(R.id.rvMySkills);
        tvEmpty    = findViewById(R.id.tvEmpty);
        rvMySkills.setLayoutManager(new LinearLayoutManager(this));

        ExtendedFloatingActionButton fab = findViewById(R.id.fabAddNew);
        if (fab != null) fab.setOnClickListener(v ->
                startActivity(new Intent(this, AddSkillActivity.class)));

        loadMySkills();
    }

    @Override protected void onResume() { super.onResume(); loadMySkills(); }

    private void loadMySkills() {
        repo.getSkillsByProviderId(sessionManager.getUserId(),
                new FirebaseRepository.Callback<List<Skill>>() {
                    @Override public void onSuccess(List<Skill> skills) {
                        if (skills == null) skills = new ArrayList<>();
                        if (skills.isEmpty()) {
                            rvMySkills.setVisibility(View.GONE);
                            if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
                        } else {
                            rvMySkills.setVisibility(View.VISIBLE);
                            if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
                            adapter = new MySkillAdapter(skills, MySkillsActivity.this);
                            rvMySkills.setAdapter(adapter);
                        }
                    }
                    @Override public void onError(String e) {}
                });
    }

    @Override
    public void onEdit(Skill skill) {
        Intent i = new Intent(this, AddSkillActivity.class);
        i.putExtra("skill_doc_id",      skill.getDocumentId());
        i.putExtra("skill_title",       skill.getTitle());
        i.putExtra("skill_description", skill.getDescription());
        i.putExtra("skill_price",       skill.getPrice());
        i.putExtra("skill_category",    skill.getCategoryName());
        startActivity(i);
    }

    @Override
    public void onDelete(Skill skill) {
        repo.deleteSkill(skill.getDocumentId(), r -> {
            Toast.makeText(this, "Skill deleted", Toast.LENGTH_SHORT).show();
            loadMySkills();
        });
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
