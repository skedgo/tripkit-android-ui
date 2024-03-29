package com.skedgo.tripkit.ui.routing.settings

import android.content.SharedPreferences
import android.content.res.Resources
import com.skedgo.tripkit.ui.R

internal class CyclingSpeedRepositoryImpl constructor(
    private val resources: Resources,
    private val prefs: SharedPreferences
) : CyclingSpeedRepository {
  override suspend fun putCyclingSpeed(cyclingSpeed: CyclingSpeed) =
          prefs.edit().putString(resources.getString(R.string.pref_cycling_speed),
                  cyclingSpeed.value.toString()).apply()

  override suspend fun getCyclingSpeed(): CyclingSpeed {
      return prefs.getString(resources.getString(R.string.pref_cycling_speed), null)?.toInt()?.toCyclingSpeed()
              ?: CyclingSpeed.Medium

  }
}