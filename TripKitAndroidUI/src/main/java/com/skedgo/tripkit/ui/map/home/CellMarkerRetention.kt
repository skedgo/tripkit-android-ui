package com.skedgo.tripkit.ui.map.home

import com.skedgo.tripkit.ui.data.places.LatLngBounds

internal data class CellMarkerDelta(
    val enteredCells: Set<String>,
    val exitedCells: Set<String>,
    val stableCells: Set<String>,
    val changedCells: Set<String>,
    val unchangedCells: Set<String>,
    val missingHashCount: Int
)

internal data class BoundsDelta(
    val changed: Boolean,
    val expanded: Boolean
)

internal data class MarkerReloadDecision(
    val suppressReload: Boolean,
    val cellDelta: CellMarkerDelta,
    val boundsDelta: BoundsDelta,
    val reason: String
)

internal fun computeCellMarkerDelta(
    previousVisibleCellHashes: Map<String, Long?>,
    newVisibleCellHashes: Map<String, Long?>
): CellMarkerDelta {
    val previousCells = previousVisibleCellHashes.keys
    val newCells = newVisibleCellHashes.keys
    val enteredCells = newCells - previousCells
    val exitedCells = previousCells - newCells
    val stableCells = previousCells intersect newCells
    val changedCells = linkedSetOf<String>()
    val unchangedCells = linkedSetOf<String>()
    var missingHashCount = 0

    for (cell in stableCells) {
        val previousHash = previousVisibleCellHashes[cell]
        val newHash = newVisibleCellHashes[cell]
        if (previousHash == null || newHash == null) {
            missingHashCount += 1
            if (previousHash == newHash) {
                unchangedCells += cell
            } else {
                changedCells += cell
            }
            continue
        }
        if (previousHash == newHash) {
            unchangedCells += cell
        } else {
            changedCells += cell
        }
    }

    return CellMarkerDelta(
        enteredCells = enteredCells,
        exitedCells = exitedCells,
        stableCells = stableCells,
        changedCells = changedCells,
        unchangedCells = unchangedCells,
        missingHashCount = missingHashCount
    )
}

internal fun computeBoundsDelta(
    previousBounds: LatLngBounds,
    newBounds: LatLngBounds,
    tolerance: Double = 0.001
): BoundsDelta {
    val latDiff = kotlin.math.abs(previousBounds.northeast.latitude - newBounds.northeast.latitude) +
        kotlin.math.abs(previousBounds.southwest.latitude - newBounds.southwest.latitude)
    val lngDiff = kotlin.math.abs(previousBounds.northeast.longitude - newBounds.northeast.longitude) +
        kotlin.math.abs(previousBounds.southwest.longitude - newBounds.southwest.longitude)
    val changed = latDiff >= tolerance || lngDiff >= tolerance
    val expanded = containsBounds(newBounds, previousBounds) && !containsBounds(previousBounds, newBounds)
    return BoundsDelta(changed = changed, expanded = expanded)
}

internal fun shouldSuppressMarkerReload(
    previousVisibleCellHashes: Map<String, Long?>,
    newVisibleCellHashes: Map<String, Long?>,
    previousBounds: LatLngBounds,
    newBounds: LatLngBounds
): MarkerReloadDecision {
    val cellDelta = computeCellMarkerDelta(previousVisibleCellHashes, newVisibleCellHashes)
    val boundsDelta = computeBoundsDelta(previousBounds, newBounds)
    val hasCellChanges = cellDelta.enteredCells.isNotEmpty() ||
        cellDelta.exitedCells.isNotEmpty() ||
        cellDelta.changedCells.isNotEmpty()
    val suppressReload = !hasCellChanges && !boundsDelta.changed
    val reason = when {
        hasCellChanges -> "cell_delta_requires_reload"
        boundsDelta.changed -> "bounds_changed_requires_reload"
        else -> "unchanged_cells_and_bounds"
    }
    return MarkerReloadDecision(
        suppressReload = suppressReload,
        cellDelta = cellDelta,
        boundsDelta = boundsDelta,
        reason = reason
    )
}

private fun containsBounds(outer: LatLngBounds, inner: LatLngBounds): Boolean {
    return outer.southwest.latitude <= inner.southwest.latitude &&
        outer.southwest.longitude <= inner.southwest.longitude &&
        outer.northeast.latitude >= inner.northeast.latitude &&
        outer.northeast.longitude >= inner.northeast.longitude
}
