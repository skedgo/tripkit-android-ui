package com.skedgo.tripkit.ui.routing.settings

import io.reactivex.Completable
import io.reactivex.Observable

interface CyclingSpeedRepository {
  suspend fun putCyclingSpeed(cyclingSpeed: CyclingSpeed)
  suspend fun getCyclingSpeed(): CyclingSpeed
}