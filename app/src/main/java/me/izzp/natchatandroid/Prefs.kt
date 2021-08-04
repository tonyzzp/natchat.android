package me.izzp.natchatandroid

import android.os.Build
import androidx.preference.PreferenceManager

object Prefs {

    private val sp by lazy { PreferenceManager.getDefaultSharedPreferences(App.app) }

    val serverHost get() = sp.getString("server_host", "10.0.0.1")
    val serverPort: Int
        get() {
            val sport = sp.getString("server_port", "13688")!!.trim()
            val port = sport.toIntOrNull()
            return port ?: 13688
        }
    val name get() = sp.getString("name", Build.MODEL) ?: "nobody"
}