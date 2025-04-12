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
import kotlin.math.min
import kotlin.math.sin

class MainActivity : AppCompatActivity() {

    private val frequency = 18000.0
    private val durationMs = 300
    private val sampleRate = 44100
    private val fadeDurationMs = 10

    private var audioTrack: AudioTrack? = null
    private var soundData: ShortArray? = null
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

        val sendButton: Button = findViewById(R.id.sendButton)
        sendButton.setOnClickListener {
            if (!isPlaying) {
                thread {
                    playSound()
                }
            } else {
                Log.d("AudioDebug", "Already playing, button press ignored.")
            }
        }

        generateSound()
    }

    private fun generateSound() {
        val numSamples = (durationMs / 1000.0 * sampleRate).toInt()
        val fadeSamples = (fadeDurationMs / 1000.0 * sampleRate).toInt()
        val generatedSound = ShortArray(numSamples)

        if (numSamples <= 2 * fadeSamples && fadeSamples > 0) {
            Log.w("AudioDebug", "Duration ($durationMs ms) is too short for the given fade time ($fadeDurationMs ms)! Generating without fade.")
            for (i in 0 until numSamples) {
                val sample = sin(2.0 * Math.PI * i.toDouble() / (sampleRate / frequency))
                generatedSound[i] = (sample * Short.MAX_VALUE).toInt().toShort()
            }
            soundData = generatedSound
            return
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
        soundData = generatedSound
        Log.d("AudioDebug", "Sound generated. Samples: $numSamples, FadeSamples: $fadeSamples")
    }

    private fun prepareAudioTrack() {
        val currentSoundData = soundData ?: return
        if (audioTrack != null && audioTrack?.state != AudioTrack.STATE_UNINITIALIZED) {
            Log.d("AudioDebug", "AudioTrack already initialized.")
            return
        }


        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBufferSize, currentSoundData.size * 2) // *2 т.к. Short = 2 байта

        try {
            audioTrack?.release()

            audioTrack = AudioTrack.Builder()
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

            val writeResult = audioTrack?.write(currentSoundData, 0, currentSoundData.size)
            if (writeResult != currentSoundData.size) {
                Log.e("AudioDebug", "Failed to write full data to AudioTrack. Wrote $writeResult instead of ${currentSoundData.size}")
                audioTrack?.release()
                audioTrack = null
                return
            }
            Log.d("AudioDebug", "AudioTrack prepared and data loaded. State: ${audioTrack?.state}")

        } catch (e: Exception) {
            Log.e("AudioDebug", "Error preparing AudioTrack: ${e.message}", e)
            audioTrack?.release()
            audioTrack = null
        }
    }


    private fun playSound() {
        if (isPlaying) {
            Log.w("AudioDebug", "playSound called while already playing.")
            return
        }

        val currentSoundData = soundData ?: run {
            Log.e("AudioDebug", "Sound data is null, cannot play.")
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
                Log.e("AudioDebug", "Failed to write full data to temp AudioTrack. Wrote $writeResult instead of ${currentSoundData.size}")
                throw IllegalStateException("Failed to write audio data") // Выбросим исключение, чтобы попасть в catch/finally
            }

            Log.d("AudioDebug", "Playing sound...")
            tempAudioTrack.play()

            val sleepTime = (durationMs + 50).toLong()
            Thread.sleep(sleepTime)

            tempAudioTrack.stop()
            Log.d("AudioDebug", "Playback finished.")

        } catch (e: Exception) {
            Log.e("AudioDebug", "Error playing sound: ${e.message}", e)
        } finally {
            tempAudioTrack?.release()
            Log.d("AudioDebug", "Temporary AudioTrack released.")
            isPlaying = false
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d("AudioDebug", "onStop called.")
        if (audioTrack != null) {
            Log.d("AudioDebug", "Releasing persistent AudioTrack in onStop.")
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        }
        isPlaying = false
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("AudioDebug", "onDestroy called.")
        if (audioTrack != null) {
            Log.d("AudioDebug", "Ensuring persistent AudioTrack is released in onDestroy.")
            audioTrack?.release()
            audioTrack = null
        }
    }
}