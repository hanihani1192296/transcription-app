# リアルタイム文字起こしアプリ セットアップ手順

対象: AQUOS Wish 4（Android 16）/ Windows 11 PC  
バージョン: 1.0（2026-06-12 時点）

---

## 1. Android Studio のインストール（Windows）

1. https://developer.android.com/studio にアクセスし「Download Android Studio」をクリック
2. ダウンロードした `.exe` を実行し、すべてデフォルトのまま「Next」で進める
3. 初回起動時のセットアップウィザードも「Standard」を選択してデフォルトで進める
   - Android SDK・エミュレータ・JDK が自動的にインストールされます
4. 完了までインターネット接続で 10〜20 分程度

## 2. プロジェクトのセットアップ

1. Android Studio を起動 →「Open」→ `C:\work\study\transcription-app` を選択
2. 「Trust Project」を聞かれたら Trust を選択
3. 右上に「Sync Now」が出たらクリック → 初回は依存ライブラリのダウンロードで数分かかる
4. Sync が成功すれば準備完了

> **Note:** `gradlew.bat` と `gradle/wrapper/gradle-wrapper.jar` はリポジトリに含まれています。  
> 自動生成が必要な場合: File → New → New Project で空プロジェクトを作り、そこから3ファイルをコピーする。

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

**方法A: Android Studio の Run ボタン（推奨）**
- 上部のデバイス選択に「SHARP AQUOS Wish 4」が出ていることを確認
- 緑の ▶（Run）ボタンをクリック → ビルド＋インストール＋起動まで自動

**方法B: ADB コマンド**
```powershell
$env:Path += ";$env:LOCALAPPDATA\Android\Sdk\platform-tools"
adb devices                # デバイスが表示されることを確認
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

インストール後に権限を手動付与する場合（初回のみ）:
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
