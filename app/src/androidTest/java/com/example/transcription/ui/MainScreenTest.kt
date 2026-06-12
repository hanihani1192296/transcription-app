package com.example.transcription.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.transcription.ui.main.MainScreenContent
import com.example.transcription.viewmodel.RecordingState
import org.junit.Rule
import org.junit.Test

class MainScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun 録音ボタンが表示される() {
        composeRule.setContent {
            MainScreenContent(
                state = RecordingState.IDLE,
                segments = emptyList(),
                partialText = "",
                showSaveDialog = false,
                onToggleRecording = {},
                onCopyAll = {},
                onSave = {},
                onDiscard = {},
            )
        }
        composeRule.onNodeWithTag("recordButton").assertIsDisplayed()
    }

    @Test
    fun ボタンタップで録音インジケーターが表示される() {
        composeRule.setContent {
            var state by remember { mutableStateOf(RecordingState.IDLE) }
            MainScreenContent(
                state = state,
                segments = emptyList(),
                partialText = "",
                showSaveDialog = false,
                onToggleRecording = { state = RecordingState.RECORDING },
                onCopyAll = {},
                onSave = {},
                onDiscard = {},
            )
        }
        composeRule.onNodeWithTag("recordButton").performClick()
        composeRule.onNodeWithTag("recordingIndicator").assertIsDisplayed()
    }

    @Test
    fun テキストエリアが表示される() {
        composeRule.setContent {
            MainScreenContent(
                state = RecordingState.IDLE,
                segments = emptyList(),
                partialText = "",
                showSaveDialog = false,
                onToggleRecording = {},
                onCopyAll = {},
                onSave = {},
                onDiscard = {},
            )
        }
        composeRule.onNodeWithTag("transcriptArea").assertIsDisplayed()
    }
}
