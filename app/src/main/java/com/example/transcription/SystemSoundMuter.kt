package com.example.transcription

import android.content.Context
import android.media.AudioManager

/**
 * 録音中に SpeechRecognizer の開始・終了音(システム操作音)を消すため、
 * 関連ストリームを一時消音する。restore() で必ず元に戻すこと。
 * 端末によっては DND 権限がないと特定ストリームの操作で SecurityException に
 * なるため、失敗したストリームは黙ってスキップする。
 */
class SystemSoundMuter(context: Context) {

    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val targetStreams = intArrayOf(
        AudioManager.STREAM_MUSIC,
        AudioManager.STREAM_SYSTEM,
        AudioManager.STREAM_NOTIFICATION,
    )

    private val mutedStreams = mutableSetOf<Int>()

    fun mute() {
        if (mutedStreams.isNotEmpty()) return
        for (stream in targetStreams) {
            try {
                audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_MUTE, 0)
                mutedStreams += stream
            } catch (_: SecurityException) {
            }
        }
    }

    fun restore() {
        for (stream in mutedStreams) {
            try {
                audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_UNMUTE, 0)
            } catch (_: SecurityException) {
            }
        }
        mutedStreams.clear()
    }
}
