package com.skillconnect.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Stores the user's preferred app language.
 * "en" = English (default), "hi" = Hindi, "gu" = Gujarati
 */
public class LanguagePreferenceManager {

    private static final String PREF_NAME = "SkillConnectLangPrefs";
    private static final String KEY_LANGUAGE = "pref_app_language";

    private final SharedPreferences prefs;

    public LanguagePreferenceManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getLanguage() {
        return prefs.getString(KEY_LANGUAGE, "en");
    }

    public void setLanguage(String langCode) {
        prefs.edit().putString(KEY_LANGUAGE, langCode).apply();
    }

    public boolean isTranslationNeeded() {
        return !"en".equals(getLanguage());
    }

    /** Human-readable label for the current language */
    public String getLanguageLabel() {
        switch (getLanguage()) {
            case "hi": return "हिन्दी";
            case "gu": return "ગુજરાતી";
            default:   return "English";
        }
    }
}
