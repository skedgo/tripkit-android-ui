package com.skedgo.tripkit.ui.dialog.v2.datetimepicker

sealed interface DateTimePickerState {
    data class OnError(
        val error: Throwable
    ) : DateTimePickerState
    object OnShowTimePicker : DateTimePickerState
    object OnConfirmTime : DateTimePickerState
    object OnConfirmDateTime: DateTimePickerState
}