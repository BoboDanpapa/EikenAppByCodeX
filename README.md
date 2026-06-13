# EikenAppByCodeX

英検4級から1級までの単語・熟語を楽しく覚えるための、子ども向け学習アプリプロジェクトです。

## What Is Included

- Original web demo: `EikenStudy.html`
- PWA build: `pwa/`
- PWA Gemini backend: `pwa-backend/`
- Android WebView app source: `android-app/`
- Vocabulary data: `eiken_grade2_words.js` and Android asset copy
- Additional 500-entry vocabulary data: `eiken_grade4_words.js`, `eiken_grade3_words.js`, `eiken_pre1_words.js`, `eiken_grade1_words.js`
- Custom/original vocabulary backups: `*_custom.js`
- Character image assets: `images/`
- Browser preview for next-version UI flow: `EikenStudyPreview.html`
- Durable project notes for future work: `PROJECT_MEMORY.md`

## Current APK Features

- Multiple local users without passwords
- Separate user progress
- 英検4級 / 3級 / 準2級 / 2級 / 準1級 / 1級 course selection
- Course-specific vocabulary cards
- 500-entry vocabulary and phrase sets for 英検4級 / 3級 / 準2級 / 準1級 / 1級
- 復習ノート
- 四択クイズ
- 階級アップと図鑑解放
- ローカル進度保存
- Android Text-to-Speech based pronunciation
- English TTS engine fallback and failure messaging

## Preview Page

Open `EikenStudyPreview.html` in a browser to review the planned next-version flow:

- Multiple local users
- User switching
- User deletion confirmation
- Shared course selection page
- Separate progress for each Eiken course
- Current-user progress reset confirmation

The preview page is not the production APK. It is for UI and flow confirmation.

## PWA Build

The `pwa/` directory is a self-contained Progressive Web App version of the latest production WebView assets. It includes:

- `index.html`
- `manifest.webmanifest`
- `service-worker.js`
- `eiken_grade4_words.js` / `eiken_grade3_words.js` / `eiken_pre2_words.js`
- `eiken_grade2_words.js` / `eiken_pre1_words.js` / `eiken_grade1_words.js`
- `images/`
- `icons/`
- optional `backend-config.js` for the Gemini teacher backend URL

To test locally:

```sh
cd pwa
python3 -m http.server 8766
```

Then open `http://localhost:8766/index.html`. For public use, deploy the contents of `pwa/` to any HTTPS static host. PWA installation and service worker caching require HTTPS, except on localhost.

Phone installation instructions for iPhone/iPad and Android are in `PWA_INSTALL_GUIDE.md`.

## PWA Gemini Backend

The PWA cannot safely store a Gemini API key in browser-visible files. Use `pwa-backend/` as a Cloudflare Worker API for the PWA teacher:

```sh
cd pwa-backend
npm install
npx wrangler login
npx wrangler secret put GEMINI_API_KEY
npm run deploy
```

These commands assume only that you already have a Cloudflare account. Wrangler will create/deploy the Worker from `pwa-backend/wrangler.toml`.

After deploying the Worker, set `window.EIKEN_GEMINI_BACKEND_URL` in `pwa/backend-config.js`. The PWA calls `/api/teacher/ask` on that backend and falls back to the offline teacher when the backend is not configured.

## Android Build Notes

The checked-in Android project intentionally does not include local SDK paths, build outputs, or signing keys.

Do not commit:

- `android-app/local.properties`
- `android-app/keystore/`
- `.android-build-tools/`
- APK/AAB build outputs

To build locally, install Android SDK/JDK/Gradle or recreate local build tooling, then configure `android-app/local.properties` with your local SDK path.

## Releases

APK files are distributed as GitHub Release assets:

- `EikenMagicWords-v1.0.0-official.apk`
- `eiken-magicwords-v1.0.1-original-characters.apk`
- `EikenMagicWords-v1.1.0.apk`
- `EikenMagicWords-v1.0.0.apk`

APK files are not stored in Git history.

## Local Release Convention

When generating a new APK, always:

- Increase `versionCode` and `versionName` in `android-app/app/build.gradle`
- Build the release APK
- Copy the generated APK into the project `releases/` directory
- Name the copied APK with the new version number and a short change description, for example:
  `releases/eiken-magicwords-v1.1.5-pre2-500-quiz-lock.apk`

## Next Version TODO

- Continue testing Android TTS behavior on Japan and China mainland Xiaomi devices

## Important Note

The current character images are original PNG assets. Reward labels and any other themed references should still be reviewed before public distribution.
