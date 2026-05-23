package com.eiken.magicwords;

import android.app.Activity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {
    private WebView webView;
    private TextToSpeech textToSpeech;
    private boolean ttsReady = false;
    private String ttsStatus = "Initializing pronunciation...";
    private boolean triedDefaultEngine = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initTextToSpeech("com.google.android.tts");

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

    private void initTextToSpeech(String engine) {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                configureEnglishVoice(engine);
            } else if (!triedDefaultEngine && engine != null) {
                triedDefaultEngine = true;
                initTextToSpeech(null);
            } else {
                ttsReady = false;
                ttsStatus = "TTS engine initialization failed.";
            }
        }, engine);
    }

    private void configureEnglishVoice(String engine) {
        Locale[] candidates = new Locale[] { Locale.US, Locale.ENGLISH, Locale.UK };
        for (Locale locale : candidates) {
            int result = textToSpeech.setLanguage(locale);
            if (result >= TextToSpeech.LANG_AVAILABLE) {
                textToSpeech.setSpeechRate(0.85f);
                ttsReady = true;
                ttsStatus = "Ready: " + locale.toLanguageTag();
                return;
            }
        }
        ttsReady = false;
        ttsStatus = "English voice data is missing or unsupported.";
        if (!triedDefaultEngine && engine != null) {
            triedDefaultEngine = true;
            initTextToSpeech(null);
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
                runOnUiThread(() -> Toast.makeText(MainActivity.this, ttsStatus, Toast.LENGTH_LONG).show());
                return ttsStatus;
            }
            runOnUiThread(() -> textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "eiken-tts"));
            return "ok";
        }

        @JavascriptInterface
        public String getStatus() {
            return ttsStatus;
        }
    }
}
