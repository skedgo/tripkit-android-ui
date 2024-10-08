package com.skedgo.tripkit.ui.tripresult;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.skedgo.tripkit.common.model.location.Location;
import com.skedgo.tripkit.logging.ErrorLogger;
import com.skedgo.tripkit.model.ViewTrip;
import com.skedgo.tripkit.routing.Trip;
import com.skedgo.tripkit.routing.TripGroup;
import com.skedgo.tripkit.ui.TripKitUI;
import com.skedgo.tripkit.ui.booking.BookViewClickEventHandler;
import com.skedgo.tripkit.ui.core.BaseTripKitFragment;
import com.skedgo.tripkit.ui.databinding.TripResultPagerBinding;
import com.skedgo.tripkit.ui.map.home.TripKitMapContributor;
import com.skedgo.tripkit.ui.model.TripKitButtonConfigurator;
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButtonHandlerFactory;
import com.squareup.otto.Bus;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

public class TripResultPagerFragment extends BaseTripKitFragment implements ViewPager.OnPageChangeListener, TripSegmentListFragment.OnTripKitButtonClickListener {
    private static final String KEY_CURRENT_PAGE = "currentPage";
    private static final String KEY_SHOW_CLOSE_BUTTON = "showCloseButton";
    private final BookViewClickEventHandler bookViewClickEventHandler = BookViewClickEventHandler.create(this);
    public TripSegmentListFragment.OnTripSegmentClickListener tripSegmentClickListener = null;
    OnTripKitButtonClickListener tripButtonClickListener = null;
    OnTripUpdatedListener tripUpdatedListener = null;
    /* TODO: Replace with RxJava-based approach. */
    @Inject
    @Deprecated
    Bus bus;
    @Inject
    TripResultPagerViewModel viewModel;
    @Inject
    ErrorLogger errorLogger;
    private TripGroupsPagerAdapter tripGroupsPagerAdapter;
    private TripResultPagerBinding binding;
    private TripResultMapContributor mapContributor = new TripResultMapContributor();
    private ActionButtonHandlerFactory actionButtonHandlerFactory = null;
    private List<TripGroup> initialTripGroupList = null;
    private Location queryFromLocation = null;
    private Location queryToLocation = null;
    @Nullable
    private PagerFragmentArguments args;
    private int currentPage = -1;

    public void setOnTripKitButtonClickListener(OnTripKitButtonClickListener listener) {
        this.tripButtonClickListener = listener;
    }

    public void setOnTripUpdatedListener(OnTripUpdatedListener listener) {
        this.tripUpdatedListener = listener;
    }

    public void setActionButtonHandlerFactory(ActionButtonHandlerFactory actionButtonHandlerFactory) {
        this.actionButtonHandlerFactory = actionButtonHandlerFactory;
    }

    public void setQueryLocations(Location from, Location to) {
        queryFromLocation = from;
        queryToLocation = to;
    }

    @Nullable
    @Override
    public View onCreateView(
        LayoutInflater inflater,
        @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState) {
        final TripResultPagerBinding binding = TripResultPagerBinding.inflate(inflater);
        this.binding = binding;

        binding.setViewModel(viewModel);
        binding.tripGroupsPager.setAdapter(tripGroupsPagerAdapter);

        binding.tripGroupsPager.setCurrentItem(currentPage);
        viewModel.getCurrentPage().set(currentPage);
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
            .subscribe(
                        /*
                        groups -> {
                            if (!groups.isEmpty()) {
                                Map<String, Long> idsMap = tripGroupsPagerAdapter.getTripIds();
                                Map.Entry<String, Long> entry = idsMap.entrySet().iterator().next();
                                int index = 0;
                                for (TripGroup group : groups) {
                                    if (group.uuid().equals(entry.getKey())) {
                                        currentPage = index;
                                        binding.tripGroupsPager.setCurrentItem(currentPage);
                                    }
                                    index++;
                                }
                            }
                        }
                        */
            ));

        getAutoDisposable().add(viewModel.observeInitialPage()
            .subscribe());

        getAutoDisposable().add(viewModel.updateSelectedTripGroup()
            .subscribe());

        getAutoDisposable().add(viewModel.loadFetchingRealtimeStatus()
            .subscribe());

        assert args != null;
        getAutoDisposable().add(viewModel.getSortedTripGroups(args, initialTripGroupList)
            .subscribe(tripGroup -> {
                if (args instanceof FavoriteTrip) {
                    // The trip group will possibly have changed after reloading it, so set the map to the correct one here
                    mapContributor.setTripGroupId(viewModel.getCurrentTripGroupId().get(), null);
                }
            }, errorLogger::trackError));

