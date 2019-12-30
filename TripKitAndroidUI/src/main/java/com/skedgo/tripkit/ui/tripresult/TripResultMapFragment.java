package com.skedgo.tripkit.ui.tripresult;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.Nullable;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.*;
import com.google.maps.android.MarkerManager;
import com.skedgo.tripkit.common.model.Location;
import com.skedgo.tripkit.common.model.RealtimeAlert;
import com.skedgo.tripkit.ui.R;
import com.skedgo.tripkit.ui.TripKitUI;
import com.skedgo.tripkit.ui.core.RxPicassoKt;
import com.skedgo.tripkit.ui.core.UnableToFetchBitmapError;
import com.skedgo.tripkit.ui.map.*;
import com.skedgo.tripkit.ui.map.adapter.SegmentInfoWindowAdapter;
import com.skedgo.tripkit.ui.map.adapter.ServiceStopInfoWindowAdapter;
import com.skedgo.tripkit.ui.map.adapter.SimpleInfoWindowAdapter;
import com.squareup.picasso.Picasso;
import dagger.Lazy;
import io.reactivex.schedulers.Schedulers;
import kotlin.Pair;
import com.skedgo.tripkit.logging.ErrorLogger;
import com.skedgo.tripkit.routing.TripSegment;

import javax.inject.Inject;
import java.util.*;

import static com.skedgo.rxtry.ToTryTransformers.toTrySingle;
import static com.skedgo.tripkit.common.util.TransportModeUtils.getIconUrlForModeInfo;
import static com.skedgo.tripkit.ui.routing.UpdateCameraKt.updateCamera;
import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.computation;

public class TripResultMapFragment extends LocationEnhancedMapFragment {
  public static String ARG_TRIP_GROUP_ID = "tripGroupId";

  private final HashMap<Marker, TripSegment> marker2SegmentCache = new LinkedHashMap<>();
  private final HashMap<Long, Marker> alertIdToMarkerCache = new LinkedHashMap<>();
  private final List<Polyline> tripLines = Collections.synchronizedList(new ArrayList<>());

  @Inject
  SegmentStopMarkerMaker segmentStopMarkerMaker;
  @Inject
  ServiceAlertMarkerMaker alertMarkerMaker;
  @Inject Picasso picasso;
  @Inject
  ServiceStopInfoWindowAdapter serviceStopCalloutAdapter;
  @Inject
  SegmentInfoWindowAdapter segmentCalloutAdapter;
  @Inject
  Lazy<TripVehicleMarkerCreator> vehicleMarkerCreatorLazy;
  @Inject Lazy<VehicleMarkerIconFetcher> vehicleMarkerIconFetcherLazy;
  @Inject Lazy<AlertMarkerIconFetcher> alertMarkerIconFetcherLazy;
  @Inject CreateSegmentMarkers createSegmentMarkers;
  @Inject Lazy<GetTripLine> getTripLineLazy;
  @Inject TripResultMapViewModel viewModel;
  @Inject
  ErrorLogger errorLogger;
  @Inject SegmentMarkerIconMaker segmentMarkerIconMaker;


  public void setTripGroupId(String tripGroupId) {
    viewModel.setTripGroupId(tripGroupId);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    segmentCalloutAdapter.setSegmentCache(marker2SegmentCache);
    setMyLocationEnabled();
  }

  @SuppressWarnings("MissingPermission") @Override public void onStart() {
    super.onStart();
  }

  @Override public void onStop() {
    super.onStop();
  }

