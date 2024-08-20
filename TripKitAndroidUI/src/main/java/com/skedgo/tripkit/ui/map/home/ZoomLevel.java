package com.skedgo.tripkit.ui.map.home;

import androidx.annotation.Nullable;

public enum ZoomLevel {
    INNER(15.2f), OUTER(13f);
    public static final float ZOOM_VALUE_TO_SHOW_CITIES = -7f;

    public final float level;

    ZoomLevel(float level) {
        this.level = level;
    }

    @Nullable
    public static ZoomLevel fromLevel(float level) {
        for (ZoomLevel zoomLevel : ZoomLevel.values()) {
            if (Float.compare(level, zoomLevel.level) >= 0) {
                return zoomLevel;
            }
        }
        return null;
    }
}
