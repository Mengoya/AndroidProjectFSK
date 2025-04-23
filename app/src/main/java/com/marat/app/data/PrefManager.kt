package com.marat.app.data

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

class PrefManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED, false)

    fun register(username: String, password: String): Boolean {
        if (prefs.contains(KEY_USER)) return false          // уже есть учётка
        prefs.edit()
            .putString(KEY_USER, username)
            .putString(KEY_PASS, hash(password))
            .apply()
        return true
    }

    fun login(username: String, password: String): Boolean {
        val ok = username == prefs.getString(KEY_USER, null) &&
                hash(password) == prefs.getString(KEY_PASS, null)
        if (ok) prefs.edit().putBoolean(KEY_IS_LOGGED, true).apply()
        return ok
    }

    fun logout() = prefs.edit().putBoolean(KEY_IS_LOGGED, false).apply()

    private fun hash(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
            .joinToString("") { "%02x".format(it) }

    companion object {
        private const val KEY_USER = "user"
        private const val KEY_PASS = "pass"
        private const val KEY_IS_LOGGED = "logged"
    }
}