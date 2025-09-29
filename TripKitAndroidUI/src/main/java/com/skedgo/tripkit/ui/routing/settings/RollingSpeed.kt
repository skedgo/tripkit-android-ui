package com.skedgo.tripkit.ui.routing.settings

enum class RollingSpeed(val value: Int) {
    Slow(0), Medium(1), Fast(2), Impaired(-1)
}

fun Int.toRollingSpeed(): RollingSpeed = when (this) {
    0 -> RollingSpeed.Slow
    1 -> RollingSpeed.Medium
    2 -> RollingSpeed.Fast
    else -> RollingSpeed.Impaired
}