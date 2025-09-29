package com.skedgo.tripkit.ui.routing.settings

interface RollingSpeedRepository {
    fun putRollingSpeed(rollingSpeed: RollingSpeed)
    fun getRollingSpeed(): RollingSpeed
}