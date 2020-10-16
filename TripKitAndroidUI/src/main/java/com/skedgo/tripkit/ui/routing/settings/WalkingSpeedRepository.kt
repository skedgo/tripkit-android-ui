package com.skedgo.tripkit.ui.routing.settings

import io.reactivex.Completable
import io.reactivex.Observable

interface WalkingSpeedRepository {
  suspend fun putWalkingSpeed(walkingSpeed: WalkingSpeed)
  suspend fun getWalkingSpeed(): WalkingSpeed
}