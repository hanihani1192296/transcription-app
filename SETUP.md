# リアルタイム文字起こしアプリ セットアップ手順

対象: AQUOS Wish 4（Android 16）/ Windows 11 PC  
バージョン: 1.0（2026-06-12 時点）

---

## 1. Android Studio のインストール（Windows）

### 1-1. インストーラーのダウンロード

1. ブラウザで `https://developer.android.com/studio` を開く
2. 青い「**Download Android Studio**」ボタンをクリック
3. 利用規約を確認して「I have read and agree ...」にチェック → 「Download」
4. `android-studio-*.exe`（約 1GB）がダウンロードされるまで待つ

### 1-2. インストーラーの実行

1. ダウンロードした `.exe` をダブルクリックして起動
   - 「このアプリがデバイスに変更を加えることを許可しますか？」→ **はい**
2. セットアップウィザードが開いたら **Next** をクリック（変更不要）
3. インストール先は変えなくてよい（デフォルト: `C:\Program Files\Android\Android Studio`）
4. スタートメニューへの登録もそのまま **Install**
5. インストール完了 → **Next** → **Finish** で Android Studio が自動起動する

### 1-3. 初回起動時のセットアップウィザード

Android Studio が初めて起動すると「Android Studio Setup Wizard」が開きます。

1. **Welcome** 画面 → **Next**
2. **Install Type** 画面  
   → 「**Standard**」を選択（推奨）→ **Next**  
   ※ Custom を選ぶと細かい設定が必要になるので Standard でよい
3. **Select UI Theme** → 好みで選ぶ（どちらでも動作に影響なし）→ **Next**
4. **Verify Settings** 画面  
   インストールされるコンポーネントが一覧表示される。以下が含まれていれば OK:
   ```
   Android SDK                  ← アプリのビルドに必須
   Android SDK Platform         ← 各 Android バージョンの API
   Android Virtual Device       ← エミュレーター（今回は実機使用なので必須ではない）
   Performance (Intel HAXM)     ← エミュレーター高速化（実機のみなら不要）
   ```
   → **Next**
5. **License Agreement** 画面  
   「android-sdk-license」「intel-android-extra-license」などが表示される  
   → 各ライセンスを選択して「**Accept**」→ 全て承諾したら **Finish**
6. コンポーネントのダウンロードが始まる（インターネット接続が必要、10〜20 分）
7. 「**Android SDK is up to date.**」と表示されたら完了 → **Finish**

### 1-4. SDK のインストール確認

Android Studio のメイン画面が開いたら、SDK が正しく入っているか確認します。

1. メニューバー「**Tools**」→「**SDK Manager**」を開く  
   （または右上の歯車アイコン → SDK Manager）
2. 「**SDK Platforms**」タブ を開く  
   → 「Android 15.0 ("VanillaIceCream")」などにチェックが入っていれば OK
3. 「**SDK Tools**」タブ を開く  
   → 以下にチェックが入っていることを確認:
   ```
   ✅ Android SDK Build-Tools
   ✅ Android SDK Platform-Tools   ← adb コマンドが含まれる
   ✅ Android Emulator
   ```
4. 問題なければ **OK** で閉じる

> **SDK のインストール先**: `C:\Users\ユーザー名\AppData\Local\Android\Sdk`  
> このパスは後の手順で使います。

---

## 2. プロジェクトのセットアップ

### 2-1. プロジェクトを開く

1. Android Studio のメイン画面（Welcome 画面）で「**Open**」をクリック
   - すでに別プロジェクトが開いている場合: メニュー「File」→「Open...」
2. フォルダ選択ダイアログで `C:\work\study\transcription-app` を選択 → **OK**
3. 「**Trust Project?**」ダイアログが出たら「**Trust Project**」をクリック

### 2-2. local.properties の確認（自動生成）

プロジェクトを開くと、Android Studio が自動的に `local.properties` を生成します。  
このファイルに SDK のパスが書き込まれ、ビルド時に参照されます。

```
# local.properties（自動生成・Gitには含まない）
sdk.dir=C\:\\Users\\h-sato\\AppData\\Local\\Android\\Sdk
```

