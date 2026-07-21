package com.skedgo.tripkit.ui.map

import android.os.Debug
import com.google.maps.android.collections.MarkerManager
import timber.log.Timber
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

internal object TripGoMapMarkerDiag {
    const val DEBUG_MAP_MARKER_RESOURCE_COUNTERS = false

    private const val TAG = "TripGoMapMarkerDiag"
    private const val MIN_LOG_INTERVAL_MS = 2_000L
    private val fdThresholds = intArrayOf(300, 500, 800, 1000, 1500)

    private val nextFdThresholdIndex = AtomicInteger(0)
    private val lastLogAtMs = AtomicLong(0L)

    private val cameraEvents = AtomicLong(0L)
    private val cameraIdleEvents = AtomicLong(0L)
    private val markerEmissions = AtomicLong(0L)
    private val markerRenderBatches = AtomicLong(0L)
    private val markersAdded = AtomicLong(0L)
    private val markersRemoved = AtomicLong(0L)
    private val markersHidden = AtomicLong(0L)
    private val markersShown = AtomicLong(0L)
    private val iconFetchCalls = AtomicLong(0L)
    private val bitmapCreations = AtomicLong(0L)
    private val bitmapScaleOrCopy = AtomicLong(0L)
    private val bitmapRecycles = AtomicLong(0L)
    private val bitmapDescriptorsFromBitmap = AtomicLong(0L)
    private val bitmapDescriptorCacheHits = AtomicLong(0L)
    private val bitmapDescriptorCacheMisses = AtomicLong(0L)
    private val dbQueries = AtomicLong(0L)
    private val locationFetches = AtomicLong(0L)

    fun isEnabled(): Boolean = DEBUG_MAP_MARKER_RESOURCE_COUNTERS

    fun recordCameraEvent() {
        if (!DEBUG_MAP_MARKER_RESOURCE_COUNTERS) return
        cameraEvents.incrementAndGet()
    }

    fun recordCameraIdle(snapshot: MarkerSnapshot) {
        if (!DEBUG_MAP_MARKER_RESOURCE_COUNTERS) return
        val count = cameraIdleEvents.incrementAndGet()
        maybeLog("cameraIdle event=idle cameraIdle=$count ${snapshot.format()}")
    }

    fun recordMarkerBatch(
        newMarkerOptions: Int,
        removedIds: Int,
        added: Int,
        removed: Int,
        snapshot: MarkerSnapshot
    ) {
        if (!DEBUG_MAP_MARKER_RESOURCE_COUNTERS) return
        markerEmissions.incrementAndGet()
        val batch = markerRenderBatches.incrementAndGet()
        markersAdded.addAndGet(added.toLong())
        markersRemoved.addAndGet(removed.toLong())
        maybeLog(
            "markerBatch event=render batch=$batch options=$newMarkerOptions removedIds=$removedIds " +
                "added=$added removed=$removed ${snapshot.format()}"
        )
    }

    fun recordMarkerVisibility(hidden: Int, shown: Int, snapshot: MarkerSnapshot) {
        if (!DEBUG_MAP_MARKER_RESOURCE_COUNTERS) return
        markersHidden.addAndGet(hidden.toLong())
        markersShown.addAndGet(shown.toLong())
        maybeLog("markerViewport event=hideViewport hidden=$hidden shown=$shown ${snapshot.format()}")
    }

    fun recordMarkerCleanup(event: String, before: Int, after: Int, snapshot: MarkerSnapshot) {
        if (!DEBUG_MAP_MARKER_RESOURCE_COUNTERS) return
        maybeLog("markerCleanup event=$event before=$before after=$after ${snapshot.format()}")
    }

    fun recordIconFetch() {
        if (!DEBUG_MAP_MARKER_RESOURCE_COUNTERS) return
        iconFetchCalls.incrementAndGet()
    }

    fun recordBitmapCreation(created: Int = 1, scaleOrCopy: Int = 0) {
        if (!DEBUG_MAP_MARKER_RESOURCE_COUNTERS) return
        bitmapCreations.addAndGet(created.toLong())
        bitmapScaleOrCopy.addAndGet(scaleOrCopy.toLong())
    }

    fun recordBitmapRecycle(recycled: Int = 1) {
        if (!DEBUG_MAP_MARKER_RESOURCE_COUNTERS) return
        bitmapRecycles.addAndGet(recycled.toLong())
    }

    fun recordBitmapDescriptorFromBitmap() {
        if (!DEBUG_MAP_MARKER_RESOURCE_COUNTERS) return
        bitmapDescriptorsFromBitmap.incrementAndGet()
        maybeLog("iconDescriptor event=fromBitmap")
    }

    fun recordBitmapDescriptorCacheHit(cacheSize: Int) {
        if (!DEBUG_MAP_MARKER_RESOURCE_COUNTERS) return
        bitmapDescriptorCacheHits.incrementAndGet()
        maybeLog("iconDescriptor event=cacheHit cacheSize=$cacheSize")
    }

