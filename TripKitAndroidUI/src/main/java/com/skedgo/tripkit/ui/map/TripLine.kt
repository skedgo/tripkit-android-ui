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
import java.util.LinkedList
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
                createPolylineListForTravelledLines(it.first) + createPolylineListForNonTravelledLines(
                    it.second
                )
            }

    open fun executeForTravelledLine(polylineConfig: PolylineConfig, segments: List<TripSegment>) =
        getTravelledLineForTrip.execute(segments, 50.0).toList().toObservable()
            .map { createPolylineListForTravelledLines(polylineConfig, it) }

    private fun createPolylineListForNonTravelledLines(nonTravelledLinesToDraw: List<List<LineSegment>>?): List<SegmentsPolyLineOptions> {
        if (nonTravelledLinesToDraw.isNullOrEmpty()) {
            return listOf(SegmentsPolyLineOptions(emptyList(), false))
        }

        return nonTravelledLinesToDraw.map { lineSegments ->
            val points = lineSegments.flatMap { line ->
                listOf(
                    LatLng(line.start.latitude, line.start.longitude),
                    LatLng(line.end.latitude, line.end.longitude)
                )
            }
            val polylineOptions = points.takeIf { it.isNotEmpty() }
                ?.let {
                    listOf(
                        PolylineOptions()
                            .addAll(it)
                            .color(NON_TRAVELLED_LINE_COLOR)
                            .width(7f)
                    )
                }
                .orEmpty()
            SegmentsPolyLineOptions(
                polylineOptions,
                false,
                lineSegments.firstOrNull()?.segmentId
            )
        }
    }

    private fun createPolylineListForTravelledLines(results: List<List<LineSegment>>?): List<SegmentsPolyLineOptions> {
        if (results.isNullOrEmpty()) {
            return listOf(SegmentsPolyLineOptions(emptyList(), true))
        }

        return results.map { lineSegments ->
            SegmentsPolyLineOptions(
                lineSegments.toPolylineOptions(),
                true,
                lineSegments.firstOrNull()?.segmentId
            )
        }
    }

    private fun createPolylineListForTravelledLines(
        config: PolylineConfig,
        results: List<List<LineSegment>>?
    ): List<SegmentsPolyLineOptions> {
        if (results.isNullOrEmpty()) {
            return listOf(SegmentsPolyLineOptions(emptyList(), true))
        }

        return results.map { lineSegments ->
            SegmentsPolyLineOptions(
                lineSegments.toPolylineOptions { lineSegment ->
                    if (config.activeTripUuid != null && config.activeTripUuid == lineSegment.tripUuid) {
                        PolylineStyle(config.activeColor, 5.0f)
                    } else {
                        PolylineStyle(config.inActiveColor, 2.0f)
                    }
                },
                true,
                lineSegments.firstOrNull()?.segmentId
            )
        }
    }

    private data class PolylineStyle(
        val color: Int,
        val zIndex: Float? = null
    )

    private fun List<LineSegment>.toPolylineOptions(
        styleProvider: (LineSegment) -> PolylineStyle = { PolylineStyle(it.color) }
    ): List<PolylineOptions> {
        val polylineOptionsList = mutableListOf<PolylineOptions>()
        var currentStyle: PolylineStyle? = null
        var currentPoints = LinkedList<LatLng>()

        fun flushCurrentLine() {
            val style = currentStyle ?: return
            if (currentPoints.size < 2) return

            if (style.color != Color.BLACK) {
                polylineOptionsList.add(
                    PolylineOptions()
                        .addAll(currentPoints)
                        .color(Color.BLACK)
                        .width(20f)
                        .apply { style.zIndex?.let(::zIndex) }
                )
            }

            polylineOptionsList.add(
                PolylineOptions()
                    .addAll(currentPoints)
                    .color(style.color)
                    .width((if (style.color != Color.BLACK) 14 else 15).toFloat())
                    .apply { style.zIndex?.let(::zIndex) }
            )
        }

        forEach { lineSegment ->
            val style = styleProvider(lineSegment)
            val start = LatLng(lineSegment.start.latitude, lineSegment.start.longitude)
            val end = LatLng(lineSegment.end.latitude, lineSegment.end.longitude)
            val continuesCurrentLine = currentPoints.lastOrNull() == start

            if (currentStyle != style || !continuesCurrentLine) {
                flushCurrentLine()
                currentStyle = style
                currentPoints = LinkedList()
                currentPoints.add(start)
            }
            currentPoints.add(end)
        }

        flushCurrentLine()
        return polylineOptionsList
    }
}
