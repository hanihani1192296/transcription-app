package com.example.transcription

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.transcription.engine.CloudSpeechEngine
import com.example.transcription.engine.EngineMode
import com.example.transcription.engine.LocalSpeechEngine
import com.example.transcription.ui.history.HistoryScreen
import com.example.transcription.ui.main.MainScreen
import com.example.transcription.ui.settings.SettingsScreen
import com.example.transcription.ui.theme.TranscriptionTheme
import com.example.transcription.viewmodel.HistoryViewModel
import com.example.transcription.viewmodel.MainViewModel
import com.example.transcription.viewmodel.RecordingState

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as TranscriptionApp
        setContent {
            TranscriptionTheme {
                AppRoot(app)
            }
        }
    }
}

class AppViewModelFactory(private val app: TranscriptionApp) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val dao = app.database.transcriptionDao()
        return when {
            modelClass.isAssignableFrom(MainViewModel::class.java) -> MainViewModel(
                settingsRepository = app.settingsRepository,
                dao = dao,
                engineFactory = { mode, apiKey ->
                    when (mode) {
                        EngineMode.LOCAL -> LocalSpeechEngine(app)
                        EngineMode.CLOUD -> CloudSpeechEngine(app, apiKey)
                    }
                },
            ) as T

            modelClass.isAssignableFrom(HistoryViewModel::class.java) ->
                HistoryViewModel(dao) as T

            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}

@Composable
fun AppRoot(app: TranscriptionApp) {
    val factory = remember { AppViewModelFactory(app) }
    val mainViewModel: MainViewModel = viewModel(factory = factory)
    val historyViewModel: HistoryViewModel = viewModel(factory = factory)

    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    val error by mainViewModel.errorMessage.collectAsState()
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            mainViewModel.clearError()
        }
    }

    // 録音中はシステム操作音(認識開始・終了音)を消音する(設定でOFF可)
    val recordingState by mainViewModel.state.collectAsState()
    val muteSystemSound by app.settingsRepository.muteSystemSound.collectAsState(initial = false)
    val soundMuter = remember { SystemSoundMuter(app) }
    LaunchedEffect(recordingState, muteSystemSound) {
        if (recordingState == RecordingState.RECORDING && muteSystemSound) soundMuter.mute()
        else soundMuter.restore()
    }
    DisposableEffect(Unit) {
        onDispose { soundMuter.restore() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    label = { Text("文字起こし") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text("履歴") },
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("設定") },
                )
            }
        },
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (tab) {
                0 -> MainScreen(mainViewModel)
                1 -> HistoryScreen(historyViewModel)
                else -> SettingsScreen(app.settingsRepository)
            }
        }
    }
}
