package com.skedgo.tripkit.ui.dialog.v2.datetimepicker

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.viewModels
import com.skedgo.tripkit.common.model.TimeTag
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.BaseDialog
import com.skedgo.tripkit.ui.databinding.FragmentTkuiDateTimePickerDialogBinding
import com.skedgo.tripkit.ui.dialog.v2.datetimepicker.DateTimePickerState.OnConfirmDateTime
import com.skedgo.tripkit.ui.dialog.v2.datetimepicker.DateTimePickerState.OnConfirmTime
import com.skedgo.tripkit.ui.dialog.v2.datetimepicker.DateTimePickerState.OnError
import com.skedgo.tripkit.ui.dialog.v2.datetimepicker.DateTimePickerState.OnShowTimePicker
import com.skedgo.tripkit.ui.utils.observe
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit.MILLISECONDS

class TKUIDateTimePickerDialogFragment : BaseDialog<FragmentTkuiDateTimePickerDialogBinding>() {

    companion object {
        fun newInstance(config: DateTimePickerConfig) = TKUIDateTimePickerDialogFragment().apply {
            this.config = config
        }
    }

    override val layoutRes: Int
        get() = R.layout.fragment_tkui_date_time_picker_dialog
    override val isFullScreen: Boolean
        get() = true
    override val isFullWidth: Boolean
        get() = true
    override val isTransparentBackground: Boolean
        get() = true

    private val viewModel: TKUIDateTimePickerDialogViewModel by viewModels()
    private var config: DateTimePickerConfig? = null

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().timePickerComponent().inject(this)
        super.onAttach(context)
    }

    override fun onCreated(savedInstanceState: Bundle?) {
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        config?.let { viewModel.setup(it) }
        initViews()
        initObservers()
    }

    private fun initViews() {
        binding.tvTime.setOnClickListener {
            viewModel.setState(OnShowTimePicker)
        }
        binding.tvDone.setOnClickListener {
            viewModel.setState(OnConfirmTime)
        }
        binding.layoutBack.setOnClickListener { dialog?.dismiss() }
        binding.bConfirm.setOnClickListener {
            viewModel.setState(OnConfirmDateTime)
        }
    }

    private fun initObservers() {
        viewModel.apply {
            observe(config) {
                it?.let { handleDateTimeConfig(it) }
            }
            observe(state) {
                it?.let { handleState(it)}
            }
        }

        binding.timePicker.setOnTimeChangedListener { timePicker, selectedHour, selectedMinute ->
            // Create a Calendar instance to handle AM/PM conversion
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
            calendar.set(Calendar.MINUTE, selectedMinute)

            // Format the time as hh:mm a (12-hour format with AM/PM)
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val formattedTime = sdf.format(calendar.time)

            viewModel.setTime(formattedTime)
        }

        binding.dpDate.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance(
                viewModel.config.value?.timeZone ?: TimeZone.getDefault()
            )
            calendar.set(year, month, dayOfMonth)
            viewModel.setSelectedDate(calendar.timeInMillis)
        }
    }

    private fun handleDateTimeConfig(config: DateTimePickerConfig) {
        val minDateCalendar = Calendar.getInstance(config.timeZone)

        minDateCalendar.timeInMillis = config.minDateTime
        minDateCalendar.set(Calendar.HOUR_OF_DAY, 0)
        minDateCalendar.set(Calendar.MINUTE, 0)
        binding.dpDate.minDate = minDateCalendar.timeInMillis

        val showDateCalendar = Calendar.getInstance(config.timeZone)
        // Set the time in millis according to the provided time zone
        showDateCalendar.timeInMillis = config.defaultDateTime

        setupTimePicker(
            showDateCalendar.get(Calendar.HOUR_OF_DAY),
            showDateCalendar.get(Calendar.MINUTE)
        )

        // Set the DatePicker to show the specific date
        showDateCalendar.set(Calendar.HOUR_OF_DAY, 0)
        showDateCalendar.set(Calendar.MINUTE, 0)
        binding.dpDate.date = showDateCalendar.timeInMillis
    }

    private fun setupTimePicker(hour: Int, minute: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding.timePicker.hour = hour
            binding.timePicker.minute = minute
        } else {
            binding.timePicker.currentHour = hour
            binding.timePicker.currentMinute = minute
        }
    }

    private fun handleState(state: DateTimePickerState) {
        when(state) {
            is OnConfirmDateTime -> {
                confirmSelectedDate()
            }
            else -> {
                // Do nothing
            }
        }
    }

    private fun confirmSelectedDate() {
        val dateTime = viewModel.combineDateTime(
            viewModel.selectedDate.value ?: binding.dpDate.date,
            getTimeHour(),
            getMinute()
        )

        if ( viewModel.isOneWayOnly.value != true &&
            dateTime.timeInMillis < (viewModel.config.value?.minDateTime ?: 0)) {
            viewModel.setState(
                OnError(Throwable("Please select a valid date and time"))
            )
            return
        }

        val result = if (viewModel.isOneWayOnly.value == true) {
            TimeTag.createForLeaveNow()
        } else {
            TimeTag.createForTimeType(
                TimeTag.TIME_TYPE_LEAVE_AFTER,
                MILLISECONDS.toSeconds(dateTime.timeInMillis)
            )
        }

        viewModel.config.value?.onDateTimeSelected?.invoke(result)
        dialog?.dismiss()
    }

    private fun getTimeHour() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        binding.timePicker.hour
    } else {
        binding.timePicker.currentHour
    }

    private fun getMinute() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        binding.timePicker.minute
    } else {
        binding.timePicker.currentMinute
    }
}