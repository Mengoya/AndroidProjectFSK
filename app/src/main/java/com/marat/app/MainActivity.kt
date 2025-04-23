package com.marat.app

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private val freqZero = 17500.0
    private val freqOne = 17800.0
    private val sampleRate = 44100
    private val bitDurationMs = 100
    private val fadeDurationMs = 75
    private val preamble = listOf(1, 0, 1)
    private val dataBitsCount = 8

    private val command1Data = listOf(1, 0, 1, 0, 1, 0, 1, 0)
    private val command2Data = listOf(0, 0, 0, 1, 0, 1, 0, 1)

    @Volatile private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val sendButton1: Button = findViewById(R.id.sendButton1)
        val sendButton2: Button = findViewById(R.id.sendButton2)

        sendButton1.setOnClickListener {
            val bitsToSend = preamble + command1Data
            playSoundAsync(bitsToSend)
        }

        sendButton2.setOnClickListener {
            val bitsToSend = preamble + command2Data
            playSoundAsync(bitsToSend)
        }
    }

    private fun playSoundAsync(bits: List<Int>) {
        if (!isPlaying) {
            thread {
                val soundData = generateFSKSound(bits)
                if (soundData != null) {
                    playSoundInternal(soundData, bits)
                } else {
                    Log.e("AudioDebug", "Failed to generate sound data for bits: $bits")
                }
            }
        } else {
            Log.d("AudioDebug", "Already playing, button press ignored.")
        }
    }

    private fun generateFSKSound(bits: List<Int>): ShortArray {
        val samplesPerBit = (bitDurationMs / 1000.0 * sampleRate).toInt()
        val fadeSamples   = (3 / 1000.0 * sampleRate).toInt()
        val payload       = samplesPerBit * bits.size
        val tailSamples   = (0.01 * sampleRate).toInt()        // 10 мс
        val buf           = ShortArray(payload + tailSamples)

        var phase = 0.0
        var idx   = 0
        for (bit in bits) {
            val freq      = if (bit == 0) freqZero else freqOne
            val phaseStep = 2 * Math.PI * freq / sampleRate
            for (i in 0 until samplesPerBit) {
                val env = when {
                    i < fadeSamples ->
                        0.5 * (1 - kotlin.math.cos(Math.PI * i / fadeSamples))
                    i >= samplesPerBit - fadeSamples ->
                        0.5 * (1 - kotlin.math.cos(Math.PI *
                                (samplesPerBit - 1 - i) / fadeSamples))
                    else -> 1.0
                }
                buf[idx++] = (kotlin.math.sin(phase) * env * Short.MAX_VALUE)
                    .toInt().toShort()
                phase += phaseStep
                if (phase >= 2 * Math.PI) phase -= 2 * Math.PI
            }
        }
        return buf
    }

    private fun playSoundInternal(currentSoundData: ShortArray, bits: List<Int>) {
        if (isPlaying) return
        isPlaying = true
        var track: AudioTrack? = null
        try {
            val bufBytes = currentSoundData.size * 2
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(bufBytes)
                .build()

            track.write(currentSoundData, 0, currentSoundData.size)
            track.play()

            while (track.playbackHeadPosition < currentSoundData.size) {
                Thread.sleep(10)
            }

        } catch (e: Exception) {
            Log.e("AudioDebug", "play error: ${e.message}", e)
        } finally {
            track?.release()
            isPlaying = false
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d("AudioDebug", "onStop called.")
        isPlaying = false
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("AudioDebug", "onDestroy called.")
        isPlaying = false
    }
}