package com.skedgo.tripkit.ui.routing.settings

import android.content.SharedPreferences
import android.content.res.Resources
import com.skedgo.tripkit.ui.R
import javax.inject.Inject

class UnitsRepositoryImpl @Inject constructor(
    private val resources: Resources,
    private val prefs: SharedPreferences
) : UnitsRepository {
    override fun putUnit(unit: String) {
        prefs.edit().putString(resources.getString(R.string.pref_distance_unit), unit).apply()
    }

    override fun getUnit(): String {
        return prefs.getString(resources.getString(R.string.pref_distance_unit), "auto") ?: "auto"
    }
}