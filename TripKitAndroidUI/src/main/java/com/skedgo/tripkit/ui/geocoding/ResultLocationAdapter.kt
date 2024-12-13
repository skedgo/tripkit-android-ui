package com.skedgo.tripkit.ui.geocoding

import com.skedgo.tripkit.ui.data.places.Place

interface ResultLocationAdapter<T : Place?> {
    fun getPlace(): T
}