package com.skedgo.tripkit.ui.trippreview.v2

import android.content.Context
import android.util.AtomicFile
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.routing.TripSegment
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/** Private cache snapshots keep large route graphs out of FragmentManager's state Bundle. */
internal class TripPreviewSegmentState(context: Context) {
    private val directory = File(context.cacheDir, "trip-preview-segments")
    private val gson = Gson()

    private data class Snapshot(
        val version: Int = 1,
        val segmentId: String?,
        val segmentNumber: Long?,
        val tripUuid: String?,
        val segment: TripSegment?,
        val trip: Trip?,
        val group: TripGroup?
    )

    fun save(key: String, segment: TripSegment): Boolean {
        val file = file(key) ?: return false
        directory.mkdirs()
        pruneExpiredSnapshots()
        val data = gson.toJson(Snapshot(
            segmentId = segment.id, segmentNumber = segment.segmentId, tripUuid = segment.trip?.uuid,
            segment = segment, trip = segment.trip, group = segment.trip?.group
        ))
        return try {
            val stream = file.startWrite()
            try {
                stream.write("${checksum(data)}\n$data".toByteArray(Charsets.UTF_8))
                file.finishWrite(stream)
            } catch (error: IOException) {
                file.failWrite(stream)
                throw error
            }
            true
        } catch (_: IOException) {
            // Cache may be unavailable. The recreated dialog will be discarded instead.
            false
        }
    }

    fun restore(key: String): TripSegment? {
        pruneExpiredSnapshots()
        val file = file(key) ?: return null
        val snapshot = try {
            val parts = String(file.readFully(), Charsets.UTF_8).split('\n', limit = 2)
            if (parts.size != 2 || parts[0] != checksum(parts[1])) return null
            gson.fromJson(parts[1], Snapshot::class.java)
        } catch (_: IOException) {
            return null
        } catch (_: JsonParseException) {
            return null
        } ?: return null
        if (snapshot.version != 1) return null
        val segment = snapshot.segment ?: return null
        if (snapshot.segmentId == null || segment.id != snapshot.segmentId ||
            segment.segmentId != snapshot.segmentNumber) return null
        val trip = snapshot.trip
        if (trip?.uuid != snapshot.tripUuid) return null
        val group = snapshot.group
        if (group != null && trip == null) return null
        // Gson bypasses property setters, so restore the transient graph explicitly.
        val groupTrips: List<Trip?>? = group?.trips
        if (group != null && (groupTrips == null || groupTrips.any { it == null })) return null
        groupTrips?.filterNotNull()?.forEach { candidate ->
            candidate.group = group
            val segments: List<TripSegment?>? = candidate.segmentList
            if (segments == null || segments.any { it == null }) return null
            segments.filterNotNull().forEach { it.trip = candidate }
        }
        val parent = if (group != null) {
            groupTrips?.filterNotNull()?.singleOrNull { it.uuid == trip?.uuid } ?: return null
        } else trip
        if (parent != null) {
            val segments: List<TripSegment?>? = parent.segmentList
            if (segments == null || segments.any { it == null }) return null
            val matches = segments.filterNotNull().withIndex().filter {
                it.value.id == segment.id && it.value.segmentId == segment.segmentId
            }
            if (matches.size != 1) return null
            parent.segmentList[matches.single().index] = segment
            parent.segmentList.forEach { it.trip = parent }
        }
        segment.trip = parent
        return segment
    }

    fun delete(key: String) { file(key)?.delete() }

    private fun pruneExpiredSnapshots() {
        // Tasks discarded after process death never deliver onDismiss. Bound their cache lifetime.
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        directory.listFiles()?.filter { it.lastModified() < cutoff }?.forEach { it.delete() }
    }

    private fun checksum(data: String): String = MessageDigest.getInstance("SHA-256")
        .digest(data.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    private fun file(key: String): AtomicFile? =
        if (key.matches(Regex("[a-f0-9-]{36}"))) AtomicFile(File(directory, key)) else null
}
