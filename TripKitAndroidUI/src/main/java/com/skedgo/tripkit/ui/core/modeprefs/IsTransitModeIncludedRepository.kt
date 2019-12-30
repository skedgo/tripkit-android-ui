package com.skedgo.tripkit.ui.core.modeprefs

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

typealias TransitModeId = String
typealias IsIncluded = Boolean

interface IsTransitModeIncludedRepository {
  fun isTransitModeIncluded(modeId: TransitModeId): Single<Boolean>
  fun getAll(): Observable<IsTransitModeIncluded>
  fun setIsTransitModeIncluded(modeId: TransitModeId, isIncluded: IsIncluded): Completable
}
