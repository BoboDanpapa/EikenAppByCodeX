# EikenAppByCodeX

英検2級の単語・熟語を楽しく覚えるための、子ども向け学習アプリプロジェクトです。

## What Is Included

- Original web demo: `EikenStudy.html`
- Android WebView app source: `android-app/`
- Vocabulary data: `words.js` and Android asset copy
- Character image assets: `images/`
- Browser preview for next-version UI flow: `EikenStudyPreview.html`

## Current APK Features

- Multiple local users without passwords
- Separate user progress
- 英検準2級 / 英検2級 course selection
- 英検2級単語カード
- 英検準2級 test vocabulary structure
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
- Separate 英検準2級 / 英検2級 progress
- Current-user progress reset confirmation

The preview page is not the production APK. It is for UI and flow confirmation.

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

- `EikenMagicWords-v1.1.0.apk`
- `EikenMagicWords-v1.0.0.apk`

APK files are not stored in Git history.

## Next Version TODO

- Prepare an original 500-entry 英検準2級 word/phrase set
- Replace temporary 英検準2級 test entries with the full original vocabulary set
- Continue testing Android TTS behavior on Japan and China mainland Xiaomi devices

## Important Note

The current character images and 鬼殺隊-themed reward labels are intended for personal learning use only. Review asset rights before any public distribution.
