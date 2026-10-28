package com.skedgo.tripkit.ui.realtime

import com.skedgo.tripkit.common.agenda.IRealTimeElement
import com.skedgo.tripkit.routing.RealTimeVehicle
import io.reactivex.Single

interface RealTimeRepository {

    fun getUpdates(region: String, elements: List<IRealTimeElement>): Single<List<RealTimeVehicle>>

    /** Map locations may include alternatives that are not assigned to the requested services. */
    fun getMapUpdates(region: String, elements: List<IRealTimeElement>): Single<List<RealTimeVehicle>> =
        getUpdates(region, elements)
}