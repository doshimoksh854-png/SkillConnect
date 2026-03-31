package com.skillconnect;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
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
import com.skillconnect.models.JobPost;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class NewJobActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etDescription, etBudget;
    private AutoCompleteTextView actvCategory;
    private MaterialButton btnPost, btnSetDeadline, btnAddImage, btnAttachFile;
    private ImageView ivJobImageFull;
    private TextView tvDeadline, tvImageStatus, tvFileStatus, tvUploadStatus;
    private LinearProgressIndicator uploadProgress;

    private FirebaseRepository repo;
    private SessionManager session;

    private Uri selectedImageUri = null;
    private Uri selectedFileUri  = null;
    private String selectedFileName = "";
    private long deadlineMillis = 0;

    // ── ActivityResultLaunchers ──────────────────────────────────────────

    /** Pick image from gallery */
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                selectedImageUri = result.getData().getData();
                if (selectedImageUri != null) {
                    ivJobImageFull.setVisibility(View.VISIBLE);
                    Glide.with(this).load(selectedImageUri).into(ivJobImageFull);
                    tvImageStatus.setText("Image selected ✓");
                    tvImageStatus.setTextColor(getColor(android.R.color.holo_green_dark));
                }
            }
        });

    /** Pick any file / document */
    private final ActivityResultLauncher<Intent> filePickerLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                selectedFileUri = result.getData().getData();
                if (selectedFileUri != null) {
                    selectedFileName = getFileName(selectedFileUri);
                    tvFileStatus.setText(selectedFileName.isEmpty() ? "File selected ✓" : selectedFileName);
                    tvFileStatus.setTextColor(getColor(android.R.color.holo_green_dark));
                }
            }
        });

    /** Speech to text */
    private final ActivityResultLauncher<Intent> speechRecognizerLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                java.util.ArrayList<String> matches = result.getData()
                        .getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS);
                if (matches != null && !matches.isEmpty()) {
                    String spokenText = matches.get(0);
                    String currentText = etDescription.getText() != null ? etDescription.getText().toString() : "";
                    if (!currentText.isEmpty()) {
                        etDescription.setText(currentText + " " + spokenText);
                    } else {
                        etDescription.setText(spokenText);
                    }
                    etDescription.setSelection(etDescription.getText().length());
                }
            }
        });

    // ── Lifecycle ────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_job);

        repo    = FirebaseRepository.getInstance();
        session = new SessionManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        etTitle        = findViewById(R.id.etTitle);
        etDescription  = findViewById(R.id.etDescription);
        etBudget       = findViewById(R.id.etBudget);
        actvCategory   = findViewById(R.id.actvCategory);
        btnPost        = findViewById(R.id.btnPostJob);
        btnSetDeadline = findViewById(R.id.btnSetDeadline);
        btnAddImage    = findViewById(R.id.btnAddImage);
        btnAttachFile  = findViewById(R.id.btnAttachFile);
        ivJobImageFull = findViewById(R.id.ivJobImageFull);
        tvDeadline     = findViewById(R.id.tvDeadline);
        tvImageStatus  = findViewById(R.id.tvImageStatus);
        tvFileStatus   = findViewById(R.id.tvFileStatus);
        tvUploadStatus = findViewById(R.id.tvUploadStatus);
        uploadProgress = findViewById(R.id.uploadProgress);

        String[] categories = {
                "Software Development", "Tech Support", "Creative & Design",
                "Education", "Digital Marketing", "Business & Remote IT"
        };
        actvCategory.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, categories));

        com.google.android.material.textfield.TextInputLayout tilDescription = findViewById(R.id.tilDescription);
        if (tilDescription != null) {
            tilDescription.setEndIconOnClickListener(v -> startVoiceInput());
        }

        btnSetDeadline.setOnClickListener(v -> showDatePicker());
        btnAddImage.setOnClickListener(v    -> openImagePicker());
        btnAttachFile.setOnClickListener(v  -> openFilePicker());
        btnPost.setOnClickListener(v        -> postJob());
    }

    private void startVoiceInput() {
        Intent intent = new Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Speak job description...");
        try {
            speechRecognizerLauncher.launch(intent);
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, "Speech recognition not supported on this device", Toast.LENGTH_SHORT).show();
        }
    }

    // ── Date Picker ──────────────────────────────────────────────────────

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        if (deadlineMillis > 0) cal.setTimeInMillis(deadlineMillis);

        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, day) -> {
                    Calendar chosen = Calendar.getInstance();
                    chosen.set(year, month, day, 23, 59, 59);
                    deadlineMillis = chosen.getTimeInMillis();
                    SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault());
                    tvDeadline.setText("Deadline: " + sdf.format(chosen.getTime()));
                    tvDeadline.setTextColor(getColor(android.R.color.holo_green_dark));
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMinDate(System.currentTimeMillis());
        dialog.show();
    }

    // ── File/Image Pickers ───────────────────────────────────────────────

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        String[] mimeTypes = {"application/pdf", "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "text/plain", "image/*"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        filePickerLauncher.launch(Intent.createChooser(intent, "Select File"));
    }

    // ── Post Job ─────────────────────────────────────────────────────────

    private void postJob() {
        String title = etTitle.getText().toString().trim();
        String desc  = etDescription.getText().toString().trim();
        String cat   = actvCategory.getText().toString().trim();
        String budg  = etBudget.getText().toString().trim();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(desc)
                || TextUtils.isEmpty(cat) || TextUtils.isEmpty(budg)) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double budgetVal;
        try {
            budgetVal = Double.parseDouble(budg);
        } catch (Exception e) {
            etBudget.setError("Invalid number");
            return;
        }

        if (deadlineMillis == 0) {
            Toast.makeText(this, "Please set a project deadline", Toast.LENGTH_SHORT).show();
            return;
        }

        btnPost.setEnabled(false);
        btnPost.setText("Posting...");

        final JobPost job = new JobPost(
                session.getUserId(), session.getUserName(), title, desc, cat, budgetVal);
        job.setDeadline(deadlineMillis);

        // Determine how many uploads are needed
        boolean needImageUpload = selectedImageUri != null;
        boolean needFileUpload  = selectedFileUri  != null;

        if (!needImageUpload && !needFileUpload) {
            // No uploads needed — just post directly
            createPost(job);
            return;
        }

        // Upload flow — count completions
        showUploadProgress(true);
        AtomicInteger remaining = new AtomicInteger(
                (needImageUpload ? 1 : 0) + (needFileUpload ? 1 : 0));

        if (needImageUpload) {
            tvUploadStatus.setText("Uploading image…");
            repo.uploadJobImage(selectedImageUri, new FirebaseRepository.Callback<String>() {
                @Override
                public void onSuccess(String url) {
                    job.setImageUrl(url);
                    if (remaining.decrementAndGet() == 0) createPost(job);
                }
                @Override
                public void onError(String error) {
                    showUploadProgress(false);
                    resetPostButton();
                    Toast.makeText(NewJobActivity.this,
                            "Image upload failed: " + error, Toast.LENGTH_LONG).show();
                }
            });
        }

        if (needFileUpload) {
            tvUploadStatus.setText("Uploading file…");
            repo.uploadJobFile(selectedFileUri, new FirebaseRepository.Callback<String>() {
                @Override
                public void onSuccess(String url) {
                    job.setAttachmentUrl(url);
                    job.setAttachmentName(selectedFileName);
                    if (remaining.decrementAndGet() == 0) createPost(job);
                }
                @Override
                public void onError(String error) {
                    showUploadProgress(false);
                    resetPostButton();
                    Toast.makeText(NewJobActivity.this,
                            "File upload failed: " + error, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void createPost(JobPost job) {
        runOnUiThread(() -> tvUploadStatus.setText("Publishing job…"));
        repo.createJobPost(job, new FirebaseRepository.Callback<String>() {
            @Override
            public void onSuccess(String jobId) {
                showUploadProgress(false);
                Toast.makeText(NewJobActivity.this,
                        "Job Posted Successfully!", Toast.LENGTH_SHORT).show();
                finish();
            }
            @Override
            public void onError(String error) {
                showUploadProgress(false);
                resetPostButton();
                Toast.makeText(NewJobActivity.this,
                        "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private void showUploadProgress(boolean show) {
        runOnUiThread(() -> {
            uploadProgress.setVisibility(show ? View.VISIBLE : View.GONE);
            tvUploadStatus.setVisibility(show ? View.VISIBLE : View.GONE);
        });
    }

    private void resetPostButton() {
        runOnUiThread(() -> {
            btnPost.setEnabled(true);
            btnPost.setText("Post Job");
        });
    }

    /** Try to get a human-readable filename from a URI */
    private String getFileName(Uri uri) {
        String result = "";
        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor cursor = getContentResolver().query(
                    uri, new String[]{android.provider.OpenableColumns.DISPLAY_NAME},
                    null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(0);
                }
            } catch (Exception ignored) {}
        }
        if (result.isEmpty()) {
            result = uri.getLastPathSegment();
            if (result != null && result.contains("/")) {
                result = result.substring(result.lastIndexOf('/') + 1);
            }
        }
        return result != null ? result : "file";
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
