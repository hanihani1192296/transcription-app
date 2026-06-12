package com.example.transcription.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TranscriptionSession::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transcriptionDao(): TranscriptionDao
}
