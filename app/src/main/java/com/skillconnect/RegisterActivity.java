package com.skillconnect;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.skillconnect.data.FirebaseRepository;
import com.skillconnect.data.SessionManager;
import com.skillconnect.models.User;

/**
 * Register using Firebase Auth — async, no ANR possible.
 */
public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilName, tilEmail, tilPassword;
    private TextInputEditText etName, etEmail, etPassword;
    private SwitchMaterial switchRole;
    private MaterialButton btnRegister, btnLogin;
    private CircularProgressIndicator progressBar;

    private FirebaseRepository repo;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        repo           = FirebaseRepository.getInstance();
        sessionManager = new SessionManager(this);

        tilName     = findViewById(R.id.tilName);
        tilEmail    = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etName      = findViewById(R.id.etName);
        etEmail     = findViewById(R.id.etEmail);
        etPassword  = findViewById(R.id.etPassword);
        switchRole  = findViewById(R.id.switchRole);
        btnRegister = findViewById(R.id.btnRegister);
        btnLogin    = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBarRegister);

        btnRegister.setOnClickListener(v -> handleRegister());
        if (btnLogin != null) btnLogin.setOnClickListener(v -> finish());
    }

    private void handleRegister() {
        String name     = etName.getText()     != null ? etName.getText().toString().trim()    : "";
        String email    = etEmail.getText()    != null ? etEmail.getText().toString().trim()    : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String role     = switchRole != null && switchRole.isChecked() ? "provider" : "customer";

        tilName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);

        if (TextUtils.isEmpty(name)) {
            tilName.setError(getString(R.string.error_empty_name)); return;
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(TextUtils.isEmpty(email)
                    ? getString(R.string.error_empty_email)
                    : getString(R.string.error_invalid_email));
            return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            tilPassword.setError(password.isEmpty()
                    ? getString(R.string.error_empty_password)
                    : "Password must be at least 6 characters");
            return;
        }

        setLoading(true);

        repo.register(name, email, password, role, new FirebaseRepository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);
                sessionManager.createLoginSession(user.getId(), user.getName(),
                        user.getEmail(), user.getRole());
                Toast.makeText(RegisterActivity.this,
                        R.string.success_register, Toast.LENGTH_SHORT).show();
                startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                finish();
            }
            @Override
            public void onError(String error) {
                setLoading(false);
                String msg = error != null && error.contains("already in use")
                        ? "Email already registered"
                        : "Registration failed: " + error;
                tilEmail.setError(msg);
            }
        });
    }

    private void setLoading(boolean loading) {
        btnRegister.setEnabled(!loading);
        if (progressBar != null)
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
