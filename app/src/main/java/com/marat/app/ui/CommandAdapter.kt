package com.marat.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.marat.app.databinding.ItemCommandBinding

class CommandAdapter(
    private val items: MutableList<Command>,
    private val onSend:  (Command) -> Unit,
    private val onLong:  (Int)     -> Unit
) : RecyclerView.Adapter<CommandAdapter.VH>() {

    inner class VH(val b: ItemCommandBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemCommandBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) = with(h.b) {
        val item = items[pos]

        ivIcon.setImageResource(item.iconRes)
        tvTitle.text = item.title
        tvDesc.text  = item.description

        btnSend.setOnClickListener  { onSend(item) }
        root.setOnClickListener     { onSend(item) }
        root.setOnLongClickListener { onLong(pos); true }
    }
}