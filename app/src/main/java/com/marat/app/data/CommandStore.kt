package com.marat.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.marat.app.ui.Command

class CommandStore(context: Context) {

    private val prefs = context.getSharedPreferences("commands_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun load(username: String): MutableList<Command> {
        val json = prefs.getString(key(username), null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<Command>>() {}.type
        return gson.fromJson(json, type)
    }

    fun save(username: String, list: List<Command>) {
        prefs.edit().putString(key(username), gson.toJson(list)).apply()
    }

    private fun key(u: String) = "cmd_$u"
}