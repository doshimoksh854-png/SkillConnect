package com.skillconnect;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.skillconnect.data.FirebaseRepository;
import com.skillconnect.data.SessionManager;
import de.hdodenhof.circleimageview.CircleImageView;

public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPhone;
    private MaterialButton btnSave;
    private CircleImageView ivProfilePhoto;
    private LinearProgressIndicator progressUpload;
    private FirebaseRepository repo;
    private SessionManager sessionManager;
    private String uploadedImageUrl = null;

    private final ActivityResultLauncher<String> pickImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) uploadProfileImage(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        repo           = FirebaseRepository.getInstance();
        sessionManager = new SessionManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Edit Profile");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etName         = findViewById(R.id.etName);
        etEmail        = findViewById(R.id.etEmail);
        etPhone        = findViewById(R.id.etPhone);
        btnSave        = findViewById(R.id.btnSave);
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        progressUpload = findViewById(R.id.progressUpload);

        etName.setText(sessionManager.getUserName());
        etEmail.setText(sessionManager.getUserEmail());
        etPhone.setText(sessionManager.getUserPhone());

        // Load existing profile photo
        loadExistingPhoto();

        // Photo picker
        View btnChangePhoto = findViewById(R.id.btnChangePhoto);
        if (btnChangePhoto != null) {
            btnChangePhoto.setOnClickListener(v -> pickImage.launch("image/*"));
        }
        if (ivProfilePhoto != null) {
            ivProfilePhoto.setOnClickListener(v -> pickImage.launch("image/*"));
        }

        btnSave.setOnClickListener(v -> handleSave());
    }

    private void loadExistingPhoto() {
        String uid = sessionManager.getUserId();
        repo.getUserById(uid, new FirebaseRepository.Callback<com.skillconnect.models.User>() {
            @Override public void onSuccess(com.skillconnect.models.User user) {
                String url = user.getProfileImageUrl();
                if (url != null && !url.isEmpty() && ivProfilePhoto != null) {
                    Glide.with(EditProfileActivity.this)
                         .load(url)
                         .placeholder(R.drawable.ic_profile_placeholder)
                         .into(ivProfilePhoto);
                    uploadedImageUrl = url; // keep existing
                }
            }
            @Override public void onError(String e) {}
        });
    }

    private void uploadProfileImage(Uri imageUri) {
        if (progressUpload != null) progressUpload.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Uploading photo...", Toast.LENGTH_SHORT).show();

        repo.uploadJobImage(imageUri, new FirebaseRepository.Callback<String>() {
            @Override public void onSuccess(String url) {
                uploadedImageUrl = url;
                if (progressUpload != null) progressUpload.setVisibility(View.GONE);
                if (ivProfilePhoto != null) {
                    Glide.with(EditProfileActivity.this)
                         .load(url)
                         .placeholder(R.drawable.ic_profile_placeholder)
                         .into(ivProfilePhoto);
                }
                Toast.makeText(EditProfileActivity.this, "Photo uploaded! ✅", Toast.LENGTH_SHORT).show();
            }
            @Override public void onError(String error) {
                if (progressUpload != null) progressUpload.setVisibility(View.GONE);
                Toast.makeText(EditProfileActivity.this, "Upload failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleSave() {
        String name  = etName.getText()  != null ? etName.getText().toString().trim()  : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim()  : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim()  : "";

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name and email are required", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        String uid = sessionManager.getUserId();

        // Save profile image URL to Firestore
        if (uploadedImageUrl != null && !uploadedImageUrl.isEmpty()) {
            repo.updateUserProfileImage(uid, uploadedImageUrl, null);
        }

        repo.updateUser(uid, name, email, phone,
                new FirebaseRepository.Callback<Boolean>() {
                    @Override public void onSuccess(Boolean ok) {
                        btnSave.setEnabled(true);
                        sessionManager.updateProfile(name, email, phone);
                        Toast.makeText(EditProfileActivity.this,
                                "Profile updated! ✅", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    @Override public void onError(String e) {
                        btnSave.setEnabled(true);
                        Toast.makeText(EditProfileActivity.this,
                                "Update failed: " + e, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
