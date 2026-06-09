package org.openintents.shopping.data

import android.content.Context
import android.preference.PreferenceManager

/**
 * Read/write access to the app's settings. Backed by the SAME default
 * SharedPreferences that the legacy PreferenceActivity.getXxxFromPrefs() helpers
 * read, so a Compose settings screen drives the existing app behavior unchanged.
 */
interface SettingsRepository {
    fun getBoolean(key: String, default: Boolean): Boolean
    fun setBoolean(key: String, value: Boolean)
    fun getString(key: String, default: String): String
    fun setString(key: String, value: String)
}

@Suppress("DEPRECATION") // android.preference is the file the legacy getters use
class SharedPrefsSettingsRepository(context: Context) : SettingsRepository {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

    override fun getBoolean(key: String, default: Boolean): Boolean =
        prefs.getBoolean(key, default)

    override fun setBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    override fun getString(key: String, default: String): String =
        prefs.getString(key, default) ?: default

    override fun setString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
}
