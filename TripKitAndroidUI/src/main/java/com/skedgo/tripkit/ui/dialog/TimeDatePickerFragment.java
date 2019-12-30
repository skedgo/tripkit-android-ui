package com.skedgo.tripkit.ui.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.Time;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.jakewharton.rxrelay2.BehaviorRelay;
import com.skedgo.tripkit.ui.R;
import kankan.wheel.widget.WheelView;
import kankan.wheel.widget.adapters.ArrayWheelAdapter;
import kankan.wheel.widget.adapters.DaysAdapter;
import kankan.wheel.widget.adapters.NumericWheelAdapter;

import java.util.Calendar;

public class TimeDatePickerFragment extends DialogFragment implements View.OnClickListener {
  private static final String ARG_TIME_TYPE = "timeType";
  private static final String ARG_INITIATOR_ID = "initiatorId";
  private static final String ARG_INITIAL_TIME = "initialTimeInMillis";
  private static final String ARG_TITLE = "title";
  private Calendar mCalendar;
  private String mTitle;
  private String mInitiatorId;
  /**
   * Should show date and time or not.
   */
  private boolean mShouldShowTime = true;
  private NumericWheelAdapter mHoursAdapter;
  private NumericWheelAdapter mMinutesAdapter;
  private ArrayWheelAdapter<String> mAmPmAdapter;
  private DaysAdapter mDaysAdapter;
  private WheelView mHoursView;
  private WheelView mMinutesView;
  private WheelView mAmPmView;
  private WheelView mDaysView;
  private int mTimeType;
  public BehaviorRelay<Long> timeRelay = BehaviorRelay.create();

  public static TimeDatePickerFragment newInstance(int timeType, String initiatorId, @Nullable String title, long initialTimeInMillis) {
    Bundle args = new Bundle();
    args.putString(ARG_INITIATOR_ID, initiatorId);
    args.putInt(ARG_TIME_TYPE, timeType);
    args.putLong(ARG_INITIAL_TIME, initialTimeInMillis);
    if (!TextUtils.isEmpty(title)) {
      args.putString(ARG_TITLE, title);
    }

    TimeDatePickerFragment fragment = new TimeDatePickerFragment();
    fragment.setArguments(args);
    return fragment;
  }

  public static TimeDatePickerFragment newInstance(int timeType) {
    return newInstance(timeType, null, null, System.currentTimeMillis());
  }

  public static TimeDatePickerFragment newInstance(String title) {
    return newInstance(TimeDatePickedEvent.TIME_TYPE_OTHER, null, title, System.currentTimeMillis());
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mTimeType = getArguments().getInt(ARG_TIME_TYPE);
    if (mTimeType == TimeDatePickedEvent.TIME_TYPE_BEGIN) {
      mTitle = getString(R.string.set_start_time);
    } else if (mTimeType == TimeDatePickedEvent.TIME_TYPE_END) {
      mTitle = getString(R.string.set_end_time);
    } else {
      mTitle = getArguments().getString(ARG_TITLE);
      if (mTitle == null) {
        mTitle = getString(R.string.set_time);
      }
    }


    long time = getArguments().getLong(ARG_INITIAL_TIME, System.currentTimeMillis());
    timeRelay.accept(time);
    mInitiatorId = getArguments().getString(ARG_INITIATOR_ID);
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    FlatAlertDialogBuilder dialogBuilder = new FlatAlertDialogBuilder(getActivity());
    Dialog dialog = dialogBuilder
        .setTitle(mTitle)
        .setContentView(R.layout.dialog_time_date_picker)
        .setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int buttonType) {
            TimeDatePickerFragment.this.onClick(null);
          }
        })
        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int buttonType) {
            dismiss();
          }
        })
        .create();

    View view = dialogBuilder.getContentView();

    //set hour view
    mHoursView = (WheelView) view.findViewById(R.id.hoursView);
    mHoursAdapter = new NumericWheelAdapter(this.getActivity(), 1, 12, "%02d");
    mHoursAdapter.setItemResource(R.layout.v4_view_wheel_time);
    mHoursAdapter.setItemTextResource(R.id.text);
    mHoursView.setViewAdapter(mHoursAdapter);
    mHoursView.setCyclic(true);

    //set min view
    mMinutesView = (WheelView) view.findViewById(R.id.minutesView);
    mMinutesAdapter = new NumericWheelAdapter(this.getActivity(), 0, 59, "%02d");
    mMinutesAdapter.setItemResource(R.layout.v4_view_wheel_time);
    mMinutesAdapter.setItemTextResource(R.id.text);
    mMinutesView.setViewAdapter(mMinutesAdapter);
    mMinutesView.setCyclic(true);

    //set am, pm view
    mAmPmView = (WheelView) view.findViewById(R.id.amPmView);
    mAmPmAdapter = new ArrayWheelAdapter<String>(this.getActivity(), new String[] {"AM", "PM"});
    mAmPmAdapter.setItemResource(R.layout.v4_view_wheel_time);
    mAmPmAdapter.setItemTextResource(R.id.text);
    mAmPmView.setViewAdapter(mAmPmAdapter);

    if (!mShouldShowTime) {
      mHoursView.setVisibility(View.GONE);
      mMinutesView.setVisibility(View.GONE);
      mAmPmView.setVisibility(View.GONE);
    }

    long initialTimeInMillis = getArguments().getLong("initialTimeInMillis");
    mCalendar = Calendar.getInstance();
    mCalendar.setTimeInMillis(initialTimeInMillis);

    // set time
    //index count from 0, but hour count is face-value
    mHoursView.setCurrentItem(mCalendar.get(Calendar.HOUR) - 1);
    mMinutesView.setCurrentItem(mCalendar.get(Calendar.MINUTE));
    mAmPmView.setCurrentItem(mCalendar.get(Calendar.AM_PM));

    mDaysView = (WheelView) view.findViewById(R.id.daysView);
    int dayRange = 60;
    mDaysAdapter = new DaysAdapter(this.getActivity(), mCalendar, dayRange);
    mDaysView.setViewAdapter(mDaysAdapter);
    mDaysView.setCurrentItem((dayRange + 1) / 2);

    return dialog;
  }

  @Override
  public void onClick(View view) {
    int hour = 0;
    int mins = 0;
    String amPm = "AM";

    if (mShouldShowTime) {
      //what index does it return if it is cyclic => answer: correct index
      int hourIndex = mHoursView.getCurrentItem();
      int minuteIndex = mMinutesView.getCurrentItem();
      int apmIndex = mAmPmView.getCurrentItem();

      hour = mHoursAdapter.getItemValue(hourIndex);
      mins = mMinutesAdapter.getItemValue(minuteIndex);
      amPm = (String) mAmPmAdapter.getItemText(apmIndex);
    }

    int dayIndex = mDaysView.getCurrentItem();
    long dayRepresentedByMillis = mDaysAdapter.getItemValue(dayIndex);

    //note: the day/time wheel does not imply timezone. Tz must be explicitly set.
    Time time = new Time();
    time.set(dayRepresentedByMillis);
    time.normalize(false);

    //12=>0, 1=>1, .etc
    int hourCorrectedBy0 = hour % 12;
    if (amPm.equalsIgnoreCase("AM")) {
      time.hour = hourCorrectedBy0;
    } else {
      time.hour = hourCorrectedBy0 + 12;
    }

    time.minute = mins;

    //normalize after setting the hour and min, if Tz is set, remember to normalize
    time.normalize(false);
    timeRelay.accept(time.toMillis(false));
    dismiss();
  }
}