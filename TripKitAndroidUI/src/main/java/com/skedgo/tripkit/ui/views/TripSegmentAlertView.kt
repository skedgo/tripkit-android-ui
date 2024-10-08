package com.skedgo.tripkit.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.skedgo.tripkit.common.model.realtimealert.RealtimeAlert
import com.skedgo.tripkit.ui.R


class TripSegmentAlertView : LinearLayout {
    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()
    }


    private fun init() {
        orientation = VERTICAL
    }

    fun setAlerts(alerts: ArrayList<RealtimeAlert>?) {
        removeAllViews()
        if (alerts.isNullOrEmpty()) {
            return
        }

        val inflator = LayoutInflater.from(context)
        val headerView = inflator.inflate(R.layout.trip_segment_alert_header_item, this, true)
        val headerText = headerView.findViewById(R.id.header_text) as TextView
        headerText.text =
            resources.getQuantityString(R.plurals.number_of_alerts, alerts.size, alerts.size)

        alerts.forEach { alert ->
            val view =
                inflator.inflate(R.layout.trip_segment_alert_view_item, this, false) as TextView
            view.text = alert.title()
            addView(view)
        }
        invalidate()
    }
}