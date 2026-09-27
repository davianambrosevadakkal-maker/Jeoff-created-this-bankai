package com.example.domain.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

enum class VoiceRecordState {
    IDLE,
    RECORDING,
    PLAYING
}

class VoiceRecordingHelper(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private val outputFile: File by lazy {
        File(context.cacheDir, "temp_voice_attempt.m4a")
    }

    var state: VoiceRecordState = VoiceRecordState.IDLE
        private set

    fun hasRecording(): Boolean = outputFile.exists() && outputFile.length() > 0

    fun startRecording(onStateChange: (VoiceRecordState) -> Unit, onError: (String) -> Unit) {
        stopPlayback()
        try {
            if (outputFile.exists()) {
                outputFile.delete()
            }

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            state = VoiceRecordState.RECORDING
            onStateChange(state)
        } catch (e: Exception) {
            Log.e("VoiceRecordingHelper", "Error starting recording", e)
            mediaRecorder?.release()
            mediaRecorder = null
            state = VoiceRecordState.IDLE
            onStateChange(state)
            onError(e.message ?: "Failed to start audio recording")
        }
    }

    fun stopRecording(onStateChange: (VoiceRecordState) -> Unit) {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("VoiceRecordingHelper", "Error stopping recording", e)
        } finally {
            mediaRecorder = null
            state = VoiceRecordState.IDLE
            onStateChange(state)
        }
    }

    fun playRecording(onStateChange: (VoiceRecordState) -> Unit, onCompleted: () -> Unit) {
        if (!outputFile.exists() || outputFile.length() == 0L) {
            return
        }
        stopPlayback()

        try {
            val player = MediaPlayer().apply {
                setDataSource(outputFile.absolutePath)
                prepare()
                setOnCompletionListener {
                    state = VoiceRecordState.IDLE
                    onStateChange(state)
                    onCompleted()
                }
                start()
            }
            mediaPlayer = player
            state = VoiceRecordState.PLAYING
            onStateChange(state)
        } catch (e: IOException) {
            Log.e("VoiceRecordingHelper", "Error playing recording", e)
            mediaPlayer?.release()
            mediaPlayer = null
            state = VoiceRecordState.IDLE
            onStateChange(state)
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e("VoiceRecordingHelper", "Error releasing player", e)
        } finally {
            mediaPlayer = null
            state = VoiceRecordState.IDLE
        }
    }

    fun release() {
        stopRecording {}
        stopPlayback()
    }
}
