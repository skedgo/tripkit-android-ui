package com.skedgo.tripkit.ui.tripresult

import android.content.Context
import com.skedgo.tripkit.common.model.Region
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.routingresults.TripGroupRepository
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import com.skedgo.tripkit.agenda.ConfigRepository
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.routing.TripSegment
import javax.inject.Inject

open class GetAlternativeTripForAlternativeService
@Inject constructor(
    val context: Context,
    val tripGroupRepository: TripGroupRepository,
    val regionService: RegionService,
    val configRepository: ConfigRepository
) {

    open fun execute(
        trip: Trip,
        tripSegmentId: Long,
        selectedService: TimetableEntry
    ): Single<TripGroup> {
        return regionService.getRegionByLocationAsync(trip.from)
            .singleOrError()
            .zipWith(tripGroupRepository.getTripSegmentByIdAndTripId(
                segmentId = tripSegmentId,
                tripId = trip.uuid()
            ),
                BiFunction<Region, TripSegment, Pair<Region, TripSegment>> { region, segment -> region to segment })
            .flatMap { (region, segment) ->
                Single.create(
                    WaypointTask(
                        context, configRepository,
                        WayPointTaskParam.ForChangingService(
                            region,
                            trip.segments,
                            segment,
                            selectedService
                        )
                    )
                )
                    .subscribeOn(Schedulers.io())
            }
            .map { it.first() }
    }
}