package com.skillconnect;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.firebase.auth.FirebaseUser;
import com.skillconnect.data.FirebaseRepository;
import com.skillconnect.data.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION  = 2000;
    private static final int ANIM_DURATION    = 900;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager session = new SessionManager(this);
        AppCompatDelegate.setDefaultNightMode(session.isDarkMode()
                ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_splash);

        ImageView ivLogo    = findViewById(R.id.ivLogo);
        TextView  tvAppName = findViewById(R.id.tvAppName);
        TextView  tvTagline = findViewById(R.id.tvTagline);

        // Animate logo
        ivLogo.setScaleX(0f); ivLogo.setScaleY(0f); ivLogo.setAlpha(0f);
        AnimatorSet logo = new AnimatorSet();
        ObjectAnimator sx = ObjectAnimator.ofFloat(ivLogo, "scaleX", 0f, 1f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(ivLogo, "scaleY", 0f, 1f);
        ObjectAnimator sa = ObjectAnimator.ofFloat(ivLogo, "alpha",  0f, 1f);
        sx.setInterpolator(new OvershootInterpolator(1.5f));
        sy.setInterpolator(new OvershootInterpolator(1.5f));
        logo.playTogether(sx, sy, sa);
        logo.setDuration(ANIM_DURATION);
        logo.start();

        // Fade in text
        if (tvAppName != null) { tvAppName.setAlpha(0f); tvAppName.animate().alpha(1f).setStartDelay(400).setDuration(500).start(); }
        if (tvTagline != null) { tvTagline.setAlpha(0f); tvTagline.animate().alpha(1f).setStartDelay(700).setDuration(500).start(); }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Use Firebase Auth to check session — not SharedPreferences
            FirebaseUser fbUser = FirebaseRepository.getInstance().getCurrentUser();
            if (fbUser != null && session.isLoggedIn()) {
                // Verify user profile exists in Firestore to avoid crash loop
                FirebaseRepository.getInstance().getUserById(fbUser.getUid(),
                    new FirebaseRepository.Callback<com.skillconnect.models.User>() {
                        @Override
                        public void onSuccess(com.skillconnect.models.User user) {
                            // Profile found — safe to go to main screen
                            startActivity(new Intent(SplashActivity.this, MainActivity.class));
                            finish();
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        }
                        @Override
                        public void onError(String error) {
                            // Profile missing or Firestore error — reset session to break crash loop
                            FirebaseRepository.getInstance().logout();
                            session.logout();
                            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                            finish();
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        }
                    });
            } else {
                // Clear stale session
                FirebaseRepository.getInstance().logout();
                session.logout();
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        }, SPLASH_DURATION);
    }
}
