package com.skillconnect;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.skillconnect.adapters.JobAdapter;
import com.skillconnect.data.FirebaseRepository;
import com.skillconnect.data.SessionManager;
import com.skillconnect.models.JobPost;
import java.util.ArrayList;
import java.util.List;

/**
 * Shows ALL open jobs to providers so they can bid.
 * No category-filter—providers can bid on any job they like.
 */
public class ProviderJobsActivity extends AppCompatActivity {

    private RecyclerView rvJobs;
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;
    private JobAdapter adapter;
    private FirebaseRepository repo;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_jobs);

        repo    = FirebaseRepository.getInstance();
        session = new SessionManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Available Jobs");
        }

        rvJobs      = findViewById(R.id.rvJobs);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        progressBar = findViewById(R.id.progressBar);

        rvJobs.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadJobs();
    }

    private void loadJobs() {
        progressBar.setVisibility(View.VISIBLE);
        rvJobs.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);

        // Fetch ALL jobs; filter to 'open' status only
        repo.getAllJobPosts(new FirebaseRepository.Callback<List<JobPost>>() {
            @Override
            public void onSuccess(List<JobPost> result) {
                progressBar.setVisibility(View.GONE);

                List<JobPost> openJobs = new ArrayList<>();
                if (result != null) {
                    for (JobPost j : result) {
                        if ("open".equalsIgnoreCase(j.getStatus())) {
                            openJobs.add(j);
                        }
                    }
                }

                if (openJobs.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                } else {
                    rvJobs.setVisibility(View.VISIBLE);
                    if (adapter == null) {
                        adapter = new JobAdapter(openJobs, job -> openJobDetail(job));
                        rvJobs.setAdapter(adapter);
                    } else {
                        adapter.updateJobs(openJobs);
                    }
                }
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                layoutEmpty.setVisibility(View.VISIBLE);
                Toast.makeText(ProviderJobsActivity.this,
                        "Failed to load jobs: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openJobDetail(JobPost job) {
        Intent i = new Intent(this, JobDetailActivity.class);
        i.putExtra("job_id",             job.getDocumentId());
        i.putExtra("job_title",          job.getTitle());
        i.putExtra("job_desc",           job.getDescription());
        i.putExtra("job_budget",         job.getBudget());
        i.putExtra("job_category",       job.getCategory());
        i.putExtra("job_customer_id",    job.getCustomerId());
        i.putExtra("job_customer_nm",    job.getCustomerName());
        i.putExtra("job_status",         job.getStatus());
        i.putExtra("job_deadline",       job.getDeadline());
        i.putExtra("job_image_url",      job.getImageUrl()        != null ? job.getImageUrl()        : "");
        i.putExtra("job_attachment_url", job.getAttachmentUrl()   != null ? job.getAttachmentUrl()   : "");
        i.putExtra("job_attachment_name",job.getAttachmentName()  != null ? job.getAttachmentName()  : "");
        startActivity(i);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
