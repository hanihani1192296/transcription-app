package com.example.transcription.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.transcription.engine.EngineMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val MODE = stringPreferencesKey("engine_mode")
        val API_KEY = stringPreferencesKey("api_key")
        val MUTE_SYSTEM_SOUND = booleanPreferencesKey("mute_system_sound")
    }

    val mode: Flow<EngineMode> = dataStore.data.map { preferences ->
        preferences[Keys.MODE]
            ?.let { stored -> runCatching { EngineMode.valueOf(stored) }.getOrNull() }
            ?: EngineMode.LOCAL
    }

    val apiKey: Flow<String> = dataStore.data.map { it[Keys.API_KEY] ?: "" }

    suspend fun setMode(mode: EngineMode) {
        dataStore.edit { it[Keys.MODE] = mode.name }
    }

    suspend fun setApiKey(key: String) {
        dataStore.edit { it[Keys.API_KEY] = key }
    }

    /** 録音中にシステム操作音(認識開始・終了音)を消音するか。既定は ON。 */
    val muteSystemSound: Flow<Boolean> =
        dataStore.data.map { it[Keys.MUTE_SYSTEM_SOUND] ?: true }

    suspend fun setMuteSystemSound(enabled: Boolean) {
        dataStore.edit { it[Keys.MUTE_SYSTEM_SOUND] = enabled }
    }
}
