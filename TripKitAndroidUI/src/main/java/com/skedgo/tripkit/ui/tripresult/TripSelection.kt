package com.skedgo.tripkit.ui.tripresult

import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup

/**
 * Server-assigned identity of a trip that survives re-parsing and persistence, unlike the
 * client-generated [Trip.uuid] (random per instance) and [Trip.tripId] (often 0).
 */
val Trip.stableKey: String?
    get() = saveURL?.takeIf { it.isNotBlank() }
        ?: temporaryURL?.takeIf { it.isNotBlank() }
        ?: updateURL?.takeIf { it.isNotBlank() }

/**
 * Finds the trip the user selected in this group: by [uuid], then a non-zero [tripId], then the
 * [stableKey]. Falls back to the group's display trip only when none match, so a preview restored
 * after rotation or a font-size change keeps showing the selected trip (Redmine #19351).
 */
fun TripGroup.findSelectedTrip(uuid: String?, tripId: Long?, stableKey: String?): Trip? {
    val candidates = trips.orEmpty()
    return candidates.firstOrNull { !uuid.isNullOrEmpty() && it.uuid == uuid }
        ?: candidates.firstOrNull { tripId != null && tripId != 0L && it.tripId == tripId }
        ?: candidates.firstOrNull { !stableKey.isNullOrEmpty() && it.stableKey == stableKey }
        ?: displayTrip
}
