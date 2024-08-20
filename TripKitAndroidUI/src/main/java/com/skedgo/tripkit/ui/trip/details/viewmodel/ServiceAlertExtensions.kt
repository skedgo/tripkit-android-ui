package com.skedgo.tripkit.ui.trip.details.viewmodel

import com.skedgo.tripkit.common.model.RealtimeAlert

fun List<RealtimeAlert>?.getMostSevereAlert(): RealtimeAlert? {
    return this.orEmpty()
        .let {
            arrayOf(
                it.firstOrNull { it.severity() == RealtimeAlert.SEVERITY_ALERT },
                it.firstOrNull { it.severity() == RealtimeAlert.SEVERITY_WARNING })
        }
        .firstOrNull()
}