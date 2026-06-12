package com.example.transcription.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transcription.data.SegmentsJson
import com.example.transcription.data.SettingsRepository
import com.example.transcription.data.TranscriptionDao
import com.example.transcription.data.TranscriptionSession
import com.example.transcription.engine.EngineMode
import com.example.transcription.engine.SpeechEngine
import com.example.transcription.engine.TranscriptSegment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class RecordingState { IDLE, RECORDING, PROCESSING }

class MainViewModel(
    private val settingsRepository: SettingsRepository,
    private val dao: TranscriptionDao,
    private val engineFactory: (EngineMode, String) -> SpeechEngine,
) : ViewModel(), SpeechEngine.Listener {

    private val _state = MutableStateFlow(RecordingState.IDLE)
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    private val _segments = MutableStateFlow<List<TranscriptSegment>>(emptyList())
    val segments: StateFlow<List<TranscriptSegment>> = _segments.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _showSaveDialog = MutableStateFlow(false)
    val showSaveDialog: StateFlow<Boolean> = _showSaveDialog.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var engine: SpeechEngine? = null
    private var currentMode: EngineMode = EngineMode.LOCAL

    fun startRecording() {
        if (_state.value != RecordingState.IDLE) return
        viewModelScope.launch {
            val mode = settingsRepository.mode.first()
            val apiKey = settingsRepository.apiKey.first()
            if (mode == EngineMode.CLOUD && apiKey.isBlank()) {
                _errorMessage.value = "APIキーが未設定です。設定画面で入力してください"
                return@launch
            }
            currentMode = mode
            _segments.value = emptyList()
            _partialText.value = ""
            _state.value = RecordingState.RECORDING
            engine = engineFactory(mode, apiKey)
            engine?.start(this@MainViewModel)
        }
    }

    fun stopRecording() {
        if (_state.value != RecordingState.RECORDING) return
        _state.value =
            if (currentMode == EngineMode.CLOUD) RecordingState.PROCESSING
            else RecordingState.IDLE
        _partialText.value = ""
        engine?.stop()
        engine = null
        if (currentMode == EngineMode.LOCAL) maybeShowSaveDialog()
    }

    fun saveSession() {
        val currentSegments = _segments.value
        if (currentSegments.isEmpty()) {
            _showSaveDialog.value = false
            return
        }
        viewModelScope.launch {
            val plainText = currentSegments.joinToString("") { it.text }
            dao.insert(
                TranscriptionSession(
                    createdAt = System.currentTimeMillis(),
                    mode = currentMode.name,
                    segmentsJson = SegmentsJson.encode(currentSegments),
                    title = plainText.take(30).ifBlank { "(無題)" },
                )
            )
            _showSaveDialog.value = false
            _segments.value = emptyList()
        }
    }

    fun discard() {
        _showSaveDialog.value = false
        _segments.value = emptyList()
        _partialText.value = ""
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun fullText(): String = _segments.value.joinToString("\n") { segment ->
        if (segment.speaker != null) "[${segment.speaker}] ${segment.text}" else segment.text
    }

    private fun maybeShowSaveDialog() {
        if (_segments.value.isNotEmpty()) _showSaveDialog.value = true
    }

    // ---- SpeechEngine.Listener ----

    override fun onPartialText(text: String) {
        _partialText.value = text
    }

    override fun onSegment(segment: TranscriptSegment) {
        _segments.value = _segments.value + segment
    }

    override fun onCloudResult(segments: List<TranscriptSegment>) {
        if (segments.isNotEmpty()) _segments.value = segments
        _state.value = RecordingState.IDLE
        maybeShowSaveDialog()
    }

    override fun onEngineError(message: String) {
        _errorMessage.value = message
        if (_state.value == RecordingState.RECORDING) {
            engine?.stop()
            engine = null
        }
        if (_state.value != RecordingState.IDLE) {
            _state.value = RecordingState.IDLE
            // 暫定結果が残っていれば保存の機会を与える
            maybeShowSaveDialog()
        }
    }

    override fun onCleared() {
        engine?.stop()
        engine = null
        super.onCleared()
    }
}
