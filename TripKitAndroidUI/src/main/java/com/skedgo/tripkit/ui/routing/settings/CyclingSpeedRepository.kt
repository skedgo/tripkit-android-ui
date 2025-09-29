package com.skedgo.tripkit.ui.routing.settings

interface CyclingSpeedRepository {
    fun putCyclingSpeed(cyclingSpeed: CyclingSpeed)
    fun getCyclingSpeed(): CyclingSpeed
}