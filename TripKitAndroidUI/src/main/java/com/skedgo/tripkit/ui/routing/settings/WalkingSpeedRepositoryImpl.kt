package skedgo.tripgo.agenda.legacy

import android.content.SharedPreferences
import android.content.res.Resources
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.routing.settings.WalkingSpeed
import com.skedgo.tripkit.ui.routing.settings.WalkingSpeedRepository
import com.skedgo.tripkit.ui.routing.settings.toWalkingSpeed
import io.reactivex.Completable
import io.reactivex.Observable

/** TODO: Refactor to move this class to TripGoData. */
internal class WalkingSpeedRepositoryImpl constructor(
    private val resources: Resources,
    private val prefs: SharedPreferences
) : WalkingSpeedRepository {
  override fun putWalkingSpeed(walkingSpeed: WalkingSpeed): Completable {
    // FIXME: Should use this Repository to change walking speed.
    TODO("Not implemented yet.")
  }

  override fun getWalkingSpeed(): Observable<WalkingSpeed>
      = Observable
      .fromCallable {
        prefs.getString(resources.getString(R.string.pref_walking_speed), null)
      }
      .filter { !it.isNullOrEmpty() }
      .map { it.toInt().toWalkingSpeed() }
      .defaultIfEmpty(WalkingSpeed.Medium)
}