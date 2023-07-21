package com.skedgo.tripkit.ui.timetables

import com.skedgo.tripkit.ui.model.TimetableEntry
import javax.inject.Inject

open class GetServiceTertiaryText @Inject constructor() {
    open fun execute(service: TimetableEntry): String {
        return when {
            service.startPlatform.isNullOrBlank().not() -> {
                "${service.startPlatform} • ${service.serviceDirection}"
            }
            service.startStopShortName.isNullOrBlank() -> {
                return when {
                    service.serviceDirection.isNullOrBlank() -> ""
                    else -> service.serviceDirection!!
                }
            }
            else -> "${service.startStopShortName} • ${service.serviceDirection}"
        }
    }
}