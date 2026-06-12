package com.example.transcription.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.example.transcription.data.TranscriptionSession
import com.example.transcription.ui.history.HistoryScreenContent
import org.junit.Rule
import org.junit.Test

class HistoryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sessions = listOf(
        TranscriptionSession(
            id = 1,
            createdAt = 1_700_000_000_000L,
            mode = "LOCAL",
            segmentsJson = """[{"speaker":null,"text":"テスト本文1"}]""",
            title = "テスト履歴1",
        ),
        TranscriptionSession(
            id = 2,
            createdAt = 1_700_000_100_000L,
            mode = "CLOUD",
            segmentsJson = """[{"speaker":"話者A","text":"テスト本文2"}]""",
            title = "テスト履歴2",
        ),
    )

    @Test
    fun 履歴リストが表示される() {
        composeRule.setContent {
            HistoryScreenContent(sessions = sessions, onDelete = {}, onDeleteAll = {})
        }
        composeRule.onNodeWithText("テスト履歴1").assertIsDisplayed()
        composeRule.onNodeWithText("テスト履歴2").assertIsDisplayed()
    }

    @Test
    fun 長押しで削除ダイアログが表示される() {
        composeRule.setContent {
            HistoryScreenContent(sessions = sessions, onDelete = {}, onDeleteAll = {})
        }
        composeRule.onAllNodesWithTag("historyItem")[0].performTouchInput { longClick() }
        composeRule.onNodeWithText("この履歴を削除しますか？").assertIsDisplayed()
    }

    @Test
    fun 選択モードでチェックボックスが表示される() {
        composeRule.setContent {
            HistoryScreenContent(sessions = sessions, onDelete = {}, onDeleteAll = {})
        }
        composeRule.onNodeWithTag("selectModeButton").performClick()
        composeRule.onAllNodesWithTag("historyCheckbox")[0].assertIsDisplayed()
        composeRule.onAllNodesWithTag("historyCheckbox")[1].assertIsDisplayed()
    }

    @Test
    fun チェック選択して削除ボタンで確認ダイアログが表示される() {
        composeRule.setContent {
            HistoryScreenContent(sessions = sessions, onDelete = {}, onDeleteAll = {})
        }
        composeRule.onNodeWithTag("selectModeButton").performClick()
        composeRule.onAllNodesWithTag("historyCheckbox")[0].performClick()
        composeRule.onNodeWithTag("bulkDeleteButton").performClick()
        composeRule.onNodeWithText("選択した1件を削除しますか？").assertIsDisplayed()
    }
}
