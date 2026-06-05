package com.skedgo.tripkit.ui.map.home

/**
 * The single authoritative marker-level policy used by BOTH the fetch path
 * ([FetchStopsByViewport]) and the display path ([TripKitMapFragment]).
 *
 * - [CITY_ONLY]        super zoomed out: only city markers (region metadata), no stop fetch.
 * - [REGION_ONLY]      zoomed out: only region-level (parent) stop markers.
 * - [REGION_AND_LOCAL] zoomed in: region-level + local stop markers.
 */
enum class MarkerZoomMode {
    CITY_ONLY,
    REGION_ONLY,
    REGION_AND_LOCAL
}

enum class ZoomLevel(val level: Float) {
    CITY(8.0f), REGIONAL(15.0f), LOCAL(15.0f);

    companion object {
        const val ZOOM_VALUE_TO_SHOW_CITIES: Float = 8.0f
        const val ZOOM_START_VALUE_TO_SHOW_REGIONAL: Float = 8.1f
        const val ZOOM_START_VALUE_FOR_LOCAL: Float = 13.5f

        fun fromLevel(level: Float): ZoomLevel? {
            return when {
                level <= ZOOM_VALUE_TO_SHOW_CITIES -> CITY
                level > ZOOM_START_VALUE_TO_SHOW_REGIONAL && level <= ZOOM_START_VALUE_FOR_LOCAL -> REGIONAL
                level > ZOOM_START_VALUE_FOR_LOCAL -> LOCAL
                else -> null
            }
        }

        /**
         * Maps a Google Maps zoom value to the authoritative [MarkerZoomMode].
         * Both marker fetching and marker display must use this so the two never
         * disagree (previously fetch used 13.5 while display used 8.1/12.0/12.1/14.5).
         */
        fun markerModeFor(zoom: Float): MarkerZoomMode {
            return when {
                zoom <= ZOOM_VALUE_TO_SHOW_CITIES -> MarkerZoomMode.CITY_ONLY
                zoom <= ZOOM_START_VALUE_FOR_LOCAL -> MarkerZoomMode.REGION_ONLY
                else -> MarkerZoomMode.REGION_AND_LOCAL
            }
        }
    }
}
