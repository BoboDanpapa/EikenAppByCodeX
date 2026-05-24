package com.eiken.magicwords;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.LiveGenerativeModel;
import com.google.firebase.ai.java.LiveModelFutures;
import com.google.firebase.ai.java.LiveSessionFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerativeBackend;
import com.google.firebase.ai.type.LiveGenerationConfig;
import com.google.firebase.ai.type.ResponseModality;
import com.google.firebase.ai.type.SpeechConfig;
import com.google.firebase.ai.type.Transcription;
import com.google.firebase.ai.type.Voice;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlin.Unit;

public class MainActivity extends Activity {
    private static final String TAG = "EikenTTS";
    private static final String GEMINI_TAG = "EikenGeminiLive";
    private static final String ACTION_TTS_SETTINGS = "com.android.settings.TTS_SETTINGS";
    private static final String GEMINI_LIVE_MODEL = "gemini-live-2.5-flash-preview";
    private static final int REQUEST_RECORD_AUDIO = 4001;
    private WebView webView;
    private TextToSpeech textToSpeech;
    private boolean ttsReady = false;
    private String ttsStatus = "Initializing pronunciation...";
    private String ttsDebugInfo = "TTS debug not ready yet.";
    private final List<String> ttsEngineQueue = new ArrayList<>();
    private int ttsEngineIndex = 0;
    private String requestedEngine = "not-started";
    private final ExecutorService geminiExecutor = Executors.newSingleThreadExecutor();
    private LiveSessionFutures geminiSession;
    private boolean geminiConversationActive = false;
    private int geminiRequestId = 0;
    private String pendingGeminiContextJson;
    private int teacherTurnCount = 0;
    private int teacherMaxTurns = 5;
    private boolean waitingForStudentInput = false;
    private boolean answerSeenForCurrentInput = false;
    private String lastInputText = "";
    private String lastCountedInputText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initTextToSpeechQueue();

