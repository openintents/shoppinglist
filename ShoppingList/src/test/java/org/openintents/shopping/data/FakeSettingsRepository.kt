package org.openintents.shopping.data

/** In-memory [SettingsRepository] for framework-free ViewModel tests. */
class FakeSettingsRepository : SettingsRepository {
    private val bools = mutableMapOf<String, Boolean>()
    private val strings = mutableMapOf<String, String>()

    override fun getBoolean(key: String, default: Boolean): Boolean = bools[key] ?: default
    override fun setBoolean(key: String, value: Boolean) { bools[key] = value }
    override fun getString(key: String, default: String): String = strings[key] ?: default
    override fun setString(key: String, value: String) { strings[key] = value }
}
