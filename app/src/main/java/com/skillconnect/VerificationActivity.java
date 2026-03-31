package com.skillconnect;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.skillconnect.data.FirebaseRepository;
import com.skillconnect.data.SessionManager;
import java.util.HashMap;
import java.util.Map;

public class VerificationActivity extends AppCompatActivity {

    private ImageView ivStatusIcon;
    private TextView tvVerifyStatus, tvVerifyDesc;
    private LinearLayout layoutForm;
    private TextInputEditText etLegalName, etIdNumber, etExperience;
    private MaterialButton btnRequest;

    private FirebaseRepository repo;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);

        repo = FirebaseRepository.getInstance();
        session = new SessionManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ivStatusIcon   = findViewById(R.id.ivStatusIcon);
        tvVerifyStatus = findViewById(R.id.tvVerifyStatus);
        tvVerifyDesc   = findViewById(R.id.tvVerifyDesc);
        layoutForm     = findViewById(R.id.layoutForm);
        etLegalName    = findViewById(R.id.etLegalName);
        etIdNumber     = findViewById(R.id.etIdNumber);
        etExperience   = findViewById(R.id.etExperience);
        btnRequest     = findViewById(R.id.btnRequestVerify);

        btnRequest.setOnClickListener(v -> submitVerification());

        loadStatus();
    }

    private void loadStatus() {
        repo.getVerificationStatus(session.getUserId(), new FirebaseRepository.Callback<String>() {
            @Override
            public void onSuccess(String status) {
                switch (status) {
                    case "verified":
                        tvVerifyStatus.setText("✅ Verified");
                        tvVerifyDesc.setText("Your account is verified! The badge is visible on your profile.");
                        layoutForm.setVisibility(View.GONE);
                        ivStatusIcon.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
                        break;
                    case "pending":
                        tvVerifyStatus.setText("⏳ Under Review");
                        tvVerifyDesc.setText("Your verification request is being reviewed. This usually takes 24-48 hours.");
                        layoutForm.setVisibility(View.GONE);
                        break;
                    case "rejected":
                        tvVerifyStatus.setText("❌ Rejected");
                        tvVerifyDesc.setText("Your previous request was rejected. Please re-submit with valid documents.");
                        layoutForm.setVisibility(View.VISIBLE);
                        break;
                    default:
                        tvVerifyStatus.setText("Not Verified");
                        tvVerifyDesc.setText("Get a verified badge to boost trust and visibility");
                        layoutForm.setVisibility(View.VISIBLE);
                        break;
                }
            }

            @Override
            public void onError(String error) {
                tvVerifyStatus.setText("Not Verified");
            }
        });
    }

    private void submitVerification() {
        String name = etLegalName.getText() != null ? etLegalName.getText().toString().trim() : "";
        String idNum = etIdNumber.getText() != null ? etIdNumber.getText().toString().trim() : "";
        String exp = etExperience.getText() != null ? etExperience.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) { etLegalName.setError("Required"); return; }
        if (TextUtils.isEmpty(idNum) || idNum.length() < 6) { etIdNumber.setError("Enter a valid ID number"); return; }

        btnRequest.setEnabled(false);
        btnRequest.setText("Submitting...");

        Map<String, Object> data = new HashMap<>();
        data.put("legalName", name);
        data.put("idNumber", idNum);
        data.put("experience", exp);
        data.put("email", session.getUserEmail());

        repo.requestVerification(session.getUserId(), data, new FirebaseRepository.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                Toast.makeText(VerificationActivity.this,
                    "Verification request submitted! We'll review it within 48 hours.",
                    Toast.LENGTH_LONG).show();
                loadStatus();
            }

            @Override
            public void onError(String error) {
                btnRequest.setEnabled(true);
                btnRequest.setText("Request Verification");
                Toast.makeText(VerificationActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
