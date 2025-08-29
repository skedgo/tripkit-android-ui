package com.skedgo.tripkit.ui.map.home

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
    }
}
