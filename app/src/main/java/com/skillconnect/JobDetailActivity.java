package com.skillconnect;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.skillconnect.adapters.BidAdapter;
import com.skillconnect.data.FirebaseRepository;
import com.skillconnect.data.LanguagePreferenceManager;
import com.skillconnect.data.SessionManager;
import com.skillconnect.data.TranslatorManager;
import com.skillconnect.models.Bid;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class JobDetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvCustomerName, tvCategory, tvBudget, tvDescription, tvStatus, tvNoBids;
    private TextView tvDeadline, tvDaysLeft, tvFileName;
    private ImageView ivJobImage;
    private LinearLayout layoutDeadline, layoutAttachments, layoutFileAttachment;
    private MaterialButton btnOpenFile;
    private RecyclerView rvBids;
    private LinearLayout layoutSubmitBid;
    private TextInputEditText etBidAmount, etProposal;
    private MaterialButton btnSubmitBid;

    private String jobImageUrl = "";
    private String jobAttachmentUrl = "";
    private String jobAttachmentName = "";
    private long   jobDeadline = 0;

    private FirebaseRepository repo;
    private SessionManager session;
    private BidAdapter adapter;

    private String jobId, jobStatus, jobCustomerName;
    private boolean isCustomer;
    private com.google.firebase.firestore.ListenerRegistration bidsListener;

    private ExtendedFloatingActionButton fabTranslate;
    private boolean isTranslated = false;
    private String originalTitle, originalDesc;
    private TranslatorManager translatorManager;
    private LanguagePreferenceManager langPrefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_detail);

        repo = FirebaseRepository.getInstance();
        session = new SessionManager(this);
        langPrefManager = new LanguagePreferenceManager(this);
        translatorManager = TranslatorManager.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        initViews();
        unpackIntent();

        // Customer true if they didn't get here from Provider jobs screen 
        // Or strictly if role is customer
        isCustomer = "customer".equalsIgnoreCase(session.getUserRole());

        // Setup UI rules
        tvStatus.setText(jobStatus != null ? jobStatus.toUpperCase() : "OPEN");
        
        if (!isCustomer && "open".equalsIgnoreCase(jobStatus)) {
            layoutSubmitBid.setVisibility(View.VISIBLE);
        } else {
            layoutSubmitBid.setVisibility(View.GONE);
        }

        btnSubmitBid.setOnClickListener(v -> submitBid());
        
        setupTranslation();
        loadBids();
    }

    private void setupTranslation() {
        if (!langPrefManager.isTranslationNeeded()) {
            fabTranslate.setVisibility(View.GONE);
            return;
        }

        String targetLang = langPrefManager.getLanguage();
        if (translatorManager.isModelReady(targetLang)) {
            fabTranslate.setVisibility(View.VISIBLE);
        } else {
            fabTranslate.setVisibility(View.GONE);
        }

        fabTranslate.setOnClickListener(v -> {
            if (isTranslated) {
                // Revert to original
                tvTitle.setText(originalTitle);
                tvDescription.setText(originalDesc);
                fabTranslate.setText("🌐 Translate");
                isTranslated = false;
            } else {
                // Translate
                fabTranslate.setText("⏳ Translating...");
                fabTranslate.setEnabled(false);

                translatorManager.translate(originalTitle, targetLang, new TranslatorManager.TranslateCallback() {
                    @Override
                    public void onSuccess(String translatedTitle) {
                        tvTitle.setText(translatedTitle);
                        translatorManager.translate(originalDesc, targetLang, new TranslatorManager.TranslateCallback() {
                            @Override
                            public void onSuccess(String translatedDesc) {
                                tvDescription.setText(translatedDesc);
                                fabTranslate.setText("Original");
                                isTranslated = true;
                                fabTranslate.setEnabled(true);
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(JobDetailActivity.this, "Translation failed", Toast.LENGTH_SHORT).show();
                                fabTranslate.setText("🌐 Translate");
                                fabTranslate.setEnabled(true);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(JobDetailActivity.this, "Translation failed", Toast.LENGTH_SHORT).show();
                        fabTranslate.setText("🌐 Translate");
                        fabTranslate.setEnabled(true);
                    }
                });
            }
        });
    }

    private void initViews() {
        tvTitle        = findViewById(R.id.tvJobTitle);
        tvCustomerName = findViewById(R.id.tvCustomerName);
        tvCategory     = findViewById(R.id.tvCategory);
        tvBudget       = findViewById(R.id.tvBudget);
        tvDescription  = findViewById(R.id.tvDescription);
        tvStatus       = findViewById(R.id.tvStatus);
        tvNoBids       = findViewById(R.id.tvNoBids);

        // New: deadline & attachment views
        layoutDeadline       = findViewById(R.id.layoutDeadline);
        layoutAttachments    = findViewById(R.id.layoutAttachments);
        layoutFileAttachment = findViewById(R.id.layoutFileAttachment);
        tvDeadline           = findViewById(R.id.tvDeadline);
        tvDaysLeft           = findViewById(R.id.tvDaysLeft);
        ivJobImage           = findViewById(R.id.ivJobImage);
        tvFileName           = findViewById(R.id.tvFileName);
        btnOpenFile          = findViewById(R.id.btnOpenFile);

        rvBids = findViewById(R.id.rvBids);
        rvBids.setLayoutManager(new LinearLayoutManager(this));

        layoutSubmitBid = findViewById(R.id.layoutSubmitBid);
        etBidAmount     = findViewById(R.id.etBidAmount);
        etProposal      = findViewById(R.id.etProposal);
        btnSubmitBid    = findViewById(R.id.btnSubmitBid);

        fabTranslate    = findViewById(R.id.fabTranslate);
    }

    private void unpackIntent() {
        jobId           = getIntent().getStringExtra("job_id");
        
        originalTitle = getIntent().getStringExtra("job_title");
        if (originalTitle == null) originalTitle = "";
        tvTitle.setText(originalTitle);
        
        originalDesc = getIntent().getStringExtra("job_desc");
        if (originalDesc == null) originalDesc = "";
        tvDescription.setText(originalDesc);
        
        tvCategory.setText(getIntent().getStringExtra("job_category"));
        jobStatus       = getIntent().getStringExtra("job_status");

        jobCustomerName = getIntent().getStringExtra("job_customer_nm");
        tvCustomerName.setText("Posted by: " + (jobCustomerName != null ? jobCustomerName : "Customer"));

        double budget = getIntent().getDoubleExtra("job_budget", 0);
        tvBudget.setText(String.format(Locale.getDefault(), "₹%.0f", budget));

        // ── New: deadline ───────────────────────────────────────────────
        jobDeadline = getIntent().getLongExtra("job_deadline", 0);
        if (jobDeadline > 0) {
            layoutDeadline.setVisibility(View.VISIBLE);
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault());
            tvDeadline.setText(sdf.format(new Date(jobDeadline)));

            // Days remaining
            long now = System.currentTimeMillis();
            long diffMs = jobDeadline - now;
            long diffDays = diffMs / (1000 * 60 * 60 * 24);
            if (diffDays < 0) {
                tvDaysLeft.setText("Overdue");
                tvDaysLeft.setTextColor(getColor(android.R.color.holo_red_dark));
            } else if (diffDays == 0) {
                tvDaysLeft.setText("Due today");
            } else {
                tvDaysLeft.setText(diffDays + (diffDays == 1 ? " day left" : " days left"));
            }
        }

        // ── New: image / file attachments ───────────────────────────────
        jobImageUrl      = getIntent().getStringExtra("job_image_url");
        jobAttachmentUrl = getIntent().getStringExtra("job_attachment_url");
        jobAttachmentName= getIntent().getStringExtra("job_attachment_name");

        boolean hasImage = jobImageUrl      != null && !jobImageUrl.isEmpty();
        boolean hasFile  = jobAttachmentUrl != null && !jobAttachmentUrl.isEmpty();

        if (hasImage || hasFile) {
            layoutAttachments.setVisibility(View.VISIBLE);

            if (hasImage) {
                ivJobImage.setVisibility(View.VISIBLE);
                Glide.with(this).load(jobImageUrl).into(ivJobImage);
            }

            if (hasFile) {
                layoutFileAttachment.setVisibility(View.VISIBLE);
                tvFileName.setText(jobAttachmentName != null && !jobAttachmentName.isEmpty()
                        ? jobAttachmentName : "View Attachment");
                btnOpenFile.setOnClickListener(v -> {
                    try {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse(jobAttachmentUrl));
                        startActivity(browserIntent);
                    } catch (Exception e) {
                        Toast.makeText(this, "Cannot open file", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    private void loadBids() {
        if (bidsListener != null) {
            bidsListener.remove();
        }
        bidsListener = repo.listenBidsForJob(jobId, new FirebaseRepository.Callback<List<Bid>>() {
            @Override
            public void onSuccess(List<Bid> bids) {
                if (bids == null || bids.isEmpty()) {
                    tvNoBids.setVisibility(View.VISIBLE);
                    rvBids.setVisibility(View.GONE);
                } else {
                    tvNoBids.setVisibility(View.GONE);
                    rvBids.setVisibility(View.VISIBLE);
                    boolean isJobOpen = "open".equalsIgnoreCase(jobStatus);
                    
                    if (adapter == null) {
                        adapter = new BidAdapter(bids, isCustomer, isJobOpen, session.getUserId(), new BidAdapter.OnBidActionListener() {
                            @Override
                            public void onAcceptBid(Bid bid) {
                                acceptBid(bid);
                            }

                            @Override
                            public void onChatBid(Bid bid) {
                                chatWithBidder(bid);
                            }
                        });
                        rvBids.setAdapter(adapter);
                    } else {
                        adapter.updateBids(bids, isJobOpen);
                    }

                    // If provider has already bid, hide the submit form
                    if (!isCustomer) {
                        boolean hasBid = false;
                        for (Bid b : bids) {
                            if (b.getProviderId().equals(session.getUserId())) {
                                hasBid = true;
                                break;
                            }
                        }
                        layoutSubmitBid.setVisibility(hasBid ? View.GONE : View.VISIBLE);
                    }
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(JobDetailActivity.this, "Failed to load bids", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitBid() {
        String amountStr = etBidAmount.getText().toString().trim();
        String proposal  = etProposal.getText().toString().trim();

        if (TextUtils.isEmpty(amountStr) || TextUtils.isEmpty(proposal)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double amt = 0;
        try { amt = Double.parseDouble(amountStr); } 
        catch (NumberFormatException e) { etBidAmount.setError("Invalid amount"); return; }

        btnSubmitBid.setEnabled(false);
        btnSubmitBid.setText("Submitting...");

        Bid bid = new Bid(jobId, session.getUserId(), session.getUserName(), amt, proposal);

        repo.createBid(bid, new FirebaseRepository.Callback<String>() {
            @Override
            public void onSuccess(String bidId) {
                Toast.makeText(JobDetailActivity.this, "Proposal submitted!", Toast.LENGTH_SHORT).show();
                layoutSubmitBid.setVisibility(View.GONE);
                loadBids(); // Refresh the list
            }

            @Override
            public void onError(String error) {
                btnSubmitBid.setEnabled(true);
                btnSubmitBid.setText("Submit Proposal");
                Toast.makeText(JobDetailActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void acceptBid(Bid bid) {
        btnSubmitBid.setEnabled(false); // Prevent double clicks
        
        repo.acceptBidAndCreateBooking(jobId, bid, new FirebaseRepository.Callback<String>() {
            @Override
            public void onSuccess(String newBookingId) {
                Toast.makeText(JobDetailActivity.this, 
                    "Accepted! Redirecting to payment...", 
                    Toast.LENGTH_SHORT).show();
                
                jobStatus = "awarded";
                tvStatus.setText("AWARDED");
                loadBids(); // Refresh UI to hide accept buttons

                // Launch Payment Activity
                Intent intent = new Intent(JobDetailActivity.this, PaymentActivity.class);
                intent.putExtra("booking_id", newBookingId);
                intent.putExtra("provider_id", bid.getProviderId());
                intent.putExtra("provider_name", bid.getProviderName());
                intent.putExtra("job_title", tvTitle.getText().toString());
                intent.putExtra("amount", bid.getBidAmount());
                startActivity(intent);
            }

            @Override
            public void onError(String error) {
                btnSubmitBid.setEnabled(true);
                Toast.makeText(JobDetailActivity.this, "Failed to accept bid: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void chatWithBidder(Bid bid) {
        String partnerId;
        String partnerName;
        
        if (isCustomer) {
            // Customer chats with the provider who made the bid
            partnerId = bid.getProviderId();
            partnerName = bid.getProviderName();
        } else {
            // Provider chats with the job's customer
            partnerId = getIntent().getStringExtra("job_customer_id");
            partnerName = jobCustomerName; 
            
            if (partnerId == null || partnerId.isEmpty()) {
                Toast.makeText(this, "Customer profile not found", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Intent ci = new Intent(this, com.skillconnect.ChatActivity.class);
        ci.putExtra("chat_partner_id", partnerId);
        ci.putExtra("chat_partner_name", partnerName != null ? partnerName : "User");
        startActivity(ci);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bidsListener != null) {
            bidsListener.remove();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
