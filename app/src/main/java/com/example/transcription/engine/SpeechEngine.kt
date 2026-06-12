package com.example.transcription.engine

/** 文字起こし結果の1区切り。speaker が null の場合はラベルなしで表示する。 */
data class TranscriptSegment(
    val speaker: String?,
    val text: String,
)

enum class EngineMode { LOCAL, CLOUD }

interface SpeechEngine {

    interface Listener {
        /** 認識途中の暫定テキスト。確定すると空文字で通知される。 */
        fun onPartialText(text: String)

        /** 確定した発話セグメント。 */
        fun onSegment(segment: TranscriptSegment)

        /** クラウド解析の最終結果（話者ラベル付き）。ローカルエンジンは呼ばない。 */
        fun onCloudResult(segments: List<TranscriptSegment>)

        fun onEngineError(message: String)
    }

    fun start(listener: Listener)

    fun stop()
}
