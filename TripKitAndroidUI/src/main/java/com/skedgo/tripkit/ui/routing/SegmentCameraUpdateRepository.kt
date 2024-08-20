package com.skedgo.tripkit.ui.routing

import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.routing.SegmentType
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.routingresults.GetSelectedTrip
import io.reactivex.Observable

open class SegmentCameraUpdateRepository(
    private val getSelectedTrip: GetSelectedTrip
) {
    private val segmentRelay = PublishRelay.create<TripSegment>()

    fun getSegmentCameraUpdate(): Observable<SegmentCameraUpdate> =
        segmentRelay
            .switchMap { segment ->
                getSelectedTrip.execute()
                    .map { it.segments }
                    .filter { it.contains(segment) }
                    .map { Pair(it as List<TripSegment>, segment) }
            }
            .map { (segments, selectedSegment) ->
                // See the requirement in https://redmine.buzzhives.com/issues/8816#note-4.
                when {
                    selectedSegment.type != SegmentType.STATIONARY -> selectedSegment
                    else -> {
                        val index = segments.indexOf(selectedSegment)
                        when {
                            index < segments.lastIndex -> segments[index + 1]
                            else -> selectedSegment
                        }
                    }
                }
            }
            .map { getSegmentCameraPositions(it) }

    private fun getSegmentCameraPositions(segment: TripSegment): SegmentCameraUpdate {
        val locations = listOfNotNull(
            segment.from,
            segment.singleLocation,
            segment.to
        )
        return when {
            locations.size >= 2 -> return SegmentCameraUpdate.HasTwoLocations(
                segment.id,
                segment.from,
                segment.to
            )
            locations.size == 1 -> SegmentCameraUpdate.HasOneLocation(
                segment.id,
                locations.first()
            )
            else -> SegmentCameraUpdate.HasEmptyLocations(segment.id)
        }
    }

    fun putSegment(segment: TripSegment) = segmentRelay.accept(segment)
}
