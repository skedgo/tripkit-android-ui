package com.skedgo.tripkit.ui.routing.settings
enum class WalkingSpeed(val value: Int) {
  Slow(0), Medium(1), Fast(2)
}

fun Int.toWalkingSpeed(): WalkingSpeed = when (this) {
  0 -> WalkingSpeed.Slow
  1 -> WalkingSpeed.Medium
  2 -> WalkingSpeed.Fast
  else -> throw IllegalArgumentException("Unable to convert to WalkingSpeed for $this.")
}