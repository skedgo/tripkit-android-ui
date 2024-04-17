package com.skedgo.tripkit.ui.utils

import android.content.Context
import android.content.SharedPreferences
import com.skedgo.tripkit.common.model.TransportMode
import javax.inject.Inject

const val PREF_KEY_TRANSPORT_MODE = "TransportPreferences"

// TODO convert "TransportPreferences" usage to use this class instead
class TransportModeSharedPreference @Inject constructor(private val context: Context)  {

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREF_KEY_TRANSPORT_MODE, Context.MODE_PRIVATE)
    }

    fun isSchoolBusModeEnabled() = sharedPreferences.getBoolean(TransportMode.ID_SCHOOL_BUS, false)
}