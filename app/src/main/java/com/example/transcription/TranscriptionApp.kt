package com.example.transcription

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.example.transcription.data.AppDatabase
import com.example.transcription.data.SettingsRepository

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class TranscriptionApp : Application() {

    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "transcription.db").build()
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(settingsDataStore)
    }
}
