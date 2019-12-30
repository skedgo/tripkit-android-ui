package com.skedgo.tripkit.model

import com.skedgo.tripkit.common.model.Query
import com.skedgo.tripkit.analytics.TripSource

sealed class ViewTrips {
  abstract fun query(): Query
  open fun additionalLocationInfo(): Int = -1
  open fun tripSource(): TripSource = TripSource.Unknown
  open val shouldPerformRouting = false

  data class ByManualA2bTripRequest(
      val query: Query,
      val additionalLocationInfo: Int
  ) : ViewTrips() {
    override fun query(): Query = query
    override fun additionalLocationInfo(): Int = additionalLocationInfo
    override fun tripSource(): TripSource = TripSource.Manual
    override val shouldPerformRouting: Boolean
      get() = true
  }

  data class ForFavorite(val query: Query) : ViewTrips() {
    override fun query(): Query = query
    override fun tripSource(): TripSource = TripSource.Favorite
    override val shouldPerformRouting = true
  }

  data class ForAgenda(val query: Query) : ViewTrips() {
    override fun query(): Query = query
    override fun tripSource(): TripSource = TripSource.Agenda
  }

  data class ForExternal(val query: Query) : ViewTrips() {
    override fun query(): Query = query
    override fun tripSource(): TripSource = TripSource.External
  }

  data class ForTimetable(val query: Query) : ViewTrips() {
    override fun query(): Query = query
    override val shouldPerformRouting = true
  }

  data class ForWidget(val query: Query) : ViewTrips() {
    override fun query(): Query = query
    override val shouldPerformRouting = true
  }

  data class ForTripBroken(val query: Query) : ViewTrips() {
    override fun query(): Query = query
  }

  data class ForViewLocation(val query: Query) : ViewTrips() {
    override fun query(): Query = query
    override val shouldPerformRouting = true
  }

  data class ForAlternativeTrips(val query: Query, val tripSource: TripSource) : ViewTrips() {
    override fun query(): Query = query
  }

  data class ForGoingUpFromSingleTripResult(val query: Query, val tripSource: TripSource) : ViewTrips() {
    override fun query(): Query = query
  }
}