package com.skillconnect;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.skillconnect.data.FirebaseRepository;
import com.skillconnect.data.SessionManager;

/**
 * Login screen using Firebase Auth — no main-thread DB work, no ANR possible.
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, tvRegisterLink;
    private CircularProgressIndicator progressBar;

    private FirebaseRepository repo;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        repo           = FirebaseRepository.getInstance();
        sessionManager = new SessionManager(this);

        // If already authenticated via Firebase, verify profile then skip login
        if (repo.getCurrentUser() != null && sessionManager.isLoggedIn()) {
            String uid = repo.getCurrentUser().getUid();
            repo.getUserById(uid, new FirebaseRepository.Callback<com.skillconnect.models.User>() {
                @Override public void onSuccess(com.skillconnect.models.User user) {
                    // Update session with latest role from Firestore
                    sessionManager.createLoginSession(user.getId(), user.getName(),
                            user.getEmail(), user.getRole());
                    goToMain();
                }
                @Override public void onError(String error) {
                    // Profile missing — clear session and show login
                    repo.logout();
                    sessionManager.logout();
                }
            });
            return;
        }

        tilEmail    = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail     = findViewById(R.id.etEmail);
        etPassword  = findViewById(R.id.etPassword);
        btnLogin    = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
        tvRegisterLink = findViewById(R.id.tvRegisterLink);

        btnLogin.setOnClickListener(v -> handleLogin());
        if (tvRegisterLink != null)
            tvRegisterLink.setOnClickListener(v ->
                    startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void handleLogin() {
        String email    = etEmail.getText()    != null ? etEmail.getText().toString().trim()    : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        tilEmail.setError(null);
        tilPassword.setError(null);

        if (email.isEmpty())    { tilEmail.setError("Please enter email"); return; }
        if (password.isEmpty()) { tilPassword.setError("Please enter password"); return; }

        setLoading(true);

        // Firebase Auth handles everything asynchronously — zero ANR risk
        repo.login(email, password, new FirebaseRepository.Callback<com.skillconnect.models.User>() {
            @Override
            public void onSuccess(com.skillconnect.models.User user) {
                setLoading(false);
                sessionManager.createLoginSession(user.getId(), user.getName(),
                        user.getEmail(), user.getRole());
                if (user.getPhone() != null && !user.getPhone().isEmpty())
                    sessionManager.updateProfile(user.getName(), user.getEmail(), user.getPhone());
                Toast.makeText(LoginActivity.this,
                        getString(R.string.success_login), Toast.LENGTH_SHORT).show();
                goToMain();
            }
            @Override
            public void onError(String error) {
                setLoading(false);
                // Give specific error messages based on Firebase error
                String msg;
                if (error == null) {
                    msg = "Login failed. Please try again.";
                } else if (error.contains("no user record") || error.contains("user-not-found") || error.contains("USER_NOT_FOUND")) {
                    msg = "Account not found. Please register first.";
                } else if (error.contains("password") || error.contains("INVALID_PASSWORD") || error.contains("wrong-password")) {
                    msg = "Wrong password. Please try again.";
                } else if (error.contains("network") || error.contains("timeout")) {
                    msg = "Network error. Check your internet connection.";
                } else if (error.contains("invalid-email") || error.contains("INVALID_EMAIL")) {
                    msg = "Please enter a valid email address.";
                } else {
                    msg = "Login failed: " + error;
                }
                tilPassword.setError(msg);
                Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        if (progressBar != null)
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
