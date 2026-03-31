package com.skillconnect;import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.skillconnect.data.FirebaseRepository;
import com.skillconnect.data.SessionManager;
import com.skillconnect.fragments.BookingsListFragment;
import com.skillconnect.fragments.HomeCustomerFragment;
import com.skillconnect.fragments.HomeProviderFragment;

/**
 * Main screen with bottom navigation
 * Hosts fragments for Home, Search, Bookings, and Profile
 */
public class MainActivity extends AppCompatActivity {

    private String userRole = "customer";
    private String userName = "";
    private String userEmail = "";
    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNavigation;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);

        // Read from session (primary), fall back to intent extras
        userRole = sessionManager.getUserRole();
        userName = sessionManager.getUserName();
        userEmail = sessionManager.getUserEmail();

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("user_role")) {
            userRole = intent.getStringExtra("user_role");
            userName = intent.getStringExtra("user_name");
            userEmail = intent.getStringExtra("user_email");
        }

        // Setup toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setup bottom navigation
        bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                loadFragment(getHomeFragment(), "Home");
                toolbar.setTitle(R.string.app_name);
                return true;
            } else if (id == R.id.nav_search) {
                Intent searchIntent = new Intent(this, SkillListActivity.class);
                startActivity(searchIntent);
                return true;
            } else if (id == R.id.nav_bookings) {
                loadFragment(new BookingsListFragment(), "Bookings");
                toolbar.setTitle(R.string.my_bookings_title);
                return true;
            } else if (id == R.id.nav_chats) {
                loadFragment(new com.skillconnect.fragments.ChatListFragment(), "Chats");
                toolbar.setTitle("Chats");
                return true;
            } else if (id == R.id.nav_profile) {
                Intent profileIntent = new Intent(this, ProfileActivity.class);
                profileIntent.putExtra("user_role", userRole);
                profileIntent.putExtra("user_name", userName);
                profileIntent.putExtra("user_email", userEmail);
                startActivity(profileIntent);
                return true;
            }
            return false;
        });

        // Load initial fragment
        if (savedInstanceState == null) {
            loadFragment(getHomeFragment(), "Home");
        }
    }

    private Fragment getHomeFragment() {
        if ("provider".equals(userRole)) {
            return new HomeProviderFragment();
        }
        return new HomeCustomerFragment();
    }

    private void loadFragment(Fragment fragment, String tag) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        transaction.replace(R.id.fragmentContainer, fragment, tag);
        transaction.commit();
    }

    /**
     * Switch between customer and provider roles
     */
    public void switchRole() {
        userRole = "provider".equals(userRole) ? "customer" : "provider";
        sessionManager.updateRole(userRole);
        loadFragment(getHomeFragment(), "Home");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        
        // Setup badge after menu is created
        updateNotificationBadge();
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_profile) {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.putExtra("user_role", userRole);
            intent.putExtra("user_name", userName);
            intent.putExtra("user_email", userEmail);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_search) {
            Intent searchIntent = new Intent(this, SkillListActivity.class);
            startActivity(searchIntent);
            return true;
        } else if (id == R.id.action_notifications) {
            Intent intent = new Intent(this, NotificationsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh role from session
        userRole = sessionManager.getUserRole();
        userName = sessionManager.getUserName();
        userEmail = sessionManager.getUserEmail();
        
        updateNotificationBadge();
    }
    
    @androidx.annotation.OptIn(markerClass = com.google.android.material.badge.ExperimentalBadgeUtils.class)
    private void updateNotificationBadge() {
        if (toolbar == null || sessionManager.getUserId() == null) return;
        
        FirebaseRepository.getInstance().getUnreadNotificationCount(sessionManager.getUserId(), new FirebaseRepository.Callback<Integer>() {
            @Override
            public void onSuccess(Integer count) {
                if (count > 0) {
                    BadgeDrawable badge = BadgeDrawable.create(MainActivity.this);
                    badge.setNumber(count);
                    badge.setVisible(true);
                    BadgeUtils.attachBadgeDrawable(badge, toolbar, R.id.action_notifications);
                } else {
                    BadgeUtils.detachBadgeDrawable(BadgeDrawable.create(MainActivity.this), toolbar, R.id.action_notifications);
                }
            }
        });
    }

    public String getUserRole() {
        return userRole;
    }
}
