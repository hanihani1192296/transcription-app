package com.example.transcription.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transcription.data.TranscriptionDao
import com.example.transcription.data.TranscriptionSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(private val dao: TranscriptionDao) : ViewModel() {

    val sessions: StateFlow<List<TranscriptionSession>> =
        dao.getAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun delete(session: TranscriptionSession) {
        viewModelScope.launch { dao.delete(session) }
    }

    fun deleteAll() {
        viewModelScope.launch { dao.deleteAll() }
    }
}
