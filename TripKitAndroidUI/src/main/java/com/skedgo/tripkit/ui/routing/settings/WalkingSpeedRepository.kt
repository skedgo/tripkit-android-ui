package com.skedgo.tripkit.ui.routing.settings

interface WalkingSpeedRepository {
    fun putWalkingSpeed(walkingSpeed: WalkingSpeed)
    fun getWalkingSpeed(): WalkingSpeed
}