package com.skedgo.tripkit.ui.routing.settings

import android.content.SharedPreferences
import android.content.res.Resources
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.routing.settings.WalkingSpeed
import com.skedgo.tripkit.ui.routing.settings.WalkingSpeedRepository
import com.skedgo.tripkit.ui.routing.settings.toWalkingSpeed
import io.reactivex.Completable
import io.reactivex.Observable

internal class WalkingSpeedRepositoryImpl constructor(
    private val resources: Resources,
    private val prefs: SharedPreferences
) : WalkingSpeedRepository {
  override suspend fun putWalkingSpeed(walkingSpeed: WalkingSpeed) = prefs.edit().putString(resources.getString(R.string.pref_walking_speed),
          walkingSpeed.value.toString()).apply()

  override suspend fun getWalkingSpeed(): WalkingSpeed {
      return prefs.getString(resources.getString(R.string.pref_walking_speed), null)?.toInt()?.toWalkingSpeed()
              ?: WalkingSpeed.Medium
    }
}