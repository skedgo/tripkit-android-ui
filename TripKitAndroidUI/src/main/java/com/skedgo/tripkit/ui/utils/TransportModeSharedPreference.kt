package com.skedgo.tripkit.ui.utils

import android.content.Context
import android.content.SharedPreferences
import com.skedgo.tripkit.common.model.TransportMode
import javax.inject.Inject

const val PREF_KEY_TRANSPORT_MODE = "TransportPreferences"

/**
 * Walking and wheelchair are two faces of a single user choice: the wheelchair mode is selected
 * exactly when walking is not. Only [TransportMode.ID_WALK] is ever written to
 * [PREF_KEY_TRANSPORT_MODE] (see `PrefsBasedTransportViewFilter.setSelected`), so every reader
 * must derive the wheelchair state from it instead of keeping a second flag.
 *
 * This is the single source of truth for "is the user routing by wheelchair?". It is deliberately
 * *not* [com.skedgo.tripkit.TripPreferences.isWheelchairPreferred], which is the separate
 * "wheelchair information" option and only asks for transit accessibility information.
 */
fun SharedPreferences.isWheelchairModeSelected(): Boolean =
    !getBoolean(TransportMode.ID_WALK, true)

/**
 * Convenience overload of [SharedPreferences.isWheelchairModeSelected] for callers that only hold
 * a [Context].
 */
fun Context.isWheelchairModeSelected(): Boolean =
    getSharedPreferences(PREF_KEY_TRANSPORT_MODE, Context.MODE_PRIVATE).isWheelchairModeSelected()

object TransportModeDefaults {
    private var defaultTransportModes = listOf(
        TransportMode.ID_PUBLIC_TRANSPORT,
        TransportMode.ID_SCHOOL_BUS,
        TransportMode.ID_BICYCLE,
        TransportMode.ID_TAXI,
        TransportMode.ID_CAR,
        TransportMode.ID_MOTORBIKE,
        TransportMode.ID_WALK,
    )

    fun getDefaultTransportModes() = defaultTransportModes

    fun setDefaultModes(vararg modeIds: String) {
        defaultTransportModes = modeIds.toList()
    }

    fun isModeInDefaultModes(modeId: String) =
        defaultTransportModes.contains(modeId)
}

// TODO convert "TransportPreferences" usage to use this class instead
class TransportModeSharedPreference @Inject constructor(private val context: Context) {

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREF_KEY_TRANSPORT_MODE, Context.MODE_PRIVATE)
    }

    fun isSchoolBusModeEnabled() = sharedPreferences.getBoolean(TransportMode.ID_SCHOOL_BUS, false)

    fun isTransportModeEnabled(modeId: String) =
        sharedPreferences.getBoolean(modeId, getModeDefaultValue(modeId))

    fun saveTransportModeState(modeId: String, enabled: Boolean) {
        sharedPreferences.edit().putBoolean(modeId, enabled).apply()
    }

    fun hasTransportMode(modeId: String): Boolean = sharedPreferences.contains(modeId)

    /**
     * Whether the user selected the wheelchair mode rather than walking.
     * See [SharedPreferences.isWheelchairModeSelected].
     */
    fun isWheelchairModeSelected(): Boolean = sharedPreferences.isWheelchairModeSelected()

    private fun getModeDefaultValue(modeId: String): Boolean =
        TransportModeDefaults.isModeInDefaultModes(modeId)
}