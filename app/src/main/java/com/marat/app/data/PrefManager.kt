package com.marat.app.data

import android.content.Context
import java.security.MessageDigest

class PrefManager(context: Context) {

    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun isLoggedIn()      = prefs.getBoolean(KEY_IS_LOGGED, false)
    fun getUsername()     = prefs.getString(KEY_USER, null)

    fun register(u: String, p: String): Boolean {
        if (prefs.contains(KEY_USER)) return false
        prefs.edit().putString(KEY_USER, u)
            .putString(KEY_PASS, hash(p)).apply()
        return true
    }

    fun login(u: String, p: String): Boolean {
        val ok = u == prefs.getString(KEY_USER, null) &&
                hash(p) == prefs.getString(KEY_PASS, null)
        if (ok) prefs.edit().putBoolean(KEY_IS_LOGGED, true).apply()
        return ok
    }

    fun logout() = prefs.edit().putBoolean(KEY_IS_LOGGED, false).apply()

    fun changePassword(old: String, new: String): Boolean {
        if (hash(old) != prefs.getString(KEY_PASS, null)) return false
        prefs.edit().putString(KEY_PASS, hash(new)).apply()
        return true
    }

    private fun hash(t: String) =
        MessageDigest.getInstance("SHA-256")
            .digest(t.toByteArray())
            .joinToString("") { "%02x".format(it) }

    companion object {
        private const val KEY_USER = "user"
        private const val KEY_PASS = "pass"
        private const val KEY_IS_LOGGED = "logged"
    }
}