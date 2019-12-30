package com.skedgo.tripkit.ui.core.modeprefs

import android.content.SharedPreferences
import android.content.res.Resources
import com.skedgo.tripkit.common.model.TransportMode
import com.skedgo.tripkit.data.util.onChanged
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.module.IsModeIncludedInTripsRepositoryModule
import com.skedgo.tripkit.ui.routingresults.IsModeIncludedInTripsRepository
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Named

internal class IsModeIncludedInTripsRepositoryImpl @Inject constructor(
    @Named(IsModeIncludedInTripsRepositoryModule.IsModeIncludedInTripsPrefs)
    private val prefs: SharedPreferences,
    private val resources: Resources
) : IsModeIncludedInTripsRepository {
  override fun setModeIncluded(modeId: String, isIncluded: Boolean): Completable =
      Completable.fromCallable {
        prefs.edit()
            .putBoolean(modeId, isIncluded)
            .apply()
      }

  override fun onChanged(): Observable<String> =
      prefs.onChanged()

  override fun isModeIncludedForRouting(modeId: String): Single<Boolean> =
      Single.fromCallable {
        when (modeId) {
          // As walking mode is always included in routing requests,
          // we'll ignore any value is persisted from disk.
          TransportMode.ID_WALK -> true
          else -> prefs.getBoolean(modeId, resources.getBoolean(R.bool.are_modes_included_by_default))
        }
      }

}