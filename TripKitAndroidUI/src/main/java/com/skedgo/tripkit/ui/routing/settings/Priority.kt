package com.skedgo.tripkit.ui.routing.settings

sealed class Priority(private val _value: Int) {
  init {
    require(_value in 0..100) {
      "Value must be in range of 0 to 100. Found: $_value."
    }
  }

  data class Budget(val value: Int = 50) : Priority(value)
  data class Time(val value: Int = 50) : Priority(value)
  data class Environment(val value: Int = 50) : Priority(value)
  data class Convenience(val value: Int = 50) : Priority(value)
  data class Exercise(val value: Int = 50) : Priority(value)
}