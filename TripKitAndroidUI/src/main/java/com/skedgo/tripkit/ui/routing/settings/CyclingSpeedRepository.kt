package com.skedgo.tripkit.ui.routing.settings

import io.reactivex.Completable
import io.reactivex.Observable

interface CyclingSpeedRepository {
  fun putCyclingSpeed(cyclingSpeed: CyclingSpeed): Completable
  fun getCyclingSpeed(): Observable<CyclingSpeed>
}