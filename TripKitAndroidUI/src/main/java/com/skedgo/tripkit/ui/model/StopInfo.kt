package com.skedgo.tripkit.ui.model

import com.skedgo.tripkit.common.model.realtimealert.RealTimeStatus
import com.skedgo.tripkit.common.model.stop.ServiceStop

/**
 * Thuy's remark: This should have been [ServiceStop].
 * We parse network response into ServiceStops,
 * then persist them into SQLite database.
 * However, when loading, we use such [StopInfo] to indicate service' stops.
 */
data class StopInfo(
    val id: Int,
    val realTimeStatus: RealTimeStatus?,
    val sortByArrive: Boolean,
    val stop: ServiceStop,
    val serviceColor: Int,
    var travelled: Boolean
)
