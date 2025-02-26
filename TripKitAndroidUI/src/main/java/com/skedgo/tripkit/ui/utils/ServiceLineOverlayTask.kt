package com.skedgo.tripkit.ui.utils

import android.graphics.Color
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.skedgo.tripkit.ui.utils.ServiceLineOverlayTask.ServiceLineInfo
import io.reactivex.functions.Function
import java.util.LinkedList

class ServiceLineOverlayTask : Function<List<ServiceLineInfo>, List<PolylineOptions>> {
    override fun apply(serviceLineInfos: List<ServiceLineInfo>): List<PolylineOptions> {

        val mNonTravelledLinesToDraw: MutableList<List<LineSegment>> = LinkedList()
        val linesToDraw: MutableList<List<LineSegment>> = LinkedList()

        for (line in serviceLineInfos) {
            if (line.waypoints.isNullOrEmpty()) {
                continue
            }

            val nonTravelledLines: MutableList<LineSegment> = ArrayList()
            val lineSegmentsToDraw: MutableList<LineSegment> = ArrayList()

            var j = 0
            val size = line.waypoints.size - 1
            while (j < size) {
                val start = line.waypoints[j]
                val end = line.waypoints[j + 1]

                if (line.travelled) {
                    if (!nonTravelledLines.isEmpty()) {
                        mNonTravelledLinesToDraw.add(nonTravelledLines)
                        nonTravelledLines.clear()
                    }

                    lineSegmentsToDraw.add(LineSegment(start, end, LineSegment.SOLID, line.color))
                } else {
                    nonTravelledLines.add(LineSegment(start, end, LineSegment.SOLID, line.color))
                }
                j++
            }

            linesToDraw.add(lineSegmentsToDraw)

            if (!nonTravelledLines.isEmpty()) {
                mNonTravelledLinesToDraw.add(nonTravelledLines)
            }
        }

        return getPolylines(linesToDraw, mNonTravelledLinesToDraw)
    }

    private fun getPolylines(
        mLinesToDraw: List<List<LineSegment>>,
        mNonTravelledLinesToDraw: List<List<LineSegment>>
    ): List<PolylineOptions> {
        val polylineOptions: MutableList<PolylineOptions> = ArrayList()
        if (!mLinesToDraw.isEmpty()) {
            val lines: MutableList<LatLng> = ArrayList()
            for (list in mLinesToDraw) {
                lines.clear()
                for (line in list) {
                    lines.add(line.start)
                    lines.add(line.end)
                }

                if (!lines.isEmpty()) {
                    val color = list[0].color

                    //If we have a non-black color, draw an outline!
                    if (color != Color.BLACK) {
                        polylineOptions.add(
                            PolylineOptions()
                                .addAll(lines)
                                .color(Color.BLACK)
                                .width(20f)
                                .geodesic(true)
                        )
                    }

                    polylineOptions.add(
                        PolylineOptions()
                            .addAll(lines)
                            .color(color)
                            .width((if (color != Color.BLACK) 12 else 14).toFloat())
                            .geodesic(true)
                    )
                }
            }
        }

        if (!mNonTravelledLinesToDraw.isEmpty()) {
            val lines: MutableList<LatLng> = LinkedList()
            for (list in mNonTravelledLinesToDraw) {
                lines.clear()
                for (line in list) {
                    lines.add(line.start)
                    lines.add(line.end)
                }

                if (!lines.isEmpty()) {
                    polylineOptions.add(
                        PolylineOptions().addAll(lines).color(
                            NON_TRAVELLED_LINE_COLOR
                        ).width(14f)
                    )
                }
            }
        }
        return polylineOptions
    }

    class LineSegment(var start: LatLng, var end: LatLng, var type: Int, var color: Int) {
        companion object {
            const val SOLID: Int = 0
        }
    }

    class ServiceLineInfo(val waypoints: List<LatLng>?, val color: Int, var travelled: Boolean)
    companion object {
        const val NON_TRAVELLED_LINE_COLOR: Int = -0x77555556
    }
}