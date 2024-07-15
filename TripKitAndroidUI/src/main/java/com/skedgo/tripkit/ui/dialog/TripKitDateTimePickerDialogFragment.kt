package com.skedgo.tripkit.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.core.view.allViews
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
import java.lang.Exception
import java.lang.reflect.Field
import java.util.*
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
        timePickerViewModel.handleArguments(arguments)
    }

    /**
     * @suppress
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogBuilder = FlatAlertDialogBuilder(requireActivity())

        if (timePickerViewModel.showPositiveAction().get()) {
            dialogBuilder.setPositiveButton(
                timePickerViewModel.positiveActionLabel().get(),
                onPositiveClickListener()
            )
        }

        if (timePickerViewModel.showNegativeAction().get()) {
            dialogBuilder.setNegativeButton(
                timePickerViewModel.negativeActionLabel().get(),
                onNegativeClickListener(),
                object : View.AccessibilityDelegate() {
                    override fun sendAccessibilityEvent(host: View, eventType: Int) {

                        var hostText: String = ""

                        if (host is Button) {
                            hostText = host.text.toString()
                        }

                        binding?.timePicker?.let {
                            host.contentDescription =
                                "$hostText ${getDateTimeForContentDescription(it)}"
                        }
                        super.sendAccessibilityEvent(host, eventType)
                    }
                }
            )

        }

        val dialog = dialogBuilder.setContentView(R.layout.dialog_date_time_picker)
            .setTitle(timePickerViewModel.dialogTitle().get())
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
        timePickerViewModel.isLeaveAfter.set(true)
        updateTimePicker()
    }

    /**
     * @suppress
     */
    fun onClickArriveBy(view: View) {
        timePickerViewModel.isLeaveAfter.set(false)
        updateTimePicker()
    }

    /**
     * @suppress
     */
    override fun onTimeChanged(timePicker: TimePicker, hour: Int, minute: Int) {
        val convertedMinute = minute * timePickerViewModel.timePickerMinuteInterval.get()
        if (isTimeValid(hour, convertedMinute)) {
            timePickerViewModel.updateTime(hour, convertedMinute)
        } else {
            var hours = timePickerViewModel.dateTimeMinLimit()?.hours
            var minutes = timePickerViewModel.dateTimeMinLimit()?.minutes
            timePickerViewModel.dateTimeMinLimit()?.let {
                val calendar = getCalendarFromDateWithTimezone(it, timePickerViewModel.timezone)
                hours = calendar.get(Calendar.HOUR_OF_DAY)
                minutes = calendar.get(Calendar.MINUTE)
            }
            updateTimePicker(hours, minutes)
        }
    }

    private fun getCalendarFromDateWithTimezone(date: Date, timezone: TimeZone?): Calendar {
        val calendar = timezone?.let { Calendar.getInstance(timezone) } ?: Calendar.getInstance()
        calendar.time = date
        return calendar
    }

    private fun isTimeValid(hour: Int, minute: Int): Boolean {
        val selectedDateCalendar = timePickerViewModel.selectedDate
        selectedDateCalendar?.let { sDateCal ->
            return timePickerViewModel.dateTimeMinLimit()?.let { minDateTime ->
                val minDateTimeCalendar = getCalendarFromDateWithTimezone(
                    minDateTime, timePickerViewModel.timezone
                )

                selectedDateCalendar.set(Calendar.HOUR_OF_DAY, hour)
                selectedDateCalendar.set(Calendar.MINUTE, minute)

                minDateTimeCalendar.timeInMillis < selectedDateCalendar.timeInMillis
            } ?: true
        }

        return true
    }

    private fun onNegativeClickListener(): DialogInterface.OnClickListener {
        return DialogInterface.OnClickListener { dialog, buttonType ->
            timeSelectedListener?.onTimeSelected(timePickerViewModel.leaveNow())
            dismiss()
        }
    }

    private fun onPositiveClickListener(): DialogInterface.OnClickListener {
        return DialogInterface.OnClickListener { dialog, buttonType ->
            timeSelectedListener?.onTimeSelected(timePickerViewModel.done())
            dismiss()
        }
    }

    private fun updateTimePicker(hour: Int? = null, minute: Int? = null) {
        val timePicker = binding!!.timePicker
        timePicker.setOnTimeChangedListener(null)

        initTimeSelectionDisplay(timePicker)

        timePicker.currentHour = hour ?: timePickerViewModel.hour
        timePicker.currentMinute = (minute ?: timePickerViewModel.minute) /
            timePickerViewModel.timePickerMinuteInterval.get()

        timePicker.setOnTimeChangedListener(this)

        timePicker.accessibilityDelegate = object : View.AccessibilityDelegate() {
            //To override and bypass accessibility reading since timepicker class
            //is inaccessible and cannot set content description programmatically
            override fun sendAccessibilityEvent(host: View, eventType: Int) {
                if (host is TimePicker) {
                    binding?.timePicker?.let {
                        host.contentDescription = getDateTimeForContentDescription(it)
                    }
                }

                super.sendAccessibilityEvent(host, eventType)
            }
        }
    }

    private fun getDateTimeForContentDescription(timePicker: TimePicker): String {
        val currentMinute =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                timePicker.minute
            } else {
                timePicker.currentMinute
            }

        var currentHour =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                timePicker.hour
            } else {
                timePicker.currentHour
            }

        var amPm = "AM"

        if (!timePicker.is24HourView) {
            if (currentHour > 11) {
                amPm = "PM"
                currentHour -= 12
            }
        }

        val convertedMinute = currentMinute * timePickerViewModel.timePickerMinuteInterval.get()
        val hourForDescription = "${if (currentHour == 0) 12 else currentHour}"
        val minutesForDescription = "${if (convertedMinute > 0) convertedMinute else ""}"
        return "$hourForDescription $minutesForDescription ${if (!timePicker.is24HourView) amPm else ""}"
    }

    @SuppressLint("PrivateApi")
    private fun initTimeSelectionDisplay(timePicker: TimePicker) {

        var minuteSpinner: NumberPicker? = null

        try {
            val classForId = Class.forName("com.android.internal.R\$id")
            val field: Field = classForId.getField("minute")

            minuteSpinner = timePicker.findViewById(field.getInt(null)) as NumberPicker

        } catch (e: NoSuchFieldException) {
            try {
                minuteSpinner = timePicker.allViews.filter {
                    it::class.java.simpleName == "NumberPicker"
                }.toList()[1] as NumberPicker
            } catch (e: Exception) {
                timePickerViewModel.timePickerMinuteInterval.set(1)
            }
        }

        minuteSpinner?.let {
            minuteSpinner.minValue = 0
            minuteSpinner.maxValue = (60 / timePickerViewModel.timePickerMinuteInterval.get()) - 1
            val displayedValues = mutableListOf<String>()
            for (i in 0..60 step timePickerViewModel.timePickerMinuteInterval.get()) {
                displayedValues.add(String.format("%02d", i))
            }
            minuteSpinner.displayedValues = displayedValues.toTypedArray()
        }

    }

    private fun initDateSpinner() {
        val adapter = DateSpinnerAdapter(
            activity,
            android.R.layout.simple_spinner_item,
            timePickerViewModel.dates().get()
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        val dateSpinner = binding!!.dateSpinner
        dateSpinner.adapter = adapter
        dateSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                timePickerViewModel.selectedPosition().set(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        timePickerViewModel.dates()
            .addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: Observable, propertyId: Int) {
                    adapter.setDates(timePickerViewModel.dates().get())
                }
            })
    }

    /**
     * Used to create a new instance of the fragment.
     */
    class Builder {
        private var title: String? = null
        private var departureTimezone: String? = null
        private var arrivalTimezone: String? = null
        private var timeType = TimeTag.TIME_TYPE_LEAVE_AFTER
        private var timeMillis = System.currentTimeMillis()
        private var leaveAtLabel: String? = null
        private var arriveByLabel: String? = null
        private var positiveActionLabel: Int = 0
        private var negativeActionLabel: Int = 0
        private var showPositiveAction: Boolean = false
        private var showNegativeAction: Boolean = false
        private var isSingleSelection: Boolean = false
        private var singleLabel: String? = null
        private var dateTimeMinLimit: Date? = null
        private var timePickerMinutesInterval: Int = 1

        fun withTitle(title: String?): Builder {
            this.title = title
            return this
        }

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

        fun withPositiveAction(label: Int?): Builder {
            this.positiveActionLabel = label ?: 0
            this.showPositiveAction = true
            return this
        }

        fun withNegativeAction(label: Int?): Builder {
            this.negativeActionLabel = label ?: 0
            this.showNegativeAction = true
            return this
        }

        fun setLeaveAtLabel(leaveAtLabel: String?): Builder {
            this.leaveAtLabel = leaveAtLabel
            return this
        }

        fun setArriveByLabel(arriveByLabel: String?): Builder {
            this.arriveByLabel = arriveByLabel
            return this
        }

        fun isSingleSelection(label: String?): Builder {
            this.singleLabel = label
            this.isSingleSelection = true
            this.timeType = TimeTag.TIME_TYPE_SINGLE_SELECTION
            return this
        }

        fun withDateTimeMinLimit(dateTimeMinLimit: Date): Builder {
            this.dateTimeMinLimit = dateTimeMinLimit
            return this
        }

        fun setTimePickerMinutesInterval(interval: Int): Builder {
            this.timePickerMinutesInterval = interval
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
            args.putString(InterCityTimePickerViewModel.ARG_TITLE, title)
            args.putBoolean(
                InterCityTimePickerViewModel.ARG_SHOW_POSITIVE_ACTION,
                showPositiveAction
            )
            args.putInt(InterCityTimePickerViewModel.ARG_POSITIVE_ACTION, positiveActionLabel)
            args.putBoolean(
                InterCityTimePickerViewModel.ARG_SHOW_NEGATIVE_ACTION,
                showNegativeAction
            )
            args.putInt(InterCityTimePickerViewModel.ARG_NEGATIVE_ACTION, negativeActionLabel)
            args.putString(InterCityTimePickerViewModel.ARG_LEAVE_AT_LABEL, leaveAtLabel)
            args.putString(InterCityTimePickerViewModel.ARG_ARRIVE_BY_LABEL, arriveByLabel)
            args.putString(InterCityTimePickerViewModel.ARG_SINGLE_SELECTION_LABEL, singleLabel)
            args.putString(InterCityTimePickerViewModel.ARG_TITLE, title)
            args.putString(InterCityTimePickerViewModel.ARG_DEPARTURE_TIMEZONE, departureTimezone)
            args.putString(InterCityTimePickerViewModel.ARG_ARRIVAL_TIMEZONE, arrivalTimezone)
            args.putInt(InterCityTimePickerViewModel.ARG_TIME_TYPE, timeType)
            args.putLong(InterCityTimePickerViewModel.ARG_TIME_IN_MILLIS, timeMillis)
            args.putInt(
                InterCityTimePickerViewModel.ARG_TIME_PICKER_MINUTES_INTERVAL,
                timePickerMinutesInterval
            )
            dateTimeMinLimit?.let {
                args.putLong(InterCityTimePickerViewModel.ARG_DATE_TIME_PICKER_MIN_LIMIT, it.time)
            }
            fragment.arguments = args
            return fragment
        }
    }
}