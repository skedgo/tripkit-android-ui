package com.skedgo.tripkit.ui.trippreview.drt

import androidx.annotation.StringDef

@Retention(AnnotationRetention.RUNTIME)
@StringDef(
        DrtItem.MOBILITY_OPTIONS,
        DrtItem.PURPOSE,
        DrtItem.ADD_NOTE,
        DrtItem.RETURN_TRIP
)
annotation class DrtItem {
    companion object {
        const val MOBILITY_OPTIONS = "Mobility Options"
        const val PURPOSE = "Purpose"
        const val ADD_NOTE = "Add note"
        const val RETURN_TRIP = "Return trip"
    }
}