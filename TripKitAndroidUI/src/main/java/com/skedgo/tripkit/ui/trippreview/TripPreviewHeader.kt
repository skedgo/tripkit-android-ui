package com.skedgo.tripkit.ui.trippreview

import android.graphics.drawable.Drawable

data class TripPreviewHeader(
        val title: String?,
        val subTitle: String?,
        val icon: Drawable?,
        val pages: List<Int>
)
