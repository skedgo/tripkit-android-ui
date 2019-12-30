package com.skedgo.tripkit.ui.dialog;

import android.content.Context;
import android.widget.ArrayAdapter;

import java.util.List;

public class DateSpinnerAdapter extends ArrayAdapter<String> {
  private List<String> dates;

  public DateSpinnerAdapter(Context context, int resource, List<String> dates) {
    super(context, resource, dates);
    this.dates = dates;
  }

  public void setDates(List<String> dates) {
    this.dates.clear();
    this.dates.addAll(dates);
    this.notifyDataSetChanged();
  }

  @Override
  public String getItem(int position) {
    return dates.get(position);
  }
}
