package com.example.transcription.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.transcription.engine.EngineMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SettingsRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    // DataStore は内部でスコープ常駐のコルーチンを起動するため、
    // テストスケジューラとは独立した実ディスパッチャ上で動かし、終了時に破棄する
    private val dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    private fun createRepository(): SettingsRepository {
        val dataStore = PreferenceDataStoreFactory.create(scope = dataStoreScope) {
            File(tmp.root, "test_${System.nanoTime()}.preferences_pb")
        }
        return SettingsRepository(dataStore)
    }

    @Test
    fun APIキーを保存して読み出せる() = runBlocking {
        val repository = createRepository()
        assertEquals("", repository.apiKey.first())
        repository.setApiKey("test-key-123")
        assertEquals("test-key-123", repository.apiKey.first())
    }

    @Test
    fun モードを切り替えて永続化できる() = runBlocking {
        val repository = createRepository()
        assertEquals(EngineMode.LOCAL, repository.mode.first())
        repository.setMode(EngineMode.CLOUD)
        assertEquals(EngineMode.CLOUD, repository.mode.first())
        repository.setMode(EngineMode.LOCAL)
        assertEquals(EngineMode.LOCAL, repository.mode.first())
    }
}
