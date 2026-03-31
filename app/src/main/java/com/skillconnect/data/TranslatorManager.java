package com.skillconnect.data;

import android.util.Log;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.TranslateRemoteModel;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton that wraps ML Kit On-Device Translation.
 * Supports English → Hindi, English → Gujarati.
 * Models (~30MB each) are downloaded on first use and cached.
 */
public class TranslatorManager {

    private static final String TAG = "TranslatorManager";
    private static TranslatorManager instance;

    private final Map<String, Translator> translators = new HashMap<>();
    private final Map<String, Boolean> modelReady = new HashMap<>();

    public interface TranslateCallback {
        void onSuccess(String translatedText);
        void onError(String error);
    }

    public interface DownloadCallback {
        void onComplete(boolean success, String error);
    }

    private TranslatorManager() {}

    public static synchronized TranslatorManager getInstance() {
        if (instance == null) instance = new TranslatorManager();
        return instance;
    }

    /**
     * Get ML Kit language code from our app language code.
     */
    private String mlLangCode(String langCode) {
        switch (langCode) {
            case "hi": return TranslateLanguage.HINDI;
            case "gu": return TranslateLanguage.GUJARATI;
            default:   return TranslateLanguage.ENGLISH;
        }
    }

    /**
     * Download the translation model for the target language.
     * Call this when user selects a language in Settings.
     */
    public void downloadModel(String targetLang, DownloadCallback callback) {
        if ("en".equals(targetLang)) {
            modelReady.put(targetLang, true);
            if (callback != null) callback.onComplete(true, null);
            return;
        }

        String mlLang = mlLangCode(targetLang);
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(mlLang)
                .build();

        Translator translator = Translation.getClient(options);
        translators.put(targetLang, translator);

        DownloadConditions conditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();

        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(v -> {
                    Log.i(TAG, "Model downloaded for: " + targetLang);
                    modelReady.put(targetLang, true);
                    if (callback != null) callback.onComplete(true, null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Model download failed: " + e.getMessage());
                    modelReady.put(targetLang, false);
                    if (callback != null) callback.onComplete(false, e.getMessage());
                });
    }

    /**
     * Check if a model is ready for translation.
     */
    public boolean isModelReady(String targetLang) {
        if ("en".equals(targetLang)) return true;
        Boolean ready = modelReady.get(targetLang);
        return ready != null && ready;
    }

    /**
     * Translate text from English to the target language.
     * Model must be downloaded first via downloadModel().
     */
    public void translate(String text, String targetLang, TranslateCallback callback) {
        if ("en".equals(targetLang) || text == null || text.isEmpty()) {
            callback.onSuccess(text);
            return;
        }

        Translator translator = translators.get(targetLang);
        if (translator == null) {
            // Try creating one on the fly
            String mlLang = mlLangCode(targetLang);
            TranslatorOptions options = new TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.ENGLISH)
                    .setTargetLanguage(mlLang)
                    .build();
            translator = Translation.getClient(options);
            translators.put(targetLang, translator);
        }

        translator.translate(text)
                .addOnSuccessListener(translated -> {
                    Log.d(TAG, "Translated: " + text.substring(0, Math.min(30, text.length())) + "...");
                    callback.onSuccess(translated);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Translation failed: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Delete a downloaded model to free space.
     */
    public void deleteModel(String targetLang) {
        if ("en".equals(targetLang)) return;
        String mlLang = mlLangCode(targetLang);
        TranslateRemoteModel model = new TranslateRemoteModel.Builder(mlLang).build();
        RemoteModelManager.getInstance().deleteDownloadedModel(model);
        translators.remove(targetLang);
        modelReady.remove(targetLang);
    }

    /** Release all translator resources */
    public void close() {
        for (Translator t : translators.values()) t.close();
        translators.clear();
        modelReady.clear();
    }
}
