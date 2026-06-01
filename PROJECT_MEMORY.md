# Project Memory

This file records durable project context for future Codex sessions. Read this before making product, build, or release changes.

## Product

- This is a kid-friendly Eiken vocabulary Android WebView app.
- Main app asset: `android-app/app/src/main/assets/EikenStudy.html`
- Browser preview: `EikenStudyPreview.html`
- Eiken Grade 2 vocabulary is kept in `eiken_grade2_words.js` and `android-app/app/src/main/assets/eiken_grade2_words.js`.
- Eiken Pre-2 vocabulary is kept separately in `eiken_pre2_words.js` and `android-app/app/src/main/assets/eiken_pre2_words.js`.
- Eiken Grade 4, Grade 3, Pre-1, and Grade 1 vocabulary are kept in `eiken_grade4_words.js`, `eiken_grade3_words.js`, `eiken_pre1_words.js`, and `eiken_grade1_words.js`; each has 500 app-curated entries.
- Custom/original vocabulary backups use the `*_custom.js` suffix, for example `eiken_pre2_words_custom.js`, `eiken_grade3_words_custom.js`, and `eiken_pre1_words_custom.js`.
- Character images live in `images/` and `android-app/app/src/main/assets/images/`.

## Current Stable Version

- Current version after the six-course vocabulary release: `1.1.14`
- Current `versionCode`: `16`
- Latest local release APK:
  `releases/eiken-magicwords-v1.1.14-six-course-vocab.apk`

## Major Version History

- `v1.0.0`: Initial Eiken app project.
- `v1.1.0`: Added multi-user course selection and separate progress.
- `v1.1.1`: Debug TTS release.
- `v1.1.2`: TTS engine scan release.
- `v1.1.3`: Fixed Android TTS engine fallback.
- `v1.1.4`: Responsive layout release.
- `v1.1.5`: Added separate 500-entry Eiken Pre-2 vocabulary/phrase set and fixed the quiz tab-switch cheating issue.
- `v1.1.6`: Revised Eiken Pre-2 entries into study-priority order and replaced template examples with original textbook-style natural sentences.
- `v1.1.7`: Added an independent quiz deck order so challenges are not alphabetical and do not follow the magic-card study order.
- `v1.1.8`: Added a Japanese UI layout demo for the English teacher question button and five-turn teacher panel.
- `v1.1.9`: Integrated Android Gemini Live teacher bridge, microphone permission flow, and five-turn word-question session handling. Keep the Firebase GoogleAI backend on `gemini-2.5-flash-native-audio-preview-12-2025`; `gemini-live-2.5-flash-preview` can close the Firebase AI Logic WebSocket during connection. The transcript-handler turn counter was rolled back because it could prevent audio replies on device; future precise per-answer counting needs a custom receive/audio pipeline or another stable SDK event. The stable Live session should be kept across word changes in the study tab; send an `APP_CONTEXT_UPDATE` text message instead of reconnecting, and only stop Live on `とめる`, leaving the study tab, back/exit, or Activity destroy.
- `v1.1.10`: Added the settings entry, visible app version badge, theme selection demo, and teacher safety settings display. The alternate theme is `Navy + Sky`; settings persist in local storage.
- `v1.1.13`: Added Gemini teacher usage limits: 5 questions per word via Live transcription callbacks, 20 minutes per word, 60 minutes per user per local day, and visible teacher time counters. The current Android implementation still keeps lazy context updates so words are sent to Gemini only when the teacher panel is opened.
- `v1.1.14`: Added 英検4級・3級・準1級・1級 to the course selector, synced all six courses to Android/PWA assets, and kept user progress separated per course.
- `v1.1.15`: PWA teacher listening fix: removed the manual language selector, removed Chinese from teacher input requirements, internally alternates English/Japanese browser speech recognition, only final speech recognition results may trigger answers/counting, and PWA/Web teacher rules remain current-card-only with Japanese/English student questions and English teacher answers.
- `v1.1.16`: Added the PWA Gemini backend integration path. The static PWA reads `window.EIKEN_GEMINI_BACKEND_URL` from `pwa/backend-config.js` and calls `/api/teacher/ask`; the new `pwa-backend/` Cloudflare Worker keeps `GEMINI_API_KEY` in a Worker secret and applies the same current-card-only teacher rules. If no backend URL is configured, PWA falls back to the local offline teacher.
- `v1.1.17`: Improved PWA browser microphone language handling. PWA starts Web Speech Recognition with Japanese first, falls back to English automatically, and rejects low-confidence transcripts without counting a question. This protects the PWA text-backend path from sending obvious browser transcription mistakes to Gemini, but it is still not equivalent to APK Gemini Live audio understanding; true parity needs a future PWA Live-audio or audio-upload backend path.
- `v1.1.18`: Changed the PWA teacher microphone design to explicit manual input-language selection (`日本語` / `English`). Because browser `SpeechRecognition` is single-language per session and cannot reliably infer bilingual speech like APK Gemini Live, the user chooses the language before pressing the microphone; PWA must not auto-switch languages during a listening attempt.
- `v1.1.19`: Improved PWA teacher transcript handling. The browser now accumulates final speech recognition segments for one listening attempt and waits briefly before sending the complete transcript to Gemini, instead of sending the first final fragment. The selected input language is also sent in teacher context, and the Worker prompt tells Gemini to answer the student's exact question first.
- `v1.1.20`: Fixed PWA teacher answer playback being cut off. A browser microphone attempt now stops after one complete question, waits for teacher TTS playback to finish, and only then re-enables the mic; PWA must not auto-restart speech recognition while teacher TTS is speaking. Worker answer budget increased to reduce truncated Gemini text.
- `v1.1.21`: Added PWA teacher incomplete-answer protection. If Gemini returns an apparently unfinished sentence, the PWA must reject it and use the local fallback instead of showing a half answer. The fallback includes explicit handling for `反対言葉` / `opposite`, including `a lot of` -> `a few` / `a little`.
- `v1.1.22`: Added per-word PWA teacher conversation memory. The PWA keeps recent student/teacher turns for the current word and sends them to the backend with each question so follow-up questions can refer to earlier answers. This memory resets when the study word changes and should not cross words/courses.
- `v1.1.23`: Fixed repeated example answers in the PWA teacher. When the student asks for another/new/different example sentence, the backend prompt must not reuse the card's original example, and the PWA must reject Gemini answers that repeat the original example and use a new-example fallback instead.
- `v1.1.24`: Added release automation hardening: GitHub Actions for PWA backend deploy/checks, reusable PWA release test scripts, and a service worker strategy that uses network-first for navigation/fresh config so friends get new PWA versions on the next open without manual cache clearing.
- `v1.1.25`: Improved PWA teacher answer quality for follow-up questions. The backend prompt now detects question intent, uses recent same-word history for follow-ups, and explicitly forbids repeating card or prior teacher examples; the PWA fallback rotates examples instead of returning the same sentence every time.
- `v1.1.26`: Tightened PWA teacher intent detection. Treat "sample" as an example request, refuse Japanese/English small-talk requests locally, and avoid falling back to the card's default example for unclear questions.
- PWA teacher panel should not display the `のこり` remaining-time row; keep the daily limit internally, but only show current-card time and today's used teacher time.

