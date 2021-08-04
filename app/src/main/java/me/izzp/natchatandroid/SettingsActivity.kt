package me.izzp.natchatandroid

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.post("settings_changed")
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        private val serverHost by lazy { findPreference<EditTextPreference>("server_host")!! }
        private val serverPort by lazy { findPreference<EditTextPreference>("server_port")!! }
        private val name by lazy { findPreference<EditTextPreference>("name")!! }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            println("SettingsFragment.onCreatePreferences:$rootKey")
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            serverHost.text = Prefs.serverHost
            serverPort.text = Prefs.serverPort.toString()
            name.text = Prefs.name
            serverPort.setOnPreferenceChangeListener { preference, newValue ->
                val port = (newValue as String).trim().toIntOrNull()
                return@setOnPreferenceChangeListener port != null
            }
        }
    }
}