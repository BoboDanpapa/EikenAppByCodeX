# EikenAppByCodeX

英検2級の単語・熟語を楽しく覚えるための、子ども向け学習アプリプロジェクトです。

## What Is Included

- Original web demo: `EikenStudy.html`
- Android WebView app source: `android-app/`
- Vocabulary data: `words.js` and Android asset copy
- Character image assets: `images/`
- Browser preview for next-version UI flow: `EikenStudyPreview.html`

## Current APK Features

- 英検2級単語カード
- 復習ノート
- 四択クイズ
- 階級アップと図鑑解放
- ローカル進度保存
- Android Text-to-Speech based pronunciation

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

## Release

The first APK is distributed as a GitHub Release asset:

- `EikenMagicWords-v1.0.0.apk`

The APK is not stored in Git history.

## Next Version TODO

- Integrate the previewed multi-user flow into the Android app
- Add a dedicated 英検準2級 vocabulary file
- Prepare an original 500-entry 英検準2級 word/phrase set
- Improve Android TTS handling for China mainland Xiaomi devices
- Add TTS engine/language availability checks and fallback messages

## Important Note

The current character images and 鬼殺隊-themed reward labels are intended for personal learning use only. Review asset rights before any public distribution.

