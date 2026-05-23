package com.eiken.magicwords;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String TAG = "EikenTTS";
    private static final String ACTION_TTS_SETTINGS = "com.android.settings.TTS_SETTINGS";
    private WebView webView;
    private TextToSpeech textToSpeech;
    private boolean ttsReady = false;
    private String ttsStatus = "Initializing pronunciation...";
    private String ttsDebugInfo = "TTS debug not ready yet.";
    private final List<String> ttsEngineQueue = new ArrayList<>();
    private int ttsEngineIndex = 0;
    private String requestedEngine = "not-started";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initTextToSpeechQueue();

        webView = new WebView(this);
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new AndroidTtsBridge(), "AndroidTTS");

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        setContentView(webView);
        webView.loadUrl("file:///android_asset/EikenStudy.html");
    }

    private void initTextToSpeechQueue() {
        ttsEngineQueue.clear();
        addEngineCandidate("com.google.android.tts");
        addEngineCandidate(null);

        Intent intent = new Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE);
        List<ResolveInfo> services = getPackageManager().queryIntentServices(intent, 0);
        for (ResolveInfo service : services) {
            if (service.serviceInfo != null) {
                addEngineCandidate(service.serviceInfo.packageName);
            }
        }

        ttsEngineIndex = 0;
        appendTtsDebug("discovered engines=" + describeEngineQueue());
        tryNextTtsEngine("startup");
    }

    private void addEngineCandidate(String engine) {
        for (String candidate : ttsEngineQueue) {
            if (candidate == null && engine == null) return;
            if (candidate != null && candidate.equals(engine)) return;
        }
        ttsEngineQueue.add(engine);
    }

    private void tryNextTtsEngine(String reason) {
        if (ttsEngineIndex >= ttsEngineQueue.size()) {
            updateTtsStatus(false, "TTS engine initialization failed.", "all engines failed; candidates=" + describeEngineQueue() + ", reason=" + reason);
            return;
        }

        String engine = ttsEngineQueue.get(ttsEngineIndex);
        ttsEngineIndex += 1;
        initTextToSpeech(engine, reason);
    }

    private void initTextToSpeech(String engine, String reason) {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        requestedEngine = describeEngine(engine);
        updateTtsStatus(false, "Initializing pronunciation...", "init engine=" + requestedEngine + ", reason=" + reason);
        textToSpeech = new TextToSpeech(this, status -> {
            appendTtsDebug("init result=" + status + ", requested=" + requestedEngine + ", default=" + getDefaultEngineName());
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        appendTtsDebug("utterance start id=" + utteranceId);
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        appendTtsDebug("utterance done id=" + utteranceId);
                    }

                    @Override
                    public void onError(String utteranceId) {
                        appendTtsDebug("utterance error id=" + utteranceId);
                    }
                });
                configureEnglishVoice(engine);
            } else {
                appendTtsDebug("init failed status=" + status + ", requested=" + requestedEngine);
                tryNextTtsEngine("init failed status=" + status);
            }
        }, engine);
    }

    private void configureEnglishVoice(String engine) {
        Locale[] candidates = new Locale[] { Locale.US, Locale.ENGLISH, Locale.UK };
        for (Locale locale : candidates) {
            int result = textToSpeech.setLanguage(locale);
            appendTtsDebug("setLanguage " + locale.toLanguageTag() + " result=" + result + ", requested=" + requestedEngine + ", default=" + getDefaultEngineName());
            if (result >= TextToSpeech.LANG_AVAILABLE) {
                textToSpeech.setSpeechRate(0.85f);
                updateTtsStatus(true, "Ready: " + locale.toLanguageTag(), "selected locale=" + locale.toLanguageTag() + ", requested=" + requestedEngine + ", default=" + getDefaultEngineName());
                return;
            }
        }

        int fallbackResult = textToSpeech.setLanguage(Locale.ENGLISH);
        textToSpeech.setSpeechRate(0.85f);
        updateTtsStatus(true, "Ready: default TTS engine", "fallback locale result=" + fallbackResult + ", requested=" + requestedEngine + ", default=" + getDefaultEngineName());
    }

    private void updateTtsStatus(boolean ready, String status, String debug) {
        ttsReady = ready;
        ttsStatus = status;
        ttsDebugInfo = status + " | " + debug;
        Log.d(TAG, ttsDebugInfo);
    }

    private void appendTtsDebug(String debug) {
        ttsDebugInfo = ttsStatus + " | " + debug;
        Log.d(TAG, ttsDebugInfo);
    }

    private String describeEngine(String engine) {
        return engine == null ? "system-default" : engine;
    }

    private String getDefaultEngineName() {
        if (textToSpeech == null) return "none";
        String engine = textToSpeech.getDefaultEngine();
        return engine == null ? "unknown" : engine;
    }

    private String describeEngineQueue() {
        if (ttsEngineQueue.isEmpty()) return "none";
        List<String> names = new ArrayList<>();
        for (String engine : ttsEngineQueue) {
            names.add(describeEngine(engine));
        }
        return names.toString();
    }

    private void openTextToSpeechSettings() {
        Intent intent = new Intent(ACTION_TTS_SETTINGS);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            appendTtsDebug("TTS settings activity not found");
            Toast.makeText(this, "Android TTS settings could not be opened.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }

    private class AndroidTtsBridge {
        @JavascriptInterface
        public String speak(String text) {
            if (!ttsReady || text == null || text.trim().isEmpty()) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, ttsDebugInfo, Toast.LENGTH_LONG).show());
                return ttsStatus;
            }
            int result = textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "eiken-tts");
            appendTtsDebug("speak result=" + result + ", text=" + text + ", requested=" + requestedEngine + ", default=" + getDefaultEngineName());
            return result == TextToSpeech.SUCCESS ? "ok" : "speak failed: " + result;
        }

        @JavascriptInterface
        public String getStatus() {
            return ttsStatus;
        }

        @JavascriptInterface
        public String getDebugInfo() {
            return ttsDebugInfo;
        }

        @JavascriptInterface
        public void openTtsSettings() {
            runOnUiThread(MainActivity.this::openTextToSpeechSettings);
        }
    }
}
