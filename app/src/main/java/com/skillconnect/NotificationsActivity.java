package com.skillconnect;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.skillconnect.adapters.NotificationAdapter;
import com.skillconnect.data.FirebaseRepository;
import com.skillconnect.data.SessionManager;
import com.skillconnect.models.Notification;
import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity
        implements NotificationAdapter.OnNotificationClickListener {

    private RecyclerView rvNotifications;
    private View layoutEmpty;
    private MaterialButton btnMarkAllRead;
    private NotificationAdapter adapter;
    private List<Notification> notifList = new ArrayList<>();
    private FirebaseRepository repo;
    private SessionManager sessionManager;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        repo           = FirebaseRepository.getInstance();
        sessionManager = new SessionManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvNotifications = findViewById(R.id.rvNotifications);
        layoutEmpty     = findViewById(R.id.layoutEmpty);
        btnMarkAllRead  = findViewById(R.id.btnMarkAllRead);

        adapter = new NotificationAdapter(notifList, this);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);

        btnMarkAllRead.setOnClickListener(v -> markAllRead());

        loadNotifications();
    }

    private void loadNotifications() {
        String uid = sessionManager.getUserId();
        repo.getNotificationsForUser(uid, new FirebaseRepository.Callback<List<Notification>>() {
            @Override public void onSuccess(List<Notification> result) {
                notifList.clear();
                notifList.addAll(result);
                adapter.notifyDataSetChanged();
                boolean empty = notifList.isEmpty();
                layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                rvNotifications.setVisibility(empty ? View.GONE : View.VISIBLE);
            }
            @Override public void onError(String e) {
                layoutEmpty.setVisibility(View.VISIBLE);
                rvNotifications.setVisibility(View.GONE);
            }
        });
    }

    private void markAllRead() {
        String uid = sessionManager.getUserId();
        repo.markAllNotificationsRead(uid, result -> {
            for (Notification n : notifList) n.setRead(true);
            adapter.notifyDataSetChanged();
        });
    }

    @Override public void onNotificationClick(Notification n) {
        if (!n.isRead()) {
            repo.markNotificationRead(n.getId(), null);
            n.setRead(true);
            adapter.notifyDataSetChanged();
        }
    }

    @Override public void onNotificationDelete(Notification n, int position) {
        repo.deleteNotification(n.getId(), null);
        adapter.removeItem(position);
        if (notifList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvNotifications.setVisibility(View.GONE);
        }
    }
}
