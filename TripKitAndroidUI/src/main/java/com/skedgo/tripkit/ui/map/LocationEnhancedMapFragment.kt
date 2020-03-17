package com.skedgo.tripkit.ui.map

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.button.MaterialButton
import com.skedgo.tripkit.logging.ErrorLogger
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.permissions.*
import com.skedgo.tripkit.ui.core.permissions.PermissionResult.Granted
import com.skedgo.tripkit.ui.map.home.TripKitMapFragment
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.Consumer
import javax.inject.Inject

open class LocationEnhancedMapFragment : BaseMapFragment() {
    @Inject
    lateinit var locationStream: Observable<Location>
    @Inject
    lateinit var errorLogger: ErrorLogger

    private var settingsButton: MaterialButton? = null

    fun setSettingsIconOnClickListener(listener: View.OnClickListener?) {
        settingsButton?.setOnClickListener(listener)
        settingsButton?.visibility = View.VISIBLE
    }

    fun setSettingsIconOnClickListener(l: (View) -> Unit) {
        settingsButton?.setOnClickListener(l)
        settingsButton?.visibility = View.VISIBLE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        whenSafeToUseMap(Consumer { googleMap: GoogleMap -> applyDefaultSettings(googleMap) })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val originalView = super.onCreateView(inflater, container, savedInstanceState) as ViewGroup?
        // Add the settings button
        if (originalView != null) {

            settingsButton = inflater.inflate(R.layout.map_settings_button, originalView, false) as MaterialButton
            settingsButton!!.visibility = View.GONE
            originalView.addView(settingsButton)

            // And the location button if possible
            if (activity is CanRequestPermission) {
                val myLocationButton = inflater.inflate(R.layout.view_my_location_button, originalView, false)
                myLocationButton.setOnClickListener {  animateToMyLocation() }
                originalView.addView(myLocationButton)
            }
        }
        return originalView
    }

    protected open fun animateToMyLocation() {
        if (activity == null) {
            return
        }
        requestLocationPermission2()
                .flatMapObservable { result: PermissionResult? ->
                    if (result is Granted) {
                        return@flatMapObservable locationStream
                    } else {
                        return@flatMapObservable Observable.error<Location>(PermissionDenialError())
                    }
                }
                .take(1).singleOrError()
                .map { location: Location -> LatLng(location.latitude, location.longitude) }
                .map { latLng: LatLng? -> CameraUpdateFactory.newLatLng(latLng) }
                .compose(bindToLifecycle())
                .subscribe(
                        { cameraUpdate: CameraUpdate? -> whenSafeToUseMap(Consumer { map: GoogleMap -> map.animateCamera(cameraUpdate) }) }) { error: Throwable? -> errorLogger!!.logError(error!!) }
    }

    private fun applyDefaultSettings(googleMap: GoogleMap) {
        val settings = googleMap.uiSettings
        settings.isMapToolbarEnabled = false
        settings.isCompassEnabled = false
        settings.isMyLocationButtonEnabled = false
        settings.isZoomControlsEnabled = false
        //    googleMap.setMapType(SettingsFragment.Companion.getPersistentMapType(getActivity()));
        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        //    googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.map_style));
    }

    private fun requestLocationPermission2(): Single<PermissionResult> {
        return (activity as CanRequestPermission?)!!
                .requestPermissions(
                        PermissionsRequest.Location(),
                        activity!!.showGenericRationale(null, getString(R.string.access_to_location_services_required_dot)),
                        activity!!.dealWithNeverAskAgainDenial(getString(R.string.access_to_location_services_required_dot))
                )
    }
}