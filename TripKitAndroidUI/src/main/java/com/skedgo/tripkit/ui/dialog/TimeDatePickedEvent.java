package com.skedgo.tripkit.ui.dialog;

import android.text.format.Time;

import org.jetbrains.annotations.NotNull;

public class TimeDatePickedEvent {
    /* To avoid cross dependency between EventsDetailsFragment and TimeDatePickerFragment,
    those constants are tucked away here
     */
    public static final String DATE_FORMAT_STRING = "EE, MMM d yyyy 'at' hh:mm a";
    public static final String DATE_FORMAT_STRING_NO_TIME = "EE, MMM d yyyy";
    public static final int TIME_TYPE_BEGIN = 1;
    public static final int TIME_TYPE_END = 2;
    public static final int TIME_TYPE_OTHER = 200;
    public final String initiatorId;

    public int timeType;
    public Time time;

    public TimeDatePickedEvent(int timeType, String initiatorId, @NotNull Time time) {
        this.timeType = timeType;
        this.initiatorId = initiatorId;
        this.time = time;
    }
}