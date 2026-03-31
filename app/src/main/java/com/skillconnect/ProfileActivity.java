package com.skillconnect;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.skillconnect.data.FirebaseRepository;
import com.skillconnect.data.SessionManager;

/**
 * Profile screen — role displayed as a read-only badge.
 * Role is decided at registration and cannot be changed.
 */
public class ProfileActivity extends AppCompatActivity {

    private TextView tvUserName, tvUserEmail, tvMyItemsLabel;
    private TextView tvRoleBadge, tvRoleDescription;
    private SwitchMaterial switchDarkMode;
    private MaterialButton btnLogout, btnEditProfile;
    private MaterialCardView cardMyItems, cardSettings, cardHelp, cardPaymentHistory, cardGetVerified, cardAdminDashboard;
    private SessionManager sessionManager;

    private boolean isUpdatingUI = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sessionManager = new SessionManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        tvUserName        = findViewById(R.id.tvUserName);
        tvUserEmail       = findViewById(R.id.tvUserEmail);
        tvMyItemsLabel    = findViewById(R.id.tvMyItemsLabel);
        tvRoleBadge       = findViewById(R.id.tvRoleBadge);
        tvRoleDescription = findViewById(R.id.tvRoleDescription);
        switchDarkMode    = findViewById(R.id.switchDarkMode);
        btnLogout         = findViewById(R.id.btnLogout);
        btnEditProfile    = findViewById(R.id.btnEditProfile);
        cardMyItems       = findViewById(R.id.cardMyItems);
        cardSettings      = findViewById(R.id.cardSettings);
        cardHelp          = findViewById(R.id.cardHelp);
        cardPaymentHistory= findViewById(R.id.cardPaymentHistory);
        cardGetVerified   = findViewById(R.id.cardGetVerified);
        cardAdminDashboard= findViewById(R.id.cardAdminDashboard);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setupListeners();
        displayUserInfo();
    }

    @Override
    protected void onResume() {
        super.onResume();
        displayUserInfo();
    }

    private void displayUserInfo() {
        isUpdatingUI = true;
        try {
            String name  = sessionManager.getUserName();
            String email = sessionManager.getUserEmail();
            String role  = sessionManager.getUserRole();

            tvUserName.setText(name.isEmpty()   ? "Guest User"             : name);
            tvUserEmail.setText(email.isEmpty() ? "guest@skillconnect.com" : email);

            boolean isProvider = "provider".equals(role);
            if (tvRoleBadge != null) {
                tvRoleBadge.setText(isProvider ? "Provider" : "Customer");
            }
            if (tvRoleDescription != null) {
                tvRoleDescription.setText(isProvider
                        ? "Offering services to clients"
                        : "Hiring services from providers");
            }

            if (switchDarkMode != null) switchDarkMode.setChecked(sessionManager.isDarkMode());
            updateMyItemsLabel();
        } finally {
            isUpdatingUI = false;
        }
    }

    private void setupListeners() {
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v ->
                    startActivity(new Intent(this, EditProfileActivity.class)));
        }

        if (switchDarkMode != null) {
            switchDarkMode.setOnCheckedChangeListener((btn, isChecked) -> {
                if (isUpdatingUI) return;
                sessionManager.setDarkMode(isChecked);
                AppCompatDelegate.setDefaultNightMode(
                        isChecked ? AppCompatDelegate.MODE_NIGHT_YES
                                  : AppCompatDelegate.MODE_NIGHT_NO);
            });
        }

        if (cardMyItems != null) {
            cardMyItems.setOnClickListener(v -> {
                if ("provider".equals(sessionManager.getUserRole())) {
                    startActivity(new Intent(this, MySkillsActivity.class));
                } else {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.putExtra("open_tab", "bookings");
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
            });
        }

        if (cardSettings != null) {
            cardSettings.setOnClickListener(v ->
                    startActivity(new Intent(this, SettingsActivity.class)));
        }

        if (cardPaymentHistory != null) {
            cardPaymentHistory.setOnClickListener(v ->
                    startActivity(new Intent(this, PaymentHistoryActivity.class)));
        }

        // Show "Get Verified" only for providers
        if (cardGetVerified != null && "provider".equals(sessionManager.getUserRole())) {
            cardGetVerified.setVisibility(android.view.View.VISIBLE);
            cardGetVerified.setOnClickListener(v ->
                    startActivity(new Intent(this, VerificationActivity.class)));
        }

        if (cardAdminDashboard != null) {
            String email = sessionManager.getUserEmail();
            if (email != null && (email.toLowerCase().contains("admin") || email.equalsIgnoreCase("skillconnect@exampl.com"))) {
                cardAdminDashboard.setVisibility(android.view.View.VISIBLE);
                cardAdminDashboard.setOnClickListener(v -> 
                    startActivity(new Intent(this, AdminDashboardActivity.class)));
            }
        }

        if (cardHelp != null) cardHelp.setOnClickListener(v -> showHelpDialog());
        if (btnLogout != null) btnLogout.setOnClickListener(v -> showLogoutConfirmation());
    }

    private void updateMyItemsLabel() {
        if (tvMyItemsLabel == null) return;
        tvMyItemsLabel.setText("provider".equals(sessionManager.getUserRole())
                ? R.string.my_skills : R.string.my_bookings);
    }

    private void showHelpDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Help & FAQ")
                .setMessage("Q: How do I book a skill?\nA: Browse categories → tap a skill → Book Now.\n\n"
                        + "Q: How do I add a skill as provider?\nA: Home → tap the + FAB.\n\n"
                        + "Q: How do I accept/reject a booking?\nA: Go to Bookings tab → use action buttons.\n\n"
                        + "Q: How do I leave a review?\nA: After booking is completed → Bookings → Leave Review.")
                .setPositiveButton("Got it", null)
                .show();
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.logout)
                .setMessage(R.string.logout_confirmation)
                .setPositiveButton(R.string.logout, (dialog, which) -> {
                    FirebaseRepository.getInstance().logout();
                    sessionManager.logout();
                    Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
