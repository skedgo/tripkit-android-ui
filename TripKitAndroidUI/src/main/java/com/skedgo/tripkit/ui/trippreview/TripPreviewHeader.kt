package com.skedgo.tripkit.ui.trippreview

import android.graphics.drawable.Drawable

data class TripPreviewHeader(
        val title: String? = null,
        val subTitle: String? = null,
        var icon: Drawable? = null,
        var pages: List<Int>? = null,
        val id: Long? = null,
        val description: String? = null,
        val modeId: String? = null
)