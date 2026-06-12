# リアルタイム文字起こしアプリ 再実行プロンプト

このファイルは、このリポジトリのアプリを **ゼロから再生成する** ための Claude へのプロンプトです。  
将来のセッションで「別の端末向けに作り直したい」「機能を追加したい」ときにコピー＆ペーストして使います。

---

## 推奨モデル構成（コスト効率重視）

このアプリの開発は反復的なビルド・デバッグサイクルを伴うため、**1モデルに任せきりにしない**ことがコスト効率の鍵です。

### 基本方針

```
Sonnet（メインセッション・オーケストレーター）
  │
  ├── 初期コード生成（仕様確定後・一発勝負）
  │     → Agent(model="fable") に委譲
  │        仕様書を丸ごと渡して全ファイルを一気に生成させる
  │
  ├── 複雑なバグ解析（原因が不明な難しいエラー）
  │     → Agent(model="opus") に委譲
  │        エラーログ＋関連ファイルを渡して原因と修正コードを出力させる
  │
  └── それ以外はすべて Sonnet がそのまま処理
        - ビルドエラーの読解・修正
        - 個別機能の追加・変更
        - ADB / git 操作
        - ファイル読み込み・確認
```

### なぜこの構成か

| フェーズ | 向いているモデル | 理由 |
|---|---|---|
| 初期生成（40ファイル一括） | Fable 5 | 長い仕様を一度に処理する能力が必要 |
| 複雑なバグ解析 | Opus 4.8 | 深い推論が必要、ただし一瞬だけ |
| 反復的な修正・ADB・git | Sonnet 4.6 | 文脈蓄積コストが 1/3 以下（$3 vs $10 per 1M input）|
| 定型的な調査・ファイル操作 | Sonnet 4.6 | 過剰スペック不要 |

### Fable をオーケストレーターにしてはいけない理由

Fable がオーケストレーターになると、サブエージェントの結果（コード・エラーログ・テスト結果）が
すべて Fable のコンテキストに戻ってくるため、**入力トークンが Fable 単価（$10/1M）で積み上がる**。
調整コストは変わらず、ワーカーのコストが上乗せされるだけになる。

効率が良いのは「**調整は安いモデル、重い判断だけ高いモデルに一瞬だけ任せる**」構成。

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

---

## 機種変更時の変更箇所ガイド

別の Android 端末向けにプロンプトを使い回す際に書き換える箇所をまとめます。

### 必須変更（プロンプト本文内）

| 箇所 | 現在の値 | 変更方法 |
|---|---|---|
| `## 対象環境` → ターゲット端末 | `AQUOS Wish 4（Android 16）` | 新機種名と Android バージョンに書き換える |
| `## 対象環境` → minSdk | `26` | 新機種の Android バージョンに合わせて下記の表で確認 |
| `## 対象環境` → compileSdk / targetSdk | `35` | 原則そのまま。新機種が Android 16 以降なら最新値に上げる |

**Android バージョン → API レベル対応表（minSdk の目安）**

| Android バージョン | API レベル | minSdk 推奨値 |
|---|---|---|
| Android 8.0 / 8.1 | 26 / 27 | 26（現在値・最も広い対応） |
| Android 9 | 28 | 28 |
| Android 10 | 29 | 29 |
| Android 11 | 30 | 30 |
| Android 12 / 12L | 31 / 32 | 31 |
| Android 13 | 33 | 33 |
| Android 14 | 34 | 34 |
| Android 15 | 35 | 35 |
| Android 16 | 36 | 36 |

> minSdk は「このアプリが動く最古の Android」を表す。新機種専用にするなら新機種の API レベルに合わせてよい。
> 複数機種で使い回すなら低いほうに合わせる（現在の 26 は Android 8.0 以降すべてに対応）。

### 条件付き変更

| 条件 | 変更箇所 |
|---|---|
| Android 13 未満の機種（API 32 以下）| `POST_NOTIFICATIONS` 権限の要求コードは不要（コードは自動的に分岐するが、Manifest の `uses-permission` は残してよい） |
| Android 10 未満の機種（API 28 以下）| `FOREGROUND_SERVICE_MICROPHONE` の `foregroundServiceType` 指定は API 29 以降のみ有効。プロンプトにその旨を追記すること |
| Android 8 未満をターゲットにする場合 | ForegroundService 関連 API の互換性を確認する必要あり（現行コードは minSdk 26 前提） |

### 変更不要な箇所

以下は機種を変えても書き換えなくてよい。

- ビルドツールのバージョン（Gradle / AGP / Kotlin / KSP）
- 依存ライブラリのバージョン（Room / DataStore / Compose BOM 等）
- パッケージ名（`com.example.transcription`）※ 同じ PC に両方インストールする場合のみ変更が必要
- テスト構成・テストファイル名
- UI 構成・機能要件

### 変更例（Pixel 9、Android 15 向け）

```
- ターゲット端末: AQUOS Wish 4（Android 16）
+ ターゲット端末: Pixel 9（Android 15）

- compileSdk / targetSdk: 35、minSdk: 26
+ compileSdk / targetSdk: 35、minSdk: 26   ← 広く対応させるなら変更不要
  または
+ compileSdk / targetSdk: 35、minSdk: 35   ← Pixel 9 専用にするなら
```
