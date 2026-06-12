package com.example.transcription.engine

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Android 標準の SpeechRecognizer によるローカル音声認識。
 * 無音や認識完了のたびにセッションが終了するため、stop() されるまで自動で再開する。
 * SpeechRecognizer の制約上、メインスレッドから呼び出すこと。
 */
class LocalSpeechEngine(private val context: Context) : SpeechEngine {

    private var recognizer: SpeechRecognizer? = null
    private var listener: SpeechEngine.Listener? = null
    private var active = false
    private var utteranceCount = 0

    override fun start(listener: SpeechEngine.Listener) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            listener.onEngineError("この端末では音声認識が利用できません")
            return
        }
        this.listener = listener
        active = true
        utteranceCount = 0
        createRecognizerAndListen()
    }

    override fun stop() {
        active = false
        recognizer?.run {
            try {
                stopListening()
                destroy()
            } catch (_: Exception) {
            }
        }
        recognizer = null
        listener = null
    }

    private fun createRecognizerAndListen() {
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(recognitionListener)
            startListening(buildIntent())
        }
    }

    private fun restartListening() {
        if (active) recognizer?.startListening(buildIntent())
    }

    private fun buildIntent(): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

    private val recognitionListener = object : RecognitionListener {

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
            if (!text.isNullOrBlank()) listener?.onPartialText(text)
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
            if (!text.isNullOrBlank()) {
                utteranceCount++
                listener?.onSegment(TranscriptSegment("発話 $utteranceCount", text))
            }
            listener?.onPartialText("")
            restartListening()
        }

        override fun onError(error: Int) {
            when (error) {
                // 無音区間で頻発するエラーは継続録音のため黙って再開する
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                -> restartListening()

                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                    listener?.onEngineError("マイクの権限がありません")

                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                    // 前セッションの終了処理中。作り直して再開する
                    if (active) createRecognizerAndListen()
                }

                else -> restartListening()
            }
        }

        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
