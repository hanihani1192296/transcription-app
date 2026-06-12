package com.example.transcription.ui.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.transcription.data.SegmentsJson
import com.example.transcription.data.TranscriptionSession
import com.example.transcription.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    val sessions by viewModel.sessions.collectAsState()
    HistoryScreenContent(
        sessions = sessions,
        onDelete = viewModel::delete,
        onDeleteAll = viewModel::deleteAll,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreenContent(
    sessions: List<TranscriptionSession>,
    onDelete: (TranscriptionSession) -> Unit,
    onDeleteAll: () -> Unit,
) {
    var detailTarget by remember { mutableStateOf<TranscriptionSession?>(null) }
    var deleteTarget by remember { mutableStateOf<TranscriptionSession?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }

    fun toggleSelection(id: Long) {
        if (id in selectedIds) selectedIds.remove(id) else selectedIds.add(id)
    }

    fun exitSelectionMode() {
        selectionMode = false
        selectedIds.clear()
    }
    val clipboard = LocalClipboardManager.current
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("履歴", style = MaterialTheme.typography.titleLarge)
            Row {
                if (!selectionMode) {
                    TextButton(
                        onClick = { selectionMode = true },
                        enabled = sessions.isNotEmpty(),
                        modifier = Modifier.testTag("selectModeButton"),
                    ) {
                        Text("選択")
                    }
                    TextButton(
                        onClick = { showDeleteAllDialog = true },
                        enabled = sessions.isNotEmpty(),
                        modifier = Modifier.testTag("deleteAllButton"),
                    ) {
                        Text("全削除")
                    }
                } else {
                    val allSelected = selectedIds.size == sessions.size
                    TextButton(onClick = {
                        selectedIds.clear()
                        if (!allSelected) selectedIds.addAll(sessions.map { it.id })
                    }) {
                        Text(if (allSelected) "全解除" else "全選択")
                    }
                    TextButton(
                        onClick = { showBulkDeleteDialog = true },
                        enabled = selectedIds.isNotEmpty(),
                        modifier = Modifier.testTag("bulkDeleteButton"),
                    ) {
                        Text("削除(${selectedIds.size})")
                    }
                    TextButton(onClick = { exitSelectionMode() }) {
                        Text("キャンセル")
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (sessions.isEmpty()) {
            Text("保存された履歴はありません", color = Color.Gray)
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(sessions, key = { it.id }) { session ->
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .testTag("historyItem")
                            .combinedClickable(
                                onClick = {
                                    if (selectionMode) toggleSelection(session.id)
                                    else detailTarget = session
                                },
                                onLongClick = {
                                    if (!selectionMode) deleteTarget = session
                                },
                            )
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (selectionMode) {
                                Checkbox(
                                    checked = session.id in selectedIds,
                                    onCheckedChange = { toggleSelection(session.id) },
                                    modifier = Modifier.testTag("historyCheckbox"),
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        dateFormat.format(Date(session.createdAt)),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.Gray,
                                    )
                                    Text(
                                        if (session.mode == "CLOUD") "クラウド" else "ローカル",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(session.title, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }

    detailTarget?.let { session ->
        val fullText = remember(session.id) { sessionFullText(session) }
        AlertDialog(
            onDismissRequest = { detailTarget = null },
            title = { Text(dateFormat.format(Date(session.createdAt))) },
            text = {
                Column(
                    Modifier
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(fullText)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(fullText))
                }) { Text("コピー") }
            },
            dismissButton = {
                TextButton(onClick = { detailTarget = null }) { Text("閉じる") }
            },
        )
    }

    deleteTarget?.let { session ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("この履歴を削除しますか？") },
            text = { Text(session.title) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(session)
                    deleteTarget = null
                }) { Text("削除") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("キャンセル") }
            },
        )
    }

    if (showBulkDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteDialog = false },
            title = { Text("選択した${selectedIds.size}件を削除しますか？") },
            text = { Text("この操作は取り消せません。") },
            confirmButton = {
                TextButton(onClick = {
                    sessions.filter { it.id in selectedIds }.forEach(onDelete)
                    showBulkDeleteDialog = false
                    exitSelectionMode()
                }) { Text("削除") }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteDialog = false }) { Text("キャンセル") }
            },
        )
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("すべての履歴を削除しますか？") },
            text = { Text("この操作は取り消せません。") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteAll()
                    showDeleteAllDialog = false
                }) { Text("全削除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) { Text("キャンセル") }
            },
        )
    }
}

private fun sessionFullText(session: TranscriptionSession): String =
    SegmentsJson.decode(session.segmentsJson).joinToString("\n") { segment ->
        if (segment.speaker != null) "[${segment.speaker}] ${segment.text}" else segment.text
    }
