package com.skedgo.tripkit.ui.dialog

import android.text.format.Time

class TimeDatePickedEvent(var timeType: Int, val initiatorId: String, var time: Time) {
    companion object {
        /* To avoid cross dependency between EventsDetailsFragment and TimeDatePickerFragment,
    those constants are tucked away here
     */
        const val DATE_FORMAT_STRING: String = "EE, MMM d yyyy 'at' hh:mm a"
        const val DATE_FORMAT_STRING_NO_TIME: String = "EE, MMM d yyyy"
        const val TIME_TYPE_BEGIN: Int = 1
        const val TIME_TYPE_END: Int = 2
        const val TIME_TYPE_OTHER: Int = 200
    }
}