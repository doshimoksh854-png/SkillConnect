package com.skillconnect;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.skillconnect.data.FirebaseRepository;
import com.skillconnect.data.LanguagePreferenceManager;
import com.skillconnect.data.SessionManager;
import com.skillconnect.data.TranslatorManager;
import com.skillconnect.models.Booking;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import java.util.Locale;

/**
 * Skill detail screen — booking now writes to Firestore (async, no ANR).
 */
public class SkillDetailActivity extends AppCompatActivity {

    private CollapsingToolbarLayout collapsingToolbar;
    private TextView tvSkillTitle, tvPrice, tvRating, tvReviewCount, tvDescription, tvProviderName;
    private MaterialButton btnBookNow;
    private MaterialCardView cardProvider;
    private TextInputEditText etNotes;

    private String skillDocId, skillProviderId, skillTitle, skillDescription, providerName;
    private double skillPrice;
    private float  skillRating;
    private int    reviewCount;

    private FirebaseRepository repo;
    private SessionManager sessionManager;
    private LanguagePreferenceManager langPrefManager;
    private TranslatorManager translatorManager;
    private ExtendedFloatingActionButton fabTranslate;
    private boolean isTranslated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skill_detail);

        repo           = FirebaseRepository.getInstance();
        sessionManager = new SessionManager(this);
        langPrefManager = new LanguagePreferenceManager(this);
        translatorManager = TranslatorManager.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        collapsingToolbar = findViewById(R.id.collapsingToolbar);
        tvSkillTitle      = findViewById(R.id.tvSkillTitle);
        tvPrice           = findViewById(R.id.tvPrice);
        tvRating          = findViewById(R.id.tvRating);
        tvReviewCount     = findViewById(R.id.tvReviewCount);
        tvDescription     = findViewById(R.id.tvDescription);
        tvProviderName    = findViewById(R.id.tvProviderName);
        btnBookNow = findViewById(R.id.btnBookNow);
        MaterialButton btnChat = findViewById(R.id.btnChat);
        cardProvider      = findViewById(R.id.cardProvider);
        etNotes           = findViewById(R.id.etNotes);
        fabTranslate      = findViewById(R.id.fabTranslate);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        loadFromIntent();
        displayDetails();
        setupTranslation();

        // Hide Book/Chat buttons when provider views their own skill
        boolean isOwnSkill = sessionManager.getUserId().equals(skillProviderId);
        if (isOwnSkill) {
            if (btnBookNow != null) btnBookNow.setVisibility(android.view.View.GONE);
            if (btnChat    != null) btnChat.setVisibility(android.view.View.GONE);
        } else {
            btnBookNow.setOnClickListener(v -> handleBooking());
            if (btnChat != null) {
                btnChat.setOnClickListener(v -> {
                    Intent ci = new Intent(SkillDetailActivity.this, ChatActivity.class);
                    ci.putExtra("chat_partner_id",   skillProviderId);
                    ci.putExtra("chat_partner_name", providerName);
                    startActivity(ci);
                });
            }
        }

        if (cardProvider != null) {
            cardProvider.setOnClickListener(v -> {
                Intent i = new Intent(this, ProviderProfileActivity.class);
                i.putExtra("provider_id",     skillProviderId);
                i.putExtra("provider_name",   providerName);
                i.putExtra("provider_rating", skillRating);
                startActivity(i);
            });
        }
    }

    private void loadFromIntent() {
        Intent i = getIntent();
        if (i == null) return;
        skillDocId      = i.getStringExtra("skill_doc_id");
        skillProviderId = i.getStringExtra("skill_provider_id");
        skillTitle      = i.getStringExtra("skill_title");
        skillDescription= i.getStringExtra("skill_description");
        skillPrice      = i.getDoubleExtra("skill_price", 0);
        skillRating     = i.getFloatExtra("skill_rating", 0);
        providerName    = i.getStringExtra("skill_provider");
        reviewCount     = i.getIntExtra("skill_review_count", 0);
    }

    private void displayDetails() {
        if (collapsingToolbar != null) collapsingToolbar.setTitle(skillTitle);
        if (tvSkillTitle  != null) tvSkillTitle.setText(skillTitle);
        if (tvPrice       != null) tvPrice.setText(String.format(Locale.getDefault(), "₹%.0f", skillPrice));
        if (tvRating      != null) tvRating.setText(String.format(Locale.getDefault(), "%.1f", skillRating));
        if (tvReviewCount != null) tvReviewCount.setText(String.format(Locale.getDefault(), "(%d reviews)", reviewCount));
        if (tvDescription != null) tvDescription.setText(skillDescription);
        if (tvProviderName!= null) tvProviderName.setText(providerName);
    }

    private void setupTranslation() {
        if (!langPrefManager.isTranslationNeeded()) {
            if (fabTranslate != null) fabTranslate.setVisibility(View.GONE);
            return;
        }

        String targetLang = langPrefManager.getLanguage();
        if (translatorManager.isModelReady(targetLang)) {
            if (fabTranslate != null) fabTranslate.setVisibility(View.VISIBLE);
        } else {
            if (fabTranslate != null) fabTranslate.setVisibility(View.GONE);
        }

        if (fabTranslate != null) {
            fabTranslate.setOnClickListener(v -> {
                if (isTranslated) {
                    if (collapsingToolbar != null) collapsingToolbar.setTitle(skillTitle);
                    if (tvSkillTitle != null) tvSkillTitle.setText(skillTitle);
                    if (tvDescription != null) tvDescription.setText(skillDescription);
                    fabTranslate.setText("🌐 Translate");
                    isTranslated = false;
                } else {
                    fabTranslate.setText("⏳ Translating...");
                    fabTranslate.setEnabled(false);

                    translatorManager.translate(skillTitle, targetLang, new TranslatorManager.TranslateCallback() {
                        @Override
                        public void onSuccess(String translatedTitle) {
                            if (collapsingToolbar != null) collapsingToolbar.setTitle(translatedTitle);
                            if (tvSkillTitle != null) tvSkillTitle.setText(translatedTitle);
                            translatorManager.translate(skillDescription, targetLang, new TranslatorManager.TranslateCallback() {
                                @Override
                                public void onSuccess(String translatedDesc) {
                                    if (tvDescription != null) tvDescription.setText(translatedDesc);
                                    fabTranslate.setText("Original");
                                    isTranslated = true;
                                    fabTranslate.setEnabled(true);
                                }

                                @Override
                                public void onError(String error) {
                                    Toast.makeText(SkillDetailActivity.this, "Translation failed", Toast.LENGTH_SHORT).show();
                                    fabTranslate.setText("🌐 Translate");
                                    fabTranslate.setEnabled(true);
                                }
                            });
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(SkillDetailActivity.this, "Translation failed", Toast.LENGTH_SHORT).show();
                            fabTranslate.setText("🌐 Translate");
                            fabTranslate.setEnabled(true);
                        }
                    });
                }
            });
        }
    }

    private void handleBooking() {
        String notes = (etNotes != null && etNotes.getText() != null)
                ? etNotes.getText().toString().trim() : "";

        Booking booking = new Booking(
                sessionManager.getUserId(),
                skillProviderId != null ? skillProviderId : "",
                skillDocId      != null ? skillDocId      : "",
                skillTitle, providerName, skillPrice);
        booking.setNotes(notes);

        btnBookNow.setEnabled(false);
        btnBookNow.setText("Booking…");

        repo.createBooking(booking, new FirebaseRepository.Callback<String>() {
            @Override public void onSuccess(String bookingId) {
                btnBookNow.setEnabled(true);
                btnBookNow.setText(R.string.book_now);
                Toast.makeText(SkillDetailActivity.this,
                        R.string.success_booking, Toast.LENGTH_SHORT).show();
                Intent i = new Intent(SkillDetailActivity.this, BookingStatusActivity.class);
                i.putExtra("booking_id",   bookingId);
                i.putExtra("skill_title",  skillTitle);
                i.putExtra("provider_name",providerName);
                i.putExtra("price",        skillPrice);
                startActivity(i);
            }
            @Override public void onError(String error) {
                btnBookNow.setEnabled(true);
                btnBookNow.setText(R.string.book_now);
                Toast.makeText(SkillDetailActivity.this, "Booking failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
