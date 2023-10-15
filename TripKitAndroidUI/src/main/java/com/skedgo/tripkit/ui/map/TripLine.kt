package com.skedgo.tripkit.ui.map

import android.graphics.Color
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.skedgo.tripkit.LineSegment
import com.skedgo.tripkit.a2brouting.GetNonTravelledLineForTrip
import com.skedgo.tripkit.a2brouting.GetTravelledLineForTrip
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import com.skedgo.tripkit.routing.TripSegment
import java.util.*
import javax.inject.Inject

// FIXME: Create a pure domain model to represent a trip line.
typealias TripLine = List<PolylineOptions>

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

    private fun createPolylineListForNonTravelledLines(nonTravelledLinesToDraw: List<List<LineSegment>>?): List<PolylineOptions> {
        val polylineOptionsList = mutableListOf<PolylineOptions>()
        if (nonTravelledLinesToDraw != null && !nonTravelledLinesToDraw.isEmpty()) {
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
        return polylineOptionsList
    }

    private fun createPolylineListForTravelledLines(results: List<List<LineSegment>>?): List<PolylineOptions> {
        val polylineOptionsList = mutableListOf<PolylineOptions>()
        if (!results.isNullOrEmpty()) {
            val lines = LinkedList<LatLng>()
            for (list in results) {

                list.forEach {
                    lines.clear()
                    lines.add(LatLng(it.start.latitude, it.start.longitude))
                    lines.add(LatLng(it.end.latitude, it.end.longitude))

                    if (it.color != Color.BLACK) {
                        polylineOptionsList.add(
                            PolylineOptions()
                                .addAll(lines)
                                .color(Color.BLACK)
                                .width(10f)
                        )
                    }

                    polylineOptionsList.add(
                        PolylineOptions()
                            .addAll(lines)
                            .color(it.color)
                            .width((if (it.color != Color.BLACK) 6 else 7).toFloat())
                    )
                }
            }
        }
        return polylineOptionsList
    }
}
