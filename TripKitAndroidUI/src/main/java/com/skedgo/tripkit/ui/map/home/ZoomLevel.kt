package com.skedgo.tripkit.ui.map.home

enum class ZoomLevel(val level: Float) {
    INNER(15.2f), OUTER(13f);

    companion object {
        const val ZOOM_VALUE_TO_SHOW_CITIES: Float = -7f

        fun fromLevel(level: Float): ZoomLevel? {
            for (zoomLevel in values()) {
                if (java.lang.Float.compare(level, zoomLevel.level) >= 0) {
                    return zoomLevel
                }
            }
            return null
        }
    }
}
