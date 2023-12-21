package com.skedgo.tripkit.ui.trippreview.segment

import android.graphics.drawable.Drawable

/**
 * Renamed from [TripPreviewHeader] to [TripSegmentSummary]
 * since it'll be generic and will not only be used on the preview header feature
 */
data class TripSegmentSummary(
    val title: String? = null,
    val subTitle: String? = null,
    var icon: Drawable? = null,
    var pages: List<Int>? = null,
    val id: Long? = null,
    val description: String? = null,
    val modeId: String? = null,
    val isHideExactTimes: Boolean = false
)
