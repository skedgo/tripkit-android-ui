package com.skedgo.tripkit.ui.map.home;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.*;
import com.google.maps.android.MarkerManager;
import com.skedgo.tripkit.common.model.Location;
import com.skedgo.tripkit.common.model.Region;
import com.skedgo.tripkit.data.regions.RegionService;
import com.skedgo.tripkit.tripplanner.NonCurrentType;
import com.skedgo.tripkit.tripplanner.PinUpdate;
import com.skedgo.tripkit.ui.R;
import com.skedgo.tripkit.ui.TripKitUI;
import com.skedgo.tripkit.ui.core.module.HomeMapFragmentModule;
import com.skedgo.tripkit.ui.core.permissions.*;
import com.skedgo.tripkit.ui.map.*;
import com.skedgo.tripkit.ui.map.adapter.CityInfoWindowAdapter;
import com.skedgo.tripkit.ui.map.adapter.NoActionWindowAdapter;
import com.skedgo.tripkit.ui.map.adapter.POILocationInfoWindowAdapter;
import com.skedgo.tripkit.ui.model.LocationTag;
import com.skedgo.tripkit.ui.tracking.EventTracker;
import com.skedgo.tripkit.ui.trip.options.SelectionType;
import com.squareup.otto.Bus;
import dagger.Lazy;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import kotlin.Pair;
import com.skedgo.tripkit.logging.ErrorLogger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static com.skedgo.tripkit.ui.data.ToLocationKt.toLocation;
import static com.skedgo.tripkit.ui.map.ConvertToDomainLatLngBoundsKt.convertToDomainLatLngBounds;
import static com.skedgo.tripkit.ui.map.MapMarkerUtils.createCityMarker;
import static com.skedgo.tripkit.ui.map.MapMarkerUtils.createTransparentSquaredIcon;
import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;

/**
 * A map component for an app. It automatically integrates with SkedGo's backend, display transit information without
 * any additional intervention.
 *
 * Being a fragment, it can very easily be added to an activity's layout.
 *
 * <pre> {@code
 *     <fragment
 *             android:layout_width="match_parent"
 *             android:layout_height="match_parent"
 *             android:id="@+id/map"
 *             android:name="com.skedgo.tripkit.ui.map.home.TripKitMapFragment"/> } </pre>
 *
 * Your app **must** provide a TripGo API token as `R.string.skedgo_api_key`.
 *
 */
