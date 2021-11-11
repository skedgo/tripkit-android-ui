package com.skedgo.tripkit.ui.trip.options;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.databinding.Observable;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.databinding.ObservableInt;

import com.skedgo.tripkit.common.model.TimeTag;
import com.skedgo.tripkit.time.GetNow;
import com.skedgo.tripkit.ui.R;
import com.skedgo.tripkit.ui.trip.details.viewmodel.ITimePickerViewModel;
import com.squareup.otto.Bus;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class InterCityTimePickerViewModel implements ITimePickerViewModel {
    public static final String ARG_TITLE = "title";
    public static final String ARG_LEAVE_AT_LABEL = "leave_at_label";
    public static final String ARG_ARRIVE_BY_LABEL = "arrive_by_label";
    public static final String ARG_SINGLE_LABEL = "single_label";
    public static final String ARG_IS_SINGLE_LABEL = "is_single_label";
    public static final String ARG_POSITIVE_ACTION = "positive_action";
    public static final String ARG_SHOW_POSITIVE_ACTION = "show_positive_action";
    public static final String ARG_NEGATIVE_ACTION = "negative_action";
    public static final String ARG_SHOW_NEGATIVE_ACTION = "show_negative_action";
    public static final String ARG_DEPARTURE_TIMEZONE = "departureTimezone";
    public static final String ARG_ARRIVAL_TIMEZONE = "arrivalTimezone";
    public static final String ARG_TIME_IN_MILLIS = "time_in_millis";
    public static final String ARG_TIME_TYPE = "time_type";
    private static final String DATE_FORMAT = "EEE, MMM dd";
    private static final int MAX_DATE_COUNT = 28; // 4 weeks ahead.

    private final int defaultTimeType = TimeTag.TIME_TYPE_LEAVE_AFTER;
    private final GetNow getNow;
    @VisibleForTesting
    TimeZone departureTimezone;
    TimeZone arrivalTimezone;
    long timeMillis;
    private Context context;
    private Bus eventBus;
    private ObservableField<List<String>> dates;
    private GregorianCalendar timeCalendar;
    private List<GregorianCalendar> departureCalendars;
    private List<GregorianCalendar> arrivalCalendars;
    private ObservableInt selectedPosition;
    private ObservableBoolean isLeaveAfter;
    private String defaultTimezone;
    private ObservableField<String> dialogTitle;
    private ObservableField<String> leaveAtLabel;
    private ObservableField<String> arriveByLabel;
    private ObservableField<String> singleLabel;
    private ObservableBoolean isSingleLabel;
    private ObservableInt positiveActionLabel;
    private ObservableBoolean showPositiveAction;
    private ObservableInt negativeActionLabel;
    private ObservableBoolean showNegativeAction;

    public InterCityTimePickerViewModel(
            @NonNull Context context,
            @NonNull Bus bus,
            @NonNull GetNow getNow,
            String defaultTimezone) {
        this.context = context;
        this.eventBus = bus;
        this.getNow = getNow;
        this.dates = new ObservableField<>();
        this.selectedPosition = new ObservableInt(1);
        this.isLeaveAfter = new ObservableBoolean(true);
        this.defaultTimezone = defaultTimezone;
        this.dialogTitle = new ObservableField<>();
        this.leaveAtLabel = new ObservableField(context.getString(R.string.leave_at));
        this.arriveByLabel = new ObservableField(context.getString(R.string.arrive_by));
        this.positiveActionLabel = new ObservableInt(R.string.done);
        this.showPositiveAction = new ObservableBoolean(false);
        this.negativeActionLabel = new ObservableInt(R.string.leave_now);
        this.showNegativeAction = new ObservableBoolean(false);
        this.singleLabel = new ObservableField<>();
        this.isSingleLabel = new ObservableBoolean(false);
    }


    @Override
    public void handleArguments(Bundle args) {
        if (args != null) {
            String extraTimezone;
            if (args.containsKey(ARG_DEPARTURE_TIMEZONE)) {
                extraTimezone = args.getString(ARG_DEPARTURE_TIMEZONE);
                if (!TextUtils.isEmpty(extraTimezone)) {
                    this.departureTimezone = TimeZone.getTimeZone(extraTimezone);
                } else {
                    this.departureTimezone = TimeZone.getTimeZone(defaultTimezone);
                }
            }
            if (args.containsKey(ARG_ARRIVAL_TIMEZONE)) {
                extraTimezone = args.getString(ARG_ARRIVAL_TIMEZONE);
                if (!TextUtils.isEmpty(extraTimezone)) {
                    this.arrivalTimezone = TimeZone.getTimeZone(extraTimezone);
                } else {
                    this.arrivalTimezone = TimeZone.getTimeZone(defaultTimezone);
                }
            }
            if (args.containsKey(ARG_TIME_IN_MILLIS)) {
                this.timeMillis = args.getLong(ARG_TIME_IN_MILLIS);
            }
            if (args.containsKey(ARG_TIME_TYPE)) {
                int timeType = args.getInt(ARG_TIME_TYPE);
                this.isLeaveAfter.set(timeType == defaultTimeType);
            }
            if (args.containsKey(ARG_TITLE)) {
                this.dialogTitle.set(args.getString(ARG_TITLE, ""));
            }
            if (args.containsKey(ARG_SHOW_POSITIVE_ACTION)) {
                boolean show = args.getBoolean(ARG_SHOW_POSITIVE_ACTION);
                if (show && args.containsKey(ARG_POSITIVE_ACTION)) {
                    int label = args.getInt(ARG_POSITIVE_ACTION, 0);
                    this.positiveActionLabel.set(label);
                    this.showPositiveAction.set(label != 0);
                }
            }
            if (args.containsKey(ARG_SHOW_NEGATIVE_ACTION)) {
                boolean show = args.getBoolean(ARG_SHOW_NEGATIVE_ACTION);
                if (show && args.containsKey(ARG_NEGATIVE_ACTION)) {
                    int label = args.getInt(ARG_NEGATIVE_ACTION, 0);
                    this.negativeActionLabel.set(label);
                    this.showNegativeAction.set(label != 0);
                }
            }
            if (args.containsKey(ARG_LEAVE_AT_LABEL)) {
                this.leaveAtLabel.set(args.getString(ARG_LEAVE_AT_LABEL, ""));
            }
            if (args.containsKey(ARG_ARRIVE_BY_LABEL)) {
                this.arriveByLabel.set(args.getString(ARG_ARRIVE_BY_LABEL, ""));
            }
            if (args.containsKey(ARG_IS_SINGLE_LABEL)) {
                boolean isSingleLabel = args.getBoolean(ARG_IS_SINGLE_LABEL);
                if (isSingleLabel && args.containsKey(ARG_SINGLE_LABEL)) {
                    this.isSingleLabel.set(isSingleLabel);
                    this.singleLabel.set(args.getString(ARG_SINGLE_LABEL, ""));
                }
            }
            initValues();
        }
    }

    @Override
    public ObservableField<String> dialogTitle() {
        return dialogTitle;
    }

    @Override
    public ObservableField<String> leaveAtLabel() {
        return leaveAtLabel;
    }

    @Override
    public ObservableField<String> arriveByLabel() {
        return arriveByLabel;
    }

    @Override
    public ObservableField<String> singleLabel() {
        return singleLabel;
    }

    @Override
    public ObservableBoolean isSingleLabel() {
        return isSingleLabel;
    }

    @Override
    public ObservableInt positiveActionLabel() {
        return positiveActionLabel;
    }

    @Override
    public ObservableBoolean showPositiveAction() {
        return showPositiveAction;
    }

    @Override
    public ObservableInt negativeActionLabel() {
        return negativeActionLabel;
    }

    @Override
    public ObservableBoolean showNegativeAction() {
        return showNegativeAction;
    }

    @Override
    public ObservableField<List<String>> dates() {
        return dates;
    }

    @Override
    public int getHour() {
        return timeCalendar.get(Calendar.HOUR_OF_DAY);
    }

    @Override
    public int getMinute() {
        return timeCalendar.get(Calendar.MINUTE);
    }

    @Override
    public ObservableInt selectedPosition() {
        return selectedPosition;
    }

    @Override
    public ObservableBoolean isLeaveAfter() {
        return isLeaveAfter;
    }

    @Override
    public void updateTime(int hour, int minute) {
        timeCalendar.set(Calendar.HOUR_OF_DAY, hour);
        timeCalendar.set(Calendar.MINUTE, minute);
    }

    @Override
    public TimeTag leaveNow() {
        return TimeTag.createForLeaveNow();
    }

    @Override
    public TimeTag done() {
        GregorianCalendar dateCalendar = this.isLeaveAfter.get() ?
                departureCalendars.get(selectedPosition.get()) : arrivalCalendars.get(selectedPosition.get());
        return getTimeTagFromDateTime(dateCalendar, timeCalendar);
    }

    /**
     * Visible only for testing.
     */
    Calendar offsetTimeZone(Date date, TimeZone fromTimeZone, TimeZone toTimeZone) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(fromTimeZone);
        calendar.setTime(date);
        // FROM TimeZone to UTC
        calendar.add(Calendar.MILLISECOND, fromTimeZone.getRawOffset() * -1);
        if (fromTimeZone.inDaylightTime(calendar.getTime())) {
            calendar.add(Calendar.MILLISECOND, calendar.getTimeZone().getDSTSavings() * -1);
        }
        // UTC to TO TimeZone
        calendar.add(Calendar.MILLISECOND, toTimeZone.getRawOffset());
        if (toTimeZone.inDaylightTime(calendar.getTime())) {
            calendar.add(Calendar.MILLISECOND, toTimeZone.getDSTSavings());
        }
        return calendar;
    }

    private void initValues() {
        GregorianCalendar departureTime = createTime(departureTimezone);
        GregorianCalendar arrivalTime = createTime(arrivalTimezone);
        this.departureCalendars = createDateRange((GregorianCalendar) departureTime.clone());
        this.arrivalCalendars = createDateRange((GregorianCalendar) arrivalTime.clone());
        this.isLeaveAfter.addOnPropertyChangedCallback(onTimeTypePropertyChanged());
        moveToLastSelectedTime();
    }

    @NonNull
    private Observable.OnPropertyChangedCallback onTimeTypePropertyChanged() {
        return new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                notifyTimeTypeChange();
            }
        };
    }

    private GregorianCalendar createTime(TimeZone timezone) {
        GregorianCalendar calendar = new GregorianCalendar(timezone);
        calendar.setTimeInMillis(getNow.execute().getMillis());
        return calendar;
    }

    private List<GregorianCalendar> createDateRange(@NonNull GregorianCalendar date) {
        List<GregorianCalendar> dateRange = new ArrayList<>(MAX_DATE_COUNT);
        date.add(Calendar.DAY_OF_MONTH, -1);
        GregorianCalendar newDate;
        for (int i = 0; i < MAX_DATE_COUNT; ++i) {
            newDate = (GregorianCalendar) date.clone();
            dateRange.add(newDate);
            date.add(Calendar.DAY_OF_MONTH, 1);
        }
        return dateRange;
    }

    private TimeTag getTimeTagFromDateTime(@NonNull GregorianCalendar date,
                                           @NonNull GregorianCalendar time) {
        TimeZone timeZone = this.isLeaveAfter.get() ? departureTimezone : arrivalTimezone;
        int timeType = this.isLeaveAfter.get() ? defaultTimeType : TimeTag.TIME_TYPE_ARRIVE_BY;
        GregorianCalendar newTime = combineDateTime(time, date, timeZone);
        return TimeTag.createForTimeType(
                timeType,
                TimeUnit.MILLISECONDS.toSeconds(newTime.getTimeInMillis())
        );
    }

    private void moveToLastSelectedTime() {
        List<GregorianCalendar> selectedCalendars = this.isLeaveAfter.get()
                ? departureCalendars : arrivalCalendars;
        //Set last selected time
        this.timeCalendar = new GregorianCalendar(selectedCalendars.get(0).getTimeZone());
        this.timeCalendar.setTimeInMillis(this.timeMillis);

        //Set last selected position
        int date = this.timeCalendar.get(Calendar.DATE);
        GregorianCalendar temp;
        for (int i = 0; i < MAX_DATE_COUNT; ++i) {
            temp = selectedCalendars.get(i);
            if (temp.get(Calendar.DATE) == date) {
                this.selectedPosition.set(i);
                break;
            }

        }
        //Set last selected calendars
        this.dates.set(formatDateTime(selectedCalendars));
    }

    private void notifyTimeTypeChange() {
        if (departureTimezone.equals(arrivalTimezone)) // if same timezone -> no change
        {
            return;
        }
        TimeZone fromTimeZone;
        TimeZone toTimeZone;
        GregorianCalendar dateCalendar;
        if (this.isLeaveAfter.get()) {
            fromTimeZone = arrivalTimezone;
            toTimeZone = departureTimezone;
            dateCalendar = arrivalCalendars.get(selectedPosition.get());
        } else {
            fromTimeZone = departureTimezone;
            toTimeZone = arrivalTimezone;
            dateCalendar = departureCalendars.get(selectedPosition.get());
        }
        GregorianCalendar dateTimeCalendar = combineDateTime(
                timeCalendar,
                dateCalendar,
                fromTimeZone
        );
        this.timeCalendar = (GregorianCalendar) offsetTimeZone(
                dateTimeCalendar.getTime(),
                fromTimeZone,
                toTimeZone
        );

        refreshDateTime();
    }

    /**
     * Create new calendar by time and date
     */
    private GregorianCalendar combineDateTime(@NonNull GregorianCalendar time,
                                              @NonNull GregorianCalendar dateCalendar,
                                              TimeZone timeZone) {
        GregorianCalendar newTime = new GregorianCalendar(timeZone);
        newTime.set(Calendar.YEAR, dateCalendar.get(Calendar.YEAR));
        newTime.set(Calendar.MONTH, dateCalendar.get(Calendar.MONTH));
        newTime.set(Calendar.DATE, dateCalendar.get(Calendar.DATE));
        newTime.set(Calendar.HOUR_OF_DAY, time.get(Calendar.HOUR_OF_DAY));
        newTime.set(Calendar.MINUTE, time.get(Calendar.MINUTE));
        return newTime;
    }

    private void refreshDateTime() {
        List<GregorianCalendar> selectedCalendars = this.isLeaveAfter.get()
                ? departureCalendars : arrivalCalendars;
        int startIndex;
        int position = selectedPosition.get();
        if (position == 0) {
            startIndex = position;
        } else {
            startIndex = position - 1;
        }
        int date = timeCalendar.get(Calendar.DATE);
        GregorianCalendar tempCalendar;
        for (int i = startIndex; i <= startIndex + 2 && i < MAX_DATE_COUNT; ++i) {
            tempCalendar = selectedCalendars.get(i);
            if (tempCalendar.get(Calendar.DATE) == date) {
                selectedPosition.set(i);
                break;
            }

        }
        this.dates.set(formatDateTime(selectedCalendars));
    }

    private List<String> formatDateTime(@NonNull List<GregorianCalendar> dateRange) {
        List<String> formatDates = new ArrayList<>(MAX_DATE_COUNT);
        formatDates.add(context.getString(R.string.yesterday));
        formatDates.add(context.getString(R.string.today));
        formatDates.add(context.getString(R.string.tomorrow));
        String dateString;
        for (int i = 3; i < MAX_DATE_COUNT; ++i) {
            dateString = DateFormat.format(DATE_FORMAT, dateRange.get(i)).toString();
            formatDates.add(dateString);
        }
        return formatDates;
    }
}
