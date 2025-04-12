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
import kotlin.math.floor

class MainActivity : AppCompatActivity() {

    private val freqZero = 17500.0
    private val freqOne = 18000.0
    private val sampleRate = 44100
    private val bitDurationMs = 100
    private val fadeDurationMs = 75 // Увеличено
    private val preamble = listOf(1, 0, 1)
    private val dataBitsCount = 8 // Возвращено 8 бит

    private val command1Data = listOf(1, 0, 1, 0, 1, 0, 1, 0) // 8 бит
    private val command2Data = listOf(0, 1, 0, 1, 0, 1, 0, 1) // 8 бит

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

    private fun generateFSKSound(bits: List<Int>): ShortArray? {
        val totalDurationMs = bits.size * bitDurationMs
        val numSamples = (totalDurationMs / 1000.0 * sampleRate).toInt()
        val samplesPerBit = (bitDurationMs / 1000.0 * sampleRate).toInt()
        val fadeSamples = (fadeDurationMs / 1000.0 * sampleRate).toInt()

        if (numSamples <= 0 || samplesPerBit <= 0) {
            Log.e("AudioDebug", "Invalid sample calculation. numSamples=$numSamples, samplesPerBit=$samplesPerBit")
            return null
        }

        if (numSamples <= 2 * fadeSamples && fadeSamples > 0) {
            Log.w("AudioDebug", "Duration ($totalDurationMs ms) too short for fade ($fadeDurationMs ms)! Generating without fade.")
            val generatedSound = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val bitIndex = floor(i.toDouble() / samplesPerBit).toInt().coerceIn(0, bits.size - 1)
                val currentBit = bits[bitIndex]
                val frequency = if (currentBit == 0) freqZero else freqOne
                val time = i.toDouble() / sampleRate
                val sampleValue = sin(2.0 * Math.PI * frequency * time)
                generatedSound[i] = (sampleValue * Short.MAX_VALUE).toInt().toShort()
            }
            return generatedSound
        }

        val generatedSound = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val bitIndex = floor(i.toDouble() / samplesPerBit).toInt().coerceIn(0, bits.size - 1)
            val currentBit = bits[bitIndex]
            val frequency = if (currentBit == 0) freqZero else freqOne

            val time = i.toDouble() / sampleRate
            val sampleValue = sin(2.0 * Math.PI * frequency * time)

            val envelope: Double = when {
                i < fadeSamples && fadeSamples > 0 -> i.toDouble() / (fadeSamples -1).coerceAtLeast(1).toDouble()
                i >= numSamples - fadeSamples && fadeSamples > 0 -> (numSamples - 1 - i).toDouble() / (fadeSamples -1).coerceAtLeast(1).toDouble()
                else -> 1.0
            }.coerceIn(0.0, 1.0)

            generatedSound[i] = (sampleValue * envelope * Short.MAX_VALUE).toInt().toShort()
        }
        Log.d("AudioDebug", "FSK Sound generated. Bits: ${bits.size}, Samples: $numSamples")
        return generatedSound
    }

    private fun playSoundInternal(currentSoundData: ShortArray, bits: List<Int>) {
        if (isPlaying) {
            Log.w("AudioDebug", "playSoundInternal called while isPlaying is true.")
            return
        }
        isPlaying = true

        val totalDurationMs = bits.size * bitDurationMs
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBufferSize, currentSoundData.size * 2)
        var tempAudioTrack: AudioTrack? = null

        try {
            tempAudioTrack = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC).build()

            val writeResult = tempAudioTrack.write(currentSoundData, 0, currentSoundData.size)
            if (writeResult != currentSoundData.size) {
                Log.e("AudioDebug", "Failed write. Wrote $writeResult / ${currentSoundData.size}")
                throw IllegalStateException("Failed write audio data")
            }

            Log.d("AudioDebug", "Playing FSK sound...")
            tempAudioTrack.play()

            val sleepTime = (totalDurationMs + fadeDurationMs + 50).toLong()
            Thread.sleep(sleepTime)

            tempAudioTrack.stop()
            Log.d("AudioDebug", "FSK Playback finished.")

        } catch (e: Exception) {
            Log.e("AudioDebug", "Error playing FSK sound: ${e.message}", e)
        } finally {
            tempAudioTrack?.release()
            Log.d("AudioDebug", "FSK Track released.")
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