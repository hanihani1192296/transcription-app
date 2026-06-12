package com.example.transcription.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudSpeechEngineParserTest {

    @Test
    fun レスポンスJSONを正しくパースできる() {
        val json = """
        {
          "results": [
            { "alternatives": [ { "transcript": "こんにちはよろしく", "confidence": 0.92 } ] },
            { "alternatives": [ {
                "words": [
                  { "word": "こんにちは", "speakerTag": 1 },
                  { "word": "よろしく", "speakerTag": 2 },
                  { "word": "お願いします", "speakerTag": 2 }
                ]
            } ] }
          ]
        }
        """.trimIndent()

        val segments = CloudResponseParser.parse(json)

        assertEquals(2, segments.size)
        assertEquals(TranscriptSegment("話者A", "こんにちは"), segments[0])
        assertEquals(TranscriptSegment("話者B", "よろしくお願いします"), segments[1])
    }

    @Test
    fun speakerTagが話者ラベルに変換される() {
        assertEquals("話者A", CloudResponseParser.speakerLabel(1))
        assertEquals("話者B", CloudResponseParser.speakerLabel(2))
        assertEquals("話者C", CloudResponseParser.speakerLabel(3))
    }

    @Test
    fun speakerTagが欠損しているレスポンスでもクラッシュしない() {
        val json = """
        {
          "results": [
            { "alternatives": [ {
                "transcript": "ラベルなしの結果です",
                "words": [ { "word": "ラベルなしの結果です" } ]
            } ] }
          ]
        }
        """.trimIndent()

        val segments = CloudResponseParser.parse(json)

        assertEquals(1, segments.size)
        assertNull(segments[0].speaker)
        assertEquals("ラベルなしの結果です", segments[0].text)
    }

    @Test
    fun 空レスポンスで空リストを返す() {
        assertTrue(CloudResponseParser.parse("{}").isEmpty())
        assertTrue(CloudResponseParser.parse("""{"results":[]}""").isEmpty())
        assertTrue(CloudResponseParser.parse("not json").isEmpty())
    }
}
