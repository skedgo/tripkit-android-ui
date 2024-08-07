package com.skedgo.tripkit.ui.routing.settings

interface CyclingSpeedRepository {
    suspend fun putCyclingSpeed(cyclingSpeed: CyclingSpeed)
    suspend fun getCyclingSpeed(): CyclingSpeed
}