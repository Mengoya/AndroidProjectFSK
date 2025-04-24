package com.marat.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.marat.app.databinding.ItemCommandBinding

class CommandAdapter(
    private val items: List<Command>,
    private val click: (Command) -> Unit
) : RecyclerView.Adapter<CommandAdapter.VH>() {

    inner class VH(val b: ItemCommandBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(p: ViewGroup, vType: Int): VH =
        VH(ItemCommandBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) = with(h.b) {
        val item = items[pos]
        ivIcon.setImageResource(item.iconRes)
        tvTitle.text = item.title
        tvDesc.text  = item.description
        root.setOnClickListener { click(item) }
    }
}