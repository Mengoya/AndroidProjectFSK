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
import kotlin.math.sin

class MainActivity : AppCompatActivity() {

    private val frequency1 = 18000.0
    private val frequency2 = 17800.0
    private val durationMs = 300
    private val sampleRate = 44100
    private val fadeDurationMs = 25 // Увеличено

    private var soundData1: ShortArray? = null
    private var soundData2: ShortArray? = null
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

        soundData1 = generateSound(frequency1)
        soundData2 = generateSound(frequency2)

        sendButton1.setOnClickListener {
            playSoundAsync(soundData1, frequency1)
        }

        sendButton2.setOnClickListener {
            playSoundAsync(soundData2, frequency2)
        }
    }

    private fun playSoundAsync(soundData: ShortArray?, freq: Double) {
        if (!isPlaying) {
            if (soundData != null) {
                thread {
                    playSoundInternal(soundData, freq)
                }
            } else {
                Log.e("AudioDebug", "Sound data for freq $freq is null.")
            }
        } else {
            Log.d("AudioDebug", "Already playing, button press ignored.")
        }
    }

    private fun generateSound(frequency: Double): ShortArray? {
        val numSamples = (durationMs / 1000.0 * sampleRate).toInt()
        val fadeSamples = (fadeDurationMs / 1000.0 * sampleRate).toInt()

        if (numSamples <= 0) {
            Log.e("AudioDebug", "NumSamples is zero or negative for freq $frequency.")
            return null
        }

        val generatedSound = ShortArray(numSamples)

        if (numSamples <= 2 * fadeSamples && fadeSamples > 0) {
            Log.w("AudioDebug", "Duration ($durationMs ms) too short for fade ($fadeDurationMs ms)! Freq $frequency without fade.")
            for (i in 0 until numSamples) {
                val sample = sin(2.0 * Math.PI * i.toDouble() / (sampleRate / frequency))
                generatedSound[i] = (sample * Short.MAX_VALUE).toInt().toShort()
            }
            return generatedSound
        }

        for (i in 0 until numSamples) {
            val time = i.toDouble() / sampleRate
            val sampleValue = sin(2.0 * Math.PI * frequency * time)

            val envelope: Double = when {
                i < fadeSamples && fadeSamples > 0 -> i.toDouble() / (fadeSamples -1).coerceAtLeast(1).toDouble()
                i >= numSamples - fadeSamples && fadeSamples > 0 -> (numSamples - 1 - i).toDouble() / (fadeSamples -1).coerceAtLeast(1).toDouble()
                else -> 1.0
            }.coerceIn(0.0, 1.0)

            generatedSound[i] = (sampleValue * envelope * Short.MAX_VALUE).toInt().toShort()
        }
        Log.d("AudioDebug", "Sound generated for freq $frequency. Samples: $numSamples")
        return generatedSound
    }

    private fun playSoundInternal(currentSoundData: ShortArray, freq: Double) {
        if (isPlaying) {
            Log.w("AudioDebug", "playSoundInternal called while isPlaying is true for freq $freq.")
            return
        }
        isPlaying = true

        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBufferSize, currentSoundData.size * 2)
        var tempAudioTrack: AudioTrack? = null

        try {
            tempAudioTrack = AudioTrack.Builder()
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
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            val writeResult = tempAudioTrack.write(currentSoundData, 0, currentSoundData.size)
            if (writeResult != currentSoundData.size) {
                Log.e("AudioDebug", "Failed write for freq $freq. Wrote $writeResult")
                throw IllegalStateException("Failed write audio data for freq $freq")
            }

            Log.d("AudioDebug", "Playing sound freq $freq...")
            tempAudioTrack.play()

            val sleepTime = (durationMs + fadeDurationMs + 20).toLong() // Ждем чуть дольше + fade + запас
            Thread.sleep(sleepTime)

            tempAudioTrack.stop()
            Log.d("AudioDebug", "Playback finished freq $freq.")

        } catch (e: Exception) {
            Log.e("AudioDebug", "Error playing freq $freq: ${e.message}", e)
        } finally {
            tempAudioTrack?.release()
            Log.d("AudioDebug", "Track released freq $freq.")
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