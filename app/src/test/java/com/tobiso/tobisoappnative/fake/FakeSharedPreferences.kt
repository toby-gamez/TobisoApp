package com.tobiso.tobisoappnative.fake

import android.content.SharedPreferences

/** In-memory [SharedPreferences] for JVM tests; edits apply immediately and listeners fire synchronously. */
class FakeSharedPreferences : SharedPreferences {
    private val data = LinkedHashMap<String, Any?>()
    private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun getAll(): MutableMap<String, *> = HashMap(data)
    override fun getString(key: String, defValue: String?) = data[key] as? String ?: defValue
    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String, defValues: MutableSet<String>?) = (data[key] as? Set<String>)?.toMutableSet() ?: defValues
    override fun getInt(key: String, defValue: Int) = data[key] as? Int ?: defValue
    override fun getLong(key: String, defValue: Long) = data[key] as? Long ?: defValue
    override fun getFloat(key: String, defValue: Float) = data[key] as? Float ?: defValue
    override fun getBoolean(key: String, defValue: Boolean) = data[key] as? Boolean ?: defValue
    override fun contains(key: String) = data.containsKey(key)
    override fun edit(): SharedPreferences.Editor = Editor()
    override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener) { listeners += l }
    override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener) { listeners -= l }

    private inner class Editor : SharedPreferences.Editor {
        private val pending = LinkedHashMap<String, Any?>()
        private var clear = false
        private fun put(k: String, v: Any?) = apply { pending[k] = v }
        override fun putString(k: String, v: String?) = put(k, v)
        override fun putStringSet(k: String, v: MutableSet<String>?) = put(k, v?.toSet())
        override fun putInt(k: String, v: Int) = put(k, v)
        override fun putLong(k: String, v: Long) = put(k, v)
        override fun putFloat(k: String, v: Float) = put(k, v)
        override fun putBoolean(k: String, v: Boolean) = put(k, v)
        override fun remove(k: String) = put(k, null)
        override fun clear() = apply { clear = true }
        override fun commit(): Boolean { apply(); return true }
        override fun apply() {
            val changed = mutableListOf<String>()
            if (clear) { changed += data.keys; data.clear() }
            pending.forEach { (k, v) -> if (v == null) data.remove(k) else data[k] = v; changed += k }
            changed.forEach { k -> listeners.toList().forEach { it.onSharedPreferenceChanged(this@FakeSharedPreferences, k) } }
        }
    }
}