  @Override
  public void onAttach(Context context){
    TripKitUI.getInstance().tripDetailsComponent().inject(this);
    super.onAttach(context);
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    if (getArguments() != null && getArguments().containsKey(ARG_TRIP_GROUP_ID)) {
      String tripGroupId = getArguments().getString(ARG_TRIP_GROUP_ID, "");
      viewModel.setTripGroupId(tripGroupId);
    }

    whenSafeToUseMap(map -> {

      final MarkerManager markerManager = new MarkerManager(map);

      final MarkerManager.Collection travelledStopMarkers = markerManager.newCollection("travelledStopMarkers");
      travelledStopMarkers.setOnInfoWindowAdapter(serviceStopCalloutAdapter);
      travelledStopMarkers.setOnInfoWindowClickListener(marker -> {
//        final StopMarkerViewModel stopMarkerViewModel = (StopMarkerViewModel) marker.getTag();
//        if (stopMarkerViewModel != null) {
//          askToGetOffOrGetOn(stopMarkerViewModel);
//        }
      });

      final MarkerManager.Collection nonTravelledStopMarkers = markerManager.newCollection("nonTravelledStopMarkers");
      nonTravelledStopMarkers.setOnInfoWindowAdapter(serviceStopCalloutAdapter);

      final MarkerManager.Collection vehicleMarkers = markerManager.newCollection("vehicleMarkers");

      final MarkerManager.Collection segmentMarkers = markerManager.newCollection("segmentMarkers");
      segmentMarkers.setOnInfoWindowAdapter(segmentCalloutAdapter);
      final GoogleMap.OnInfoWindowClickListener listener = marker -> {
        final TripSegment segment = (TripSegment) marker.getTag();
//        bus.post(new SegmentInfoWindowClickEvent(segment));
      };
      segmentMarkers.setOnInfoWindowClickListener(listener);

      final MarkerManager.Collection alertMarkers = markerManager.newCollection("alertMarkers");
      alertMarkers.setOnInfoWindowClickListener(listener);

      map.setOnInfoWindowClickListener(markerManager);
      map.setInfoWindowAdapter(new SimpleInfoWindowAdapter() {
        @Override
        public View getInfoContents(Marker marker) {
          return markerManager.getInfoContents(marker);
        }
      });
      map.setIndoorEnabled(false);
      map.getUiSettings().setRotateGesturesEnabled(true);
      viewModel.getSegments()
          .flatMap(it -> createSegmentMarkers.execute(it))
          .subscribeOn(computation())
          .observeOn(mainThread())
              .compose(bindToLifecycle())
          .subscribe(
              it -> showSegmentMarkers(it, segmentMarkers),
              errorLogger::trackError);

      viewModel.getVehicleMarkerViewModels()
              .compose(bindToLifecycle())
          .subscribe(
              it -> showVehicleMarkers(it, vehicleMarkers),
              errorLogger::trackError);

      viewModel.getAlertMarkerViewModels()
              .compose(bindToLifecycle())
          .subscribe(
              it -> showAlertMarkers(it, alertMarkers),
              errorLogger::trackError);

      viewModel.getTravelledStopMarkerViewModels()
              .compose(bindToLifecycle())
          .subscribe(
              it -> showStopMarkers(it, travelledStopMarkers),
              errorLogger::trackError);

      viewModel.getNonTravelledStopMarkerViewModels()
              .compose(bindToLifecycle())
          .subscribe(
              it -> showStopMarkers(it, nonTravelledStopMarkers),
              errorLogger::trackError);


      viewModel.getSegments()
          .flatMap(segments -> getTripLineLazy.get().execute(segments))
          .subscribeOn(computation())
          .observeOn(mainThread())
              .compose(bindToLifecycle())
          .subscribe(
              polylineOptionsList -> showTripLines(map, polylineOptionsList),
              errorLogger::trackError);

      viewModel.onTripSegmentTapped()
          .observeOn(mainThread())
          .compose(bindToLifecycle())
          .subscribe(x -> {
            map.animateCamera(x.getFirst());
            showMarkerForSegment(map, x.getSecond());
          }, errorLogger::trackError);

//      viewModel.getTripCameraUpdate()
//              .compose(bindToLifecycle())
//              .subscribe(
//                      cameraUpdate -> updateCamera(map, cameraUpdate),
//                      errorLogger::trackError);

      CameraUpdate update = viewModel.getCameraUpdate();
      if (update != null) {
        updateCamera(map, new MapCameraUpdate.Move(update));
      }
    });
  }


  private synchronized void showTripLines(GoogleMap map, List<PolylineOptions> polylineOptionsList) {
    // To remove old lines before adding new ones.
    for (Polyline line : tripLines) {
      line.remove();
    }
    tripLines.clear();

    for (PolylineOptions polylineOption : polylineOptionsList) {
      tripLines.add(map.addPolyline(polylineOption));
    }
  }

  private synchronized void showVehicleMarkers(
      List<VehicleMarkerViewModel> vehicleMarkerViewModels,
      MarkerManager.Collection vehicleMarkers) {
    vehicleMarkers.clear();

    for (VehicleMarkerViewModel viewModel : vehicleMarkerViewModels) {
      createVehicleMarker(viewModel.getSegment(), vehicleMarkers);
    }
  }

  private void createVehicleMarker(TripSegment segment, MarkerManager.Collection vehicleMarkers) {
    final MarkerOptions vehicleMarkerOptions = vehicleMarkerCreatorLazy.get().call(getResources(), segment);
    final Marker marker = vehicleMarkers.addMarker(vehicleMarkerOptions);
    vehicleMarkerIconFetcherLazy.get().call(marker, segment.getRealTimeVehicle());
  }

  private void showAlertMarkers(List<AlertMarkerViewModel> alertMarkerViewModels, MarkerManager.Collection alertMarkers) {
    alertMarkers.clear();
    alertIdToMarkerCache.clear();

    for (AlertMarkerViewModel alertMarkerViewModel : alertMarkerViewModels) {
      final TripSegment segment = alertMarkerViewModel.getSegment();
      final RealtimeAlert alert = alertMarkerViewModel.getAlert();

      final Marker marker = alertMarkers.addMarker(alertMarkerMaker.make(alert));
      marker.setTag(segment);

      alertMarkerIconFetcherLazy.get().call(marker, alert);
      alertIdToMarkerCache.put(alert.remoteHashCode(), marker);
    }
  }


