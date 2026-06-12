# リアルタイム文字起こしアプリ 再実行プロンプト

このファイルは、このリポジトリのアプリを **ゼロから再生成する** ための Claude へのプロンプトです。  
将来のセッションで「別の端末向けに作り直したい」「機能を追加したい」ときにコピー＆ペーストして使います。

---

## プロンプト本文（ここから下をコピー）

```
以下の仕様で Android のリアルタイム文字起こしアプリを Kotlin + Jetpack Compose で実装してください。
Google Play へのリリースは不要で、Windows PC から ADB 経由でサイドロードする想定です。
実装・テスト・バグ修正まで一貫して行い、最終的に ADB でインストールできる状態にしてください。

---

## 対象環境

- 開発PC: Windows 11
- ターゲット端末: AQUOS Wish 4（Android 16）
- ビルドツール: Gradle 8.9 / AGP 8.7.3 / Kotlin 2.0.21 / KSP 2.0.21-1.0.28
- compileSdk / targetSdk: 35、minSdk: 26

---

## 機能要件

### 音声認識（2モード切り替え）

**ローカルモード（無料・オフライン）**
- Android 標準の SpeechRecognizer を使用（ja-JP）
- EXTRA_PARTIAL_RESULTS=true でリアルタイム表示
- 無音・タイムアウト時は自動的に再開し、stop() まで継続録音
- 各発話を「発話 1」「発話 2」とラベリング

**クラウドモード（Google Cloud Speech-to-Text v1 REST API）**
- 話者分離（speakerDiarizationConfig）で話者A/B/Cに分類
- 録音: AudioRecord 16kHz/16bit/MONO で PCM キャプチャ
- ローカルモードを同時起動して録音中の暫定テキストを表示
- 停止後に PCM を HTTP POST で送信、確定結果を話者ラベル付きで表示
- 長尺対応: 55秒ごとにチャンク分割して送信
- HttpURLConnection を使用（gRPC 不使用）
- 設定画面で Google Cloud API キーを入力

---

## UI 構成（Jetpack Compose + Material 3）

### 文字起こしタブ
- 録音開始/停止ボタン（トグル）
- 録音中は赤いパルスアニメーションのインジケーター表示
- リアルタイムテキスト表示エリア（LazyColumn、自動スクロール）
  - 確定テキスト: 通常色
  - 暫定テキスト: グレー・イタリック
  - 話者ラベル付きの場合は「[話者A]」をボールド表示
- 「📋 全文コピー」ボタン
- 停止後に「保存しますか？」ダイアログ（保存/破棄）

### 履歴タブ
- 保存済みセッション一覧（Room DB から取得）
- タップ: 全文表示ダイアログ（スクロール可）＋コピーボタン
- 長押し: 1件削除の確認ダイアログ
- 「選択」ボタン: チェックボックス選択モード
  - チェックした件数を「削除(N)」ボタンで一括削除
  - 「全選択」「キャンセル」ボタンも表示
- 「全削除」ボタン: 全件削除の確認ダイアログ
- 履歴が多くなっても LazyColumn でスクロール可能

### 設定タブ
- ローカル / クラウド モード切り替え（RadioButton）
- Google Cloud API キー入力（クラウドモード時のみ表示、パスワード表示）
- 「録音中の操作音を消音する」スイッチ（デフォルト ON）
  - AudioManager で STREAM_MUSIC / STREAM_SYSTEM / STREAM_NOTIFICATION をミュート
  - 録音終了時に元の音量に戻す

---

## 技術設計

### データ層
- **Room** でセッション履歴を永続化
  - TranscriptionSession エンティティ: id, createdAt, mode, segmentsJson, title
  - List<TranscriptSegment> ↔ JSON 文字列の変換は Gson を使用
- **DataStore Preferences** で設定を永続化
  - engineMode（LOCAL/CLOUD）、apiKey、muteSystemSound

### エンジン層
```kotlin
data class TranscriptSegment(val speaker: String?, val text: String)
enum class EngineMode { LOCAL, CLOUD }

