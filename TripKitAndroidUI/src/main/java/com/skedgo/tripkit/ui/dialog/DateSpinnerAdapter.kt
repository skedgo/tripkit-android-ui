package com.skedgo.tripkit.ui.dialog

import android.content.Context
import android.widget.ArrayAdapter

class DateSpinnerAdapter(
    context: Context?,
    resource: Int,
    private val dates: MutableList<String>
) : ArrayAdapter<String>(
    context!!, resource, dates
) {
    fun setDates(dates: List<String>) {
        this.dates.clear()
        this.dates.addAll(dates)
        this.notifyDataSetChanged()
    }

    override fun getItem(position: Int): String {
        return dates[position]
    }
}
