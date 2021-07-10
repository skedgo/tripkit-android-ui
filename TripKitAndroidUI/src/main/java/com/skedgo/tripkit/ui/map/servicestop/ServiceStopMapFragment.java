package com.skedgo.tripkit.ui.map.servicestop;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProviders;

import com.gojuno.koptional.Some;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.*;
import com.skedgo.tripkit.common.model.ScheduledStop;
import com.skedgo.tripkit.common.model.ServiceStop;
import com.skedgo.tripkit.common.util.DateTimeFormats;
import com.skedgo.tripkit.common.util.StringUtils;
import com.skedgo.tripkit.data.regions.RegionService;
import com.skedgo.tripkit.routing.TripSegment;
import com.skedgo.tripkit.ui.R;
import com.skedgo.tripkit.ui.TripKitUI;
import com.skedgo.tripkit.ui.map.LocationEnhancedMapFragment;
import com.skedgo.tripkit.ui.map.SimpleCalloutView;
import com.skedgo.tripkit.ui.map.TimeLabelMaker;
import com.skedgo.tripkit.ui.map.VehicleMarkerIconCreator;
import com.skedgo.tripkit.ui.model.TimetableEntry;
import com.skedgo.tripkit.ui.realtime.RealTimeChoreographerViewModel;
import com.skedgo.tripkit.ui.realtime.RealTimeViewModelFactory;
import com.skedgo.tripkit.ui.servicedetail.GetStopDisplayText;
import com.skedgo.tripkit.ui.servicedetail.ServiceDetailFragment;
import com.skedgo.tripkit.ui.timetables.TimetableFragment;
import com.squareup.otto.Bus;

import dagger.Lazy;
import kotlin.Pair;

import org.jetbrains.annotations.NotNull;

import com.skedgo.tripkit.logging.ErrorLogger;
import com.skedgo.tripkit.routing.RealTimeVehicle;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;


