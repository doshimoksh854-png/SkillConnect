package com.skillconnect;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.skillconnect.adapters.JobAdapter;
import com.skillconnect.data.FirebaseRepository;
import com.skillconnect.data.SessionManager;
import com.skillconnect.models.JobPost;
import java.util.ArrayList;
import java.util.List;

public class CustomerJobsActivity extends AppCompatActivity {

    private RecyclerView rvJobs;
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;
    private JobAdapter adapter;
    private FirebaseRepository repo;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_jobs);

        repo = FirebaseRepository.getInstance();
        session = new SessionManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        rvJobs       = findViewById(R.id.rvJobs);
        layoutEmpty  = findViewById(R.id.layoutEmpty);
        progressBar  = findViewById(R.id.progressBar);

        rvJobs.setLayoutManager(new LinearLayoutManager(this));

        FloatingActionButton fabPostJob = findViewById(R.id.fabPostJob);
        fabPostJob.setOnClickListener(v -> {
            startActivity(new Intent(CustomerJobsActivity.this, NewJobActivity.class));
        });
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

        repo.getCustomerJobPosts(session.getUserId(), new FirebaseRepository.Callback<List<JobPost>>() {
            @Override
            public void onSuccess(List<JobPost> result) {
                progressBar.setVisibility(View.GONE);
                if (result == null || result.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                } else {
                    rvJobs.setVisibility(View.VISIBLE);
                    if (adapter == null) {
                        adapter = new JobAdapter(result, job -> openJobDetail(job));
                        rvJobs.setAdapter(adapter);
                    } else {
                        adapter.updateJobs(result);
                    }
                }
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                layoutEmpty.setVisibility(View.VISIBLE);
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
