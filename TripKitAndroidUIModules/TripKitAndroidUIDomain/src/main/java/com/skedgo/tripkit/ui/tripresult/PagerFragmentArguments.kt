package com.skedgo.tripkit.ui.tripresult

sealed class PagerFragmentArguments

// TODO Refactor this
class FromRoutes(val tripGroupId: String, val sortOrder: Int, val requestId: String, val arriveBy: Long) : PagerFragmentArguments(), HasInitialTripGroupId {
  override fun tripGroupId(): String = tripGroupId
}

class SingleTrip(val tripGroupId: String) : PagerFragmentArguments(), HasInitialTripGroupId {
  override fun tripGroupId(): String = tripGroupId
}

//class FavoriteTrip(val favoriteTripId: String) : PagerFragmentArguments()

interface HasInitialTripGroupId {
  fun tripGroupId(): String
}