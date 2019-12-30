package com.skedgo.tripkit.ui.data.places

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class LatLngBounds (
        val southwest: LatLng,
        val northeast: LatLng
) : Parcelable