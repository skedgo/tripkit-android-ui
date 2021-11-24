package com.skedgo.tripkit.ui.map

import android.content.Context
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.araujo.jordan.excuseme.ExcuseMe
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.button.MaterialButton
import com.skedgo.tripkit.logging.ErrorLogger
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.core.permissions.*
import com.skedgo.tripkit.ui.core.permissions.PermissionResult.Granted
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

    fun setSettingsButtonVisibility(show: Boolean) {
        settingsButton?.visibility = if (show) {
            View.VISIBLE
        } else {
            View.GONE
        }
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

            /*
            val myLocationButton = inflater.inflate(R.layout.view_my_location_button, originalView, false)
            myLocationButton.setOnClickListener {  animateToMyLocation() }
            originalView.addView(myLocationButton)
            */
        }
        return originalView
    }

    open fun animateToMyLocation() {
        if (activity == null) {
            return
        }
        ExcuseMe.couldYouGive(this).permissionFor(android.Manifest.permission.ACCESS_FINE_LOCATION) {
            if (it.granted.contains(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                locationStream
                        .take(1)
                        .singleOrError()
                        .map { location: Location -> LatLng(location.latitude, location.longitude) }
                        .map { latLng: LatLng? -> CameraUpdateFactory.newLatLng(latLng) }
                        .subscribe(
                                { cameraUpdate: CameraUpdate? -> whenSafeToUseMap(Consumer { map: GoogleMap -> map.animateCamera(cameraUpdate) }) }) { error: Throwable? -> errorLogger!!.logError(error!!) }
                        .addTo(autoDisposable)
            }
        }

    }

    fun changeActionsAccessibilityFocus(canBeAccessed: Boolean) {
        settingsButton?.importantForAccessibility = if (canBeAccessed) 1 else 2
    }

    private fun applyDefaultSettings(googleMap: GoogleMap) {
        val settings = googleMap.uiSettings
        settings.isMapToolbarEnabled = false
        settings.isCompassEnabled = false
        settings.isMyLocationButtonEnabled = false
        settings.isZoomControlsEnabled = false
    }
}