package skedgo.tripgo.agenda.legacy

import android.content.SharedPreferences
import android.content.res.Resources
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.routing.settings.CyclingSpeed
import com.skedgo.tripkit.ui.routing.settings.CyclingSpeedRepository
import com.skedgo.tripkit.ui.routing.settings.toCyclingSpeed
import io.reactivex.Completable
import io.reactivex.Observable

/** TODO: Refactor to move this class to TripGoData. */
internal class CyclingSpeedRepositoryImpl constructor(
    private val resources: Resources,
    private val prefs: SharedPreferences
) : CyclingSpeedRepository {
  override fun putCyclingSpeed(cyclingSpeed: CyclingSpeed): Completable {
    // FIXME: Should use this Repository to change cycling speed.
    TODO("Not implemented yet.")
  }

  override fun getCyclingSpeed(): Observable<CyclingSpeed>
      = Observable
      .fromCallable {
        prefs.getString(resources.getString(R.string.pref_cycling_speed), null)
      }
      .filter { !it.isNullOrEmpty() }
      .map { it.toInt().toCyclingSpeed() }
      .defaultIfEmpty(CyclingSpeed.Medium)
}