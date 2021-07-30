package me.izzp.natchatandroid

import android.content.Context
import androidx.core.content.edit

object Prefs {

    private val sp by lazy { App.app.getSharedPreferences("prefs", Context.MODE_PRIVATE) }

    var name: String = ""
        get() {
            if (field != "") {
                return field
            }
            field = sp.getString("name", "")!!
            return field
        }
        set(value) {
            field = value
            sp.edit {
                putString("name", value)
            }
        }
}