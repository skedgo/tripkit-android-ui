package com.skedgo.tripkit.ui.booking;

import android.view.View;
import androidx.annotation.NonNull;
import com.squareup.otto.Bus;
import com.skedgo.tripkit.routing.TripSegment;

public class BookViewClickEvent implements View.OnClickListener {
  public final TripSegment segment;
  private final Bus bus;

  public BookViewClickEvent(
      @NonNull Bus bus,
      @NonNull TripSegment segment) {
    this.bus = bus;
    this.segment = segment;
  }

  @Override
  public void onClick(View v) {
    bus.post(this);
  }
}