> もし「**SDK location not found**」エラーが出た場合は、ファイルが存在しないか  
> パスが誤っています。Android Studio で一度プロジェクトを開き直すと自動修正されます。

### 2-3. Gradle Sync（依存ライブラリの取得）

プロジェクトを開いた直後、画面右上または下部のバーに通知が表示されます。

```
Gradle files have changed since last project sync.  [Sync Now]
```

1. 「**Sync Now**」をクリック
2. 画面下部の「**Build**」タブにログが流れ始める  
   → 初回は Room・Compose・DataStore など全依存ライブラリをダウンロード（数分）
3. 以下のメッセージで完了:
   ```
   BUILD SUCCESSFUL in Xs
   ```
   または下部ステータスバーが「**Gradle sync finished**」になる

**Sync が失敗する場合の確認ポイント:**

| エラーメッセージ | 原因 | 対処 |
|---|---|---|
| `Could not resolve ...` | インターネット未接続 / プロキシ | ネット接続を確認して再度 Sync Now |
| `SDK location not found` | local.properties なし | プロジェクトを閉じて再度開く |
| `Unsupported class file major version` | JDK バージョン不一致 | 手順3の JAVA_HOME 設定を確認 |

### 2-4. Sync 完了の確認

左側の「**Project**」パネルで以下のフォルダ構造が見えれば準備完了です。

```
transcription-app/
├── app/
│   └── src/
│       ├── main/       ← アプリ本体のソースコード
│       ├── test/       ← ユニットテスト
│       └── androidTest/← 実機テスト
├── build.gradle.kts
└── settings.gradle.kts
```

> **Note:** `gradlew.bat` と `gradle/wrapper/gradle-wrapper.jar` はリポジトリに含まれています。  
> 自動生成が必要な場合: File → New → New Project で空プロジェクトを作り、そこから 3 ファイルをコピーする。

## 3. ビルド

**方法A: Android Studio の GUI（初心者向け・推奨）**
- メニュー Build → Build App Bundle(s) / APK(s) → Build APK(s)
- 完了通知の「locate」をクリックすると `app\build\outputs\apk\debug\app-debug.apk` が開く

**方法B: コマンドライン**
```powershell
cd C:\work\study\transcription-app
.\gradlew.bat assembleDebug
```

`JAVA_HOME` が未設定のときは Android Studio 同梱の JDK を指定:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

## 4. スマホ（AQUOS Wish 4）の開発者モード有効化

1. 設定 → デバイス情報 → 「ビルド番号」を **7回連続タップ**
   - 「デベロッパーになりました」と表示される
2. 設定 → システム → 開発者向けオプション → 「USBデバッグ」を ON
3. USB ケーブルで PC と接続
4. スマホに「USBデバッグを許可しますか？」と出たら「このパソコンからのUSBデバッグを常に許可する」にチェックして許可

## 5. スマホへのインストール

### 方法A: Android Studio の Run ボタン（推奨・初心者向け）

1. USB ケーブルでスマホと PC を接続した状態で Android Studio を開く
2. 画面上部のツールバーにあるデバイス選択欄を確認する  
   → 「**SHARP AQUOS Wish 4**」が表示されていれば認識されている  
   → 表示されない場合は手順4の USBデバッグ設定を再確認
3. 緑の「**▶ Run**」ボタンをクリック  
   → ビルド → APK 生成 → スマホへのインストール → アプリ起動 まで自動で行われる

### 方法B: ADB コマンド（ビルド済み APK を直接インストール）

**Step 1: adb コマンドを使えるようにする**

`adb` は Android SDK の `platform-tools` フォルダに入っています。  
PowerShell を開いて以下を実行します（現在のセッションのみ有効）:

```powershell
$env:Path += ";$env:LOCALAPPDATA\Android\Sdk\platform-tools"
```

毎回設定するのが面倒な場合はシステムの環境変数に永続登録できます:
1. スタートメニュー → 「環境変数」で検索 → 「システム環境変数の編集」
2. 「環境変数」ボタン → ユーザー環境変数の「Path」を選択 → 「編集」
3. 「新規」→ 以下のパスを追加:
   ```
   C:\Users\h-sato\AppData\Local\Android\Sdk\platform-tools
   ```
