package com.example.transcription.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.transcription.engine.EngineMode
import com.example.transcription.ui.settings.SettingsScreenContent
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setStatefulContent() {
        composeRule.setContent {
            var mode by remember { mutableStateOf(EngineMode.LOCAL) }
            var apiKey by remember { mutableStateOf("") }
            SettingsScreenContent(
                mode = mode,
                apiKey = apiKey,
                onModeChange = { mode = it },
                onApiKeyChange = { apiKey = it },
            )
        }
    }

    @Test
    fun ローカルとクラウドのラジオボタンが表示される() {
        setStatefulContent()
        composeRule.onNodeWithTag("localModeRadio").assertIsDisplayed()
        composeRule.onNodeWithTag("cloudModeRadio").assertIsDisplayed()
    }

    @Test
    fun クラウドを選択するとAPIキーフィールドが表示される() {
        setStatefulContent()
        composeRule.onNodeWithTag("apiKeyField").assertDoesNotExist()
        composeRule.onNodeWithTag("cloudModeRadio").performClick()
        composeRule.onNodeWithTag("apiKeyField").assertIsDisplayed()
    }

    @Test
    fun ローカルに戻すとAPIキーフィールドが非表示になる() {
        setStatefulContent()
        composeRule.onNodeWithTag("cloudModeRadio").performClick()
        composeRule.onNodeWithTag("apiKeyField").assertIsDisplayed()
        composeRule.onNodeWithTag("localModeRadio").performClick()
        composeRule.onNodeWithTag("apiKeyField").assertDoesNotExist()
    }
}
