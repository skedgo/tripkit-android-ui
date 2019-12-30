package com.skedgo.tripkit.ui.trip.details.viewmodel;

import android.os.Bundle;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.databinding.ObservableInt;
import com.skedgo.tripkit.common.model.TimeTag;

import java.util.List;

public interface ITimePickerViewModel {
  ObservableField<List<String>> dates();
  ObservableBoolean isLeaveAfter();
  ObservableInt selectedPosition();
  int getHour();
  int getMinute();

  void handleArguments(Bundle args);
  void updateTime(int hour, int minute);
  TimeTag leaveNow();
  TimeTag done();
}