    fun recordBitmapDescriptorCacheMiss(cacheSize: Int) {
        if (!DEBUG_MAP_MARKER_RESOURCE_COUNTERS) return
        bitmapDescriptorCacheMisses.incrementAndGet()
        maybeLog("iconDescriptor event=cacheMiss cacheSize=$cacheSize")
    }

    fun recordDbQuery(cellCount: Int, returnedStopCount: Int, usedBounds: Boolean) {
        if (!DEBUG_MAP_MARKER_RESOURCE_COUNTERS) return
        val count = dbQueries.incrementAndGet()
        maybeLog(
            "dbQuery event=queryStops query=$count cells=$cellCount stops=$returnedStopCount " +
                "path=Room bounds=$usedBounds"
        )
    }

    fun recordLocationFetch(cellCount: Int, inFlightCount: Int) {
        if (!DEBUG_MAP_MARKER_RESOURCE_COUNTERS) return
        val count = locationFetches.incrementAndGet()
        maybeLog("network event=locationsFetch fetch=$count cells=$cellCount inFlight=$inFlightCount")
    }

    private fun maybeLog(message: String) {
        val fdCount = fdCount()
        val thresholdMessage = thresholdMessage(fdCount)
        val now = System.currentTimeMillis()
        val last = lastLogAtMs.get()
        val shouldLog = thresholdMessage != null || now - last >= MIN_LOG_INTERVAL_MS
        if (!shouldLog || !lastLogAtMs.compareAndSet(last, now)) return

        Timber.tag(TAG).d(
            "%s fd=%d%s camera=%d idle=%d emissions=%d batches=%d addedTotal=%d removedTotal=%d " +
                "hiddenTotal=%d shownTotal=%d iconFetch=%d bitmapCreated=%d bitmapScaleCopy=%d " +
                "bitmapRecycled=%d descriptorFromBitmap=%d descriptorCacheHits=%d descriptorCacheMisses=%d " +
                "dbQueries=%d locationFetches=%d threads=%d javaHeapMb=%d nativeHeapMb=%d",
            message,
            fdCount,
            thresholdMessage.orEmpty(),
            cameraEvents.get(),
            cameraIdleEvents.get(),
            markerEmissions.get(),
            markerRenderBatches.get(),
            markersAdded.get(),
            markersRemoved.get(),
            markersHidden.get(),
            markersShown.get(),
            iconFetchCalls.get(),
            bitmapCreations.get(),
            bitmapScaleOrCopy.get(),
            bitmapRecycles.get(),
            bitmapDescriptorsFromBitmap.get(),
            bitmapDescriptorCacheHits.get(),
            bitmapDescriptorCacheMisses.get(),
            dbQueries.get(),
            locationFetches.get(),
            threadCount(),
            javaHeapMb(),
            nativeHeapMb()
        )
    }

    private fun fdCount(): Int = File("/proc/self/fd").list()?.size ?: -1

    private fun threadCount(): Int = Thread.getAllStackTraces().size

    private fun javaHeapMb(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / BYTES_PER_MB
    }

    private fun nativeHeapMb(): Long = Debug.getNativeHeapAllocatedSize() / BYTES_PER_MB

    private fun thresholdMessage(fdCount: Int): String? {
        if (fdCount < 0) return null
        while (true) {
            val index = nextFdThresholdIndex.get()
            if (index >= fdThresholds.size || fdCount < fdThresholds[index]) return null
            if (nextFdThresholdIndex.compareAndSet(index, index + 1)) {
                return " fdThreshold=${fdThresholds[index]}"
            }
        }
    }

    data class MarkerSnapshot(
        val visible: Int,
        val hidden: Int,
        val total: Int,
        val poiMarkers: Int,
        val byIdentifier: Int,
        val positions: Int,
        val regional: Int
    ) {
        fun format(): String =
            "visible=$visible hidden=$hidden total=$total poiMarkers=$poiMarkers " +
                "byIdentifier=$byIdentifier positions=$positions regional=$regional"
    }

    fun snapshot(
        collections: List<MarkerManager.Collection?>,
        poiMarkers: MarkerManager.Collection?,
        byIdentifierSize: Int,
        positionsSize: Int,
        regionalCount: Int
    ): MarkerSnapshot {
        if (!DEBUG_MAP_MARKER_RESOURCE_COUNTERS) {
            return MarkerSnapshot(0, 0, 0, 0, 0, 0, 0)
        }

        var visible = 0
        var hidden = 0
        for (collection in collections) {
            collection?.markers?.forEach { marker ->
                if (marker.isVisible) visible++ else hidden++
            }
        }
        val total = visible + hidden
        return MarkerSnapshot(
            visible = visible,
            hidden = hidden,
            total = total,
            poiMarkers = poiMarkers?.markers?.size ?: 0,
            byIdentifier = byIdentifierSize,
            positions = positionsSize,
            regional = regionalCount
        )
    }

    private const val BYTES_PER_MB = 1024L * 1024L
}