## Important Behavior

- Users are local only, without passwords.
- Progress is stored locally per user and per course.
- Progress for Eiken Grade 4, Grade 3, Pre-2, Grade 2, Pre-1, and Grade 1 must remain separate.
- Review notes, streak, rank progress, and unlocked gallery entries are course-specific.
- In `鬼殺隊の試練`, the current unanswered quiz word and answer options must persist when switching tabs. Do not regenerate a quiz question merely because the user leaves and returns to the quiz tab.
- Generate a new quiz question only after the user answers, after a level-up continuation, or when the saved quiz word is invalid for the current course data.

## Gemini Live Teacher Control Design

Current design for `先生に聞く`:

- The Gemini Live model remains `gemini-2.5-flash-native-audio-preview-12-2025` through Firebase AI Logic with `ResponseModality.AUDIO`.
- The teacher must answer only in English. Students may ask in Japanese or English.
- The teacher is restricted to English-learning help for the current card: meaning, usage, examples, pronunciation, similar words, and exam understanding. It must refuse casual/off-topic chat.
- These scope and language rules apply to both Android Gemini Live and PWA/Web teacher implementations. Chinese input is not a supported teacher requirement.
- PWA/Web currently receives browser-transcribed text before Gemini sees the question. Browser `SpeechRecognition` is single-language per listening session, so the PWA teacher must show an explicit `日本語` / `English` language selector before the microphone and use the selected language for that listening attempt. Do not auto-switch languages while listening. If this remains insufficient, implement PWA Live audio or audio upload so Gemini can hear the original bilingual audio like the APK path.
- PWA/Web must send the complete recognized transcript to the Gemini backend. Do not answer/count on the first partial final speech segment; accumulate final segments for the listening attempt, show the recognized text in the UI, then send that full text with the selected input language in context.
- PWA/Web teacher audio playback must finish before any new recognition starts. Do not call `speechSynthesis.cancel()` or restart recognition while `teacherSpeaking` / `teacherAnswerInFlight` is true; one microphone press should capture one question, play one complete answer, then re-enable the mic.
- PWA/Web teacher must never display a half sentence from Gemini. Validate backend answers before displaying or speaking them; if the answer looks incomplete, fall back to deterministic local guidance for the current word/question.
- PWA/Web text-backend teacher must include recent same-word conversation history in backend requests. Keep the history short, current-word-only, and reset it on word changes; use it so the 5 allowed questions behave like one short conversation rather than five isolated Gemini calls.
- When the student asks for another/new/different example sentence, the PWA/Web teacher must generate a new example using the current word. Do not repeat the card's original `enSent`; validate Gemini answers and fall back if they reuse it.
- When the student asks follow-up questions such as another one / もう一つ / それ, the PWA/Web teacher should use recent same-word history to infer the intent and must not repeat prior teacher examples.
- PWA/Web teacher example detection must include both "example" and "sample"; off-topic requests such as 雑談 / small talk should get the fixed refusal instead of the generic card example fallback.
- PWA service worker must not cache-first `index.html`, `service-worker.js`, `backend-config.js`, or manifest forever. Keep navigation/config fresh enough that a released version appears on the next app open without asking friends to manually clear browser cache.
- The Live session is kept alive across word changes to reduce reconnects. Pressing `前へ` / `次へ` must not send the new word to Gemini.
- Lazy context update is intentional: the app sends the current word to Gemini only when the user opens `先生に聞く` for that card, or when starting a new conversation from `マイク`.
- Avoid duplicate context updates. If the current word context was already sent after opening `先生に聞く`, pressing `マイク` must not send the same word again and must not trigger a second Gemini confirmation.
- If `APP_CONTEXT_UPDATE` send returns `Task was cancelled` / cancellation, treat it as transient. Do not show that error to the child; keep the session active and retry the latest context in the background.
- Stop Gemini Live on `とめる`, leaving the study tab, returning/exiting, or Activity destroy/app close.

