package com.skedgo.tripkit.analytics

enum class TripSource(val value: String) {
  Agenda("agenda"),
  Booking("booking"),
  External("external"),
  Favorite("favorite"),
  FavoriteTrip("favoriteTrip"),
  Manual("manual"),
  Unknown("unknown")
}

fun String.toTripSource(): TripSource {
  return TripSource.values().first { it.value == this }
}