interface SpeechEngine {
    interface Listener {
        fun onPartialText(text: String)
        fun onSegment(segment: TranscriptSegment)
        fun onCloudResult(segments: List<TranscriptSegment>)
        fun onEngineError(message: String)
    }
    fun start(listener: Listener)
    fun stop()
}
```

### バックグラウンド録音
- ForegroundService（FOREGROUND_SERVICE_TYPE_MICROPHONE）を使用
- 録音開始時に起動、停止時に終了
- 通知チャンネル: 「文字起こし中」を通知バーに表示
- POST_NOTIFICATIONS 権限（Android 13+）を要求

### 画面常時点灯
- 録音中・クラウド解析中は `view.keepScreenOn = true`

---

## 権限（AndroidManifest.xml）

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

---

## テスト要件

以下をすべて実装し、実機（またはエミュレータ）で全件 PASS させること。

### ユニットテスト（app/src/test/）
- MainViewModelTest: 録音開始/停止のステート遷移、セグメント追加、セッション保存、エラー処理
- HistoryViewModelTest: delete / deleteAll の呼び出し確認
- CloudSpeechEngineParserTest: speakerTag → 話者A/B/C 変換、フォールバック動作
- SettingsRepositoryTest: mode/apiKey/muteSystemSound の保存・読み込み

### 計装テスト（app/src/androidTest/）
- TranscriptionDaoTest: insert, getAll の Room DAO テスト（In-Memory DB）
- MainScreenTest: 録音ボタン表示、権限なし時のダイアログ
- HistoryScreenTest: リスト表示、長押し削除ダイアログ、選択モードのチェックボックス、一括削除ダイアログ
- SettingsScreenTest: モード切り替え表示、クラウドモード時 API キー欄表示

---

## 依存ライブラリ（主要なもの）

```kotlin
// Compose BOM
platform("androidx.compose:compose-bom:2024.12.01")

// Room
"androidx.room:room-runtime:2.6.1"
"androidx.room:room-ktx:2.6.1"
ksp("androidx.room:room-compiler:2.6.1")

// DataStore
"androidx.datastore:datastore-preferences:1.1.1"

// Gson
"com.google.code.gson:gson:2.11.0"

// テスト
"io.mockk:mockk:1.13.12"
"io.mockk:mockk-android:1.13.12"

// packaging excludes（mockk-android と JUnit5 の重複解決）
excludes += "META-INF/LICENSE.md"
excludes += "META-INF/LICENSE-notice.md"
```

---

## 既知の制約（設計上の割り切り）

- ローカルモードでマイクゲイン・音声処理をアプリ側から制御する手段はない
  （低い声・こもった音は認識精度が下がる場合がある）
- クラウドモードでは SpeechRecognizer と AudioRecord が同時にマイクを使用するため、
  機種によっては競合で片方が動作しない場合がある
- クラウドモードの話者ラベルはチャンク（55秒）ごとにリセットされる

---

## 成果物

1. ビルドが通り `assembleDebug` で APK が生成されること
2. `.\gradlew.bat test` でユニットテスト全件 PASS
3. `.\gradlew.bat connectedAndroidTest` で計装テスト全件 PASS（実機接続）
4. `adb install -r app\build\outputs\apk\debug\app-debug.apk` でインストールして動作確認
5. SETUP.md（セットアップ手順書）を作成すること
```

---

## 追加機能プロンプト集

このアプリに後から機能を追加する際のプロンプトテンプレートです。

### 男性音声の認識精度改善（Vosk エンジン追加）

```
現在の transcription-app に Vosk オフライン音声認識エンジンを追加して、
ローカルモードを「SpeechRecognizer（既存）」と「Vosk」で切り替えられるようにしてほしい。
Vosk は app/libs に AAR を配置する方式で組み込むこと。
日本語モデル（vosk-model-small-ja）を assets に同梱し、初回起動時に内部ストレージに展開する。
設定画面に「音声認識エンジン: Google / Vosk」のラジオボタンを追加すること。
```

### 文字起こし結果の共有機能追加

```
transcription-app の履歴詳細ダイアログに「共有」ボタンを追加してほしい。
Android の Intent.ACTION_SEND を使って、テキストを他アプリに共有できるようにすること。
```

### タイムスタンプ表示追加

```
transcription-app のリアルタイム文字起こし画面で、
各セグメントの左に録音開始からの経過時間（例: 0:12）を表示してほしい。
MainViewModel でセグメントに経過ミリ秒を追加し、UI で「分:秒」形式に変換して表示すること。
```
