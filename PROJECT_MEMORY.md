# Project Memory

Read this before product, build, release, PWA, or teacher/microphone work.

## File Map

- Android WebView app asset: `android-app/app/src/main/assets/EikenStudy.html`.
- Browser/PWA source: `pwa/`; older root `EikenStudy.html` is not the production app.
- Browser preview: `EikenStudyPreview.html`.
- Android vocab assets: `android-app/app/src/main/assets/eiken_*_words.js`.
- PWA vocab assets: `pwa/eiken_*_words.js`.
- Custom/original vocab backups use `*_custom.js`.
- Character images live in `images/`, `pwa/images/`, and `android-app/app/src/main/assets/images/`.

## Current Release State

- Current official release version: `1.0.0`.
- Current PWA version: `1.0.0`.
- Current Android APK version: `1.0.0`; Android `versionCode`: `25`.
- `v1.0.0` is the first formal release. It includes the latest PWA/Web teacher text fallback and microphone messaging, refreshed PWA icons, and the Android/PWA teacher-limit split: PWA/Web keeps 5 questions per word, while Android APK Gemini Live uses 10 minutes per word and 60 minutes per user per local day.

## Core Product Rules

- Users are local only, without passwords.
- Progress is stored locally per user and per course.
- Course progress, review notes, streak, rank progress, and unlocked gallery entries must stay separate for Grade 4, Grade 3, Pre-2, Grade 2, Pre-1, and Grade 1.
- Quiz state must persist across tab switches. Do not regenerate an unanswered quiz question unless the user answered, continued after level-up, or the saved quiz word is invalid for current course data.
- Study-card order must not be alphabetical. Put common/exam-important words first and mix words/phrases naturally.
- Quiz/challenge order must be independent from study order, non-alphabetical, and not easily traceable by switching tabs.

## Teacher And Mic

- Android teacher uses Firebase AI Logic Gemini Live with `gemini-2.5-flash-native-audio-preview-12-2025` and `ResponseModality.AUDIO`.
- Do not switch Android Live to `gemini-live-2.5-flash-preview`; it can close the Firebase AI Logic WebSocket during connection.
- The current Android APK path uses the Gemini Live transcript callback for question counting. Do not change APK microphone code unless the user explicitly asks for APK work and a real-device APK test is planned.
- Keep Android Live alive across word changes. Send `APP_CONTEXT_UPDATE` lazily only when the teacher panel opens or a new conversation starts. Stop Live on `とめる`, leaving the study tab, back/exit, or Activity destroy.
- Treat `Task was cancelled` during context update as transient. Keep the session active and retry latest context quietly.
- Teacher scope is current-card-only English learning help: meaning, usage, examples, pronunciation, similar words, and exam understanding. Refuse casual/off-topic chat.
- Students may ask in Japanese or English; the teacher must answer in English.
- Teacher limits are platform-specific local guardrails.
- PWA/Web teacher uses the question-count guardrail only: 5 questions per word.
- Android APK Gemini Live teacher uses time guardrails only: 10 minutes per word and 60 minutes per user per local day. Do not add the PWA/Web question-count limit back to APK UI or native Live logic.

## PWA Teacher

- PWA has no Android bridges (`AndroidTTS`, `AndroidGemini`). It uses browser `speechSynthesis` for pronunciation and a backend Worker for Gemini text Q&A.
- PWA reads `window.EIKEN_GEMINI_BACKEND_URL` from `pwa/backend-config.js` and calls `/api/teacher/ask`; never put Gemini keys into browser-visible files.
- Worker secrets belong in `EikenAppByCodeX`/Cloudflare, not the static Pages repo.
- PWA `SpeechRecognition` is single-language per listening attempt. Keep the explicit `日本語` / `English` selector and do not auto-switch while listening.
- One mic press captures one question: non-continuous recognition, stop after final or stable interim text, hard timeout, and always release the mic on no-speech/error.
- PWA teacher audio playback must finish before recognition restarts. Do not auto-restart while `teacherSpeaking` or `teacherAnswerInFlight`.
- Keep the text-question fallback (`teacher-text-question`) in the PWA teacher panel; installed PWA mic behavior can differ from normal Chrome tabs.
- PWA teacher must send recent current-word conversation history to the backend, reset it on word changes, and avoid repeating card/prior teacher examples for "another example" follow-ups.
- If Gemini is unavailable, quota-limited, high-demand, incomplete, or rejected for repeated examples, show a clear Gemini problem message and do not count the failed question. Do not pretend local fallback text is a real Gemini answer.
- PWA service worker must use network-first for navigation/fresh config assets (`index.html`, `service-worker.js`, `backend-config.js`, manifest) so new releases appear without manual cache clearing.
- Durable PWA checks: `node scripts/test-pwa-teacher-mic.mjs` and `node scripts/check-pwa-release.mjs pwa`.

## Release Boundaries

- Source/testing repo: `BoboDanpapa/EikenAppByCodeX`.
- Friend-facing static Pages repo: `BoboDanpapa/EikenMagicwordsPWA`.
- If the user says bare `push` / `推上去`, push only to `EikenAppByCodeX` unless they explicitly name `EikenMagicwordsPWA` or say the build is tested/stable for friends.
- `EikenMagicwordsPWA` may intentionally diverge from `pwa/` for stability. Its friend-facing build can keep `先生に聞く` disabled even when the testing PWA has it enabled.
- If untested PWA work is accidentally pushed to `EikenMagicwordsPWA`, revert that repo to the last stable friend-facing version.
- PWA user progress is local to each browser/device/origin, not shared cloud state.
- APK release and PWA deployment are separate surfaces.

## Android Release

- For every APK release, increment `versionCode` and `versionName` in `android-app/app/build.gradle`.
- Build release APK, copy it into `releases/`, and name it with the version and change, for example `releases/eiken-magicwords-v1.0.0-official.apk`.
- After building, inspect the APK archive to confirm changed HTML/vocab assets are included.
- Do not commit APK/AAB build outputs, `android-app/local.properties`, `android-app/keystore/`, `.android-build-tools/`, or `google-services.json`.
- Gemini Live builds require local `android-app/app/google-services.json`; never commit it. Enable Firebase App Check before public distribution.

## Local Build

- Local build tools are expected under project-root `.android-build-tools/`, not `/Users/salmonli/Downloads/.android-build-tools`.
- From repo root, use:

```sh
GRADLE_USER_HOME=$PWD/.android-build-tools/gradle-home \
JAVA_HOME=$PWD/.android-build-tools/jdk/Contents/Home \
$PWD/.android-build-tools/gradle/gradle-8.7/bin/gradle -p android-app assembleRelease --offline
```

## Known TODO

- Real-device test Android Gemini Live after any teacher/mic change; browser/PWA tests do not validate the Android native audio path.
- Continue Xiaomi/Japan/China mainland TTS checks when relevant.
