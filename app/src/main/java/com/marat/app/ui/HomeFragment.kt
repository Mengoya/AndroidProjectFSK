package com.marat.app.ui

import android.media.*
import android.os.*
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.marat.app.R
import com.marat.app.databinding.FragmentHomeBinding
import kotlin.concurrent.thread
import kotlin.math.cos
import kotlin.math.sin

class HomeFragment : Fragment() {

    private var _b: FragmentHomeBinding? = null
    private val b get() = _b!!

    /* ------------ FSK параметры и данные команд ----------- */
    private val freqZero = 17_500.0
    private val freqOne  = 17_800.0
    private val sampleRate = 44_100
    private val bitMs = 100

    private val preamble = listOf(1,0,1)
    private val cmd1Bits = listOf(1,0,1,0,1,0,1,0)      // «Open browser»
    private val cmd2Bits = listOf(0,1,0,1,0,1,0,1)      // «Block mouse»

    @Volatile private var playing = false

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentHomeBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        val items = listOf(
            Command("Open browser", "Открывает Firefox",
                R.drawable.baseline_catching_pokemon_24, preamble + cmd1Bits),
            Command("Block mouse",  "Блокирует движение мыши",
                R.drawable.baseline_mouse_24,  preamble + cmd2Bits)
        )

        b.recycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = CommandAdapter(items) { playAsync(it.bits) }
        }
    }

    private fun playAsync(bits: List<Int>) {
        if (playing) return
        thread { play(generate(bits)) }
    }

    private fun generate(bits: List<Int>): ShortArray {
        val spb = (bitMs / 1000.0 * sampleRate).toInt()
        val fade = (0.003 * sampleRate).toInt()
        val buf = ShortArray(spb * bits.size + 1000)
        var idx = 0; var phase = 0.0
        for (bit in bits) {
            val freq = if (bit==0) freqZero else freqOne
            val step = 2*Math.PI*freq/sampleRate
            repeat(spb) { i ->
                val env = when {
                    i < fade -> 0.5*(1 - cos(Math.PI*i/fade))
                    i >= spb-fade -> 0.5*(1 - cos(Math.PI*(spb-1-i)/fade))
                    else -> 1.0
                }
                buf[idx++] = (sin(phase)*env*Short.MAX_VALUE).toInt().toShort()
                phase += step
                if (phase > 2*Math.PI) phase -= 2*Math.PI
            }
        }
        return buf
    }

    private fun play(data: ShortArray) {
        playing = true
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(data.size*2)
            .build()
        try {
            track.write(data,0,data.size)
            track.play()
            while (track.playbackHeadPosition < data.size) Thread.sleep(10)
        } catch (e: Exception){ Log.e("Audio", "err", e) }
        finally { track.release(); playing = false }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}