public class TripKitMapFragment extends LocationEnhancedMapFragment implements
    GoogleMap.OnInfoWindowClickListener,
    GoogleMap.OnMapLongClickListener,
    GoogleMap.OnCameraChangeListener,
    GoogleMap.OnMarkerClickListener {

  /* TODO: Replace with RxJava-based approach. */
  @Deprecated @Inject
  Bus bus;
  @Inject MapViewModel viewModel;
  @Inject
  RegionService regionService;
  @Inject
  MapCameraController cameraController;
  @Inject
  TripLocationMarkerCreator tripLocationMarkerCreator;
  @Inject
  CityInfoWindowAdapter cityInfoWindowAdapter;
  @Inject
  NoActionWindowAdapter myLocationWindowAdapter;
  @Inject Lazy<StopMarkerIconFetcher> stopMarkerIconFetcherLazy;
  @Inject
  ErrorLogger errorLogger;
  @Inject
  EventTracker eventTracker;

  private HashMap<String, Marker> cityMarkerMap = new HashMap<>();
  private List<Region> regions = new LinkedList<>();
  private BitmapDescriptor cityIcon;
  private GoogleMap.InfoWindowAdapter infoWindowAdapter;
  private Marker myLocationMarker;
  private MarkerManager markerManager;
  private MarkerManager.Collection poiMarkers;
  private MarkerManager.Collection cityMarkers;
  private MarkerManager.Collection tripLocationMarkers;
  private MarkerManager.Collection departureMarkers;
  private MarkerManager.Collection arrivalMarkers;
  private MarkerManager.Collection currentLocationMarkers;
  private boolean tipTapIsDeleted = false;
  private boolean tipZoomIsDeleted = false;
  private boolean checkZoomOutFlag = false;
  private GoogleMap map;

  private static BitmapDescriptor asMarkerIcon(SelectionType mode) {
    if (mode == SelectionType.DEPARTURE) {
      return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
    } else {
      return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
    }
  }

  /**
   * When an icon in the map is clicked, an information window is displayed. When that information window
   * is clicked, this interface is used as a callback to notify the app of the click.
   *
   */
  public interface OnInfoWindowClickListener {
    /**
     * Called when an info window is clicked.
     *
     * @param location The location represented by the info window that was clicked
     */
    void onInfoWindowClick(Location location);
  }
  private OnInfoWindowClickListener onInfoWindowClickListener;
  public void setOnInfoWindowClickListener(OnInfoWindowClickListener listener) {
    this.onInfoWindowClickListener = listener;
  }

  @Override
  public void onAttach(Context context) {
    TripKitUI.getInstance().homeMapFragmentComponent(new HomeMapFragmentModule(this)).inject(this);
    super.onAttach(context);
  }
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    whenSafeToUseMap(map -> {
      this.map = map;
      initMarkerCollections(map);
      initMap(map);

    });
    initStuff();
    setMyLocationEnabled();

    viewModel.getOriginPinUpdate()
      .compose(bindToLifecycle())
        .observeOn(mainThread())
        .subscribe(this::updateDepartureMarker);

    viewModel.getDestinationPinUpdate()
            .compose(bindToLifecycle())
        .observeOn(mainThread())
        .subscribe(this::updateArrivalMarker);
  }

  @Override public void onPause() {
//    bus.unregister(this);
    super.onPause();

    // Warning: If we obtain GoogleMap via getMapAsync() right here, when onPause() is called in
    // the case of removing the fragment, the callback of getMapAsync() won't be invoked.
    // However, for the case of switching to a different Activity, the callback of getMapAsync()
    // will be invoked.
    if (map != null) {
      viewModel.putCameraPosition(map.getCameraPosition()).subscribe();
    }
  }

  @Override public void onResume() {
    super.onResume();
    bus.register(this);
  }

  @Override
  public void onDestroy() {

    if (currentLocationMarkers != null) {
      currentLocationMarkers.clear();
    }

    viewModel.onCleared();
    super.onDestroy();
  }

  @Override public boolean onMarkerClick(Marker marker) {
//    bus.post(new TooltipFragment.TooltipClose(TooltipFragment.PREF_TAP_PUBLIC_STOPS));
    return markerManager.onMarkerClick(marker);
  }

  /**
   * If we do not specify our own implementation,
   * GoogleMap will fall back to its default implementation for InfoWindowAdapter.
   */
  public void setInfoWindowAdapter(GoogleMap.InfoWindowAdapter infoWindowAdapter) {
    this.infoWindowAdapter = infoWindowAdapter;
  }

  @Override public void onInfoWindowClick(Marker marker) {
    markerManager.onInfoWindowClick(marker);
  }

  @Override public void onMapLongClick(LatLng point) {
//    bus.post(new MapLongClickEvent(point.latitude, point.longitude));
  }

//  @Subscribe public void onEvent(CurrentLocationSelectedEvent e) {
//    goToMyLocation();
//  }