Usage limits:

- Per word: maximum `5` detected questions.
- Per word: maximum `20` minutes of teacher time.
- Per user per local day: maximum `60` minutes total teacher time.
- Usage is local-device/local-user state, not cloud billing truth. It is a hard in-app guardrail but can be reset by clearing app data.
- The UI should show `質問`, `この単語`, `今日`, and remaining time in the teacher panel.
- The app should stop the Live session and disable `マイク` when a question/time limit is reached.

Question counting notes:

- Android v1.1.13 attempts automatic question counting with `startAudioConversation(transcriptHandler, false)`.
- Count one question when a student transcript is seen and then the teacher output transcript starts.
- Guard against duplicate transcript fragments with native turn state such as pending student question and answer-seen flags.
- This transcript-handler path previously had audio-response risk, so real-device testing must verify that Gemini still hears and answers. If audio becomes unreliable, preserve the time limits and revisit the counting pipeline instead of reintroducing `receive()` while audio conversation is active.

PWA note:

- This design depends on Android native bridges (`AndroidGemini`) and Firebase Android Live APIs. It does not directly work in browser/PWA. A real PWA teacher needs a separate browser-compatible Gemini backend/design.
- PWA/Web implementations must still preserve the same product rules: current-card-only help, refusal of casual/off-topic chat, student questions in Japanese or English, and teacher answers in English.
- Do not put Gemini API keys into `pwa/`, GitHub Pages, or any browser-visible JavaScript. PWA Gemini access must go through a server-side backend such as the Cloudflare Worker in `pwa-backend/`, configured from `pwa/backend-config.js`.
- For the first PWA Gemini backend rollout, it is acceptable to reuse the same Gemini API key as the APK/Firebase setup by storing it only as the Worker secret `GEMINI_API_KEY`. After the PWA teacher is stable, remind the user to create a separate PWA backend key for separate quota, monitoring, and revocation.
- The current PWA backend target is text question/answer first. If text Q&A does not meet the user's goal of APK-equivalent teacher behavior, remind the user to continue with a Web Live audio backend plan.

## Vocabulary Ordering Rule

This rule applies to every current and future Eiken level, not only Eiken Pre-2:

