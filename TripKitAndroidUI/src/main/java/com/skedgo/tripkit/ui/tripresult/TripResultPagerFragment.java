package com.skedgo.tripkit.ui.tripresult;

import android.content.Context;
import android.os.Bundle;
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
import com.skedgo.tripkit.ui.core.BaseTripKitFragment;
import com.skedgo.tripkit.ui.databinding.TripResultPagerBinding;
import com.skedgo.tripkit.ui.map.home.TripKitMapContributor;
import com.skedgo.tripkit.ui.map.home.TripKitMapFragment;
import com.skedgo.tripkit.ui.model.TripKitButton;
import com.squareup.otto.Bus;
import org.jetbrains.annotations.NotNull;
import com.skedgo.tripkit.logging.ErrorLogger;
import com.skedgo.tripkit.routing.TripGroup;
import timber.log.Timber;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class TripResultPagerFragment extends BaseTripKitFragment implements ViewPager.OnPageChangeListener, TripSegmentListFragment.OnTripKitButtonClickListener {
  public interface OnTripKitButtonClickListener {
    void onTripKitButtonClicked(String id, TripGroup tripGroup);
  }

  OnTripKitButtonClickListener tripButtonClickListener = null;
  public void setOnTripKitButtonClickListener(OnTripKitButtonClickListener listener) {
    this.tripButtonClickListener = listener;
  }

  private List<TripKitButton> buttons;

  private static final String KEY_CURRENT_PAGE = "currentPage";
  private static final String KEY_SHOW_CLOSE_BUTTON = "showCloseButton";
  private final BookViewClickEventHandler bookViewClickEventHandler = BookViewClickEventHandler.create(this);

  /* TODO: Replace with RxJava-based approach. */
  @Inject @Deprecated Bus bus;
  @Inject TripResultPagerViewModel viewModel;
  @Inject
  ErrorLogger errorLogger;

  private TripGroupsPagerAdapter tripGroupsPagerAdapter;
  private TripResultPagerBinding binding;
  private TripResultMapContributor mapContributor = new TripResultMapContributor();
  public TripSegmentListFragment.OnTripSegmentClickListener tripSegmentClickListener = null;

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


  @Override
  public void onResume() {
    super.onResume();
    bus.register(this);
    bus.register(bookViewClickEventHandler);
    getAutoDisposable().add(viewModel.trackViewingTrip()
            .subscribe());

    getAutoDisposable().add(viewModel.observeTripGroups()
            .subscribe());

    getAutoDisposable().add(viewModel.observeInitialPage()
            .subscribe());

    getAutoDisposable().add(viewModel.updateSelectedTripGroup()
            .subscribe());

    getAutoDisposable().add(viewModel.loadFetchingRealtimeStatus()
            .subscribe());

    assert args != null;
    getAutoDisposable().add(viewModel.getSortedTripGroups(args)
            .subscribe(tripGroup -> {}, errorLogger::trackError));

  }

  public TripKitMapContributor contributor() {
    return mapContributor;
  }

  @Override public void onDestroy() {
    super.onDestroy();
    mapContributor.cleanup();
  }

  @Override public void onStart() {
    super.onStart();
    viewModel.onStart();
    mapContributor.setup();
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
    if (binding.tripGroupsPager != null) {
      outState.putInt(KEY_CURRENT_PAGE, binding.tripGroupsPager.getCurrentItem());
    }
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
    mapContributor.initialize();
    super.onAttach(context);
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    binding.setViewModel(viewModel);
    if (savedInstanceState != null) {
      currentPage = savedInstanceState.getInt(KEY_CURRENT_PAGE);
    }

    viewModel.onCreate(savedInstanceState);

    if (savedInstanceState == null) {
      if (args instanceof HasInitialTripGroupId) {
        String id = ((HasInitialTripGroupId) args).tripGroupId();
        viewModel.setInitialSelectedTripGroupId(id);
        mapContributor.setTripGroupId(id);
      }
    }

    tripGroupsPagerAdapter = new TripGroupsPagerAdapter(getChildFragmentManager());
    tripGroupsPagerAdapter.listener = this;
    tripGroupsPagerAdapter.segmentClickListener = tripSegmentClickListener;
    tripGroupsPagerAdapter.closeListener = getOnCloseButtonListener();
    tripGroupsPagerAdapter.setButtons(buttons);
    Bundle b = getArguments();
    if (b != null) {
      tripGroupsPagerAdapter.setShowCloseButton(b.getBoolean(KEY_SHOW_CLOSE_BUTTON, false));
    }

    binding.tripGroupsPager.setAdapter(tripGroupsPagerAdapter);
  }

  public void setArgs(@NonNull PagerFragmentArguments args) {
    this.args = args;
  }

  @Override
  public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

  }

  @Override
  public void onPageSelected(int position) {
      TripGroup group = this.tripGroupsPagerAdapter.getTripGroups().get(position);
      mapContributor.setTripGroupId(group.uuid());
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
    private String tripGroupId = "";
    private Integer sortOrder = 1;
    private String requestId = "";
    private Long arriveBy = 0L;
    private boolean showCloseButton = false;
    private List<TripKitButton> buttons = new ArrayList<TripKitButton>();

    public Builder withTripButton(String id, int layoutResourceId) {
      TripKitButton b = new TripKitButton(id, layoutResourceId);
      buttons.add(b);
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

    public Builder showCloseButton() {
      this.showCloseButton = true;
      return this;
    }

    public TripResultPagerFragment build() {
      PagerFragmentArguments args = new FromRoutes(this.tripGroupId, this.sortOrder, this.requestId, this.arriveBy);
      TripResultPagerFragment fragment = new TripResultPagerFragment();
      fragment.setArgs(args);
      Bundle b = new Bundle();
      b.putBoolean(KEY_SHOW_CLOSE_BUTTON, showCloseButton);
      fragment.setArguments(b);
      fragment.setButtons(buttons);
      return fragment;
    }
  }
}
