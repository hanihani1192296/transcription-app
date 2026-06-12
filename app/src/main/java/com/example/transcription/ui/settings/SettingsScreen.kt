package com.example.transcription.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.transcription.data.SettingsRepository
import com.example.transcription.engine.EngineMode
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(repository: SettingsRepository) {
    val scope = rememberCoroutineScope()
    val mode by repository.mode.collectAsState(initial = EngineMode.LOCAL)
    val storedKey by repository.apiKey.collectAsState(initial = "")
    val muteSystemSound by repository.muteSystemSound.collectAsState(initial = true)
    // ユーザーが編集を始めたらローカル状態を優先する（DataStore 反映の遅延でカーソルが飛ぶのを防ぐ）
    var editedKey by remember { mutableStateOf<String?>(null) }

    SettingsScreenContent(
        mode = mode,
        apiKey = editedKey ?: storedKey,
        onModeChange = { newMode -> scope.launch { repository.setMode(newMode) } },
        onApiKeyChange = { newKey ->
            editedKey = newKey
            scope.launch { repository.setApiKey(newKey) }
        },
        muteSystemSound = muteSystemSound,
        onMuteSystemSoundChange = { enabled ->
            scope.launch { repository.setMuteSystemSound(enabled) }
        },
    )
}

@Composable
fun SettingsScreenContent(
    mode: EngineMode,
    apiKey: String,
    onModeChange: (EngineMode) -> Unit,
    onApiKeyChange: (String) -> Unit,
    muteSystemSound: Boolean = true,
    onMuteSystemSoundChange: (Boolean) -> Unit = {},
) {
    var showApiKeyHelp by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("設定", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        Text("音声認識モード", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .testTag("localModeRadio")
                .clickable { onModeChange(EngineMode.LOCAL) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = mode == EngineMode.LOCAL,
                onClick = { onModeChange(EngineMode.LOCAL) },
            )
            Spacer(Modifier.width(4.dp))
            Text("ローカルモード（無料・オフライン可）")
        }

        Row(
            Modifier
                .fillMaxWidth()
                .testTag("cloudModeRadio")
                .clickable { onModeChange(EngineMode.CLOUD) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = mode == EngineMode.CLOUD,
                onClick = { onModeChange(EngineMode.CLOUD) },
            )
            Spacer(Modifier.width(4.dp))
            Text("クラウドモード（話者分離あり・要インターネット・月60分無料）")
        }

        Spacer(Modifier.height(24.dp))
        Text("音", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .testTag("muteSystemSoundSwitch")
                .clickable { onMuteSystemSoundChange(!muteSystemSound) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("録音中の操作音を消音する")
                Text(
                    "音声認識の開始・終了音（ポン音）を録音中だけ消します。メディア音・通知音も一時的に消音されます",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
            }
            Switch(
                checked = muteSystemSound,
                onCheckedChange = onMuteSystemSoundChange,
            )
        }

        if (mode == EngineMode.CLOUD) {
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                label = { Text("Google Cloud APIキー") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("apiKeyField"),
            )

            Spacer(Modifier.height(8.dp))
            Text(
                if (showApiKeyHelp) "APIキーの取得方法 ▲" else "APIキーの取得方法 ▼",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { showApiKeyHelp = !showApiKeyHelp },
            )
            if (showApiKeyHelp) {
                Spacer(Modifier.height(8.dp))
                Text(
                    """
                    1. https://console.cloud.google.com にアクセスし Google アカウントでログイン
                    2. 新しいプロジェクトを作成（名前は任意）
                    3. 「APIとサービス」→「ライブラリ」で Cloud Speech-to-Text API を検索して有効化
                    4. 「APIとサービス」→「認証情報」→「認証情報を作成」→「APIキー」
                    5. 表示されたキーをこの欄に貼り付け

                    ※ 月60分までは無料です。請求先アカウントの登録を求められる場合があります。
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
            }
        }
    }
}
