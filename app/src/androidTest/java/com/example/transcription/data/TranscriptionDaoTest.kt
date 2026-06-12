package com.example.transcription.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TranscriptionDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: TranscriptionDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.transcriptionDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun session(title: String, createdAt: Long = System.currentTimeMillis()) =
        TranscriptionSession(
            createdAt = createdAt,
            mode = "LOCAL",
            segmentsJson = """[{"speaker":null,"text":"$title"}]""",
            title = title,
        )

    @Test
    fun insertして取得できる() = runBlocking {
        dao.insert(session("会議メモ"))
        val all = dao.getAll().first()
        assertEquals(1, all.size)
        assertEquals("会議メモ", all[0].title)
    }

    @Test
    fun 新しい順に並ぶ() = runBlocking {
        dao.insert(session("古い", createdAt = 1000L))
        dao.insert(session("新しい", createdAt = 2000L))
        val all = dao.getAll().first()
        assertEquals("新しい", all[0].title)
        assertEquals("古い", all[1].title)
    }

    @Test
    fun deleteで1件削除される() = runBlocking {
        dao.insert(session("残す"))
        dao.insert(session("消す"))
        val target = dao.getAll().first().first { it.title == "消す" }
        dao.delete(target)
        val all = dao.getAll().first()
        assertEquals(1, all.size)
        assertEquals("残す", all[0].title)
    }

    @Test
    fun deleteAllで全件削除される() = runBlocking {
        dao.insert(session("A"))
        dao.insert(session("B"))
        dao.deleteAll()
        assertTrue(dao.getAll().first().isEmpty())
    }
}
