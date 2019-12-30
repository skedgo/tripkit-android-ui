package com.skedgo.tripkit.ui.realtime

import com.skedgo.tripkit.common.agenda.IRealTimeElement
import io.reactivex.Single
import com.skedgo.tripkit.routing.RealTimeVehicle

interface RealTimeRepository {

  fun getUpdates(region: String, elements: List<IRealTimeElement>): Single<List<RealTimeVehicle>>
}