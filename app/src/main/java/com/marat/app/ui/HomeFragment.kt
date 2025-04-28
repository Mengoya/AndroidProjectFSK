package com.marat.app.ui

import android.media.*
import android.os.*
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.marat.app.R
import com.marat.app.data.CommandStore
import com.marat.app.data.PrefManager
import com.marat.app.databinding.FragmentHomeBinding
import kotlin.concurrent.thread
import kotlin.math.cos
import kotlin.math.sin

class HomeFragment : Fragment() {

    private var _b: FragmentHomeBinding? = null
    private val b get() = _b!!

    private lateinit var store: CommandStore
    private lateinit var username: String
    private lateinit var adapter: CommandAdapter
    private val items = mutableListOf<Command>()

    private val iconPool = listOf(
        R.drawable.baseline_public_24,
        R.drawable.baseline_block_24,
        R.drawable.baseline_mouse_24,
        R.drawable.baseline_catching_pokemon_24,
        R.drawable.baseline_add_24
    )

    private val f0 = 17_500.0
    private val f1 = 17_800.0
    private val sr = 44_100
    private val bitMs = 100
    @Volatile private var playing = false

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentHomeBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        store     = CommandStore(requireContext())
        username  = PrefManager(requireContext()).getUsername() ?: "guest"

        if (store.load(username).isEmpty()) {
            val pre = listOf(1,0,1)
            store.save(username, listOf(
                Command("Open browser","Открывает Firefox",
                    R.drawable.baseline_public_24,  pre + listOf(1,0,1,0,1,0,1,0)),
                Command("Block mouse","Блокирует движение мыши",
                    R.drawable.baseline_block_24,   pre + listOf(0,1,0,1,0,1,0,1)),
                Command("Unblock mouse","Снимает блокировку мыши",
                    R.drawable.baseline_mouse_24,   pre + listOf(0,0,1,0,1,0,1,0))
            ))
        }

        items.addAll(store.load(username))

        adapter = CommandAdapter(
            items,
            onSend = { playAsync(it.bits) },
            onLong = { idx -> confirmDelete(idx) }
        )

        b.recycler.layoutManager = LinearLayoutManager(requireContext())
        b.recycler.adapter       = adapter

        b.fabAdd.setOnClickListener { showAddDialog() }
    }

    private fun showAddDialog() {
        val dlg   = layoutInflater.inflate(R.layout.dialog_add_command, null)
        val spin  = dlg.findViewById<Spinner>(R.id.spIcons)
        val title = dlg.findViewById<EditText>(R.id.etTitle)
        val desc  = dlg.findViewById<EditText>(R.id.etDesc)
        val bits  = dlg.findViewById<EditText>(R.id.etBits)

        spin.adapter = object : ArrayAdapter<Int>(
            requireContext(), R.layout.spinner_icon_item, iconPool
        ) {
            override fun getView(p: Int, c: View?, g: ViewGroup): View =
                (c ?: layoutInflater.inflate(R.layout.spinner_icon_item, g,false)).apply {
                    (this as ImageView).setImageResource(iconPool[p])
                }
            override fun getDropDownView(p: Int, c: View?, g: ViewGroup): View = getView(p,c,g)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Новая команда")
            .setView(dlg)
            .setPositiveButton("OK") { _, _ ->
                val bitsStr = bits.text.toString().trim()
                if (!bitsStr.matches(Regex("[01]{8}"))) {
                    Toast.makeText(requireContext(),
                        "Введите ровно 8 цифр 0/1", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val preamble = listOf(1,0,1)
                val fullBits = preamble + bitsStr.map { it.digitToInt() }

                val cmd = Command(
                    title.text.toString(),
                    desc.text.toString(),
                    iconPool[spin.selectedItemPosition],
                    fullBits
                )

                items.add(cmd)
                adapter.notifyItemInserted(items.size - 1)
                store.save(username, items)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun confirmDelete(index: Int) {
        AlertDialog.Builder(requireContext())
            .setMessage("Удалить «${items[index].title}»?")
            .setPositiveButton("Удалить") { _, _ ->
                items.removeAt(index)
                adapter.notifyItemRemoved(index)
                store.save(username, items)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun playAsync(bits: List<Int>) {
        if (playing) return
        thread { play(generate(bits)) }
    }
    private fun generate(bits: List<Int>): ShortArray {
        val spb  = (bitMs / 1000.0 * sr).toInt()
        val fade = (0.003 * sr).toInt()
        val buf  = ShortArray(spb * bits.size + 1000)
        var idx = 0; var ph = 0.0
        for (b in bits) {
            val f = if (b == 0) f0 else f1
            val step = 2 * Math.PI * f / sr
            repeat(spb) { i ->
                val env = when {
                    i < fade          -> 0.5 * (1 - cos(Math.PI * i / fade))
                    i >= spb - fade   -> 0.5 * (1 - cos(Math.PI * (spb - 1 - i) / fade))
                    else              -> 1.0
                }
                buf[idx++] = (sin(ph) * env * Short.MAX_VALUE).toInt().toShort()
                ph += step; if (ph > 2 * Math.PI) ph -= 2 * Math.PI
            }
        }
        return buf
    }
    private fun play(data: ShortArray) {
        playing = true
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sr)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(data.size * 2)
            .build()
        try {
            track.write(data, 0, data.size)
            track.play()
            while (track.playbackHeadPosition < data.size) Thread.sleep(10)
        } catch (e: Exception) { Log.e("Audio", "err", e) }
        finally { track.release(); playing = false }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}