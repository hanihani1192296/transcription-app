package com.example.transcription.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.transcription.RecordingService
import com.example.transcription.engine.TranscriptSegment
import com.example.transcription.viewmodel.MainViewModel
import com.example.transcription.viewmodel.RecordingState

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val state by viewModel.state.collectAsState()
    val segments by viewModel.segments.collectAsState()
    val partialText by viewModel.partialText.collectAsState()
    val showSaveDialog by viewModel.showSaveDialog.collectAsState()

    var showPermissionDialog by remember { mutableStateOf(false) }
    // 通知権限(Android 13+)はフォアグラウンドサービスの通知表示用。拒否されても録音は可能
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results[Manifest.permission.RECORD_AUDIO] == true) viewModel.startRecording()
        else showPermissionDialog = true
    }
    // マイク許可済みの端末で通知権限だけが欠けている場合に使う(結果は録音に影響しない)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    // ① 録音・解析中は画面を消灯させない
    val view = LocalView.current
    DisposableEffect(state) {
        view.keepScreenOn = state != RecordingState.IDLE
        onDispose { view.keepScreenOn = false }
    }

    // ② 録音中はフォアグラウンドサービスを起動し、画面消灯後もマイク使用を継続させる
    LaunchedEffect(state) {
        if (state == RecordingState.RECORDING) RecordingService.start(context)
        else RecordingService.stop(context)
    }

    MainScreenContent(
        state = state,
        segments = segments,
        partialText = partialText,
        showSaveDialog = showSaveDialog,
        onToggleRecording = {
            when (state) {
                RecordingState.RECORDING -> viewModel.stopRecording()
                RecordingState.IDLE -> {
                    val granted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(
                                context, Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        }
                        viewModel.startRecording()
                    } else {
                        val permissions =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) arrayOf(
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.POST_NOTIFICATIONS,
                            ) else arrayOf(Manifest.permission.RECORD_AUDIO)
                        permissionLauncher.launch(permissions)
                    }
                }
                RecordingState.PROCESSING -> Unit
            }
        },
        onCopyAll = { clipboard.setText(AnnotatedString(viewModel.fullText())) },
        onSave = viewModel::saveSession,
        onDiscard = viewModel::discard,
    )

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("マイクの権限が必要です") },
            text = { Text("文字起こしにはマイクの使用許可が必要です。設定画面から権限を許可してください。") },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        )
                    )
                }) { Text("設定を開く") }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) { Text("閉じる") }
            },
        )
    }
}

@Composable
fun MainScreenContent(
    state: RecordingState,
    segments: List<TranscriptSegment>,
    partialText: String,
    showSaveDialog: Boolean,
    onToggleRecording: () -> Unit,
    onCopyAll: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("文字起こし", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(16.dp))
            when (state) {
                RecordingState.RECORDING -> RecordingIndicator()
                RecordingState.PROCESSING -> {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("クラウドで解析中…", style = MaterialTheme.typography.bodySmall)
                }
                RecordingState.IDLE -> Unit
            }
        }

        Spacer(Modifier.height(12.dp))

        val listState = rememberLazyListState()
        LaunchedEffect(segments.size) {
            if (segments.isNotEmpty()) listState.animateScrollToItem(segments.size - 1)
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("transcriptArea"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (segments.isEmpty() && partialText.isBlank() && state == RecordingState.IDLE) {
                item {
                    Text(
                        "「● 録音開始」を押すと文字起こしが始まります",
                        color = Color.Gray,
                    )
                }
            }
            items(segments) { segment ->
                if (segment.speaker != null) {
                    Column {
                        Text(
                            "[${segment.speaker}]",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(segment.text)
                    }
                } else {
                    Text(segment.text)
                }
            }
            if (partialText.isNotBlank()) {
                item {
                    Text(partialText, color = Color.Gray, fontStyle = FontStyle.Italic)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onToggleRecording,
                enabled = state != RecordingState.PROCESSING,
                modifier = Modifier
                    .weight(1f)
                    .testTag("recordButton"),
            ) {
                Text(
                    when (state) {
                        RecordingState.IDLE -> "● 録音開始"
                        RecordingState.RECORDING -> "■ 停止"
                        RecordingState.PROCESSING -> "解析中…"
                    }
                )
            }
            OutlinedButton(
                onClick = onCopyAll,
                enabled = segments.isNotEmpty(),
                modifier = Modifier
                    .weight(1f)
                    .testTag("copyButton"),
            ) {
                Text("📋 全文コピー")
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("保存しますか？") },
            text = { Text("この文字起こし結果を履歴に保存しますか？") },
            confirmButton = {
                TextButton(onClick = onSave) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = onDiscard) { Text("破棄") }
            },
        )
    }
}

@Composable
private fun RecordingIndicator() {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "alpha",
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .testTag("recordingIndicator")
                .size(12.dp)
                .graphicsLayer { this.alpha = alpha }
                .background(Color.Red, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text("録音中", style = MaterialTheme.typography.bodySmall, color = Color.Red)
    }
}
