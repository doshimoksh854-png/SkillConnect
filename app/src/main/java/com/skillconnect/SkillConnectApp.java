package com.skillconnect;

import android.app.Application;
import android.content.Context;
import androidx.appcompat.app.AppCompatDelegate;
import com.skillconnect.data.SessionManager;

/**
 * Application class — dark mode init only.
 * DB pre-warming no longer needed (Firebase handles its own init).
 */
public class SkillConnectApp extends Application {

    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
        SessionManager session = new SessionManager(this);
        AppCompatDelegate.setDefaultNightMode(
                session.isDarkMode()
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO);
    }

    /** Returns the application-level context, safe to call from any class */
    public static Context getContext() {
        return appContext;
    }
}
