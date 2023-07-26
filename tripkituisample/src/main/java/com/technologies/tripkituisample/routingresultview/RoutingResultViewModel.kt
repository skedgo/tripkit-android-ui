package com.technologies.tripkituisample.routingresultview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.common.model.Query
import com.skedgo.tripkit.common.model.TimeTag
import com.skedgo.tripkit.ui.core.RxViewModel
import javax.inject.Inject

class RoutingResultViewModel @Inject constructor() : RxViewModel() {

    private val _query = MutableLiveData<Query>()
    val query: LiveData<Query> = _query

    private val startLocation: Location =
        Location().apply {
            address = "Edgecliff NSW 2027, Australia"
            bearing = 2147483647
            isExact = false
            lat = -33.8789589
            lon = 151.2375895
            averageRating = -1.0f
            source = "google"
            name = "Edgecliff"
        }

    private val destinationLocation: Location =
        Location().apply {
            address = "Padstow NSW 2211, Australia"
            bearing = 2147483647
            isExact = false
            lat = -33.9518961
            lon = 151.0323912
            averageRating = -1.0f
            source = "google"
            name = "Padstow"
        }

    fun getRouteQuery() {
        _query.value = Query().apply {
            fromLocation = startLocation
            toLocation = destinationLocation
            unit = "auto"
            cyclingSpeed = 1
            walkingSpeed = 1
            environmentWeight = 50
            hassleWeight = 50
            budgetWeight = 50
            timeWeight = 50
            setTimeTag(TimeTag.createForLeaveNow())
        }
    }
}