package com.skedgo.tripkit.ui.tripresult;

import android.util.Log;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import com.skedgo.tripkit.ui.model.TripKitButton;
import com.skedgo.tripkit.routing.TripGroup;

import java.util.List;

public class TripGroupsPagerAdapter extends FragmentStatePagerAdapter {
  private List<TripGroup> tripGroups;
  private List<TripKitButton> buttons;
  private boolean showCloseButton;

  public View.OnClickListener closeListener = null;
  public TripSegmentListFragment.OnTripKitButtonClickListener listener = null;

  public TripGroupsPagerAdapter(FragmentManager fragmentManager) {
    super(fragmentManager);
  }
  public void setButtons(List<TripKitButton> buttons) {
    this.buttons = buttons;
  }
  public List<TripGroup> getTripGroups() {
    return tripGroups;
  }

  public void setTripGroups(List<TripGroup> tripGroups) {
    this.tripGroups = tripGroups;
    notifyDataSetChanged();
  }

  public void setShowCloseButton(boolean showCloseButton) {
    this.showCloseButton = showCloseButton;
  }
  @Override
  public Fragment getItem(int position) {
    TripGroup tripGroup = tripGroups.get(position);
    TripSegmentListFragment fragment = new TripSegmentListFragment.Builder()
                                              .withTripGroupId(tripGroup.uuid())
                                              .withButtons(buttons)
                                              .showCloseButton(showCloseButton)
                                              .build();
    fragment.setOnTripKitButtonClickListener(listener);
    fragment.setOnCloseButtonListener(closeListener);
    return fragment;
  }

  @Override
  public int getCount() {
    return (tripGroups == null)
        ? 0
        : tripGroups.size();
  }

}