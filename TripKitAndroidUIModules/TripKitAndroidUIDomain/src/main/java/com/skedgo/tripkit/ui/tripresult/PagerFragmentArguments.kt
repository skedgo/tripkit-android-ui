package com.skedgo.tripkit.ui.tripresult

sealed class PagerFragmentArguments

// TODO Refactor this
class FromRoutes(
    val tripGroupId: String,
    val tripId: Long?,
    val sortOrder: Int,
    val requestId: String,
    val arriveBy: Long,
) : PagerFragmentArguments(), HasInitialTripGroupId {
    override fun tripGroupId(): String = tripGroupId
    override fun tripId(): Long? = tripId
}

class SingleTrip(val tripGroupId: String, val tripId: Long?) : PagerFragmentArguments(),
    HasInitialTripGroupId {
    override fun tripGroupId(): String = tripGroupId
    override fun tripId(): Long? = tripId
}

class FavoriteTrip(val favoriteTripId: String) : PagerFragmentArguments()

interface HasInitialTripGroupId {
    fun tripGroupId(): String
    fun tripId(): Long?
}
