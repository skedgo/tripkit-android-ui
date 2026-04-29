package com.skedgo.tripkit.ui.routing.settings

enum class CyclingSpeed(val value: Int) {
    Slow(0), Medium(1), Fast(2)
}

/**
 * Maps stored preference values to cycling speed. Routing `cs` only supports 0–2;
 * legacy `-1` ("impaired") is treated as [CyclingSpeed.Slow].
 */
fun Int.toCyclingSpeed(): CyclingSpeed = when (this) {
    0 -> CyclingSpeed.Slow
    1 -> CyclingSpeed.Medium
    2 -> CyclingSpeed.Fast
    -1 -> CyclingSpeed.Slow
    else -> CyclingSpeed.Medium
}