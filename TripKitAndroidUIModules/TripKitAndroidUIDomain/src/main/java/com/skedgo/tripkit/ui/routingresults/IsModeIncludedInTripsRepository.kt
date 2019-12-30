package com.skedgo.tripkit.ui.routingresults

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

interface IsModeIncludedInTripsRepository {
  fun isModeIncludedForRouting(modeId: String): Single<Boolean>
  fun setModeIncluded(modeId: String, isIncluded: Boolean): Completable
  fun onChanged(): Observable<String>
}