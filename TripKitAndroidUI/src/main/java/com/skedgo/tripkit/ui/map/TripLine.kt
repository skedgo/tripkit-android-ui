package com.skedgo.tripkit.ui.map

import android.graphics.Color
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.skedgo.tripkit.LineSegment
import com.skedgo.tripkit.a2brouting.GetNonTravelledLineForTrip
import com.skedgo.tripkit.a2brouting.GetTravelledLineForTrip
import com.skedgo.tripkit.routing.TripSegment
import io.reactivex.Observable
import io.reactivex.functions.BiFunction

import javax.inject.Inject

// FIXME: Create a pure domain model to represent a trip line.
typealias TripLine = List<SegmentsPolyLineOptions>

data class PolylineConfig(
    val inActiveColor: Int,
    val activeColor: Int,
    val activeTripUuid: String? = null
)

open class GetTripLine @Inject internal constructor(
    private val getNonTravelledLineForTrip: GetNonTravelledLineForTrip,
    private val getTravelledLineForTrip: GetTravelledLineForTrip
) {

    private val NON_TRAVELLED_LINE_COLOR: Int = 0x88AAAAAA.toInt()

    open fun execute(segments: List<TripSegment>): Observable<TripLine> =
        Observable
            .zip(getTravelledLineForTrip.execute(segments).toList().toObservable(),
                getNonTravelledLineForTrip.execute(segments).toList().toObservable(),
                BiFunction { t: List<List<LineSegment>>, t2: List<List<LineSegment>> -> t to t2 })
            .map {
                createPolylineListForTravelledLines(it.first, segments) + createPolylineListForNonTravelledLines(
                    it.second
                )
            }

    open fun executeForTravelledLine(polylineConfig: PolylineConfig, segments: List<TripSegment>) =
        getTravelledLineForTrip.execute(segments, 50.0).toList().toObservable()
            .map { createPolylineListForTravelledLines(polylineConfig, it) }

    private fun createPolylineListForNonTravelledLines(nonTravelledLinesToDraw: List<List<LineSegment>>?): List<SegmentsPolyLineOptions> {
        val polylineOptionsList = mutableListOf<PolylineOptions>()
        if (!nonTravelledLinesToDraw.isNullOrEmpty()) {
            val lines = mutableListOf<LatLng>()
            for (list in nonTravelledLinesToDraw) {
                lines.clear()
                for (line in list) {
                    lines.add(LatLng(line.start.latitude, line.start.longitude))
                    lines.add(LatLng(line.end.latitude, line.end.longitude))
                }

                if (!lines.isEmpty()) {
                    polylineOptionsList.add(
                        PolylineOptions()
                            .addAll(lines)
                            .color(NON_TRAVELLED_LINE_COLOR)
                            .width(7f)
                    )
                }
            }
        }
        return listOf(
            SegmentsPolyLineOptions(polylineOptionsList, false)
        )
    }

    private fun createPolylineListForTravelledLines(
        results: List<List<LineSegment>>?,
        segments: List<TripSegment>
    ): List<SegmentsPolyLineOptions> {
        if (results.isNullOrEmpty()) return emptyList()

        val travelledSourceSegments = segments.filter { it.from != null && it.to != null }

        // TODO: If travelled-line result order diverges from `travelledSourceSegments` in production (wrong highlight),
        //  migrate segment identity from LineSegment.tag (or another stable field) instead of index pairing.
        val segmentPolylineOptions = mutableListOf<SegmentsPolyLineOptions>()
        results.forEachIndexed { index, list ->
            if (list.isEmpty()) return@forEachIndexed

            val polylineOptions = mutableListOf<PolylineOptions>()
            var currentColor = list.first().color
            val currentPoints = mutableListOf(
                LatLng(list.first().start.latitude, list.first().start.longitude),
                LatLng(list.first().end.latitude, list.first().end.longitude)
            )

            for (i in 1 until list.size) {
                val segment = list[i]
                if (segment.color == currentColor) {
                    currentPoints.add(LatLng(segment.start.latitude, segment.start.longitude))
                    currentPoints.add(LatLng(segment.end.latitude, segment.end.longitude))
                } else {
                    flushTravelledPolyline(polylineOptions, currentPoints, currentColor)
                    currentColor = segment.color
                    currentPoints.clear()
                    currentPoints.add(LatLng(segment.start.latitude, segment.start.longitude))
                    currentPoints.add(LatLng(segment.end.latitude, segment.end.longitude))
                }
            }
            flushTravelledPolyline(polylineOptions, currentPoints, currentColor)

            segmentPolylineOptions.add(
                SegmentsPolyLineOptions(
                    polyLineOptions = polylineOptions,
                    isTravelled = true,
                    segmentId = travelledSourceSegments.getOrNull(index)?.segmentId
                )
            )
        }
        return segmentPolylineOptions
    }

    private fun flushTravelledPolyline(
        dest: MutableList<PolylineOptions>,
        points: List<LatLng>,
        color: Int
    ) {
        if (points.isEmpty()) return
        if (color != Color.BLACK) {
            dest.add(
                PolylineOptions()
                    .addAll(points)
                    .color(Color.BLACK)
                    .width(20f)
            )
        }
        dest.add(
            PolylineOptions()
                .addAll(points)
                .color(color)
                .width((if (color != Color.BLACK) 14 else 15).toFloat())
        )
    }

    private fun createPolylineListForTravelledLines(
        config: PolylineConfig,
        results: List<List<LineSegment>>?
    ): List<SegmentsPolyLineOptions> {
        val polylineOptionsList = mutableListOf<PolylineOptions>()
        if (!results.isNullOrEmpty()) {
            for (list in results) {
                if (list.isEmpty()) continue

                val first = list.first()
                var currentColor = resolveConfigColor(config, first)
                var currentZIndex = resolveConfigZIndex(config, first)
                var currentWidth = (if (first.color != Color.BLACK) 14 else 15).toFloat()
                val currentPoints = mutableListOf(
                    LatLng(first.start.latitude, first.start.longitude),
                    LatLng(first.end.latitude, first.end.longitude)
                )

                for (i in 1 until list.size) {
                    val segment = list[i]
                    val segColor = resolveConfigColor(config, segment)
                    val segZIndex = resolveConfigZIndex(config, segment)
                    val segWidth = (if (segment.color != Color.BLACK) 14 else 15).toFloat()

                    if (segColor == currentColor && segZIndex == currentZIndex && segWidth == currentWidth) {
                        currentPoints.add(LatLng(segment.start.latitude, segment.start.longitude))
                        currentPoints.add(LatLng(segment.end.latitude, segment.end.longitude))
                    } else {
                        polylineOptionsList.add(
                            PolylineOptions()
                                .addAll(currentPoints.toList())
                                .color(currentColor)
                                .width(currentWidth)
                                .zIndex(currentZIndex)
                        )
                        currentColor = segColor
                        currentZIndex = segZIndex
                        currentWidth = segWidth
                        currentPoints.clear()
                        currentPoints.add(LatLng(segment.start.latitude, segment.start.longitude))
                        currentPoints.add(LatLng(segment.end.latitude, segment.end.longitude))
                    }
                }
                polylineOptionsList.add(
                    PolylineOptions()
                        .addAll(currentPoints.toList())
                        .color(currentColor)
                        .width(currentWidth)
                        .zIndex(currentZIndex)
                )
            }
        }
        return listOf(
            SegmentsPolyLineOptions(polylineOptionsList, true)
        )
    }

    private fun resolveConfigColor(config: PolylineConfig, segment: LineSegment): Int =
        if (config.activeTripUuid != null && config.activeTripUuid == segment.tripUuid)
            config.activeColor else config.inActiveColor

    private fun resolveConfigZIndex(config: PolylineConfig, segment: LineSegment): Float =
        if (config.activeTripUuid != null && config.activeTripUuid == segment.tripUuid)
            5.0f else 2.0f
}
