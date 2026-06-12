package com.example.transcription.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptionDao {

    @Insert
    suspend fun insert(session: TranscriptionSession): Long

    @Query("SELECT * FROM sessions ORDER BY createdAt DESC")
    fun getAll(): Flow<List<TranscriptionSession>>

    @Delete
    suspend fun delete(session: TranscriptionSession)

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()
}