        viewModel.getCurrentTrip().observe(getViewLifecycleOwner(), trip -> {
            if (tripUpdatedListener != null) {
                tripUpdatedListener.onTripUpdated(trip);
            }
        });
    }

    public TripKitMapContributor contributor() {
        return mapContributor;
    }

    public void updatePagerFragmentTripGroup(@NotNull TripGroup tripGroup) {
        viewModel.setInitialSelectedTripGroupId(tripGroup.uuid());
    }

    public void updateTripGroupResult(@NotNull List<TripGroup> tripGroup) {
        viewModel.updateTripGroupResult(tripGroup);
    }

    @Override
    public void onDestroy() {
        mapContributor.cleanup();
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        viewModel.onStart();
        mapContributor.setup();
        this.binding.tripGroupsPager.addOnPageChangeListener(this);

    }

    @Override
    public void onStop() {
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

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (binding != null && binding.tripGroupsPager != null) {
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
    public void onCreate(@org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            currentPage = savedInstanceState.getInt(KEY_CURRENT_PAGE);
        }

        viewModel.onCreate(savedInstanceState);
        Long tripId = null;
        String groupId = null;
        tripGroupsPagerAdapter = new TripGroupsPagerAdapter(getChildFragmentManager(), mapContributor);

        if (savedInstanceState == null) {
            if (args instanceof HasInitialTripGroupId) {
                groupId = ((HasInitialTripGroupId) args).tripGroupId();
                tripId = ((HasInitialTripGroupId) args).tripId();
                tripGroupsPagerAdapter.getTripIds().put(groupId, tripId);
                viewModel.setInitialSelectedTripGroupId(groupId);
                mapContributor.setTripGroupId(groupId, tripId);
            }
        }

        TripKitButtonConfigurator configurator = null;
        Bundle b = getArguments();
        if (b != null) {
            tripGroupsPagerAdapter.setShowCloseButton(b.getBoolean(KEY_SHOW_CLOSE_BUTTON, false));
        }

        tripGroupsPagerAdapter.listener = this;
        tripGroupsPagerAdapter.segmentClickListener = tripSegmentClickListener;
        tripGroupsPagerAdapter.closeListener = getOnCloseButtonListener();
        tripGroupsPagerAdapter.setActionButtonHandlerFactory(actionButtonHandlerFactory);
        tripGroupsPagerAdapter.setQueryLocations(queryFromLocation, queryToLocation);
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
        mapContributor.setTripGroupId(group.uuid(), null);
        viewModel.getCurrentPage().set(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void tripKitButtonClicked(int id, @NotNull TripGroup tripGroup) {
        if (tripButtonClickListener != null) {
            tripButtonClickListener.onTripKitButtonClicked(id, tripGroup);
        }
    }

    public interface OnTripKitButtonClickListener {
        void onTripKitButtonClicked(int id, TripGroup tripGroup);
    }

    public interface OnTripUpdatedListener {
        void onTripUpdated(Trip trip);
    }

    public static class Builder {
        private String tripGroupId = "";
        private String favoriteTripId = "";
        private Long tripId = -1L;
        private Integer sortOrder = 1;
        private String requestId = "";
        private Long arriveBy = 0L;
        private Location fromLocation = null;
        private Location toLocation = null;
        private boolean showCloseButton = false;
        private boolean singleRoute = false;
        private List<TripGroup> initialTripGroupList = null;
        private ActionButtonHandlerFactory actionButtonHandlerFactory = null;

        public Builder withActionButtonHandlerFactory(ActionButtonHandlerFactory factory) {
            this.actionButtonHandlerFactory = factory;
            return this;
        }

        public Builder withViewTrip(ViewTrip trip) {
            this.tripGroupId = trip.tripGroupUUID();
            this.tripId = trip.getDisplayTripID();
            this.sortOrder = trip.getSortOrder();
            this.arriveBy = trip.query().getArriveBy();
            this.requestId = trip.query().uuid();
            this.fromLocation = trip.query().getFromLocation();
            this.toLocation = trip.query().getToLocation();
            return this;
        }

        public Builder withFavoriteTripId(String id) {
            favoriteTripId = id;
            singleRoute = true;
            return this;
        }

        public Builder withTripGroupId(String tripGroupId) {
            this.tripGroupId = tripGroupId;
            return this;
        }

        public Builder withTripId(Long tripId) {
            this.tripId = tripId;
            return this;
        }

        public Builder showSingleRoute() {
            this.singleRoute = true;
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

        public Builder withInitialTripGroupList(List<TripGroup> initialTripGroupList) {
            this.initialTripGroupList = initialTripGroupList;
            return this;
        }

        public Builder showCloseButton() {
            this.showCloseButton = true;
            return this;
        }

        public TripResultPagerFragment build() {
            PagerFragmentArguments args;
            if (singleRoute) {
                if (!favoriteTripId.isEmpty()) {
                    args = new FavoriteTrip(this.favoriteTripId);
                } else {
                    args = new SingleTrip(this.tripGroupId, this.tripId);
                }
            } else {
                args = new FromRoutes(
                    this.tripGroupId,
                    this.tripId,
                    this.sortOrder,
                    this.requestId,
                    this.arriveBy
                );
            }
            TripResultPagerFragment fragment = new TripResultPagerFragment();
            fragment.setArgs(args);
            fragment.setActionButtonHandlerFactory(actionButtonHandlerFactory);
            fragment.setQueryLocations(fromLocation, toLocation);
            Bundle b = new Bundle();
            b.putBoolean(KEY_SHOW_CLOSE_BUTTON, showCloseButton);
            fragment.initialTripGroupList = initialTripGroupList;
            fragment.setArguments(b);
            return fragment;
        }
    }
}
