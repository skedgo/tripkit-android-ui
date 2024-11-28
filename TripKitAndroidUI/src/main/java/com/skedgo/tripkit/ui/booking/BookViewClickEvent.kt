package com.skedgo.tripkit.ui.booking

import android.view.View
import android.view.View.OnClickListener
import com.skedgo.tripkit.routing.TripSegment
import com.squareup.otto.Bus

class BookViewClickEvent(
    private val bus: Bus,
    val segment: TripSegment
) : OnClickListener {
    override fun onClick(v: View) {
        bus.post(this)
    }
}