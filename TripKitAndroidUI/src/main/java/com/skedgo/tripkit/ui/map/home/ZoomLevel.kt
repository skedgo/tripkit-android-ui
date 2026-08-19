package com.skedgo.tripkit.ui.map.home

enum class ZoomLevel(val level: Float) {
    CITY(8.0f), REGIONAL(15.0f), LOCAL(15.0f);

    companion object {
        const val ZOOM_VALUE_TO_SHOW_CITIES: Float = 8.0f
        const val ZOOM_START_VALUE_TO_SHOW_REGIONAL: Float = 8.1f
        const val ZOOM_START_VALUE_FOR_LOCAL: Float = 13.5f

        /**
         * Returns one authoritative marker band for every map zoom value.
         *
         * Google Maps reports continuous zoom values, so leaving a gap between 8.0 and 8.1
         * allows stale markers from the previous band to remain visible. Treat values below the
         * regional threshold as city zoom, then use half-open ranges for the other bands.
         */
        fun fromLevel(level: Float): ZoomLevel {
            return when {
                level < ZOOM_START_VALUE_TO_SHOW_REGIONAL -> CITY
                level < ZOOM_START_VALUE_FOR_LOCAL -> REGIONAL
                else -> LOCAL
            }
        }
    }
}
