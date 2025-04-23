package com.marat.app.ui

import android.media.*
import android.os.*
import android.util.Log
import android.view.*
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.marat.app.R
import com.marat.app.data.PrefManager
import kotlin.concurrent.thread
import kotlin.math.cos
import kotlin.math.sin

class HomeFragment: Fragment(R.layout.fragment_home) {

    private val freqZero = 17500.0
    private val freqOne = 17800.0
    private val sampleRate = 44100
    private val bitDurationMs = 100
    private val preamble = listOf(1,0,1)
    private val command1Data = listOf(1,0,1,0,1,0,1,0)
    private val command2Data = listOf(0,1,0,1,0,1,0,1)
    @Volatile private var isPlaying = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val send1: Button = view.findViewById(R.id.sendButton1)
        val send2: Button = view.findViewById(R.id.sendButton2)
        val logout: Button = view.findViewById(R.id.btnLogout)

        send1.setOnClickListener { playSoundAsync(preamble + command1Data) }
        send2.setOnClickListener { playSoundAsync(preamble + command2Data) }

        logout.setOnClickListener {
            PrefManager(requireContext()).logout()
            findNavController().navigate(R.id.action_home_to_login)
        }
    }

    private fun playSoundAsync(bits: List<Int>) {
        if (isPlaying) return
        thread {
            val buf = generateFSKSound(bits)
            playSound(buf)
        }
    }

    private fun generateFSKSound(bits: List<Int>): ShortArray {
        val samplesPerBit = (bitDurationMs / 1000.0 * sampleRate).toInt()
        val fadeSamples = (3 / 1000.0 * sampleRate).toInt()
        val payload = samplesPerBit * bits.size
        val tail = (0.01 * sampleRate).toInt()
        val buf = ShortArray(payload + tail)
        var phase = 0.0
        var idx = 0
        for (bit in bits) {
            val freq = if (bit == 0) freqZero else freqOne
            val step = 2 * Math.PI * freq / sampleRate
            for (i in 0 until samplesPerBit) {
                val env = when {
                    i < fadeSamples -> 0.5 * (1 - cos(Math.PI * i / fadeSamples))
                    i >= samplesPerBit - fadeSamples -> 0.5 * (1 - cos(Math.PI * (samplesPerBit - 1 - i) / fadeSamples))
                    else -> 1.0
                }
                buf[idx++] = (sin(phase) * env * Short.MAX_VALUE).toInt().toShort()
                phase += step
                if (phase >= 2 * Math.PI) phase -= 2 * Math.PI
            }
        }
        return buf
    }

    private fun playSound(data: ShortArray) {
        if (isPlaying) return
        isPlaying = true
        var track: AudioTrack? = null
        try {
            track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(data.size * 2)
                .build()
            track.write(data,0,data.size)
            track.play()
            while (track.playbackHeadPosition < data.size) Thread.sleep(10)
        } catch (e: Exception){ Log.e("AudioDebug","error ${e.message}",e) }
        finally { track?.release(); isPlaying = false }
    }

    override fun onStop() { super.onStop(); isPlaying=false }
}