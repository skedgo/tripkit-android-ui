package com.skedgo.tripkit.ui.dialog

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import androidx.fragment.app.DialogFragment
import com.skedgo.tripkit.ui.ARG_HOUR
import com.skedgo.tripkit.ui.ARG_MINUTE
import java.time.LocalTime


class TripKitTimePickerDialogFragment : DialogFragment() {
    var onTimeSetListener: TimePickerDialog.OnTimeSetListener? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val hour = arguments?.getInt(ARG_HOUR) ?: 9
        val minute = arguments?.getInt(ARG_MINUTE) ?: 0
        return TimePickerDialog(activity, onTimeSetListener, hour, minute,
                DateFormat.is24HourFormat(activity))
    }

    class Builder {
        private var time: LocalTime = LocalTime.of(9,0)
        fun withLocalTime(time: LocalTime) : Builder {
            this.time = time
            return this
        }

        fun build(): TripKitTimePickerDialogFragment = TripKitTimePickerDialogFragment().apply {
            val args = Bundle().apply {
                putInt(ARG_HOUR, time.hour)
                putInt(ARG_MINUTE, time.minute)

            }
            arguments = args
        }
    }
}