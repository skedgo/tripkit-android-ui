package com.skedgo.tripkit.ui.tripresults

import android.content.Context
import android.content.SharedPreferences
import com.skedgo.TripKit
import com.skedgo.tripkit.common.model.TransportMode
import com.skedgo.tripkit.ui.utils.PREF_KEY_TRANSPORT_MODE
import com.skedgo.tripkit.ui.utils.isWheelchairModeSelected
import com.skedgo.tripkit.ui.utils.TransportModeDefaults


/**
 * The TripResultListFragment uses this class to decide which results should be shown.
 */
interface TripResultTransportViewFilter {
    fun isSelected(mode: String): Boolean
    fun isMinimized(mode: String): Boolean
    fun setSelected(mode: String, selected: Boolean)
    fun setMinimized(mode: String, minimized: Boolean)
}

const val PREF_KEY_IS_DEFAULT_CONFIG_SET = "pref_key_is_set_default_config"

class PrefsBasedTransportViewFilter(val context: Context) : TripResultTransportViewFilter {

    val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_KEY_TRANSPORT_MODE, Context.MODE_PRIVATE)


    init {
        if (!prefs.getBoolean(PREF_KEY_IS_DEFAULT_CONFIG_SET, false)) {
            TransportModeDefaults.getDefaultTransportModes().forEach {
                prefs.edit().putBoolean(it, true).apply()
            }

            prefs.edit().putBoolean(PREF_KEY_IS_DEFAULT_CONFIG_SET, true).apply()
        }

    }

    override fun isSelected(mode: String): Boolean {
        if (mode == TransportMode.ID_WHEEL_CHAIR) {
            return prefs.isWheelchairModeSelected()
        }

        return prefs.getBoolean(mode, false)
    }

    override fun isMinimized(mode: String): Boolean {
        return prefs.getBoolean("{$mode}-minimized", false)
    }

    override fun setSelected(mode: String, selected: Boolean) {
        if (mode == TransportMode.ID_WHEEL_CHAIR) {
            prefs.edit().putBoolean(TransportMode.ID_WALK, !selected).apply()
        } else {
            prefs.edit().putBoolean(mode, selected).apply()
        }
    }

    override fun setMinimized(mode: String, minimized: Boolean) {
        prefs.edit().putBoolean("${mode}-minimized", minimized).apply()
    }
}

class PermissiveTransportViewFilter : TripResultTransportViewFilter {
    var showWalking = true;

    override fun isSelected(mode: String): Boolean {
        if (mode == TransportMode.ID_WALK) return showWalking
        if (mode == TransportMode.ID_WHEEL_CHAIR) return !showWalking
        return true
    }

    override fun isMinimized(mode: String): Boolean {
        return false
    }

    override fun setSelected(mode: String, selected: Boolean) {
        if (mode == TransportMode.ID_WALK) {
            showWalking = true
        } else if (mode == TransportMode.ID_WHEEL_CHAIR) {
            showWalking = false
        }
    }

    override fun setMinimized(mode: String, minimized: Boolean) {}

}