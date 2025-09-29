package com.skedgo.tripkit.ui.routing.settings

import android.content.SharedPreferences
import android.content.res.Resources
import com.skedgo.tripkit.ui.R

internal class RollingSpeedRepositoryImpl constructor(
    private val resources: Resources, private val prefs: SharedPreferences
) : RollingSpeedRepository {
    override fun putRollingSpeed(rollingSpeed: RollingSpeed) {
        prefs.edit().putString(
                resources.getString(R.string.pref_rolling_speed), rollingSpeed.value.toString()
            ).apply()
    }

    override fun getRollingSpeed(): RollingSpeed {
        val speed = prefs.getString(resources.getString(R.string.pref_rolling_speed), null)?.toInt()
        return speed?.toRollingSpeed() ?: RollingSpeed.Medium
    }
}