  @SuppressWarnings("MissingPermission") private void setMyLocationEnabled() {
//    ((BaseActivity) getActivity())
//        .checkSelfPermissionReactively(Manifest.permission.ACCESS_FINE_LOCATION)
//        .filter(result -> result)
//        .subscribe(__ -> whenSafeToUseMap(map -> map.setMyLocationEnabled(true)));
  }

  private void askToGetOffOrGetOn(StopMarkerViewModel stopMarkerViewModel) {
    new AlertDialog.Builder(getActivity())
        .setMessage(R.string.get_on_or_off_here)
        .setNegativeButton(R.string.cancel, null)
//        .setNeutralButton(R.string.get_off, (dialog, which) ->
//            bus.post(new ChangeStopEvent(
//                stopMarkerViewModel.getTrip(),
//                stopMarkerViewModel.getSegment(),
//                stopMarkerViewModel.getStop(),
//                ChangeStopEvent.Type.GetOff
//            ))
//        )
//        .setPositiveButton(R.string.get_on, (dialog, which) ->
//            bus.post(new ChangeStopEvent(
//                stopMarkerViewModel.getTrip(),
//                stopMarkerViewModel.getSegment(),
//                stopMarkerViewModel.getStop(),
//                ChangeStopEvent.Type.GetOn
//            ))
//        )
        .create()
        .show();
  }

  private synchronized void showSegmentMarkers(
      List<Pair<TripSegment, MarkerOptions>> segmentMarkerViewModels,
      MarkerManager.Collection segmentMarkers) {
    segmentMarkers.clear();

    for (Pair<TripSegment, MarkerOptions> viewModel : segmentMarkerViewModels) {
      showSegmentMarker(viewModel, segmentMarkers);
    }
  }

  private void showSegmentMarker(
      Pair<TripSegment, MarkerOptions> segmentMarkerViewModel,
      MarkerManager.Collection segmentMarkers) {
    final TripSegment segment = segmentMarkerViewModel.getFirst();
    final Marker marker = segmentMarkers.addMarker(segmentMarkerViewModel.getSecond());
    marker.setTag(segment);

    final String url = getIconUrlForModeInfo(getResources(), segment.getModeInfo());
    if (url != null) {
      RxPicassoKt.fetchAsync(picasso, url)
          .map(it -> new BitmapDrawable(getResources(), it))
          .map(it -> segmentMarkerIconMaker.make(segment, it))
          .compose(toTrySingle(error -> error instanceof UnableToFetchBitmapError))
          .toObservable()
          .compose(bindToLifecycle())
          .subscribeOn(Schedulers.io())
          .observeOn(mainThread())
          .subscribe(new SetSegmentMarkerIcon(marker), errorLogger::trackError);
    }
  }

  private void showStopMarkers(
      List<StopMarkerViewModel> stopMarkerViewModels,
      MarkerManager.Collection stopMarkers) {
    // To clear old stop markers before adding new ones.
    stopMarkers.clear();

    // To add new stop markers.
    for (StopMarkerViewModel viewModel : stopMarkerViewModels) {
      final Marker marker = stopMarkers.addMarker(segmentStopMarkerMaker.make(viewModel));
      marker.setTag(viewModel);
    }
  }

  private void showMarkerForSegment(GoogleMap map, long segmentId) {
    Set<Map.Entry<Marker, TripSegment>> entrySet = marker2SegmentCache.entrySet();
    for (Map.Entry<Marker, TripSegment> entry : entrySet) {
      final TripSegment markerSegment = entry.getValue();
      final Marker marker = entry.getKey();
      if (markerSegment != null && markerSegment.getId() == segmentId) {
        marker.showInfoWindow();
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 15.0f));
      }
    }
  }

  private void animateCameraToAlert(GoogleMap map, RealtimeAlert alert) {
    final Location location = alert.location();
    if (location != null) {
      final LatLng target = new LatLng(
          location.getLat(),
          location.getLon()
      );

      final Marker marker = alertIdToMarkerCache.get(alert.remoteHashCode());
      if (marker != null) {
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 15.0f));
        marker.showInfoWindow();
      }
    }
  }

  public static class Builder {
    private String tripGroupId;

    public Builder withTripGroupId(String tripGroupId) {
      this.tripGroupId = tripGroupId;
      return this;
    }

    public TripResultMapFragment build() {
      Bundle bundle = new Bundle();
      bundle.putString(ARG_TRIP_GROUP_ID, tripGroupId);
      TripResultMapFragment fragment = new TripResultMapFragment();
      fragment.setArguments(bundle);
      return fragment;
    }
  }
}
