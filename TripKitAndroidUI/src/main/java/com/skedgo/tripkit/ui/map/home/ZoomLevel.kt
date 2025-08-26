package com.skedgo.tripkit.ui.map.home

enum class ZoomLevel(val level: Float) {
    INNER(15.2f), REGIONAL(13f), OUTER(13f);

    companion object {
        const val ZOOM_VALUE_TO_SHOW_CITIES: Float = 8f

        fun fromLevel(level: Float): ZoomLevel? {
            return when {
                level >= 15.2f -> INNER
                level >= 13.0f -> REGIONAL
                level >= 8.1f -> OUTER
                else -> null // Only return null for zoom levels < 8.1f
            }
        }
    }
}