4. OK → OK → PowerShell を開き直せば `adb` が使えるようになる

**Step 2: スマホが認識されているか確認**

```powershell
adb devices
```

正常に認識されていれば以下のように表示されます:
```
List of devices attached
XXXXXXXXXXXXXXXX    device     ← シリアル番号と "device" が表示されれば OK
```

表示が `unauthorized` の場合はスマホ画面を見てください。  
「USBデバッグを許可しますか？」ダイアログが出ているので「許可」をタップ。

**Step 3: APK をインストール**

```powershell
cd C:\work\study\transcription-app
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

`Success` と表示されればインストール完了です。

**インストール後に権限を手動付与する場合（初回のみ）:**

```powershell
# マイク権限
adb shell pm grant com.example.transcription android.permission.RECORD_AUDIO
# 通知権限（Android 13以上）
adb shell pm grant com.example.transcription android.permission.POST_NOTIFICATIONS
```

## 6. Google Cloud API キーの取得（クラウドモード用・任意）

ローカルモードだけ使うならこの手順は不要です。

1. https://console.cloud.google.com にアクセスし Google アカウントでログイン
2. 上部のプロジェクト選択 → 「新しいプロジェクト」→ 名前は任意（例: transcription）→ 作成
3. 左メニュー「APIとサービス」→「ライブラリ」→ `Cloud Speech-to-Text API` を検索 → 「有効にする」
   - 請求先アカウントの登録を求められたら案内に従う（**月60分までは無料**）
4. 「APIとサービス」→「認証情報」→「+ 認証情報を作成」→「APIキー」
5. 表示されたキーをコピーし、アプリの設定画面 → クラウドモードを選択 → APIキー欄に貼り付け
6. （推奨）作成したキーの「キーを制限」→ API の制限で Cloud Speech-to-Text API のみに限定

## 7. よくあるエラーと対処法

| 症状 | 原因 | 対処 |
|---|---|---|
| Sync 失敗「Could not resolve ...」 | ネットワーク / プロキシ | 接続を確認して File → Sync Project with Gradle Files を再実行 |
| `SDK location not found` | local.properties 未生成 | Android Studio で一度開けば自動生成される |
| `adb devices` に何も出ない | USBデバッグ未許可 / ケーブルが充電専用 | 手順4を再確認。データ転送対応ケーブルを使う |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | 旧バージョンと署名不一致 | `adb uninstall com.example.transcription` 後に再インストール |
| 録音ボタンを押しても文字が出ない | マイク権限拒否 / Google アプリ無効 | アプリ情報から権限許可。「Google」アプリが有効か確認（音声認識エンジンとして使用） |
| 通知バーに「文字起こし中」が出ない | POST_NOTIFICATIONS 権限なし | 設定 → アプリ → 権限 → 通知 → 許可、または手順5の権限付与コマンドを実行 |
| クラウドモードで「APIキーが無効です」 | キー誤り / API 未有効化 | 手順6の3（API有効化）と5（貼り付け）を再確認 |
| クラウド結果が空になる | SpeechRecognizer と AudioRecord の同時録音競合（機種依存） | 既知の制約。ローカルモードを使うか報告してください |
| 長い録音で話者ラベルが途中で変わる | 55秒ごとのチャンク分割で speakerTag が振り直される | 既知の制約（同期APIの上限）。1発言が55秒以内なら影響は軽微 |
| テスト実行後にアプリが消える | instrumented test がアプリをアンインストールする Android の仕様 | テスト後は `adb install -r ...` で再インストールする |
| UIテストが全件 FAILED「No compose hierarchies found」 | テスト実行時にスマホ画面がロックされていた | `adb shell input keyevent KEYCODE_WAKEUP && adb shell wm dismiss-keyguard` で解除してから再実行 |

## 8. テスト実行コマンド

```powershell
cd C:\work\study\transcription-app

# ユニットテスト（PC だけで実行可能、14件）
.\gradlew.bat test

# UIテスト・DBテスト（スマホをUSB接続した状態で実行、14件）
.\gradlew.bat connectedAndroidTest

