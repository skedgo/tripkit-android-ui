package com.skedgo.tripkit.ui.utils

import android.content.Context
import android.content.SharedPreferences
import com.skedgo.tripkit.common.model.TransportMode
import javax.inject.Inject

const val PREF_KEY_TRANSPORT_MODE = "TransportPreferences"

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

    private fun getModeDefaultValue(modeId: String): Boolean =
        TransportModeDefaults.isModeInDefaultModes(modeId)
}