package com.example.transcription.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.transcription.engine.TranscriptSegment
import com.google.gson.Gson

@Entity(tableName = "sessions")
data class TranscriptionSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,          // System.currentTimeMillis()
    val mode: String,             // "LOCAL" or "CLOUD"
    val segmentsJson: String,     // JSON: [{speaker:"話者A", text:"..."}, ...]
    val title: String,            // 先頭30文字
)

object SegmentsJson {
    private val gson = Gson()

    fun encode(segments: List<TranscriptSegment>): String = gson.toJson(segments)

    fun decode(json: String): List<TranscriptSegment> =
        runCatching {
            gson.fromJson(json, Array<TranscriptSegment>::class.java).toList()
        }.getOrDefault(emptyList())
}