//  @Subscribe public void onEvent(DropPinSelectedEvent e) {
//    if (map != null) {
//      onMapLongClick(map.getCameraPosition().target);
//    }
//  }
//
//  @Subscribe public void onEvent(final ImmutableCitySelectedEvent e) {
//    whenSafeToUseMap(map -> map.moveCamera(CameraUpdateFactory.newLatLngBounds(e.bounds(), 0)));
//  }

  @Override
  public void onCameraChange(final CameraPosition position) {
    if (!isAdded()) {
      // To investigate further this scenario.
//      Crashlytics.logException(new IllegalStateException(
//          "onCameraChange() when !isAdded()"
//      ));
      return;
    }

//    boolean tipZoomToSeeTimetable = mPrefUtils.get(TooltipFragment.PREF_ZOOM_TO_SEE_TIMETABLE, false);
//    boolean tipTapPublicStops = mPrefUtils.get(TooltipFragment.PREF_TAP_PUBLIC_STOPS, false);
    boolean tipZoomToSeeTimetable = false;
    boolean tipTapPublicStops = true;

    if (map == null) {
      return;
    }

    final LatLngBounds visibleBounds = map.getProjection().getVisibleRegion().latLngBounds;
//    bus.post(new CameraChangeEvent(position, visibleBounds));

    //reason to keep zoomLevel is because it's used in so many loader classes
    ZoomLevel zoomLevel = ZoomLevel.fromLevel(position.zoom);
    if (zoomLevel != null) {
      if (!tipZoomIsDeleted && tipTapPublicStops && checkZoomOutFlag) {
//        bus.post(new TooltipFragment.TooltipClose(TooltipFragment.PREF_ZOOM_TO_SEE_TIMETABLE));
        tipZoomIsDeleted = true;
      }
      if (!tipTapPublicStops) {
//        bus.post(new RequestShowTip(TooltipFragment.PREF_TAP_PUBLIC_STOPS, getString(R.string.tap_public_transport_stops_for_access_to_timetable)));
      }
    } else {
      if (!tipTapIsDeleted) {
//        bus.post(new TooltipFragment.TooltipClose(TooltipFragment.PREF_TAP_PUBLIC_STOPS));
        tipTapIsDeleted = true;
      }
      if (!tipZoomToSeeTimetable) {
//        bus.post(new RequestShowTip(TooltipFragment.PREF_ZOOM_TO_SEE_TIMETABLE, getString(R.string.zoom_into_map_to_view_public_transport_stops)));
        checkZoomOutFlag = true;
      }
    }
    if (zoomLevel != null) {
      viewModel.onViewPortChanged(new ViewPort.CloseEnough(
          position.zoom,
          convertToDomainLatLngBounds(visibleBounds)));
    } else {
      viewModel.onViewPortChanged(new ViewPort.NotCloseEnough(
          position.zoom,
          convertToDomainLatLngBounds(visibleBounds)));
    }

    if (position.zoom <= ZoomLevel.ZOOM_VALUE_TO_SHOW_CITIES) {
      showCities(map, regions);
    } else {
      removeAllCities();
    }
  }

  public void onLocationSelected(LocationTag locationTag) {
    if (locationTag != null) {
      final SelectionType selectionType = locationTag.getType();
      final Location location = locationTag.getLocation();
      whenSafeToUseMap(map -> {
        tripLocationMarkers.clear();
        final Marker marker = tripLocationMarkers.addMarker(
            tripLocationMarkerCreator.call(location)
                .icon(asMarkerIcon(selectionType))
        );
        marker.setTag(locationTag);
        marker.showInfoWindow();
      });
    }
  }

