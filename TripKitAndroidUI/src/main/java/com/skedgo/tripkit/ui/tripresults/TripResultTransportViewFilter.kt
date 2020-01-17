package com.skedgo.tripkit.ui.tripresults

import android.content.Context
import android.content.SharedPreferences


/**
 * The TripResultListFragment uses this class to decide which results should be shown.
 */
interface TripResultTransportViewFilter {
    fun isSelected(mode: String): Boolean
    fun isMinimized(mode: String): Boolean
    fun setSelected(mode: String, selected: Boolean)
    fun setMinimized(mode: String, minimized: Boolean)
}

internal class PrefsBasedTransportViewFilter(context: Context) : TripResultTransportViewFilter {
    val prefs: SharedPreferences = context.getSharedPreferences("TransportPreferences", Context.MODE_PRIVATE)

    override fun isSelected(mode: String): Boolean {
        return prefs.getBoolean(mode, true)
    }

    override fun isMinimized(mode: String): Boolean {
        return prefs.getBoolean("{$mode}-minimized", false)
    }

    override fun setSelected(mode: String, selected: Boolean) {
        prefs.edit().putBoolean(mode, selected).apply()
    }

    override fun setMinimized(mode: String, minimized: Boolean) {
        prefs.edit().putBoolean("${mode}-minimized", minimized).apply()
    }
}

internal class PermissiveTransportViewFilter() : TripResultTransportViewFilter {
    override fun isSelected(mode: String): Boolean {
        return true
    }

    override fun isMinimized(mode: String): Boolean {
        return false
    }

    override fun setSelected(mode: String, selected: Boolean) {}
    override fun setMinimized(mode: String, minimized: Boolean) {}
}