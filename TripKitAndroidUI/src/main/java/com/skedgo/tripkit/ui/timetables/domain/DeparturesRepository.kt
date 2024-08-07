package com.skedgo.tripkit.ui.timetables.domain

import com.skedgo.tripkit.ui.model.DeparturesResponse
import com.skedgo.tripkit.ui.timetables.data.DepartureFilter
import io.reactivex.Single

interface DeparturesRepository {
    fun getTimetableEntries(
        region: String,
        embarkationStopCodes: List<String>,
        disembarkationStopCodes: List<String>?,
        timeInSecs: Long,
        limit: Int
    ): Single<DeparturesResponse>

    fun getTimetableEntries(
        region: String,
        embarkationStopCodes: List<String>,
        disembarkationStopCodes: List<String>?,
        timeInSecs: Long,
        limit: Int,
        filters: List<DepartureFilter>,
        includeStops: Boolean = false
    ): Single<DeparturesResponse>
}