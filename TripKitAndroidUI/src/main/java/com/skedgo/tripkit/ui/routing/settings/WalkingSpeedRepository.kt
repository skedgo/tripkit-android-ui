package com.skedgo.tripkit.ui.routing.settings

import io.reactivex.Completable
import io.reactivex.Observable

interface WalkingSpeedRepository {
  fun putWalkingSpeed(walkingSpeed: WalkingSpeed): Completable
  fun getWalkingSpeed(): Observable<WalkingSpeed>
}