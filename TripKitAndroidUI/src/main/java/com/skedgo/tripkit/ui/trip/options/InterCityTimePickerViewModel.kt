package com.skedgo.tripkit.ui.trip.options

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.text.format.DateFormat
import androidx.annotation.VisibleForTesting
import androidx.databinding.Observable
import androidx.databinding.Observable.OnPropertyChangedCallback
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.skedgo.tripkit.common.model.time.TimeTag
import com.skedgo.tripkit.time.GetNow
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.trip.details.viewmodel.ITimePickerViewModel
import com.squareup.otto.Bus
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit.MILLISECONDS

class InterCityTimePickerViewModel(
    private val context: Context,
    private val eventBus: Bus,
    private val getNow: GetNow,
    private val defaultTimezone: String
) : ITimePickerViewModel {
    private val defaultTimeType = TimeTag.TIME_TYPE_LEAVE_AFTER

    @VisibleForTesting
    var departureTimezone: TimeZone? = null
    var arrivalTimezone: TimeZone? = null
    var timeMillis: Long = 0
    private val dates = ObservableField<List<String>>()
    private var timeCalendar: GregorianCalendar? = null
    private var departureCalendars: List<GregorianCalendar>? = null
    private var arrivalCalendars: List<GregorianCalendar>? = null
    private var singleSelectionCalendars: List<GregorianCalendar>? = null
    private val selectedPosition = ObservableInt(1)
    private val isLeaveAfter = ObservableBoolean(true)
    private val isSingleSelection = ObservableBoolean(false)
    private val dialogTitle = ObservableField<String>()
    private val leaveAtLabel: ObservableField<String>
    private val arriveByLabel: ObservableField<String>
    private val singleLabel: ObservableField<String>
    private val positiveActionLabel: ObservableInt
    private val showPositiveAction: ObservableBoolean
    private val negativeActionLabel: ObservableInt
    private val showNegativeAction: ObservableBoolean
    private val dateTimePickerMinLimit: ObservableField<Date>
    private val timePickerMinuteInterval: ObservableInt
    private var extraSelectionCount = 0

    init {
        this.leaveAtLabel = ObservableField<String>(context.getString(R.string.leave_at))
        this.arriveByLabel = ObservableField<String>(context.getString(R.string.arrive_by))
        this.positiveActionLabel = ObservableInt(R.string.done)
        this.showPositiveAction = ObservableBoolean(false)
        this.negativeActionLabel = ObservableInt(R.string.leave_now)
        this.showNegativeAction = ObservableBoolean(false)
        this.singleLabel = ObservableField()
        this.dateTimePickerMinLimit = ObservableField()
        this.timePickerMinuteInterval = ObservableInt(1)
    }


    override fun handleArguments(args: Bundle) {
        if (args != null) {
            var extraTimezone: String?
            if (args.containsKey(ARG_DEPARTURE_TIMEZONE)) {
                extraTimezone = args.getString(ARG_DEPARTURE_TIMEZONE)
                if (!TextUtils.isEmpty(extraTimezone)) {
                    this.departureTimezone = TimeZone.getTimeZone(extraTimezone)
                } else {
                    this.departureTimezone = TimeZone.getTimeZone(
                        defaultTimezone
                    )
                }
            }
            if (args.containsKey(ARG_ARRIVAL_TIMEZONE)) {
                extraTimezone = args.getString(ARG_ARRIVAL_TIMEZONE)
                if (!TextUtils.isEmpty(extraTimezone)) {
                    this.arrivalTimezone = TimeZone.getTimeZone(extraTimezone)
                } else {
                    this.arrivalTimezone = TimeZone.getTimeZone(
                        defaultTimezone
                    )
                }
            }
            if (args.containsKey(ARG_TIME_IN_MILLIS)) {
                this.timeMillis = args.getLong(ARG_TIME_IN_MILLIS)
            }

            if (args.containsKey(ARG_TIME_TYPE)) {
                val timeType = args.getInt(ARG_TIME_TYPE)
                if (timeType == TimeTag.TIME_TYPE_SINGLE_SELECTION) {
                    isLeaveAfter.set(false)
                    isSingleSelection.set(true)
                } else {
                    isLeaveAfter.set(timeType == defaultTimeType)
                    isSingleSelection.set(false)
                }
            }
            if (args.containsKey(ARG_TITLE)) {
                dialogTitle.set(args.getString(ARG_TITLE, ""))
            }
            if (args.containsKey(ARG_SHOW_POSITIVE_ACTION)) {
                val show = args.getBoolean(ARG_SHOW_POSITIVE_ACTION)
                if (show && args.containsKey(ARG_POSITIVE_ACTION)) {
                    val label = args.getInt(ARG_POSITIVE_ACTION, 0)
                    positiveActionLabel.set(label)
                    showPositiveAction.set(label != 0)
                }
            }
            if (args.containsKey(ARG_SHOW_NEGATIVE_ACTION)) {
                val show = args.getBoolean(ARG_SHOW_NEGATIVE_ACTION)
                if (show && args.containsKey(ARG_NEGATIVE_ACTION)) {
                    val label = args.getInt(ARG_NEGATIVE_ACTION, 0)
                    negativeActionLabel.set(label)
                    showNegativeAction.set(label != 0)
                }
            }
            if (args.containsKey(ARG_LEAVE_AT_LABEL)) {
                leaveAtLabel.set(args.getString(ARG_LEAVE_AT_LABEL, ""))
            }
            if (args.containsKey(ARG_ARRIVE_BY_LABEL)) {
                arriveByLabel.set(args.getString(ARG_ARRIVE_BY_LABEL, ""))
            }
            if (args.containsKey(ARG_SINGLE_SELECTION_LABEL)) {
                singleLabel.set(args.getString(ARG_SINGLE_SELECTION_LABEL, ""))
            }
            if (args.containsKey(ARG_DATE_TIME_PICKER_MIN_LIMIT)) {
                val dateTimeLong = args.getLong(ARG_DATE_TIME_PICKER_MIN_LIMIT, -1L)
                if (dateTimeLong != -1L) {
                    dateTimePickerMinLimit.set(Date(dateTimeLong))
                }
            }
            if (args.containsKey(ARG_TIME_PICKER_MINUTES_INTERVAL)) {
                timePickerMinuteInterval.set(args.getInt(ARG_TIME_PICKER_MINUTES_INTERVAL, 1))
            }

            initValues()
        }
    }

    override fun dialogTitle(): ObservableField<String> {
        return dialogTitle
    }

    override fun leaveAtLabel(): ObservableField<String> {
        return leaveAtLabel
    }

    override fun arriveByLabel(): ObservableField<String> {
        return arriveByLabel
    }

    override fun singleSelectionLabel(): ObservableField<String> {
        return singleLabel
    }

    override fun isSingleSelection(): ObservableBoolean {
        return isSingleSelection
    }

    override fun positiveActionLabel(): ObservableInt {
        return positiveActionLabel
    }

    override fun showPositiveAction(): ObservableBoolean {
        return showPositiveAction
    }

    override fun negativeActionLabel(): ObservableInt {
        return negativeActionLabel
    }

    override fun showNegativeAction(): ObservableBoolean {
        return showNegativeAction
    }

    override fun dates(): ObservableField<List<String>> {
        return dates
    }

    override fun getHour(): Int {
        return timeCalendar!![Calendar.HOUR_OF_DAY]
    }

    override fun getMinute(): Int {
        return timeCalendar!![Calendar.MINUTE]
    }

    override fun selectedPosition(): ObservableInt {
        return selectedPosition
    }

    override fun isLeaveAfter(): ObservableBoolean {
        return isLeaveAfter
    }

    override fun dateTimeMinLimit(): Date? {
        return dateTimePickerMinLimit.get()
    }

    override fun updateTime(hour: Int, minute: Int) {
        timeCalendar!![Calendar.HOUR_OF_DAY] = hour
        timeCalendar!![Calendar.MINUTE] = minute
    }

    override fun leaveNow(): TimeTag {
        return TimeTag.createForLeaveNow()
    }

    override fun done(): TimeTag {
        val position = selectedPosition.get()
        val dateCalendar =
            if (isSingleSelection.get()) singleSelectionCalendars!![position] else if (isLeaveAfter.get()) departureCalendars!![position] else arrivalCalendars!![position]
        dateCalendar.timeZone = departureTimezone
        return getTimeTagFromDateTime(dateCalendar, timeCalendar!!)
    }

    override fun getTimePickerMinuteInterval(): ObservableInt {
        return timePickerMinuteInterval
    }

    /**
     * Visible only for testing.
     */
    fun offsetTimeZone(date: Date?, fromTimeZone: TimeZone?, toTimeZone: TimeZone?): Calendar {
        val calendar = Calendar.getInstance()
        calendar.timeZone = fromTimeZone
        calendar.time = date
        // FROM TimeZone to UTC
        calendar.add(Calendar.MILLISECOND, fromTimeZone!!.rawOffset * -1)
        if (fromTimeZone.inDaylightTime(calendar.time)) {
            calendar.add(Calendar.MILLISECOND, calendar.timeZone.dstSavings * -1)
        }
        // UTC to TO TimeZone
        calendar.add(Calendar.MILLISECOND, toTimeZone!!.rawOffset)
        if (toTimeZone.inDaylightTime(calendar.time)) {
            calendar.add(Calendar.MILLISECOND, toTimeZone.dstSavings)
        }
        return calendar
    }

    private fun initValues() {
        val departureTime = createTime(departureTimezone)
        val arrivalTime = createTime(arrivalTimezone)
        val singleSelectionTime = createTime(arrivalTimezone)
        this.departureCalendars = createDateRange(departureTime.clone() as GregorianCalendar)
        this.arrivalCalendars = createDateRange(arrivalTime.clone() as GregorianCalendar)
        this.singleSelectionCalendars =
            createDateRange(singleSelectionTime.clone() as GregorianCalendar)
        isLeaveAfter.addOnPropertyChangedCallback(onTimeTypePropertyChanged())
        isSingleSelection.addOnPropertyChangedCallback(onTimeTypePropertyChanged())
        moveToLastSelectedTime()
    }

    private fun onTimeTypePropertyChanged(): OnPropertyChangedCallback {
        return object : OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable, propertyId: Int) {
                notifyTimeTypeChange()
            }
        }
    }

    private fun createTime(timezone: TimeZone?): GregorianCalendar {
        val calendar = GregorianCalendar(timezone)
        calendar.timeInMillis = getNow.execute().millis
        return calendar
    }

    private fun createDateRange(date: GregorianCalendar): List<GregorianCalendar> {
        val dateRange: MutableList<GregorianCalendar> = ArrayList(MAX_DATE_COUNT)
        //date.add(Calendar.DAY_OF_MONTH, -1);
        var newDate: GregorianCalendar
        for (i in 0 until MAX_DATE_COUNT) {
            newDate = date.clone() as GregorianCalendar
            dateRange.add(newDate)
            date.add(Calendar.DAY_OF_MONTH, 1)
        }
        return getFilteredDateRange(dateRange)
    }

    private fun getFilteredDateRange(dateRange: List<GregorianCalendar>): List<GregorianCalendar> {
        val minLimit = dateTimeMinLimit()
        if (minLimit != null) {
            val result: MutableList<GregorianCalendar> = ArrayList()
            for (calendar in dateRange) {
                if (!calendar.time.before(minLimit)) {
                    result.add(calendar)
                }
            }

            return result
        } else {
            return dateRange
        }
    }

    private fun getTimeTagFromDateTime(
        date: GregorianCalendar,
        time: GregorianCalendar
    ): TimeTag {
        val timeZone =
            if (isSingleSelection.get()) arrivalTimezone else if (isLeaveAfter.get()) departureTimezone else arrivalTimezone
        val timeType =
            if (isSingleSelection.get()) TimeTag.TIME_TYPE_SINGLE_SELECTION else if (isLeaveAfter.get()) defaultTimeType else TimeTag.TIME_TYPE_ARRIVE_BY
        val newTime = combineDateTime(time, date, timeZone)
        return TimeTag.createForTimeType(
            timeType,
            MILLISECONDS.toSeconds(newTime.timeInMillis)
        )
    }

    private fun moveToLastSelectedTime() {
        val selectedCalendars =
            if (isSingleSelection.get()) singleSelectionCalendars else if (isLeaveAfter.get()
            ) departureCalendars else arrivalCalendars
        //Set last selected time
        val tz = selectedCalendars!![0].timeZone
        this.timeCalendar = GregorianCalendar(selectedCalendars[0].timeZone)
        val dateTime = DateTime(timeMillis, DateTimeZone.forID(tz.id))


        timeCalendar!!.clear()
        timeCalendar!!.time = dateTime.toDate()

        //Set last selected position
        val date = timeCalendar!![Calendar.DATE]
        var temp: GregorianCalendar
        for (i in selectedCalendars.indices) {
            temp = selectedCalendars[i]
            if (temp[Calendar.DATE] == date) {
                //this.selectedPosition.set(i - 1);
                selectedPosition.set(i)
                break
            }
        }
        //Set last selected calendars
        dates.set(formatDateTime(selectedCalendars))
    }

    private fun notifyTimeTypeChange() {
        if (departureTimezone == arrivalTimezone) // if same timezone -> no change
        {
            return
        }
        val fromTimeZone: TimeZone?
        val toTimeZone: TimeZone?
        val dateCalendar: GregorianCalendar
        if (isSingleSelection.get()) {
            fromTimeZone = arrivalTimezone
            toTimeZone = departureTimezone
            dateCalendar = singleSelectionCalendars!![selectedPosition.get()]
        } else {
            if (isLeaveAfter.get()) {
                fromTimeZone = arrivalTimezone
                toTimeZone = departureTimezone
                dateCalendar = arrivalCalendars!![selectedPosition.get()]
            } else {
                fromTimeZone = departureTimezone
                toTimeZone = arrivalTimezone
                dateCalendar = departureCalendars!![selectedPosition.get()]
            }
        }
        val dateTimeCalendar = combineDateTime(
            timeCalendar!!,
            dateCalendar,
            fromTimeZone
        )
        this.timeCalendar = offsetTimeZone(
            dateTimeCalendar.time,
            fromTimeZone,
            toTimeZone
        ) as GregorianCalendar

        refreshDateTime()
    }

    /**
     * Create new calendar by time and date
     */
    private fun combineDateTime(
        time: GregorianCalendar,
        dateCalendar: GregorianCalendar,
        timeZone: TimeZone?
    ): GregorianCalendar {
        val newTime = GregorianCalendar(timeZone)
        newTime[Calendar.YEAR] = dateCalendar[Calendar.YEAR]
        newTime[Calendar.MONTH] = dateCalendar[Calendar.MONTH]
        newTime[Calendar.DATE] = dateCalendar[Calendar.DATE]
        newTime[Calendar.HOUR_OF_DAY] = time[Calendar.HOUR_OF_DAY]
        newTime[Calendar.MINUTE] = time[Calendar.MINUTE]
        return newTime
    }

    private fun refreshDateTime() {
        val selectedCalendars =
            if (isSingleSelection.get()) singleSelectionCalendars else if (isLeaveAfter.get()
            ) departureCalendars else arrivalCalendars
        val startIndex: Int
        val position = selectedPosition.get()
        startIndex = if (position == 0) {
            position
        } else {
            position - 1
        }
        val date = timeCalendar!![Calendar.DATE]
        var tempCalendar: GregorianCalendar
        var i = startIndex
        while (i <= startIndex + extraSelectionCount && i < selectedCalendars!!.size) {
            tempCalendar = selectedCalendars[i]
            if (tempCalendar[Calendar.DATE] == date) {
                selectedPosition.set(i)
                break
            }

            ++i
        }
        dates.set(formatDateTime(selectedCalendars!!))
    }

    private fun formatDateTime(dateRange: List<GregorianCalendar>): List<String> {
        val formatDates: MutableList<String> = ArrayList(dateRange.size)
        extraSelectionCount = 0
        var skip = false
        while (!skip) {
            val label = checkDateForStringLabel(dateRange[extraSelectionCount].time)
            if (label != null) {
                formatDates.add(label)
            } else {
                skip = true
            }

            extraSelectionCount++
        }
        /*
        formatDates.add(context.getString(R.string.yesterday));
        formatDates.add(context.getString(R.string.today));
        formatDates.add(context.getString(R.string.tomorrow));
        */
        var dateString: String
        for (i in extraSelectionCount - 1 until dateRange.size) {
            dateString = DateFormat.format(DATE_FORMAT, dateRange[i]).toString()
            formatDates.add(dateString)
        }
        return formatDates
    }

    override fun getSelectedDate(): GregorianCalendar? {
        val result = if (isSingleSelection.get()) {
            singleSelectionCalendars!![selectedPosition.get()]
        } else if (isLeaveAfter.get()) {
            departureCalendars!![selectedPosition.get()]
        } else {
            arrivalCalendars!![selectedPosition.get()]
        }
        return result
    }

    private fun checkDateForStringLabel(oldTime: Date): String? {
        val newTime = Date()
        try {
            val cal = Calendar.getInstance()
            cal.time = newTime
            val oldCal = Calendar.getInstance()
            oldCal.time = oldTime

            val oldYear = oldCal[Calendar.YEAR]
            val year = cal[Calendar.YEAR]
            val oldDay = oldCal[Calendar.DAY_OF_YEAR]
            val day = cal[Calendar.DAY_OF_YEAR]

            if (oldYear == year) {
                val value = oldDay - day
                if (value == -1) {
                    return context.getString(R.string.yesterday)
                } else if (value == 0) {
                    return context.getString(R.string.today)
                } else if (value == 1) {
                    return context.getString(R.string.tomorrow)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override fun getTimezone(): TimeZone? {
        return if (departureTimezone != null) departureTimezone else arrivalTimezone
    }

    companion object {
        const val ARG_TITLE: String = "title"
        const val ARG_LEAVE_AT_LABEL: String = "leave_at_label"
        const val ARG_ARRIVE_BY_LABEL: String = "arrive_by_label"
        const val ARG_SINGLE_SELECTION_LABEL: String = "single_label"
        const val ARG_POSITIVE_ACTION: String = "positive_action"
        const val ARG_SHOW_POSITIVE_ACTION: String = "show_positive_action"
        const val ARG_NEGATIVE_ACTION: String = "negative_action"
        const val ARG_SHOW_NEGATIVE_ACTION: String = "show_negative_action"
        const val ARG_DEPARTURE_TIMEZONE: String = "departureTimezone"
        const val ARG_ARRIVAL_TIMEZONE: String = "arrivalTimezone"
        const val ARG_TIME_IN_MILLIS: String = "time_in_millis"
        const val ARG_TIME_TYPE: String = "time_type"
        const val ARG_DATE_TIME_PICKER_MIN_LIMIT: String = "dateTimePickerMinLimit"
        const val ARG_TIME_PICKER_MINUTES_INTERVAL: String = "timePickerMinutesInterval"
        private const val DATE_FORMAT = "EEE, MMM dd"
        private const val MAX_DATE_COUNT = 28 // 4 weeks ahead.
    }
}

