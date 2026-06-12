package com.example.transcription.viewmodel

import com.example.transcription.data.SettingsRepository
import com.example.transcription.data.TranscriptionDao
import com.example.transcription.engine.EngineMode
import com.example.transcription.engine.SpeechEngine
import com.example.transcription.engine.TranscriptSegment
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private lateinit var engine: SpeechEngine
    private lateinit var settings: SettingsRepository
    private lateinit var dao: TranscriptionDao
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        engine = mockk(relaxed = true)
        settings = mockk()
        every { settings.mode } returns flowOf(EngineMode.LOCAL)
        every { settings.apiKey } returns flowOf("")
        dao = mockk(relaxed = true)
        viewModel = MainViewModel(settings, dao) { _, _ -> engine }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun 録音開始でステートがRECORDINGになる() {
        viewModel.startRecording()
        assertEquals(RecordingState.RECORDING, viewModel.state.value)
        verify { engine.start(viewModel) }
    }

    @Test
    fun 録音停止でステートがIDLEになる() {
        viewModel.startRecording()
        viewModel.stopRecording()
        assertEquals(RecordingState.IDLE, viewModel.state.value)
        verify { engine.stop() }
    }

    @Test
    fun セグメント追加で全文テキストが正しく結合される() {
        viewModel.onSegment(TranscriptSegment("話者A", "こんにちは"))
        viewModel.onSegment(TranscriptSegment("話者B", "よろしくお願いします"))
        assertEquals(
            "[話者A] こんにちは\n[話者B] よろしくお願いします",
            viewModel.fullText(),
        )
    }

    @Test
    fun 話者ラベルなしセグメントはテキストのみ結合される() {
        viewModel.onSegment(TranscriptSegment(null, "こんにちは"))
        viewModel.onSegment(TranscriptSegment(null, "よろしく"))
        assertEquals("こんにちは\nよろしく", viewModel.fullText())
    }

    @Test
    fun 破棄でセグメントリストが空になる() {
        viewModel.onSegment(TranscriptSegment("発話 1", "テスト"))
        assertTrue(viewModel.segments.value.isNotEmpty())
        viewModel.discard()
        assertTrue(viewModel.segments.value.isEmpty())
    }
}
