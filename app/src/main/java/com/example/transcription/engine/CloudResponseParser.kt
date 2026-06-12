package com.example.transcription.engine

import com.google.gson.JsonParser

/**
 * Google Cloud Speech-to-Text v1 recognize レスポンスのパーサー。
 * Android 依存なし（JVM ユニットテスト対象）。
 */
object CloudResponseParser {

    /**
     * 話者分離が有効な場合、最後の result の alternatives[0].words に
     * 全発話分の speakerTag 付き単語が入る。それを連続する話者ごとに
     * グルーピングしてセグメント化する。
     * speakerTag がないレスポンスは transcript の連結にフォールバックする。
     */
    fun parse(json: String): List<TranscriptSegment> {
        val root = runCatching { JsonParser.parseString(json).asJsonObject }.getOrNull()
            ?: return emptyList()
        val results = root.getAsJsonArray("results") ?: return emptyList()
        if (results.size() == 0) return emptyList()

        val lastWords = results.last().asJsonObject
            .getAsJsonArray("alternatives")
            ?.takeIf { it.size() > 0 }
            ?.get(0)?.asJsonObject
            ?.getAsJsonArray("words")

        if (lastWords != null && lastWords.size() > 0 &&
            lastWords.get(0).asJsonObject.has("speakerTag")
        ) {
            val segments = mutableListOf<TranscriptSegment>()
            var currentTag = -1
            val builder = StringBuilder()
            for (element in lastWords) {
                val word = element.asJsonObject
                val text = word.get("word")?.asString ?: continue
                val tag = if (word.has("speakerTag")) {
                    word.get("speakerTag").asInt
                } else {
                    if (currentTag > 0) currentTag else 1
                }
                if (tag != currentTag && builder.isNotEmpty()) {
                    segments += TranscriptSegment(speakerLabel(currentTag), builder.toString())
                    builder.clear()
                }
                currentTag = tag
                builder.append(text)
            }
            if (builder.isNotEmpty() && currentTag > 0) {
                segments += TranscriptSegment(speakerLabel(currentTag), builder.toString())
            }
            return segments
        }

        // フォールバック: 話者ラベルなしで transcript を連結
        return results.mapNotNull { result ->
            result.asJsonObject
                .getAsJsonArray("alternatives")
                ?.takeIf { it.size() > 0 }
                ?.get(0)?.asJsonObject
                ?.get("transcript")?.asString
        }.filter { it.isNotBlank() }
            .map { TranscriptSegment(null, it.trim()) }
    }

    /** speakerTag 1 → 話者A, 2 → 話者B, ... */
    fun speakerLabel(tag: Int): String {
        val index = (tag - 1).coerceIn(0, 25)
        return "話者" + ('A' + index)
    }
}