- Vocabulary files must not be arranged alphabetically for learner-facing study order.
- Magic cards should appear in study-priority order: more important, common, and exam-relevant words/phrases first.
- Study order should mix single words and useful phrases naturally instead of grouping all words first and all phrases later.
- Quiz/challenge order must be independent from magic-card order.
- Quiz/challenge order must not be alphabetical and must not match the vocabulary file or magic-card order.
- Quiz/challenge order should mix words and phrases within priority bands, so children cannot easily switch tabs and locate answers by sequence.
- When adding a new level, include tests or manual checks that confirm the study order and quiz order differ.

## Release Convention

Every time a new APK is generated:

- Increase `versionCode` and `versionName` in `android-app/app/build.gradle`.
- Build the release APK.
- Copy the generated APK into the project `releases/` directory.
- Name the copied APK with the new version number and a short change description, for example:
  `releases/eiken-magicwords-v1.1.5-pre2-500-quiz-lock.apk`
- Confirm the APK contains the expected changed assets when vocabulary or HTML assets changed.

## PWA Deployment Plan

- The PWA build lives in `pwa/` and should remain a self-contained static web app build separate from the Android APK source.
- The separate GitHub publishing repository is `BoboDanpapa/EikenMagicwordsPWA`.
- Publish the contents of `pwa/` at the root of `BoboDanpapa/EikenMagicwordsPWA`; do not publish the full Android project there.
- GitHub Pages should deploy from the `main` branch and `/ (root)`.
- Expected GitHub Pages URL:
  `https://bobodanpapa.github.io/EikenMagicwordsPWA/`
- Keep this repository as the development/source workspace. In-progress PWA changes, debugging builds, and unfinished features stay here under `pwa/`.
- `BoboDanpapa/EikenMagicwordsPWA` is the friend-facing stable publishing repo. Do not push changes there unless the user explicitly says the PWA build has been tested and is ready/stable for friends.
- If an untested PWA change is accidentally pushed to `BoboDanpapa/EikenMagicwordsPWA`, revert the publishing repo back to the last stable version rather than leaving friends on the experimental build.
- On 2026-05-31, the untested PWA teacher usage counter commit `997128f` was mistakenly pushed to the publishing repo and then reverted with commit `d0d0de4`.
- PWA user progress is local to each browser/device/origin via web storage. It is not a shared cloud database.
- A custom domain is optional. The GitHub Pages URL is enough for sharing with friends at the current stage.
- In the PWA browser environment, Android native bridges such as `AndroidTTS` and `AndroidGemini` are not available. PWA pronunciation uses the browser Web Speech API (`speechSynthesis`) when supported, and should show a friendly unsupported-browser message otherwise. A real PWA AI teacher needs a separate Web/API backend or another browser-compatible design.
- PWA Gemini backend rollout order: first ship text Q&A through the Cloudflare Worker in `pwa-backend/` using the Worker secret `GEMINI_API_KEY`; later revisit true Live audio if the text flow is not enough.
- APK release conventions remain separate from PWA deployment.

## Build Command

Use the local Android build tooling when available:

```sh
cd android-app
GRADLE_USER_HOME=$PWD/../.android-build-tools/gradle-home JAVA_HOME=$PWD/../.android-build-tools/jdk/Contents/Home ../.android-build-tools/gradle/gradle-8.7/bin/gradle assembleRelease --offline
```

## Testing Checklist

- Validate vocabulary counts with Node when vocabulary files change.
- Check for duplicate English entries in vocabulary files.
- Run a syntax check for inline JavaScript in the HTML assets.
- For the quiz tab-switch bug, verify: enter quiz, record word/options, switch to another tab, return to quiz, and confirm word/options did not change.
- After APK build, inspect the APK archive to confirm expected assets are included.

## Known Constraints

- APK/AAB build outputs should not be committed to Git history.
- `android-app/local.properties`, `android-app/keystore/`, and `.android-build-tools/` should not be committed.
- Local release APKs may exist in `releases/` for handoff, but project docs say official GitHub Release assets are the distribution channel.
- Character images and 鬼殺隊-themed reward labels are for personal learning use; review rights before public distribution.
- Gemini Live builds require a local `android-app/app/google-services.json` from Firebase AI Logic. This file must not be committed. Enable Firebase App Check before public distribution.

## Open TODO

- Continue testing Android TTS behavior on Japan and China mainland Xiaomi devices.
- If additional conversation history exists outside this repository, manually fold any missing decisions into this file.
