package com.skedgo.tripkit.ui.tripresult;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import biz.laenger.android.vpbs.BottomSheetUtils;
import com.skedgo.tripkit.model.ViewTrip;
import com.skedgo.tripkit.ui.TripKitUI;
import com.skedgo.tripkit.ui.booking.BookViewClickEventHandler;
import com.skedgo.tripkit.ui.core.rxlifecyclecomponents.RxFragment;
import com.skedgo.tripkit.ui.databinding.TripResultPagerBinding;
import com.skedgo.tripkit.ui.model.TripKitButton;
import com.squareup.otto.Bus;
import org.jetbrains.annotations.NotNull;
import com.skedgo.tripkit.logging.ErrorLogger;
import com.skedgo.tripkit.routing.TripGroup;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class TripResultPagerFragment extends RxFragment implements ViewPager.OnPageChangeListener, TripSegmentListFragment.OnTripKitButtonClickListener {

  public interface OnTripKitButtonClickListener {
    void onTripKitButtonClicked(String id, TripGroup tripGroup);
  }

  OnTripKitButtonClickListener tripButtonClickListener = null;
  public void setOnTripKitButtonClickListener(OnTripKitButtonClickListener listener) {
    this.tripButtonClickListener = listener;
  }

  private List<TripKitButton> buttons;

  private static final String KEY_CURRENT_PAGE = "currentPage";
  private final BookViewClickEventHandler bookViewClickEventHandler = BookViewClickEventHandler.create(this);

  /* TODO: Replace with RxJava-based approach. */
  @Inject @Deprecated Bus bus;
  @Inject TripResultPagerViewModel viewModel;
  @Inject
  ErrorLogger errorLogger;

  private TripGroupsPagerAdapter tripGroupsPagerAdapter;
  private TripResultPagerBinding binding;
  @Nullable  private TripResultMapFragment mapFragment;

  @Nullable private PagerFragmentArguments args;
  private int currentPage = -1;

  @Nullable @Override
  public View onCreateView(
      LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    final TripResultPagerBinding binding = TripResultPagerBinding.inflate(inflater);
    this.binding = binding;

    BottomSheetUtils.setupViewPager(binding.tripGroupsPager);
    return binding.getRoot();
  }


  public void setMapFragment(TripResultMapFragment fragment) {
    this.mapFragment = fragment;
  }

  @Override
  public void onResume() {
    super.onResume();
    bus.register(this);
    bus.register(bookViewClickEventHandler);
  }

  @Override public void onStart() {
    super.onStart();
    viewModel.onStart();
    this.binding.tripGroupsPager.addOnPageChangeListener(this);
  }

  @Override public void onStop() {
    super.onStop();
    viewModel.onStop();
    this.binding.tripGroupsPager.removeOnPageChangeListener(this);
  }

  @Override
  public void onPause() {
    super.onPause();
    bus.unregister(this);
    bus.unregister(bookViewClickEventHandler);
  }

  @Override public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt(KEY_CURRENT_PAGE, binding.tripGroupsPager.getCurrentItem());
    viewModel.onSavedInstanceState(outState);
  }

  public Fragment getCurrentFragment() {
    return (Fragment) tripGroupsPagerAdapter.instantiateItem(
        binding.tripGroupsPager,
        currentPage == -1 ? binding.tripGroupsPager.getCurrentItem() : currentPage
    );
  }

  @Override
  public void onAttach(Context context) {
    TripKitUI.getInstance().tripDetailsComponent().inject(this);
    super.onAttach(context);
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    binding.setViewModel(viewModel);
//    viewModel.getTripSource().accept(TripSourceIntentMapperKt.mapIntentToTripSource(getActivity().getIntent()));

    if (savedInstanceState != null) {
      currentPage = savedInstanceState.getInt(KEY_CURRENT_PAGE);
    }

    viewModel.onCreate(savedInstanceState);

    if (savedInstanceState == null) {
      if (args instanceof HasInitialTripGroupId) {
        viewModel.setInitialSelectedTripGroupId(((HasInitialTripGroupId) args).tripGroupId());
      }
    }

    tripGroupsPagerAdapter = new TripGroupsPagerAdapter(getChildFragmentManager());
    tripGroupsPagerAdapter.listener = this;
    tripGroupsPagerAdapter.setButtons(buttons);

    binding.tripGroupsPager.setAdapter(tripGroupsPagerAdapter);

//    viewModel.reportPlannedTrip()
//        .compose(bindToLifecycle())
//        .subscribe();
//
    viewModel.trackViewingTrip()
        .compose(bindToLifecycle())
        .subscribe();

    viewModel.observeTripGroups()
        .compose(bindToLifecycle())
        .subscribe();

    viewModel.observeInitialPage()
        .compose(bindToLifecycle())
        .subscribe();

    viewModel.updateSelectedTripGroup()
        .compose(bindToLifecycle())
        .subscribe();

    viewModel.loadFetchingRealtimeStatus()
        .compose(bindToLifecycle())
        .subscribe();

    assert args != null;
    viewModel.getSortedTripGroups(args)
        .compose(bindToLifecycle())
        .subscribe(tripGroup -> {}, errorLogger::trackError);
  }

  public void setArgs(@NonNull PagerFragmentArguments args) {
    this.args = args;
  }

  @Override
  public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

  }

  @Override
  public void onPageSelected(int position) {
    if (this.mapFragment != null) {
      TripGroup group = this.tripGroupsPagerAdapter.getTripGroups().get(position);
      mapFragment.setTripGroupId(group.uuid());
    }
  }

  @Override
  public void onPageScrollStateChanged(int state) {

  }

  public void setButtons(List<TripKitButton> buttons) {
    this.buttons = buttons;
    if (tripGroupsPagerAdapter != null) {
      tripGroupsPagerAdapter.setButtons(buttons);
    }
  }

  @Override
  public void tripKitButtonClicked(@NotNull String id, @NotNull TripGroup tripGroup) {
    if (tripButtonClickListener != null) {
      tripButtonClickListener.onTripKitButtonClicked(id, tripGroup);
    }
  }

  public static class Builder {
    private TripResultMapFragment mapFragment = null;
    private String tripGroupId = "";
    private Integer sortOrder = 1;
    private String requestId = "";
    private Long arriveBy = 0L;
    private List<TripKitButton> buttons = new ArrayList<TripKitButton>();

    public Builder withTripButton(String id, int layoutResourceId) {
      TripKitButton b = new TripKitButton(id, layoutResourceId);
      buttons.add(b);
      return this;
    }
    public Builder withMapFragment(TripResultMapFragment mapFragment) {
      this.mapFragment = mapFragment;
      return this;
    }

    public Builder withViewTrip(ViewTrip trip) {
      this.tripGroupId = trip.tripGroupUUID();
      this.sortOrder = trip.getSortOrder();
      this.arriveBy = trip.query().getArriveBy();
      this.requestId = trip.query().uuid();
      return this;
    }

    public Builder withTripGroupId(String tripGroupId) {
      this.tripGroupId = tripGroupId;
      return this;
    }

    public Builder withSortOrder(Integer sortOrder) {
      this.sortOrder = sortOrder;
      return this;
    }

    public Builder withRequestId(String requestId) {
      this.requestId = requestId;
      return this;
    }

    public Builder withArriveBy(Long arriveBy) {
      this.arriveBy = arriveBy;
      return this;
    }

    public TripResultPagerFragment build() {
      PagerFragmentArguments args = new FromRoutes(this.tripGroupId, this.sortOrder, this.requestId, this.arriveBy);
      TripResultPagerFragment fragment = new TripResultPagerFragment();
      fragment.setArgs(args);
      fragment.setButtons(buttons);
      fragment.setMapFragment(this.mapFragment);
      return fragment;
    }
  }
}
