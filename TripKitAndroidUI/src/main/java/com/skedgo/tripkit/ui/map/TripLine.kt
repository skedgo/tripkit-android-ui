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
        getTravelledLineForTrip.execute(segments).toList().toObservable()
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

    private fun createPolylineListForTravelledLines(results: List<List<LineSegment>>?): List<SegmentsPolyLineOptions> {
        val polylineOptionsList = mutableListOf<PolylineOptions>()
        if (!results.isNullOrEmpty()) {
            val lines = LinkedList<LatLng>()
            for (list in results) {

                list.forEach {
                    lines.clear()
                    lines.add(LatLng(it.start.latitude, it.start.longitude))
                    lines.add(LatLng(it.end.latitude, it.end.longitude))

                    //Background only for non-black color lines to standout in map
                    if (it.color != Color.BLACK) {
                        polylineOptionsList.add(
                            PolylineOptions()
                                .addAll(lines)
                                .color(Color.BLACK)
                                .width(20f)
                        )
                    }

                    polylineOptionsList.add(
                        PolylineOptions()
                            .addAll(lines)
                            .color(it.color)
                            .width((if (it.color != Color.BLACK) 14 else 15).toFloat())
                    )
                }
            }
        }
        return listOf(
            SegmentsPolyLineOptions(polylineOptionsList, true)
        )
    }

    private fun createPolylineListForTravelledLines(
        config: PolylineConfig,
        results: List<List<LineSegment>>?
    ): List<SegmentsPolyLineOptions> {
        val polylineOptionsList = mutableListOf<PolylineOptions>()
        if (!results.isNullOrEmpty()) {
            val lines = LinkedList<LatLng>()
            results.forEachIndexed { index, list ->
                list.forEach {
                    val color: Int
                    val zIndex: Float
                    if(config.activeTripUuid != null &&
                        config.activeTripUuid == it.tripUuid) {
                        color = config.activeColor
                        zIndex = 5.0f
                    } else {
                        color = config.inActiveColor
                        zIndex = 2.0f
                    }

                    lines.clear()
                    lines.add(LatLng(it.start.latitude, it.start.longitude))
                    lines.add(LatLng(it.end.latitude, it.end.longitude))

                    polylineOptionsList.add(
                        PolylineOptions()
                            .addAll(lines)
                            .color(color)
                            .width((if (it.color != Color.BLACK) 14 else 15).toFloat())
                            .zIndex(zIndex)
                    )
                }
            }
        }
        return listOf(
            SegmentsPolyLineOptions(
                polylineOptionsList, true
            )
        )
    }
}
