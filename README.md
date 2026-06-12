# リアルタイム文字起こしアプリ

Android 向けのリアルタイム音声文字起こしアプリです。  
Google Play へのリリースは不要で、ADB 経由でサイドロードして使用します。

---

## 機能

- **リアルタイム文字起こし** — 話しながら即座にテキスト表示
- **2モード対応**
  - ローカルモード：無料・オフライン。Android 標準の音声認識エンジンを使用
  - クラウドモード：Google Cloud Speech-to-Text API を使用。話者A/B/C の分離に対応
- **履歴保存** — セッションごとに Room DB へ自動保存
- **全文コピー** — 文字起こし結果をワンタップでクリップボードにコピー
- **チェックボックス一括削除** — 履歴を複数選択してまとめて削除
- **バックグラウンド録音** — 画面を消灯・他アプリに移動しても録音継続
- **画面常時点灯** — 録音中は画面が自動消灯しない
- **操作音の消音** — 録音中の認識開始・終了音（ポン音）を自動ミュート

---

## スクリーンショット

| 文字起こし | 履歴 | 設定 |
|---|---|---|
| 録音開始ボタン・リアルタイム表示 | 履歴一覧・チェックボックス選択 | モード切替・APIキー入力 |

---

## 動作環境

| 項目 | 値 |
|---|---|
| 対象端末 | AQUOS Wish 4（Android 16）※ Android 8.0 以上で動作 |
| 開発PC | Windows 11 |
| 言語 | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material 3 |
| 最小 SDK | API 26（Android 8.0） |
| ターゲット SDK | API 35（Android 15） |

---

## セットアップ

詳細な手順は [SETUP.md](SETUP.md) を参照してください。

### 概要

1. [Android Studio](https://developer.android.com/studio) をインストール
2. このリポジトリを clone またはダウンロード
3. Android Studio でプロジェクトを開き Gradle Sync
4. スマホの開発者モード・USBデバッグを有効化して PC と接続
5. Run ボタンまたは ADB でインストール

```powershell
# ADB でインストールする場合
$env:Path += ";$env:LOCALAPPDATA\Android\Sdk\platform-tools"
git clone https://github.com/hanihani1192296/transcription-app.git
cd transcription-app
.\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

---

## クラウドモードの利用（任意）

ローカルモードは設定不要で使えます。  
クラウドモード（話者分離）を使う場合は Google Cloud の API キーが必要です。

1. [Google Cloud Console](https://console.cloud.google.com) でプロジェクトを作成
2. Cloud Speech-to-Text API を有効化
3. API キーを発行してアプリの設定画面に入力

> 月 60 分までは無料。超過分は約 $0.024 / 分。

---

## テスト

```powershell
# ユニットテスト（PC のみ・14件）
.\gradlew.bat test

# UIテスト・DBテスト（スマホ接続必須・14件）
.\gradlew.bat connectedAndroidTest
```

---

## 技術スタック

| レイヤー | 技術 |
|---|---|
| UI | Jetpack Compose + Material 3 |
| DB | Room 2.6.1 + KSP |
| 設定 | DataStore Preferences 1.1.1 |
| ローカル音声認識 | Android SpeechRecognizer（ja-JP） |
| クラウド音声認識 | Google Cloud Speech-to-Text v1 REST API |
| バックグラウンド録音 | ForegroundService（FOREGROUND_SERVICE_TYPE_MICROPHONE） |
| テスト | JUnit 4 + MockK 1.13.12 + Compose UI Testing |
| ビルド | Gradle 8.9 + AGP 8.7.3 |

---

## ライセンス

MIT
