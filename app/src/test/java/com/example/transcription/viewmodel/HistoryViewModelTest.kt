package com.example.transcription.viewmodel

import com.example.transcription.data.TranscriptionDao
import com.example.transcription.data.TranscriptionSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeTranscriptionDao : TranscriptionDao {
    private val flow = MutableStateFlow<List<TranscriptionSession>>(emptyList())
    private var nextId = 1L

    override suspend fun insert(session: TranscriptionSession): Long {
        val saved = session.copy(id = nextId++)
        flow.value = flow.value + saved
        return saved.id
    }

    override fun getAll(): Flow<List<TranscriptionSession>> = flow

    override suspend fun delete(session: TranscriptionSession) {
        flow.value = flow.value.filterNot { it.id == session.id }
    }

    override suspend fun deleteAll() {
        flow.value = emptyList()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private lateinit var dao: FakeTranscriptionDao
    private lateinit var viewModel: HistoryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        dao = FakeTranscriptionDao()
        viewModel = HistoryViewModel(dao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sampleSession(title: String = "テスト") = TranscriptionSession(
        createdAt = System.currentTimeMillis(),
        mode = "LOCAL",
        segmentsJson = "[]",
        title = title,
    )

    @Test
    fun セッション保存後にリストに追加される() = runTest {
        dao.insert(sampleSession("会議メモ"))
        assertEquals(1, viewModel.sessions.value.size)
        assertEquals("会議メモ", viewModel.sessions.value[0].title)
    }

    @Test
    fun セッション削除後にリストから消える() = runTest {
        dao.insert(sampleSession("残す"))
        dao.insert(sampleSession("消す"))
        val target = viewModel.sessions.value.first { it.title == "消す" }
        viewModel.delete(target)
        assertEquals(1, viewModel.sessions.value.size)
        assertEquals("残す", viewModel.sessions.value[0].title)
    }

    @Test
    fun 全削除後にリストが空になる() = runTest {
        dao.insert(sampleSession("A"))
        dao.insert(sampleSession("B"))
        viewModel.deleteAll()
        assertTrue(viewModel.sessions.value.isEmpty())
    }
}
