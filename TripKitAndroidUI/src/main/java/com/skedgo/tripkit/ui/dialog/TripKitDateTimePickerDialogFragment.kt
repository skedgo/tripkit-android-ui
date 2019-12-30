package com.skedgo.tripkit.ui.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.TimePicker
import androidx.databinding.DataBindingUtil
import androidx.databinding.Observable
import androidx.fragment.app.DialogFragment
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.common.model.TimeTag
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.databinding.DialogDateTimePickerBinding
import com.skedgo.tripkit.ui.trip.details.viewmodel.ITimePickerViewModel
import com.skedgo.tripkit.ui.trip.options.InterCityTimePickerViewModel

import javax.inject.Inject

/**
 * A DialogFragment which allows a user to set a time and choose whether it is a departure time or an arrival time.
 *
 *
 * ```
 *      DialogFragment fragment = TripKitDateTimePickerDialogFragment.Builder()
 *                                  .withLocations(toLocation, fromLocation)
 *                                  .withTimeTag(timeTagForQuery)
 *                                  .build();
 *
 *      fragment.show(supportFragmentManager, "timePicker")
 * ```
 */
class TripKitDateTimePickerDialogFragment : DialogFragment(), TimePicker.OnTimeChangedListener {

    /**
     * @suppress
     */
    var selectionChangedRelay = PublishRelay.create<TimeTag>()

    private var timeSelectedListener: OnTimeSelectedListener? = null

    /**
     * @suppress
     */
    @Inject
    lateinit var timePickerViewModel: ITimePickerViewModel

    private var binding: DialogDateTimePickerBinding? = null

    /**
     * Interface definition for a callback to be invoked when a time is selected.
     */
    interface OnTimeSelectedListener {
        /**
         * Called when the user presses the "Done" button.
         *
         * @param timeTag The chosen TimeTag
         */
        fun onTimeSelected(timeTag: TimeTag)
    }

    fun setOnTimeSelectedListener(listener: OnTimeSelectedListener) {
        this.timeSelectedListener = listener
    }

    fun setOnTimeSelectedListener(listener: (TimeTag) -> Unit) {
        this.timeSelectedListener = object : OnTimeSelectedListener {
            override fun onTimeSelected(timeTag: TimeTag) {
                listener(timeTag)
            }
        }
    }

    /**
     * @suppress
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TripKitUI.getInstance().timePickerComponent().inject(this)
        timePickerViewModel!!.handleArguments(arguments)
    }

    /**
     * @suppress
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogBuilder = FlatAlertDialogBuilder(activity)
        val dialog = dialogBuilder.setTitle(R.string.set_time)
                .setContentView(R.layout.dialog_date_time_picker)
                .setPositiveButton(R.string.done, onPositiveClickListener())
                .setNegativeButton(R.string.leave_now, onNegativeClickListener())
                .create()

        val contentView = dialogBuilder.contentView
        binding = DataBindingUtil.bind(contentView)
        binding!!.viewModel = timePickerViewModel as InterCityTimePickerViewModel?
        binding!!.handlers = this@TripKitDateTimePickerDialogFragment

        initDateSpinner()
        updateTimePicker()
        return dialog
    }

    /**
     * @suppress
     */
    fun onClickLeaveAfter(view: View) {
        timePickerViewModel!!.isLeaveAfter.set(true)
        updateTimePicker()
    }

    /**
     * @suppress
     */
    fun onClickArriveBy(view: View) {
        timePickerViewModel!!.isLeaveAfter.set(false)
        updateTimePicker()
    }

    /**
     * @suppress
     */
    override fun onTimeChanged(timePicker: TimePicker, hour: Int, minute: Int) {
        timePickerViewModel!!.updateTime(hour, minute)
    }

    private fun onNegativeClickListener(): DialogInterface.OnClickListener {
        return DialogInterface.OnClickListener { dialog, buttonType ->
            if (timeSelectedListener != null) {
                timeSelectedListener!!.onTimeSelected(timePickerViewModel!!.leaveNow())
            }
            dismiss()
        }
    }

