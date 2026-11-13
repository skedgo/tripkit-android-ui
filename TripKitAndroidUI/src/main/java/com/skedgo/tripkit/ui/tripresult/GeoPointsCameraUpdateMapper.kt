package com.skedgo.tripkit.ui.tripresult

import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.skedgo.tripkit.location.GeoPoint
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.max
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

/** Bounds fit to the padded viewport while keeping bottom-anchored time markers visible. */
internal fun LatLngBounds.expandedForTimeLabels(
    labelExtentPx: Int,
    markerHeightPx: Int,
    mapWidthPx: Int,
    mapHeightPx: Int,
    systemInsets: android.graphics.Rect,
    paddingPx: Int = CameraUpdatePadding
): LatLngBounds {
    if (mapWidthPx <= 2 * paddingPx || mapHeightPx <= 2 * paddingPx ||
        northeast.longitude < southwest.longitude) return this
    val left = max(paddingPx, labelExtentPx) + systemInsets.left
    val right = max(paddingPx, labelExtentPx) + systemInsets.right
    val top = max(paddingPx, markerHeightPx) + systemInsets.top
    val bottom = paddingPx + systemInsets.bottom
    val width = mapWidthPx - left - right
    val height = mapHeightPx - top - bottom
    if (width <= 0 || height <= 0) return this
    val dx = (northeast.longitude - southwest.longitude) / 360.0
    val north = mercatorY(northeast.latitude)
    val south = mercatorY(southwest.latitude)
    val scale = min(if (dx > 0) width / dx else Double.MAX_VALUE,
        if (north > south) height / (north - south) else Double.MAX_VALUE)
    if (scale == Double.MAX_VALUE) return this
    // Use spare room before shifting the bounds: a width-limited portrait fit may already
    // clear the full marker vertically. Moving its centre in that case regresses portrait framing.
    val horizontalMargin = (mapWidthPx - dx * scale) / 2
    val verticalMargin = (mapHeightPx - (north - south) * scale) / 2
    val shiftX = when {
        left > horizontalMargin -> -(left - horizontalMargin) / scale
        right > horizontalMargin -> (right - horizontalMargin) / scale
        else -> 0.0
    }
    val shiftY = when {
        top > verticalMargin -> (top - verticalMargin) / scale
        bottom > verticalMargin -> -(bottom - verticalMargin) / scale
        else -> 0.0
    }
    val centerX = (southwest.longitude + northeast.longitude) / 720 + shiftX
    val centerY = (north + south) / 2 + shiftY
    val spanX = (mapWidthPx - 2 * paddingPx) / scale
    val spanY = (mapHeightPx - 2 * paddingPx) / scale
    fun latitude(y: Double) = Math.toDegrees(2 * atan(exp(y * 2 * PI)) - PI / 2)
    return LatLngBounds(
        LatLng(latitude(centerY - spanY / 2), ((centerX - spanX / 2) * 360).coerceAtLeast(-180.0)),
        LatLng(latitude(centerY + spanY / 2), ((centerX + spanX / 2) * 360).coerceAtMost(180.0))
    )
}

/** Home pads the map below its camera target; projection corners can include that hidden area. */
internal fun android.graphics.Rect.bottomPaddedViewport(targetY: Int): android.graphics.Rect =
    android.graphics.Rect(left, top, right, minOf(bottom, (2 * targetY - top).coerceAtLeast(top)))

/** Chooses marker clearance inside the actual viewport without covering interactive overlays. */
internal fun LatLngBounds.insetsAvoidingOverlays(
    viewport: android.graphics.Rect,
    systemInsets: android.graphics.Rect,
    overlays: List<android.graphics.Rect>,
    labelExtentPx: Int,
    markerHeightPx: Int,
    paddingPx: Int = CameraUpdatePadding
): android.graphics.Rect? {
    val safe = android.graphics.Rect(viewport.left + systemInsets.left,
        viewport.top + systemInsets.top, viewport.right - systemInsets.right,
        viewport.bottom - systemInsets.bottom)
    var candidates = listOf(safe).filter { it.width() > 0 && it.height() > 0 }
    overlays.filterNot { it.isEmpty }.forEach { overlay ->
        candidates = candidates.flatMap { area ->
            if (!android.graphics.Rect.intersects(area, overlay)) listOf(area)
            else listOf(
                android.graphics.Rect(area.left, area.top, minOf(area.right, overlay.left), area.bottom),
                android.graphics.Rect(maxOf(area.left, overlay.right), area.top, area.right, area.bottom),
                android.graphics.Rect(area.left, area.top, area.right, minOf(area.bottom, overlay.top)),
                android.graphics.Rect(area.left, maxOf(area.top, overlay.bottom), area.right, area.bottom)
            ).filter { it.width() > 0 && it.height() > 0 }
        }.distinct()
    }
    val dx = (northeast.longitude - southwest.longitude) / 360.0
    val dy = mercatorY(northeast.latitude) - mercatorY(southwest.latitude)
    fun scale(area: android.graphics.Rect): Double {
        val width = area.width() - 2 * max(paddingPx, labelExtentPx)
        val height = area.height() - max(paddingPx, markerHeightPx) - paddingPx
        if (width <= 0 || height <= 0) return -1.0
        return min(if (dx > 0) width / dx else Double.MAX_VALUE,
            if (dy > 0) height / dy else Double.MAX_VALUE)
    }
    // The greatest feasible scale avoids needless zoom-out. At equal scale prefer the least
    // displacement, preserving portrait centring when several free rectangles are equivalent.
    val best = candidates.filter { scale(it) > 0 }.maxWithOrNull(
        compareBy<android.graphics.Rect> { scale(it) }.thenBy {
            -kotlin.math.abs(it.centerX() - viewport.centerX()) -
                kotlin.math.abs(it.centerY() - viewport.centerY())
        }
    ) ?: return null
    return android.graphics.Rect(best.left - viewport.left, best.top - viewport.top,
        viewport.right - best.right, viewport.bottom - best.bottom)
}
