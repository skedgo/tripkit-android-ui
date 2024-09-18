package com.skedgo.tripkit.ui.trip.details.viewmodel;

import android.os.Bundle;

import com.skedgo.tripkit.common.model.TimeTag;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import androidx.annotation.Nullable;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.databinding.ObservableInt;

public interface ITimePickerViewModel {

    ObservableField<String> dialogTitle();

    ObservableField<String> leaveAtLabel();

    ObservableField<String> arriveByLabel();

    ObservableField<String> singleSelectionLabel();

    ObservableBoolean isSingleSelection();

    ObservableInt positiveActionLabel();

    ObservableBoolean showPositiveAction();

    ObservableInt negativeActionLabel();

    ObservableBoolean showNegativeAction();

    ObservableField<List<String>> dates();

    ObservableBoolean isLeaveAfter();

    ObservableInt selectedPosition();

    int getHour();

    int getMinute();

    ObservableInt getTimePickerMinuteInterval();

    @Nullable
    GregorianCalendar dateTimeMinLimit();

    void handleArguments(Bundle args);

    void updateTime(int hour, int minute);

    TimeTag leaveNow();

    TimeTag done();

    @Nullable
    GregorianCalendar getSelectedDate();

    @Nullable
    TimeZone getTimezone();
}