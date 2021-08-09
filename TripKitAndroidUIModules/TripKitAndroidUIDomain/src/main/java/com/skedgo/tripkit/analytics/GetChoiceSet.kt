package com.skedgo.tripkit.analytics

import com.skedgo.tripkit.routing.*
import java.util.*
import javax.inject.Inject

open class GetChoiceSet @Inject constructor() {
    /**
     * @param selectedTrip Represents a representative [Trip] that users selected among a list of visible [TripGroup].
     * @param visibleTripGroups Represents a list of [TripGroup] that we show to users.
     */
    open fun execute(
            selectedTrip: Trip,
            visibleTripGroups: List<TripGroup>
    ): List<Choice> = visibleTripGroups
            .map { Pair(it.displayTrip, it.visibility) }
            .map {
                when (it.second.toString()) {
                    GroupVisibility.FULL.name -> Pair(it.first, Visibility.Full)
                    else -> Pair(it.first, Visibility.Minimized)
                }
            }
            .map {
                val trip = it.first!!
                Choice(
                        trip.moneyCost,
                        trip.weightedScore,
                        trip.carbonCost,
                        trip.hassleCost,
                        trip.caloriesCost,
                        getMiniSegments(trip.segments),
                        trip.uuid() == selectedTrip.uuid(),
                        it.second.value,
                        trip.endTimeInSecs,
                        trip.startTimeInSecs
                )
            }

//    open fun execute(
//            selectedTrip: Trip,
//            visibleTripGroups: List<TripGroup>
//    ): List<Choice> = visibleTripGroups
//            .map { Pair(it.displayTrip, it.visibility) }
//            .map {
//                when (it.second.toString()) {
//                    GroupVisibility.FULL.name -> Pair(it.first, Visibility.Full)
//                    else -> Pair(it.first, Visibility.Minimized)
//                }
//            }
//            .map {
//                val trip = it.first!!
//                Choice(
//                        trip.moneyCost,
//                        trip.weightedScore,
//                        trip.carbonCost,
//                        trip.hassleCost,
//                        trip.caloriesCost,
//                        getMiniSegments(trip.segments),
//                        trip.uuid() == selectedTrip.uuid(),
//                        it.second,
//                        trip.endTimeInSecs,
//                        trip.startTimeInSecs
//                )
//            }

    private fun getMiniSegments(segments: List<TripSegment>): List<MiniSegment> = segments
            .filter { it.modeInfo != null }
            .map {
                val segmentType = getSegmentMode(it)
                MiniSegment(segmentType, it.endTimeInSecs - it.startTimeInSecs)
            }

    private fun getSegmentMode(segment: TripSegment): String = when (segment.type) {
        SegmentType.STATIONARY -> segment.modeInfo?.localIconName ?: "wait"
        else -> segment.transportModeId!!
    }
}
