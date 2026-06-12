package com.example.transcription.engine

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class CloudApiException(message: String) : Exception(message)

/**
 * クラウドモード:
 * - 録音中は LocalSpeechEngine で暫定テキストを表示（話者ラベルなし）
 * - 同時に AudioRecord で 16kHz/16bit/モノラル PCM を蓄積
 * - stop() 後に Google Cloud Speech-to-Text v1 へ送信し、話者分離結果を onCloudResult で返す
 *
 * 制約: 同期 recognize API は1リクエスト約60秒までのため、55秒ごとに分割送信する。
 * チャンクをまたぐと speakerTag が振り直されるため、長時間録音では
 * チャンク境界で話者ラベルの対応が変わることがある。
 */
class CloudSpeechEngine(
    context: Context,
    private val apiKey: String,
) : SpeechEngine {

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val CHUNK_SECONDS = 55
        private const val ENDPOINT = "https://speech.googleapis.com/v1/speech:recognize"
    }

    private val localEngine = LocalSpeechEngine(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val audioBuffer = ByteArrayOutputStream()
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null

    @Volatile
    private var capturing = false
    private var listener: SpeechEngine.Listener? = null

    override fun start(listener: SpeechEngine.Listener) {
        this.listener = listener
        audioBuffer.reset()
        startCapture(listener)
        localEngine.start(object : SpeechEngine.Listener {
            override fun onPartialText(text: String) = listener.onPartialText(text)

            override fun onSegment(segment: TranscriptSegment) {
                // クラウド結果で上書きされるまでの暫定表示なのでラベルは付けない
                listener.onSegment(segment.copy(speaker = null))
            }

            override fun onCloudResult(segments: List<TranscriptSegment>) {}

            override fun onEngineError(message: String) {
                // 暫定表示用エンジンのエラーは致命的ではないため無視する
            }
        })
    }

    override fun stop() {
        localEngine.stop()
        capturing = false
        recordingThread?.join(1000)
        recordingThread = null
        audioRecord?.run {
            try {
                stop()
            } catch (_: Exception) {
            }
            release()
        }
        audioRecord = null

        val pcm = synchronized(audioBuffer) { audioBuffer.toByteArray() }
        val callback = listener
        listener = null

        if (pcm.isEmpty()) {
            callback?.onCloudResult(emptyList())
            return
        }

        scope.launch {
            try {
                val segments = transcribe(pcm)
                withContext(Dispatchers.Main) { callback?.onCloudResult(segments) }
            } catch (e: CloudApiException) {
                withContext(Dispatchers.Main) {
                    callback?.onEngineError(e.message ?: "クラウド解析に失敗しました")
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    callback?.onEngineError("ネットワークエラーが発生しました。接続を確認してください")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback?.onEngineError("クラウド解析に失敗しました: ${e.message}")
                }
            } finally {
                scope.cancel()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startCapture(listener: SpeechEngine.Listener) {
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuffer * 4,
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            listener.onEngineError("マイクを初期化できませんでした")
            return
        }
        audioRecord = record
        capturing = true
        record.startRecording()
        recordingThread = Thread {
            val buffer = ByteArray(minBuffer)
            while (capturing) {
                val read = record.read(buffer, 0, buffer.size)
                if (read > 0) {
                    synchronized(audioBuffer) { audioBuffer.write(buffer, 0, read) }
                }
            }
        }.also { it.start() }
    }

    private fun transcribe(pcm: ByteArray): List<TranscriptSegment> {
        val chunkBytes = SAMPLE_RATE * 2 * CHUNK_SECONDS
        val all = mutableListOf<TranscriptSegment>()
        var offset = 0
        while (offset < pcm.size) {
            val end = minOf(offset + chunkBytes, pcm.size)
            all += recognizeChunk(pcm.copyOfRange(offset, end))
            offset = end
        }
        return mergeConsecutive(all)
    }

    private fun recognizeChunk(chunk: ByteArray): List<TranscriptSegment> {
        val body = JsonObject().apply {
            add("config", JsonObject().apply {
                addProperty("encoding", "LINEAR16")
                addProperty("sampleRateHertz", SAMPLE_RATE)
                addProperty("languageCode", "ja-JP")
                addProperty("enableSpeakerDiarization", true)
                add("diarizationConfig", JsonObject().apply {
                    addProperty("enableSpeakerDiarization", true)
                    addProperty("minSpeakerCount", 2)
                    addProperty("maxSpeakerCount", 6)
                })
            })
            add("audio", JsonObject().apply {
                addProperty("content", Base64.encodeToString(chunk, Base64.NO_WRAP))
            })
        }

        val connection = URL("$ENDPOINT?key=$apiKey").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.doOutput = true
            connection.connectTimeout = 15_000
            connection.readTimeout = 60_000
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            return when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_OK ->
                    CloudResponseParser.parse(
                        connection.inputStream.bufferedReader().use { it.readText() }
                    )

                HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_FORBIDDEN ->
                    throw CloudApiException("APIキーが無効です。設定を確認してください")

                else -> {
                    val detail = connection.errorStream
                        ?.bufferedReader()?.use { it.readText() }
                        ?.take(200) ?: ""
                    throw CloudApiException("クラウド解析に失敗しました (HTTP $code) $detail")
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun mergeConsecutive(segments: List<TranscriptSegment>): List<TranscriptSegment> {
        val merged = mutableListOf<TranscriptSegment>()
        for (segment in segments) {
            val prev = merged.lastOrNull()
            if (prev != null && prev.speaker != null && prev.speaker == segment.speaker) {
                merged[merged.size - 1] = prev.copy(text = prev.text + segment.text)
            } else {
                merged += segment
            }
        }
        return merged
    }
}
