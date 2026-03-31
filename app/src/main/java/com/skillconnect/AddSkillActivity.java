package com.skillconnect;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.skillconnect.data.FirebaseRepository;
import com.skillconnect.data.SessionManager;
import com.skillconnect.models.Skill;

public class AddSkillActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etDescription, etPrice;
    private AutoCompleteTextView etCategory;
    private MaterialButton btnSave;
    private FirebaseRepository repo;
    private SessionManager sessionManager;
    private Skill editingSkill; // non-null when editing

    private static final String[] CATEGORIES = {
            "Software Development", "Creative & Design", "Digital Marketing",
            "Education", "Tech Support", "Business & Remote IT"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_skill);

        repo           = FirebaseRepository.getInstance();
        sessionManager = new SessionManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        etTitle       = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etPrice       = findViewById(R.id.etPrice);
        etCategory    = findViewById(R.id.spinnerCategory);
        btnSave       = findViewById(R.id.btnSave);

        etCategory.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, CATEGORIES));

        // Check if we are editing an existing skill
        String docId = getIntent().getStringExtra("skill_doc_id");
        if (docId != null) {
            getSupportActionBar().setTitle("Edit Skill");
            editingSkill = new Skill();
            editingSkill.setDocumentId(docId);
            etTitle.setText(getIntent().getStringExtra("skill_title"));
            etDescription.setText(getIntent().getStringExtra("skill_description"));
            etPrice.setText(String.valueOf(getIntent().getDoubleExtra("skill_price", 0)));
            etCategory.setText(getIntent().getStringExtra("skill_category"), false);
        } else {
            getSupportActionBar().setTitle("Add New Skill");
        }

        btnSave.setOnClickListener(v -> handleSave());
    }

    private void handleSave() {
        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String desc  = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
        String priceStr = etPrice.getText() != null ? etPrice.getText().toString().trim() : "";
        String category = etCategory.getText() != null ? etCategory.getText().toString().trim() : "";

        if (title.isEmpty() || desc.isEmpty() || priceStr.isEmpty() || category.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        try { price = Double.parseDouble(priceStr); }
        catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid price", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);

        if (editingSkill != null) {
            editingSkill.setTitle(title);
            editingSkill.setDescription(desc);
            editingSkill.setPrice(price);
            editingSkill.setCategoryName(category);
            repo.updateSkill(editingSkill, result -> {
                btnSave.setEnabled(true);
                Toast.makeText(this, "Skill updated!", Toast.LENGTH_SHORT).show();
                finish();
            });
        } else {
            Skill skill = new Skill(title, desc, price,
                    sessionManager.getUserName(), 0f);
            skill.setProviderId(sessionManager.getUserId());
            skill.setCategoryName(category);
            repo.addSkill(skill, id -> {
                btnSave.setEnabled(true);
                Toast.makeText(this, "Skill added!", Toast.LENGTH_SHORT).show();
                finish();
            });
        }
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
