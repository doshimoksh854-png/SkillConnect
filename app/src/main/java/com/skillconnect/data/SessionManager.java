package com.skillconnect.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

/**
 * Session manager — V4: Uses EncryptedSharedPreferences (AES-256).
 * All session data (userId, role, name, email) is encrypted at rest.
 * Falls back to plain SharedPreferences if encryption fails (old devices).
 */
public class SessionManager {

    private static final String TAG = "SessionManager";
    private static final String PREF_NAME        = "SkillConnectSession";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID      = "user_id";       // String UID
    private static final String KEY_USER_NAME    = "user_name";
    private static final String KEY_USER_EMAIL   = "user_email";
    private static final String KEY_USER_ROLE    = "user_role";
    private static final String KEY_USER_PHONE   = "user_phone";
    private static final String KEY_DARK_MODE    = "dark_mode";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        SharedPreferences encPrefs = null;
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            encPrefs = EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME + "_enc",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            Log.w(TAG, "EncryptedSharedPreferences unavailable, falling back to plain prefs", e);
        }

        if (encPrefs != null) {
            prefs = encPrefs;

            // Migrate data from old unencrypted prefs (one-time)
            SharedPreferences oldPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            if (oldPrefs.getBoolean(KEY_IS_LOGGED_IN, false) && !prefs.getBoolean(KEY_IS_LOGGED_IN, false)) {
                migrateOldPrefs(oldPrefs, prefs);
                oldPrefs.edit().clear().apply(); // Wipe old unencrypted data
            }
        } else {
            prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
        editor = prefs.edit();
    }

    /** One-time migration from plain prefs to encrypted prefs */
    private void migrateOldPrefs(SharedPreferences from, SharedPreferences to) {
        SharedPreferences.Editor toEditor = to.edit();
        toEditor.putBoolean(KEY_IS_LOGGED_IN, from.getBoolean(KEY_IS_LOGGED_IN, false));
        toEditor.putString(KEY_USER_ID,    from.getString(KEY_USER_ID, ""));
        toEditor.putString(KEY_USER_NAME,  from.getString(KEY_USER_NAME, ""));
        toEditor.putString(KEY_USER_EMAIL, from.getString(KEY_USER_EMAIL, ""));
        toEditor.putString(KEY_USER_ROLE,  from.getString(KEY_USER_ROLE, "customer"));
        toEditor.putString(KEY_USER_PHONE, from.getString(KEY_USER_PHONE, ""));
        toEditor.putBoolean(KEY_DARK_MODE, from.getBoolean(KEY_DARK_MODE, false));
        toEditor.apply();
        Log.i(TAG, "Migrated session data to encrypted storage");
    }

    public void createLoginSession(String userId, String name, String email, String role) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID,    userId);
        editor.putString(KEY_USER_NAME,  name);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_ROLE,  role);
        editor.apply();
    }

    public void updateProfile(String name, String email, String phone) {
        editor.putString(KEY_USER_NAME,  name);
        editor.putString(KEY_USER_EMAIL, email);
        if (phone != null) editor.putString(KEY_USER_PHONE, phone);
        editor.apply();
    }

    public boolean isLoggedIn()  { return prefs.getBoolean(KEY_IS_LOGGED_IN, false); }
    /** Returns Firebase Auth UID */
    public String getUserId()    { return prefs.getString(KEY_USER_ID, ""); }
    public String getUserName()  { return prefs.getString(KEY_USER_NAME, ""); }
    public String getUserEmail() { return prefs.getString(KEY_USER_EMAIL, ""); }
    public String getUserRole()  { return prefs.getString(KEY_USER_ROLE, "customer"); }
    public String getUserPhone() { return prefs.getString(KEY_USER_PHONE, ""); }

    public void updateRole(String role) {
        editor.putString(KEY_USER_ROLE, role);
        editor.apply();
    }

    public void logout() {
        boolean darkMode = isDarkMode();
        editor.clear();
        editor.putBoolean(KEY_DARK_MODE, darkMode);
        editor.apply();
    }

    public void setDarkMode(boolean enabled) {
        editor.putBoolean(KEY_DARK_MODE, enabled);
        editor.apply();
    }

    public boolean isDarkMode() { return prefs.getBoolean(KEY_DARK_MODE, false); }
}