public class ServiceStopMapFragment
        extends LocationEnhancedMapFragment
        implements GoogleMap.OnInfoWindowClickListener, GoogleMap.InfoWindowAdapter,
        TimetableFragment.OnTimetableEntrySelectedListener, ServiceDetailFragment.OnScheduledStopClickListener {
    private static final int LOADER_ID_STOPS = 0x01;

    @Inject
    RegionService regionService;
    @Inject
    Lazy<VehicleMarkerIconCreator> vehicleMarkerIconCreatorLazy;
    @Inject
    RealTimeViewModelFactory realTimeViewModelFactory;
    @Inject
    GetStopDisplayText getStopDisplayText;
    @Inject
    ErrorLogger errorLogger;
    @Inject
    ServiceStopMapViewModel viewModel;

    /* TODO: Replace with RxJava-based approach. */
    @Deprecated
    @Inject
    Bus bus;
    private ScheduledStop mStop;
    private TimetableEntry service;
    private Marker realTimeVehicleMarker;
    private HashMap<String, Marker> stopCodesToMarkerMap = new HashMap<>();

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TripKitUI.getInstance()
                .serviceStopMapComponent()
                .inject(this);

        final RealTimeChoreographerViewModel realTimeViewModel = ViewModelProviders.of(getActivity(), realTimeViewModelFactory)
                .get(RealTimeChoreographerViewModel.class);

        viewModel.setRealtimeViewModel(realTimeViewModel);
        TextView timeTextView = (TextView) getActivity().getLayoutInflater().inflate(R.layout.view_time_label, null);
        TimeLabelMaker timeLabelMaker = new TimeLabelMaker(timeTextView);
        ServiceStopMarkerCreator serviceStopMarkerCreator = new ServiceStopMarkerCreator(getActivity(), timeLabelMaker);
        viewModel.setServiceStopMarkerCreator(serviceStopMarkerCreator);

        setMyLocationEnabled();
    }

    @Override
    public void onStart() {
        super.onStart();
        bus.register(this);
        getAutoDisposable().add(viewModel.getDrawStops()
                .subscribe((newMarkerOptionsAndRemovedStopIdsPair) -> {
                    List<Pair<MarkerOptions, String>> newMarkerOptions = newMarkerOptionsAndRemovedStopIdsPair.getFirst();
                    Set<String> removedStopIds = newMarkerOptionsAndRemovedStopIdsPair.getSecond();
                    for (String id : removedStopIds) {
                        stopCodesToMarkerMap.get(id).remove();
                        stopCodesToMarkerMap.remove(id);
                    }
                    whenSafeToUseMap(googleMap -> {
                        for (Pair<MarkerOptions, String> markerOptionsAndStopCodes : newMarkerOptions) {
                            Marker marker = googleMap.addMarker(markerOptionsAndStopCodes.getFirst());
                            stopCodesToMarkerMap.put(markerOptionsAndStopCodes.getSecond(), marker);
                        }
                    });
                }));

        getAutoDisposable().add(viewModel.getViewPort()
                .subscribe(this::centerMapOver));

        List<Polyline> serviceLines = new ArrayList<>();

        getAutoDisposable().add(viewModel.getDrawServiceLine()
                .subscribe(polylineOptions -> whenSafeToUseMap(googleMap -> {
                    for (Polyline line : serviceLines) {
                        line.remove();
                    }
                    serviceLines.clear();
                    for (PolylineOptions polylineOption : polylineOptions) {
                        serviceLines.add(googleMap.addPolyline(polylineOption));
                    }
                })));

        getAutoDisposable().add(viewModel.getRealtimeVehicle()
                .subscribe(realTimeVehicleOptional -> {
                    if (realTimeVehicleOptional instanceof Some) {
                        setRealTimeVehicle(((Some<RealTimeVehicle>) realTimeVehicleOptional).getValue());
                    } else {
                        setRealTimeVehicle(null);
                    }
                }));
    }

    @Override
    public void onStop() {
        super.onStop();
        bus.unregister(this);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupMap();
        whenSafeToUseMap(map -> {
            if (mStop != null) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mStop.getLat(), mStop.getLon()), 15.0f));
            }
        });
    }

    @Override
    public View getInfoWindow(final Marker marker) {
        return null;
    }

    @Override
    public View getInfoContents(final Marker marker) {
        final SimpleCalloutView view = SimpleCalloutView.create(LayoutInflater.from(getActivity()));
        view.setTitle(marker.getTitle());
        view.setSnippet(marker.getSnippet());
        return view;
    }

    @Override
    public void onInfoWindowClick(final Marker marker) {
//    startActivity(StreetViewActivity.Intents.viewLocation(
//        getActivity(),
//        marker.getPosition().latitude,
//        marker.getPosition().longitude,
//        0
//    ));
    }

    private void setRealTimeVehicle(@Nullable RealTimeVehicle realTimeVehicle) {
        if (realTimeVehicleMarker != null) {
            realTimeVehicleMarker.remove();
        }

        if (realTimeVehicle == null) {
            return;
        }

        whenSafeToUseMap(map -> {
            if (realTimeVehicle.hasLocationInformation()) {
                if (service != null && TextUtils.equals(realTimeVehicle.getServiceTripId(), service.getServiceTripId())) {

                    service.setRealtimeVehicle(realTimeVehicle);
                    createVehicleMarker(realTimeVehicle);
                }
            }
        });
    }

    public void setService(TimetableEntry service) {
        viewModel.getService().accept(service);
        this.service = service;
    }

    public void setStop(ScheduledStop stop) {
        this.mStop = stop;
        this.viewModel.getStop().accept(stop);
    }

    private void createVehicleMarker(final RealTimeVehicle vehicle) {
        String title = null;
        if (TextUtils.isEmpty(service.getServiceNumber())) {
            title = "Your upcoming service";
        } else {
            if (mStop != null && mStop.getType() != null) {
                title = StringUtils.capitalizeFirst(mStop.getType().toString()) + " " + service.getServiceNumber();
            }

            if (TextUtils.isEmpty(title)) {
                title = "Service " + service.getServiceNumber();
            }
        }

        final int bearing = vehicle.getLocation() == null ? 0 : vehicle.getLocation().getBearing();
        final int color = service.getServiceColor() == null || service.getServiceColor().getColor() == Color.BLACK ? getResources().getColor(R.color.v4_color) : service.getServiceColor().getColor();
        final String text = TextUtils.isEmpty(service.getServiceNumber()) ? (mStop == null || mStop.getType() == null ? "" : StringUtils.capitalizeFirst(mStop.getType().toString())) : service.getServiceNumber();

        final Bitmap icon = vehicleMarkerIconCreatorLazy.get().call(bearing, color, text);
        final String markerTitle = title;

        final Context context = getActivity().getApplicationContext();
        whenSafeToUseMap(map -> {
            final long millis = vehicle.getLastUpdateTime() * 1000;
            final String time = DateTimeFormats.printTime(context, millis, null);
            final String snippet;
            if (TextUtils.isEmpty(vehicle.getLabel())) {
                snippet = "Real-time location as at " + time;
            } else {
                snippet = "Vehicle " + vehicle.getLabel() + " location as at " + time;
            }
            realTimeVehicleMarker = map.addMarker(
                    new MarkerOptions()
                            .icon(BitmapDescriptorFactory.fromBitmap(icon))
                            .rotation(bearing)
                            .flat(true)
                            .anchor(0.5f, 0.5f)
                            .title(markerTitle)
                            .snippet(snippet)
                            .position(new LatLng(vehicle.getLocation().getLat(), vehicle.getLocation().getLon()))
                            .draggable(false)
            );
        });
    }

    /**
     * To zoom in/out to view the whole trip or the whole service line.
     */
    private void centerMapOver(final List<LatLng> coordinates) {
        if (coordinates != null && coordinates.size() > 0) {
            whenSafeToUseMap(map -> {
                final LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (LatLng coordinate : coordinates) {
                    builder.include(coordinate);
                }

                map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 320));
            });
        }
    }

    @SuppressWarnings("MissingPermission")
    private void setMyLocationEnabled() {
//    ((BaseActivity) getActivity())
//        .checkSelfPermissionReactively(Manifest.permission.ACCESS_FINE_LOCATION)
//        .filter(result -> result)
//        .subscribe(__ -> whenSafeToUseMap(map -> map.setMyLocationEnabled(true)));
    }

    @SuppressLint("MissingPermission")
    private void setupMap() {
        whenSafeToUseMap(map -> {
            map.setOnInfoWindowClickListener(ServiceStopMapFragment.this);
            map.setInfoWindowAdapter(ServiceStopMapFragment.this);
            map.setIndoorEnabled(false);

            map.getUiSettings().setRotateGesturesEnabled(false);
            getActivity().supportInvalidateOptionsMenu();
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        viewModel.onCleared();
    }

    @Override
    public void onTimetableEntrySelected(TripSegment segment, @NotNull TimetableEntry service, @NotNull ScheduledStop stop, long minStartTime) {
        setService(service);
    }

    @Override
    public void onScheduledStopClicked(@NotNull ServiceStop stop) {
        if (!TextUtils.isEmpty(stop.getCode())) {
            final Marker marker = stopCodesToMarkerMap.get(stop.getCode());
            if (marker != null) {
                whenSafeToUseMap(googleMap -> {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
                    marker.showInfoWindow();
                });
            }
        }
    }
}