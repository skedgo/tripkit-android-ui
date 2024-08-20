package com.skedgo.tripkit.ui.routing.settings

interface WalkingSpeedRepository {
    suspend fun putWalkingSpeed(walkingSpeed: WalkingSpeed)
    suspend fun getWalkingSpeed(): WalkingSpeed
}