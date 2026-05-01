package com.skillconnect;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.skillconnect.data.LanguagePreferenceManager;
import com.skillconnect.data.SessionManager;
import com.skillconnect.data.TranslatorManager;

/**
 * Settings — Dark mode, Language selection, Notifications, Currency, Referral.
 */
public class SettingsActivity extends AppCompatActivity {

    private static final String PREF_NOTIF   = "pref_notifications";
    private static final String PREF_CURRENCY = "pref_currency_inr";

    private LanguagePreferenceManager langPref;
    private TranslatorManager translatorManager;
    private LinearProgressIndicator progressLangDownload;
    private TextView tvLangStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("Settings");
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        SessionManager session = new SessionManager(this);
        langPref = new LanguagePreferenceManager(this);
        translatorManager = TranslatorManager.getInstance();
        android.content.SharedPreferences prefs =
                getSharedPreferences("SkillConnectPrefs", MODE_PRIVATE);

        // ── Dark Mode ──
        SwitchMaterial switchDark = findViewById(R.id.switchDarkModeSettings);
        switchDark.setChecked(session.isDarkMode());
        switchDark.setOnCheckedChangeListener((btn, checked) -> {
            session.setDarkMode(checked);
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                    checked ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                            : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        });

        // ── Notifications ──
        SwitchMaterial switchNotif = findViewById(R.id.switchNotifications);
        switchNotif.setChecked(prefs.getBoolean(PREF_NOTIF, true));
        switchNotif.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean(PREF_NOTIF, checked).apply());

        // ── Currency ──
        SwitchMaterial switchCurrency = findViewById(R.id.switchCurrency);
        switchCurrency.setChecked(prefs.getBoolean(PREF_CURRENCY, true));
        switchCurrency.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean(PREF_CURRENCY, checked).apply());

        // ── Language Toggle ──
        setupLanguageToggle();

        // ── Referral ──
        setupReferral(session);
    }

    private void setupLanguageToggle() {
        MaterialButtonToggleGroup toggle = findViewById(R.id.toggleLanguage);
        progressLangDownload = findViewById(R.id.progressLangDownload);
        tvLangStatus = findViewById(R.id.tvLangStatus);

        // Set current selection
        String current = langPref.getLanguage();
        switch (current) {
            case "hi": toggle.check(R.id.btnLangHi); break;
            case "gu": toggle.check(R.id.btnLangGu); break;
            default:   toggle.check(R.id.btnLangEn); break;
        }

        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;

            String newLang;
            if (checkedId == R.id.btnLangHi) newLang = "hi";
            else if (checkedId == R.id.btnLangGu) newLang = "gu";
            else newLang = "en";

            // Don't do anything if selecting the same language
            if (newLang.equals(langPref.getLanguage())) {
                return;
            }

            langPref.setLanguage(newLang);

            if ("en".equals(newLang)) {
                showLangStatus("Language set to English ✅", false);
            } else {
                // Check if model is ready
                if (translatorManager.isModelReady(newLang)) {
                    showLangStatus(langPref.getLanguageLabel() + " ready ✅", false);
                } else if (translatorManager.isDownloadInProgress(newLang)) {
                    // Already downloading, show status
                    showLangStatus("Downloading " + langPref.getLanguageLabel() + " model...", true);
                } else {
                    // Start download
                    startModelDownload(newLang);
                }
            }
        });
    }

    private void startModelDownload(String targetLang) {
        showLangStatus("Downloading " + langPref.getLanguageLabel() + " model...", true);

        // Set a timeout for the download (30 seconds)
        android.os.Handler timeoutHandler = new android.os.Handler();
        Runnable timeoutRunnable = () -> {
            if (translatorManager.isDownloadInProgress(targetLang)) {
                runOnUiThread(() -> {
                    showLangStatus("Download timed out. Check connection and try again.", false);
                    Toast.makeText(this, "Download timed out. Please try again.", Toast.LENGTH_LONG).show();
                });
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, 30000); // 30 second timeout

        translatorManager.downloadModel(targetLang, (success, error) -> {
            // Cancel timeout
            timeoutHandler.removeCallbacks(timeoutRunnable);

            runOnUiThread(() -> {
                if (success) {
                    showLangStatus(langPref.getLanguageLabel() + " ready! ✅", false);
                    Toast.makeText(this, "Translation model downloaded successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    String errorMsg = error != null ? error : "Unknown error";
                    showLangStatus("Download failed. Try again on WiFi.", false);
                    Toast.makeText(this, "Download failed: " + errorMsg, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void showLangStatus(String text, boolean showProgress) {
        if (tvLangStatus != null) {
            tvLangStatus.setText(text);
            tvLangStatus.setVisibility(View.VISIBLE);
        }
        if (progressLangDownload != null) {
            progressLangDownload.setVisibility(showProgress ? View.VISIBLE : View.GONE);
        }
    }

    private void setupReferral(SessionManager session) {
        TextView tvReferralCode = findViewById(R.id.tvReferralCode);
        MaterialButton btnShare = findViewById(R.id.btnShareReferral);

        // Generate referral code from user ID
        String uid = session.getUserId();
        String code = "SC-" + (uid.length() >= 6 ? uid.substring(0, 6).toUpperCase() : uid.toUpperCase());
        if (tvReferralCode != null) tvReferralCode.setText(code);

        if (btnShare != null) {
            btnShare.setOnClickListener(v -> {
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_SUBJECT, "Join SkillConnect!");
                share.putExtra(Intent.EXTRA_TEXT,
                        "Hey! Join SkillConnect — the best skill marketplace! " +
                        "Use my referral code " + code + " and we both get ₹50! " +
                        "Download: https://play.google.com/store/apps/details?id=com.skillconnect");
                startActivity(Intent.createChooser(share, "Share via"));
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
