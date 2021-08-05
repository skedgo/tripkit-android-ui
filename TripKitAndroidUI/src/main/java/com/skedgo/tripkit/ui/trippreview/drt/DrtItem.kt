package com.skedgo.tripkit.ui.trippreview.drt

import androidx.annotation.StringDef

@Retention(AnnotationRetention.RUNTIME)
@StringDef(
        DrtItem.MOBILITY_OPTIONS,
        DrtItem.PURPOSE,
        DrtItem.ADD_NOTE
)
annotation class DrtItem {
    companion object {
        const val MOBILITY_OPTIONS = "Mobility Options"
        const val PURPOSE = "Purpose"
        const val ADD_NOTE = "Add note"
    }
}