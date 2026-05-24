# Project Memory

This file records durable project context for future Codex sessions. Read this before making product, build, or release changes.

## Product

- This is a kid-friendly Eiken vocabulary Android WebView app.
- Main app asset: `android-app/app/src/main/assets/EikenStudy.html`
- Browser preview: `EikenStudyPreview.html`
- Eiken Grade 2 vocabulary is kept in `words.js` and `android-app/app/src/main/assets/words.js`.
- Eiken Pre-2 vocabulary is kept separately in `pre2_words.js` and `android-app/app/src/main/assets/pre2_words.js`.
- Character images live in `images/` and `android-app/app/src/main/assets/images/`.

## Current Stable Version

- Current version after the Gemini Live teacher integration: `1.1.9`
- Current `versionCode`: `11`
- Latest local release APK:
  `releases/eiken-magicwords-v1.1.9-gemini-live-teacher.apk`

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
- `v1.1.9`: Integrated Android Gemini Live teacher bridge, microphone permission flow, and five-turn word-question session handling. Later patched the same release to use the quota-friendlier `gemini-live-2.5-flash-preview` model and count completed student/teacher exchanges instead of session starts.

## Important Behavior

- Users are local only, without passwords.
- Progress is stored locally per user and per course.
- Eiken Pre-2 and Eiken Grade 2 progress must remain separate.
- Review notes, streak, rank progress, and unlocked gallery entries are course-specific.
- In `鬼殺隊の試練`, the current unanswered quiz word and answer options must persist when switching tabs. Do not regenerate a quiz question merely because the user leaves and returns to the quiz tab.
- Generate a new quiz question only after the user answers, after a level-up continuation, or when the saved quiz word is invalid for the current course data.

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
