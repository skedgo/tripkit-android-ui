package com.skedgo.tripkit.ui.tripresult

import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.skedgo.tripkit.location.GeoPoint
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.tan

const val CameraUpdatePadding = 120
const val ZoomToCoverFirstOneKilometers = 16.0f
const val ZoomOnSingleLocation = 21.0f
const val OneKilometers = 1000.00

fun List<GeoPoint>.toCameraUpdate(): CameraUpdate = this
    .map { LatLng(it.latitude, it.longitude) }
    .toList()
    .fold(LatLngBounds.Builder(), LatLngBounds.Builder::include)
    .build()
    .toCameraUpdate(CameraUpdatePadding)

/**
 * Same as [toCameraUpdate], but leaves room for marker labels (e.g. departure/arrival time pills)
 * that extend [labelExtentPx] horizontally from markers at the edge of the trip.
 * See [expandedForHorizontalLabels].
 */
fun List<GeoPoint>.toCameraUpdate(
    labelExtentPx: Int,
    mapWidthPx: Int,
    mapHeightPx: Int
): CameraUpdate = this
    .map { LatLng(it.latitude, it.longitude) }
    .toList()
    .fold(LatLngBounds.Builder(), LatLngBounds.Builder::include)
    .build()
    .expandedForHorizontalLabels(labelExtentPx, mapWidthPx, mapHeightPx, CameraUpdatePadding)
    .toCameraUpdate(CameraUpdatePadding)

/**
 * Returns bounds widened east and west so that, when fitted into a [mapWidthPx] x [mapHeightPx]
 * map with [paddingPx] on every side, there are at least [labelExtentPx] pixels between the
 * leftmost/rightmost point and the map edge. Marker labels such as the trip time pills are drawn
 * beside their marker, so a uniform padding clips them at the screen edge once the label is wider
 * than the padding (e.g. at large font scales).
 *
 * Uses Web Mercator, so the result is exact for the zoom Google Maps picks for these bounds.
 * The label room is capped at 3/8 of the map width so the route keeps at least a quarter. Bounds
 * crossing the antimeridian, a single point, or missing map dimensions are returned unchanged.
 */
fun LatLngBounds.expandedForHorizontalLabels(
    labelExtentPx: Int,
    mapWidthPx: Int,
    mapHeightPx: Int,
    paddingPx: Int
): LatLngBounds {
    if (labelExtentPx <= paddingPx || mapWidthPx <= 0 || mapHeightPx <= 0) return this
    val west = southwest.longitude
    val east = northeast.longitude
    if (east < west) return this

    val usableWidth = (mapWidthPx - 2 * paddingPx).toDouble()
    val usableHeight = (mapHeightPx - 2 * paddingPx).toDouble()
    if (usableWidth <= 0 || usableHeight <= 0) return this
    val labelRoom = min(labelExtentPx.toDouble(), mapWidthPx * 3.0 / 8)

    // Spans as fractions of the world width (Mercator x is linear in longitude).
    val dx = (east - west) / 360.0
    val dy = mercatorY(northeast.latitude) - mercatorY(southwest.latitude)

    // Pixels per world-width the route may be drawn at while keeping labelRoom free on both sides.
    val scaleForLabels = if (dx > 0) (mapWidthPx - 2 * labelRoom) / dx else Double.MAX_VALUE
    val scaleForHeight = if (dy > 0) usableHeight / dy else Double.MAX_VALUE
    val targetScale = min(scaleForLabels, scaleForHeight)
    if (targetScale == Double.MAX_VALUE) return this

    // Width (in world fraction) the bounds need so that fitting them yields targetScale.
    val requiredDx = usableWidth / targetScale
    if (requiredDx <= dx) return this
    val extraDegrees = (requiredDx - dx) / 2 * 360.0
    return LatLngBounds(
        LatLng(southwest.latitude, (west - extraDegrees).coerceAtLeast(-180.0)),
        LatLng(northeast.latitude, (east + extraDegrees).coerceAtMost(180.0))
    )
}

private fun mercatorY(latitude: Double): Double =
    ln(tan(PI / 4 + Math.toRadians(latitude) / 2)) / (2 * PI)