# Lint 含む全チェック
.\gradlew.bat check
```

**UIテスト前の必須準備:**
```powershell
# スマホの画面を起こす（テスト中ずっと点灯させる）
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb shell svc power stayon usb   # USB 接続中は画面 ON を維持

# テスト後にリセット
adb shell svc power stayon false
```

**テスト後のアプリ再インストール:**
```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## 9. テストが失敗したときの読み方

1. コンソールの `> Task :app:testDebugUnitTest FAILED` の直後にあるクラス名・メソッド名を確認
   - 例: `MainViewModelTest > 録音開始でステートがRECORDINGになる FAILED`
2. その下の `expected:<...> but was:<...>` が「期待値」と「実際の値」
3. 詳細レポートは HTML で見るのが楽:
   - ユニットテスト: `app\build\reports\tests\testDebugUnitTest\index.html`
   - UIテスト: `app\build\reports\androidTests\connected\index.html`
4. 対処に迷ったら、失敗ログ全文を Claude に貼り付ければ修正案が返ってくる

---

## アプリの使い方（インストール後）

### 文字起こしタブ

1. 「**● 録音開始**」をタップ → マイク権限ダイアログが出たら「許可」
2. 話すとリアルタイムで文字が表示される（灰色のイタリック体 = 暫定、確定したら黒字）
3. 「**■ 停止**」をタップ → 「保存しますか？」ダイアログ → 保存 or 破棄
4. 「**📋 全文コピー**」で画面の全テキストをクリップボードにコピー
5. 録音中は画面が自動的に消灯しない。画面を閉じても録音は継続（通知バーに表示）

### 履歴タブ

| 操作 | 動作 |
|---|---|
| タップ | 全文表示ダイアログ → コピーボタンで全文コピー |
| 長押し | 1件削除の確認ダイアログ |
| 「選択」ボタン | チェックボックス選択モードに切り替え |
| チェックして「削除(N)」 | N件をまとめて削除 |
| 「全選択」 | 全件チェック（もう一度押すと全解除） |
| 「キャンセル」 | 選択モードを終了 |
| 「全削除」ボタン | 全件削除の確認ダイアログ |

### 設定タブ

| 項目 | 内容 |
|---|---|
| ローカルモード | 無料・オフライン可。話者分離なし（[発話 1][発話 2] の区切りのみ） |
| クラウドモード | 月60分無料、超過 約$0.024/分。話者分離あり（[話者A][話者B]）。要インターネット + APIキー |
| 録音中の操作音を消音する | ON にすると録音中のポン音（認識開始・終了音）を消す。メディア音・通知音も一時消音 |

### モードの違い

| | ローカル | クラウド |
|---|---|---|
| 料金 | 無料 | 月60分まで無料、超過 約$0.024/分 |
| ネット | 不要（端末内処理） | 必要 |
| 話者分離 | なし（発話番号のみ） | あり（話者A/B/C） |
| 結果表示タイミング | リアルタイム | 録音中は暫定、停止後に確定 |
| 連続録音時間 | 制限なし | 55秒ごとにチャンク送信（事実上無制限） |

---

## 技術スタック（開発者向け）

| レイヤー | 使用技術 |
|---|---|
| 言語 | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material 3 |
| DI | ViewModelFactory（手動） |
| DB | Room 2.6.1 + KSP |
| 設定永続化 | DataStore Preferences 1.1.1 |
| ローカル音声認識 | Android SpeechRecognizer（ja-JP） |
| クラウド音声認識 | Google Cloud Speech-to-Text v1 REST API |
| クラウド音声キャプチャ | AudioRecord 16kHz/16bit/MONO |
| バックグラウンド録音 | ForegroundService（FOREGROUND_SERVICE_TYPE_MICROPHONE） |
| シリアライズ | Gson 2.11.0 |
| ユニットテスト | JUnit 4 + MockK 1.13.12 |
| UIテスト | Compose UI Testing + MockK Android |
| ビルド | Gradle 8.9 + AGP 8.7.3 |
| 最小 SDK | API 26（Android 8.0） |
| ターゲット SDK | API 35（Android 15） |
