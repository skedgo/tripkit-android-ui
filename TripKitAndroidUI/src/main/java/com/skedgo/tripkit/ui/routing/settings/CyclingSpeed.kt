package com.skedgo.tripkit.ui.routing.settings

enum class CyclingSpeed(val value: Int) {
    Slow(0), Medium(1), Fast(2), Impaired(-1)
}

fun Int.toCyclingSpeed(): CyclingSpeed = when (this) {
    0 -> CyclingSpeed.Slow
    1 -> CyclingSpeed.Medium
    2 -> CyclingSpeed.Fast
    else -> CyclingSpeed.Impaired
}