//  @Subscribe
//  public void onEvent(LocationSelectedEvent event) {
//    whenSafeToUseMap(map -> cameraController.moveToLatLng(map, toLatLng(event.getLocation())));
//  }

  public void onLocationAddressDecoded(LocationTag locationTag) {
    final SelectionType selectionType = locationTag.getType();
    final Location location = locationTag.getLocation();
    whenSafeToUseMap(map -> {
      tripLocationMarkers.clear();
      final Marker marker = tripLocationMarkers.addMarker(
          tripLocationMarkerCreator.call(location)
              .icon(asMarkerIcon(selectionType))
      );
      marker.setTag(locationTag);
      marker.showInfoWindow();
    });
  }

  @Override
  protected void animateToMyLocation() {
    goToMyLocation();
  }

  void animateToCity(Location city) {
    whenSafeToUseMap(map -> {
      final CameraPosition position = new CameraPosition.Builder()
          .zoom(ZoomLevel.OUTER.level)
          .target(new LatLng(city.getLat(), city.getLon()))
          .build();
      map.animateCamera(CameraUpdateFactory.newCameraPosition(position));
    });
  }

  private void initStuff() {
    regionService.getRegionsAsync()
            .compose(bindToLifecycle())
        .observeOn(mainThread())
        .subscribe(regions -> {
          this.regions = regions;
          whenSafeToUseMap(m -> showCities(m, regions));
        }, errorLogger::logError);
  }

  private void updateArrivalMarker(PinUpdate pinUpdate) {
    whenSafeToUseMap(map -> pinUpdate.match(
        () -> arrivalMarkers.clear(),
        it -> {
          final Marker marker = arrivalMarkers.addMarker(
              tripLocationMarkerCreator.call(toLocation(it.getType()))
                  .icon(asMarkerIcon(SelectionType.ARRIVAL))
          );
          marker.setTag(it.getType());
          marker.showInfoWindow();
        }
    ));
  }

  private void updateDepartureMarker(PinUpdate pinUpdate) {
    whenSafeToUseMap(map -> pinUpdate.match(
        () -> departureMarkers.clear(),
        it -> {
          final Marker marker = departureMarkers.addMarker(
              tripLocationMarkerCreator.call(toLocation(it.getType()))
                  .icon(asMarkerIcon(SelectionType.DEPARTURE))
          );
          marker.setTag(it.getType());
          marker.showInfoWindow();
        }
    ));
  }

  @SuppressWarnings("MissingPermission") private void setMyLocationEnabled() {
    if (getActivity() instanceof CanRequestPermission) {
      whenSafeToUseMap(map -> map.setMyLocationEnabled(true));
    }
//    ((BaseActivity) getActivity())
//        .checkSelfPermissionReactively(Manifest.permission.ACCESS_FINE_LOCATION)
//        .filter(result -> result)
//            .compose(bindToLifecycle())
//        .subscribe(__ -> whenSafeToUseMap(map -> map.setMyLocationEnabled(true)));
  }

  @NonNull private Single<PermissionResult> requestLocationPermission() {
      return ((CanRequestPermission) getActivity())
              .requestPermissions(
                      new PermissionsRequest.Location(),
                      ShowGenericRationaleKt.showGenericRationale(
                              getActivity(),
                              null,
                              getString(R.string.access_to_location_services_required_dot)
                      ),
                      DealWithNeverAskAgainDenialKt.dealWithNeverAskAgainDenial(
                              getActivity(),
                              getString(R.string.access_to_location_services_required_dot)
                      )
              );
  }

  private void removeAllCities() {
    cityMarkers.clear();
    cityMarkerMap.clear();
  }

  private void showCities(GoogleMap map, @Nullable List<Region> regions) {
    if (regions != null) {
      final LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;
      for (int i = 0, regionsSize = regions.size(); i < regionsSize; i++) {
        final Region region = regions.get(i);
        final ArrayList<Region.City> cities = region.getCities();
        if (cities != null) {
          for (int j = 0, citiesSize = cities.size(); j < citiesSize; j++) {
            final Region.City city = cities.get(j);
            // If the city is in viewport, add markers if hasn't added.
            if (bounds.contains(new LatLng(city.getLat(), city.getLon()))) {
              if (cityMarkerMap.get(city.getName()) == null) {
                // Marker for this city hasn't been added yet.
                addCityMarker(city);
              }
            } else {
              removeCity(city);
            }
          }
        }
      }
    }
  }

  private void removeCity(@NonNull Region.City city) {
    cityMarkers.remove(cityMarkerMap.get(city.getName()));
    cityMarkerMap.remove(city.getName());
  }

  @SuppressLint("MissingPermission") private void setupMap(@NonNull GoogleMap map) {
    map.setOnMapLongClickListener(this);
    map.setOnInfoWindowClickListener(this);
    map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
      @Override
      public View getInfoWindow(Marker marker) {
        return markerManager.getInfoWindow(marker);
      }

      @Override
      public View getInfoContents(Marker marker) {
        return markerManager.getInfoContents(marker);
      }
    });
    map.setOnCameraChangeListener(this);
    map.setIndoorEnabled(false);
    map.setOnMarkerClickListener(this);
  }

  private Marker addCityMarker(Region.City city) {
    final MarkerOptions markerOptions = createCityMarker(city, cityIcon);
    final Marker marker = cityMarkers.addMarker(markerOptions);
    cityMarkerMap.put(city.getName(), marker);
    marker.setTag(city);
    return marker;
  }

  private void goToMyLocation() {
    if (getActivity() instanceof CanRequestPermission) {
      requestLocationPermission()
              .toObservable()
              .compose(bindToLifecycle())
              .subscribe(result -> {
                if (result instanceof PermissionResult.Granted) {
                  viewModel.goToMyLocation();
                }
              }, errorLogger::trackError);
    }
  }

  private void showMyLocation(Location myLocation) {
    // Prepare marker for my location.
    if (myLocationMarker == null) {
      MarkerOptions markerOptions = tripLocationMarkerCreator.call(myLocation);
      markerOptions.icon(createTransparentSquaredIcon(
          getResources(),
          R.dimen.spacing_small
      ));
      myLocationMarker = currentLocationMarkers.addMarker(markerOptions);
    } else {
      myLocationMarker.setPosition(new LatLng(myLocation.getLat(), myLocation.getLon()));
    }

    cameraController.moveTo(map, myLocationMarker);
    myLocationMarker.showInfoWindow();
  }

  private void initMap(GoogleMap map) {
    cityIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_map_city);
    setupMap(map);

    final Observable<Boolean> requestLocationPermission = Single
        .defer(() -> requestLocationPermission()
            .map(it -> it instanceof PermissionResult.Granted)
        )
        .toObservable()
            .compose(bindToLifecycle());

    viewModel.getInitialCameraUpdate(() -> requestLocationPermission)
            .compose(bindToLifecycle())
        .subscribe(map::moveCamera, errorLogger::trackError);

    viewModel.getMyLocation()
            .compose(bindToLifecycle())
        .subscribeOn(mainThread())
        .subscribe(this::showMyLocation, errorLogger::trackError);

    viewModel.getMyLocationError()
            .compose(bindToLifecycle())
        .subscribeOn(mainThread())
        .subscribe(__ -> showMyLocationError(), errorLogger::trackError);

    viewModel.getMarkers()
        .subscribeOn(Schedulers.io())
        .observeOn(mainThread())
        .subscribe(pair -> {
          final List<Marker> toRemove = new ArrayList<>();
          for (Marker marker : poiMarkers.getMarkers()) {
            String identifier = ((POILocation) marker.getTag()).getIdentifier();
            if (pair.getSecond().contains(identifier)) {
              toRemove.add(marker);
            }
          }
          for (Marker marker : toRemove) {
            poiMarkers.remove(marker);
          }
          for (Pair<MarkerOptions, POILocation> markerOptions : pair.getFirst()) {
            Marker marker = poiMarkers.addMarker(markerOptions.getFirst());
            marker.setTag(markerOptions.getSecond());
          }
        });
  }

  private void showMyLocationError() {
    Toast.makeText(
        getActivity(),
        R.string.could_not_determine_your_current_location_dot,
        Toast.LENGTH_SHORT
    ).show();
  }

  private void initMarkerCollections(GoogleMap map) {
    markerManager = new MarkerManager(map);

    setUpCityMarkers(markerManager);
    setUpTripLocationMarkers(markerManager);
    setUpDepartureAndArrivalMarkers(markerManager);
    setUpCurrentLocationMarkers(markerManager);
    setUpPOIMarkers(markerManager, map);
  }

  private void setUpCurrentLocationMarkers(MarkerManager markerManager) {
    if (getActivity() instanceof CanRequestPermission) {
      currentLocationMarkers = markerManager.newCollection("CurrentLocationMarkers");
      currentLocationMarkers.setOnInfoWindowAdapter(myLocationWindowAdapter);
    }
//    currentLocationMarkers.setOnInfoWindowClickListener(marker -> bus.post(
//        new CurrentLocationInfoWindowClickEvent(marker.getPosition())
//    ));
  }

  private void setUpDepartureAndArrivalMarkers(MarkerManager markerManager) {
    departureMarkers = markerManager.newCollection("DepartureMarkers");
    departureMarkers.setOnInfoWindowAdapter(infoWindowAdapter);
    departureMarkers.setOnInfoWindowClickListener(marker -> {
      final Object tag = marker.getTag();
      if (tag instanceof NonCurrentType) {
        final NonCurrentType type = (NonCurrentType) tag;
//        bus.post(new InfoWindowClickEvent(toLocation(type), true));
      }
    });

    arrivalMarkers = markerManager.newCollection("ArrivalMarkers");
    arrivalMarkers.setOnInfoWindowAdapter(infoWindowAdapter);
    arrivalMarkers.setOnInfoWindowClickListener(marker -> {
      final Object tag = marker.getTag();
      if (tag instanceof NonCurrentType) {
        final NonCurrentType type = (NonCurrentType) tag;
//        bus.post(new InfoWindowClickEvent(toLocation(type), false));
      }
    });
  }

  private void setUpTripLocationMarkers(MarkerManager markerManager) {
    tripLocationMarkers = markerManager.newCollection("TripLocationMarkers");
    tripLocationMarkers.setOnInfoWindowAdapter(infoWindowAdapter);
    tripLocationMarkers.setOnInfoWindowClickListener(marker -> {
      final Object tag = marker.getTag();
      if (tag instanceof LocationTag) {
        final LocationTag locationTag = (LocationTag) tag;
        final Location location = locationTag.getLocation();
        if (location != null) {
//          bus.post(new InfoWindowClickEvent(location));
        }
      }
    });
  }

  private void setUpCityMarkers(MarkerManager markerManager) {
    cityMarkers = markerManager.newCollection("CityMarkers");
    cityMarkers.setOnInfoWindowAdapter(cityInfoWindowAdapter);
    cityMarkers.setOnInfoWindowClickListener(marker -> {
      final Object tag = marker.getTag();
      if (tag instanceof Region.City) {
        final Region.City city = (Region.City) tag;
        animateToCity(city);
      }
    });
  }

  private void setUpPOIMarkers(MarkerManager markerManager, GoogleMap map) {
    poiMarkers = markerManager.newCollection("poiMarkers");
    final MarkerManager.Collection poiMarkers = this.poiMarkers;
    final POILocationInfoWindowAdapter poiLocationInfoWindowAdapter = new POILocationInfoWindowAdapter(getContext());
    poiMarkers.setOnInfoWindowAdapter(poiLocationInfoWindowAdapter);
    map.setOnInfoWindowCloseListener(marker -> {
      if (marker.getTag() instanceof POILocation) {
        poiLocationInfoWindowAdapter.onInfoWindowClosed(marker);
      }
    });

    poiMarkers.setOnInfoWindowClickListener(marker -> {
      if (onInfoWindowClickListener != null) {
        final POILocation poiLocation = ((POILocation) marker.getTag());
        if (poiLocation != null) {
          onInfoWindowClickListener.onInfoWindowClick(poiLocation.toLocation());
        }
      }
    });
    poiMarkers.setOnMarkerClickListener(marker -> {
      @Nullable final View view = getView();
      if (view == null) {
        return true;
      }
      final POILocation poiLocation = ((POILocation) marker.getTag());
      poiLocation.onMarkerClick(bus, eventTracker);
      marker.showInfoWindow();

      int scrollY = getResources().getDimensionPixelSize(R.dimen.routing_card_height)
          + getResources().getDimensionPixelSize(R.dimen.spacing_huge)
          + poiLocationInfoWindowAdapter.windowInfoHeightInPixel(marker)
          - view.getHeight() / 2;
      map.moveCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
      if (scrollY > 0) {
        // center the map to 64dp above the bottom of the fragment
        map.moveCamera(CameraUpdateFactory.scrollBy(0, scrollY * -1));
      }
      return true;
    });
  }
}