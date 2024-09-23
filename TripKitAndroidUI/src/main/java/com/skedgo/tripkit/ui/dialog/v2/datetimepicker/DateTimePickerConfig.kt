package com.skedgo.tripkit.ui.dialog.v2.datetimepicker

import com.skedgo.tripkit.common.model.TimeTag
import java.util.TimeZone

data class DateTimePickerConfig(
    val minDateTime: Long,
    val timeZone: TimeZone,
    val defaultDateTime: Long,
    val datePickerLabel: String,
    val onDateTimeSelected: (TimeTag) -> Unit
)