        webView = new WebView(this);
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new AndroidTtsBridge(), "AndroidTTS");
        webView.addJavascriptInterface(new AndroidGeminiBridge(), "AndroidGemini");

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
            stopGeminiConversation("back pressed");
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        stopGeminiConversation("activity destroyed");
        geminiExecutor.shutdownNow();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_RECORD_AUDIO) return;
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (granted && pendingGeminiContextJson != null) {
            String context = pendingGeminiContextJson;
            pendingGeminiContextJson = null;
            startGeminiConversation(context);
        } else {
            pendingGeminiContextJson = null;
            notifyGeminiStatus("permission_denied", "マイクの許可が必要です。Androidの設定でマイクを許可してください。");
        }
    }

    private boolean hasRecordAudioPermission() {
        return checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestRecordAudioPermission(String contextJson) {
        pendingGeminiContextJson = contextJson;
        runOnUiThread(() -> requestPermissions(new String[] { Manifest.permission.RECORD_AUDIO }, REQUEST_RECORD_AUDIO));
    }

    private void startGeminiConversation(String contextJson) {
        if (geminiConversationActive) {
            notifyGeminiStatus("active", "すでに先生が聞いています。終わるときは「とめる」を押してね。");
            return;
        }
        if (!hasRecordAudioPermission()) {
            notifyGeminiStatus("permission_required", "マイクの許可を確認しています。");
            requestRecordAudioPermission(contextJson);
            return;
        }
        if (FirebaseApp.getApps(this).isEmpty()) {
            notifyGeminiStatus("error", "Firebase設定が見つかりません。android-app/app/google-services.json を追加してからビルドしてください。");
            return;
        }

        notifyGeminiStatus("connecting", "先生につないでいます...");
        stopGeminiConversation("new Gemini conversation");
        geminiConversationActive = true;
        final int requestId = ++geminiRequestId;

        try {
            JSONObject context = new JSONObject(contextJson);
            resetTeacherTurnState(context.optInt("turns", 0), context.optInt("maxTurns", 5));
            Content systemInstruction = new Content.Builder()
                    .addText(buildGeminiSystemInstruction(context))
                    .build();
            LiveGenerativeModel liveModel = FirebaseAI.getInstance(GenerativeBackend.googleAI()).liveModel(
                    GEMINI_LIVE_MODEL,
                    new LiveGenerationConfig.Builder()
                            .setResponseModality(ResponseModality.AUDIO)
                            .setSpeechConfig(new SpeechConfig(new Voice("Leda")))
                            .build(),
                    null,
                    systemInstruction
            );
            LiveModelFutures liveModelFutures = LiveModelFutures.from(liveModel);
            ListenableFuture<LiveSessionFutures> sessionFuture = liveModelFutures.connect();
            Futures.addCallback(sessionFuture, new FutureCallback<LiveSessionFutures>() {
                @Override
                public void onSuccess(LiveSessionFutures session) {
                    if (requestId != geminiRequestId) return;
                    geminiSession = session;
                    ListenableFuture<?> startFuture = session.startAudioConversation((input, output) -> {
                        handleGeminiTranscript(input, output);
                        return Unit.INSTANCE;
                    }, false);
                    Futures.addCallback(startFuture, new FutureCallback<Object>() {
                        @Override
                        public void onSuccess(Object result) {
                            if (requestId != geminiRequestId) return;
                            geminiConversationActive = true;
                            notifyGeminiStatus("started", "聞いています。質問は日本語でも英語でも大丈夫です。");
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            if (requestId != geminiRequestId) return;
                            Log.e(GEMINI_TAG, "Could not start audio conversation", t);
                            notifyGeminiStatus("error", "Gemini Liveを開始できませんでした: " + safeError(t));
                            stopGeminiConversation("start failed");
                        }
                    }, geminiExecutor);
                }

                @Override
                public void onFailure(Throwable t) {
                    if (requestId != geminiRequestId) return;
                    Log.e(GEMINI_TAG, "Could not connect Gemini Live", t);
                    notifyGeminiStatus("error", "Gemini Liveにつなげませんでした: " + safeError(t));
                    stopGeminiConversation("connect failed");
                }
            }, geminiExecutor);
        } catch (JSONException e) {
            geminiConversationActive = false;
            notifyGeminiStatus("error", "単語データを読み取れませんでした。");
        } catch (Throwable t) {
            geminiConversationActive = false;
            Log.e(GEMINI_TAG, "Gemini Live setup failed", t);
            notifyGeminiStatus("error", "Gemini Liveの準備に失敗しました: " + safeError(t));
        }
    }

    private void stopGeminiConversation(String reason) {
        geminiRequestId++;
        LiveSessionFutures session = geminiSession;
        geminiSession = null;
        geminiConversationActive = false;
        if (session == null) return;
        try {
            ListenableFuture<?> stopFuture = session.stopAudioConversation();
            Futures.addCallback(stopFuture, new FutureCallback<Object>() {
                @Override
                public void onSuccess(Object result) {
                    notifyGeminiStatus("stopped", "先生との会話をとめました。");
                }

                @Override
                public void onFailure(Throwable t) {
                    Log.w(GEMINI_TAG, "Could not stop audio conversation: " + reason, t);
                    notifyGeminiStatus("stopped", "先生との会話をとめました。");
                }
            }, geminiExecutor);
        } catch (Throwable t) {
            Log.w(GEMINI_TAG, "Stop Gemini conversation failed: " + reason, t);
        }
    }

    private void resetGeminiConversation(String reason) {
        stopGeminiConversation(reason);
        resetTeacherTurnState(0, 5);
    }

    private void resetTeacherTurnState(int turns, int maxTurns) {
        teacherTurnCount = Math.max(0, turns);
        teacherMaxTurns = Math.max(1, maxTurns);
        waitingForStudentInput = false;
        answerSeenForCurrentInput = false;
        lastInputText = "";
        lastCountedInputText = "";
    }

    private void handleGeminiTranscript(Transcription input, Transcription output) {
        if (!geminiConversationActive) return;
        String inputText = normalizeTranscript(input);
        String outputText = normalizeTranscript(output);

        if (!inputText.isEmpty()
                && !inputText.equals(lastCountedInputText)
                && (!waitingForStudentInput || !inputText.equals(lastInputText))) {
            waitingForStudentInput = true;
            answerSeenForCurrentInput = false;
            lastInputText = inputText;
        }

        if (!outputText.isEmpty() && waitingForStudentInput && !answerSeenForCurrentInput) {
            answerSeenForCurrentInput = true;
            teacherTurnCount = Math.min(teacherMaxTurns, teacherTurnCount + 1);
            lastCountedInputText = lastInputText;
            notifyGeminiStatus("turn_completed",
                    "質問: " + teacherTurnCount + " / " + teacherMaxTurns,
                    teacherTurnCount);
            waitingForStudentInput = false;
            answerSeenForCurrentInput = false;
            lastInputText = "";

            if (teacherTurnCount >= teacherMaxTurns) {
                notifyGeminiStatus("limit_reached", "よくできました。カードの学習にもどろう！", teacherTurnCount);
                stopGeminiConversation("turn limit reached");
            }
        }
    }

    private String normalizeTranscript(Transcription transcription) {
        if (transcription == null || transcription.getText() == null) return "";
        return transcription.getText().trim().replaceAll("\\s+", " ");
    }

    private String buildGeminiSystemInstruction(JSONObject context) {
        String courseId = context.optString("courseId", "");
        String levelLabel = context.optString("levelLabel", "EIKEN");
        String word = context.optString("word", "");
        String jp = context.optString("jp", "");
        String enSent = context.optString("enSent", "");
        String jpSent = context.optString("jpSent", "");
        boolean pre2 = "pre2".equals(courseId);
        String levelStyle = pre2
                ? "Use very short sentences, common words, and a slow, gentle speaking style for an EIKEN Pre-2 child learner."
                : "Use clear child-friendly English. You may use slightly richer vocabulary, but keep explanations easy for an EIKEN Grade 2 learner.";
        return "You are an English learning teacher for a child using a vocabulary card app. "
                + "The student may ask in Japanese or English, but you MUST answer in English only. "
                + levelStyle + " "
                + "Only discuss English learning for the current card: meaning, usage, pronunciation, example sentences, similar words, exam understanding, and simple practice. "
                + "Do not chat casually. Do not answer unrelated questions. If the student goes off topic, say: I am your English teacher. Let's talk about this word. "
                + "Keep every answer brief and encouraging. "
                + "After the fifth exchange, say: Great work. Now let's go back to studying. "
                + "Current course: " + levelLabel + ". "
                + "Current word or phrase: " + word + ". "
                + "Japanese meaning for context only: " + jp + ". "
                + "Example sentence: " + enSent + ". "
                + "Japanese example meaning for context only: " + jpSent + ".";
    }

    private void notifyGeminiStatus(String status, String message) {
        notifyGeminiStatus(status, message, -1);
    }

    private void notifyGeminiStatus(String status, String message, int turns) {
        Log.d(GEMINI_TAG, status + " | " + message);
        if (webView == null) return;
        String script = "window.onGeminiTeacherStatus && window.onGeminiTeacherStatus("
                + JSONObject.quote(status) + ","
                + JSONObject.quote(message) + ","
                + turns + ");";
        runOnUiThread(() -> webView.evaluateJavascript(script, null));
    }

    private String safeError(Throwable t) {
        if (t == null) return "unknown error";
        String message = t.getMessage();
        return message == null || message.trim().isEmpty() ? t.getClass().getSimpleName() : message;
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

    private class AndroidGeminiBridge {
        @JavascriptInterface
        public String startConversation(String contextJson) {
            runOnUiThread(() -> startGeminiConversation(contextJson));
            return "starting";
        }

        @JavascriptInterface
        public String stopConversation() {
            runOnUiThread(() -> stopGeminiConversation("user stopped"));
            return "stopping";
        }

        @JavascriptInterface
        public String resetConversation() {
            runOnUiThread(() -> resetGeminiConversation("word changed"));
            return "resetting";
        }

        @JavascriptInterface
        public String getStatus() {
            return geminiConversationActive ? "active" : "idle";
        }
    }
}