    private fun onPositiveClickListener(): DialogInterface.OnClickListener {
        return DialogInterface.OnClickListener { dialog, buttonType ->
            if (timeSelectedListener != null) {
                timeSelectedListener!!.onTimeSelected(timePickerViewModel!!.done())
            }

            dismiss()
        }
    }

    private fun updateTimePicker() {
        val timePicker = binding!!.timePicker
        timePicker.setOnTimeChangedListener(null)
        timePicker.currentHour = timePickerViewModel!!.hour
        timePicker.currentMinute = timePickerViewModel!!.minute
        timePicker.setOnTimeChangedListener(this)
    }

    private fun initDateSpinner() {
        val adapter = DateSpinnerAdapter(
                activity,
                android.R.layout.simple_spinner_item,
                timePickerViewModel!!.dates().get()
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        val dateSpinner = binding!!.dateSpinner
        dateSpinner.adapter = adapter
        dateSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                timePickerViewModel!!.selectedPosition().set(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        timePickerViewModel!!.dates().addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable, propertyId: Int) {
                adapter.setDates(timePickerViewModel!!.dates().get())
            }
        })
    }

    /**
     * Used to create a new instance of the fragment.
     */
    class Builder {
        private var departureTimezone: String? = null
        private var arrivalTimezone: String? = null
        private var timeType = TimeTag.TIME_TYPE_LEAVE_AFTER
        private var timeMillis = System.currentTimeMillis()

        /**
         * Sets the departure and arrival location so that the timezones can be supported.
         *
         * @param departureLocation
         * @param arrivalLocation
         * @return this builder
         */
        fun withLocations(departureLocation: Location?, arrivalLocation: Location?): Builder {
            var departureTimezone: String? = null
            var arrivalTimezone: String? = null

            if (departureLocation != null) {
                departureTimezone = departureLocation.timeZone
            }

            if (arrivalLocation != null) {
                arrivalTimezone = arrivalLocation.timeZone
            }

            return withTimeZones(departureTimezone, arrivalTimezone)
        }

        /**
         * Sets the timezones.
         *
         * @param departureTimezone
         * @param arrivalTimezone
         * @return this builder
         */
        fun withTimeZones(departureTimezone: String?, arrivalTimezone: String?): Builder {
            this.departureTimezone = departureTimezone
            this.arrivalTimezone = arrivalTimezone
            return this
        }

        /**
         * Automatically populates the fragment using an existing TimeTag.
         *
         * @param tag
         * @return this builder
         */
        fun withTimeTag(tag: TimeTag): Builder {
            this.timeType = tag.type
            this.timeMillis = tag.timeInMillis
            return this
        }

        /**
         * Sets a [TimeType] value, which corresponds to:
         * 0 - Leave after
         * 1 - Arrive by
         *
         * @param timeType a [com.skedgo.android.common.model.TimeTag.TimeType] value.
         * @return this builder
         */
        fun withTimeType(timeType: Int): Builder {
            this.timeType = timeType
            return this
        }

        /**
         * The time in milliseconds to display.
         *
         * @param millis
         * @return this builder
         */
        fun timeMillis(millis: Long): Builder {
            this.timeMillis = millis
            return this
        }

        /**
         * Builds a new instance of the fragment.
         *
         * @return A new instance
         */
        fun build(): TripKitDateTimePickerDialogFragment {
            val fragment = TripKitDateTimePickerDialogFragment()
            val args = Bundle()
            args.putString(InterCityTimePickerViewModel.ARG_DEPARTURE_TIMEZONE, departureTimezone)
            args.putString(InterCityTimePickerViewModel.ARG_ARRIVAL_TIMEZONE, arrivalTimezone)
            args.putInt(InterCityTimePickerViewModel.ARG_TIME_TYPE, timeType)
            args.putLong(InterCityTimePickerViewModel.ARG_TIME_IN_MILLIS, timeMillis)
            fragment.arguments = args
            return fragment
        }
    }
}