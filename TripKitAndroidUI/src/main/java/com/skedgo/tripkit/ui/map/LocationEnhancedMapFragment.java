package com.skedgo.tripkit.ui.map;

import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.skedgo.tripkit.ui.R;
import com.skedgo.tripkit.ui.core.permissions.*;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.jetbrains.annotations.Nullable;
import com.skedgo.tripkit.logging.ErrorLogger;

import javax.inject.Inject;

public class LocationEnhancedMapFragment extends BaseMapFragment {
  @Inject
  Observable<Location> locationStream;
  @Inject
  ErrorLogger errorLogger;

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    whenSafeToUseMap(this::applyDefaultSettings);
  }


  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    final ViewGroup originalView = (ViewGroup) super.onCreateView(inflater, container, savedInstanceState);
    if (originalView != null && getActivity() instanceof CanRequestPermission) {
      final View myLocationButton = inflater.inflate(R.layout.view_my_location_button, originalView, false);
      myLocationButton.setOnClickListener(__ -> animateToMyLocation());
      originalView.addView(myLocationButton);
    }

    return originalView;
  }

  protected void animateToMyLocation() {
    if (getActivity() == null) {
      return;
    }
    requestLocationPermission2()
        .flatMapObservable(result -> {
          if (result instanceof PermissionResult.Granted) {
            return locationStream;
          } else {
            return Observable.error(new PermissionDenialError());
          }
        })
        .take(1).singleOrError()
        .map(location -> new LatLng(location.getLatitude(), location.getLongitude()))
        .map(CameraUpdateFactory::newLatLng)
        .compose(bindToLifecycle())
        .subscribe(
            cameraUpdate -> whenSafeToUseMap(map -> map.animateCamera(cameraUpdate)),
            errorLogger::logError);
  }

  private void applyDefaultSettings(GoogleMap googleMap) {
    final UiSettings settings = googleMap.getUiSettings();
    settings.setMapToolbarEnabled(false);
    settings.setCompassEnabled(false);
    settings.setMyLocationButtonEnabled(false);
    settings.setZoomControlsEnabled(false);
//    googleMap.setMapType(SettingsFragment.Companion.getPersistentMapType(getActivity()));
    googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
//    googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.map_style));
  }

  @NonNull private Single<PermissionResult> requestLocationPermission2() {
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
}
