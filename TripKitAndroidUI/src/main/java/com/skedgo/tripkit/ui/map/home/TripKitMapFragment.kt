package com.skedgo.tripkit.ui.map.home

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.araujo.jordan.excuseme.ExcuseMe
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener
import com.google.android.gms.maps.GoogleMap.OnCameraIdleListener
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener
import com.google.android.gms.maps.GoogleMap.OnPoiClickListener
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PointOfInterest
import com.google.maps.android.collections.MarkerManager
import com.skedgo.rxtry.subscribeWithErrorHandling
import com.skedgo.tripkit.AndroidGeocoder
import com.skedgo.tripkit.TripKitConstants.Companion.PREF_NAME_APP
import com.skedgo.tripkit.account.data.Polygon
import com.skedgo.tripkit.common.model.TransportMode
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.common.model.region.Region
import com.skedgo.tripkit.common.model.region.Region.City
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.tripplanner.NonCurrentType
import com.skedgo.tripkit.tripplanner.PinUpdate
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.core.module.HomeMapFragmentModule
import com.skedgo.tripkit.ui.data.toLocation
import com.skedgo.tripkit.ui.map.CarParkPOILocation
import com.skedgo.tripkit.ui.map.FacilityPOILocation
import com.skedgo.tripkit.ui.map.GenericIMapPoiLocation
import com.skedgo.tripkit.ui.map.IMapPoiLocation
import com.skedgo.tripkit.ui.map.StopPOILocation
import com.skedgo.tripkit.ui.map.LocationEnhancedMapFragment
import com.skedgo.tripkit.ui.map.MapCameraController
import com.skedgo.tripkit.ui.map.MapMarkerUtils
import com.skedgo.tripkit.ui.map.StopMarkerIconFetcher
import com.skedgo.tripkit.ui.map.TripLocationMarkerCreator
import com.skedgo.tripkit.ui.map.adapter.CityInfoWindowAdapter
import com.skedgo.tripkit.ui.map.adapter.NoActionWindowAdapter
import com.skedgo.tripkit.ui.map.adapter.POILocationInfoWindowAdapter
import com.skedgo.tripkit.ui.map.adapter.ViewableInfoWindowAdapter
import com.skedgo.tripkit.ui.map.convertToDomainLatLngBounds
import com.skedgo.tripkit.ui.map.home.ViewPort.CloseEnough
import com.skedgo.tripkit.ui.map.home.ViewPort.NotCloseEnough
import com.skedgo.tripkit.ui.tracking.EventTracker
import com.skedgo.tripkit.ui.trip.options.SelectionType
import com.skedgo.tripkit.ui.utils.APP_PREF_CLEAR_CAR_PODS_ONCE
import com.skedgo.tripkit.ui.utils.APP_PREF_DEACTIVATED
import com.skedgo.tripkit.ui.utils.KEY_APP_PREF
import com.skedgo.tripkit.ui.utils.getVersionCode
import com.skedgo.tripkit.ui.utils.isNetworkConnected
import com.squareup.otto.Bus
import dagger.Lazy
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import java.util.LinkedList
import javax.inject.Inject

/**
 * A map component for an app. It automatically integrates with SkedGo's backend, display transit information without
 * any additional intervention.
 *
 * Being a fragment, it can very easily be added to an activity's layout.
 *
 * <pre> `<fragment
 * android:layout_width="match_parent"
 * android:layout_height="match_parent"
 * android:id="@+id/map"
 * android:name="com.skedgo.tripkit.ui.map.home.TripKitMapFragment"/> ` </pre>
 *
 * Your app **must** provide a TripGo API token as `R.string.skedgo_api_key`.
 *
 */
class TripKitMapFragment : LocationEnhancedMapFragment(), OnInfoWindowClickListener,
    OnMapLongClickListener, OnPoiClickListener, OnCameraChangeListener, OnMarkerClickListener,
    OnCameraIdleListener {
    /* TODO: Replace with RxJava-based approach. */
    @Deprecated("")
    @Inject
    lateinit var bus: Bus

    @Inject
    lateinit var viewModel: MapViewModel

    @Inject
    lateinit var regionService: RegionService

    @Inject
    lateinit var cameraController: MapCameraController

    @Inject
    lateinit var tripLocationMarkerCreator: TripLocationMarkerCreator

    @Inject
    lateinit var cityInfoWindowAdapter: CityInfoWindowAdapter

    @Inject
    lateinit var myLocationWindowAdapter: NoActionWindowAdapter

    @Inject
    lateinit var stopMarkerIconFetcherLazy: Lazy<StopMarkerIconFetcher>

    @Inject
    lateinit var preferences: SharedPreferences

    @Inject
    lateinit var eventTracker: EventTracker

    private val appPreferences: SharedPreferences by lazy {
        requireContext().getSharedPreferences(PREF_NAME_APP, Application.MODE_PRIVATE)
    }

    private val cityMarkerMap = HashMap<String, Marker>()
    private var regions: List<Region> = LinkedList()
    private var cityIcon: BitmapDescriptor? = null
    private var infoWindowAdapter: InfoWindowAdapter? = null
    private var myLocationMarker: Marker? = null
    private var markerManager: MarkerManager? = null
    private var poiMarkers: MarkerManager.Collection? = null
    private var cityMarkers: MarkerManager.Collection? = null
    private var tripLocationMarkers: MarkerManager.Collection? = null
    private var departureMarkers: MarkerManager.Collection? = null
    private var arrivalMarkers: MarkerManager.Collection? = null
    private var currentLocationMarkers: MarkerManager.Collection? = null
    private var tipTapIsDeleted = false
    private var tipZoomIsDeleted = false
    private var checkZoomOutFlag = false
    private var map: GoogleMap? = null

    private var fromMarker: Marker? = null
    private var toMarker: Marker? = null
    private var longPressMarker: Marker? = null
    private lateinit var geocoder: AndroidGeocoder

    private var contributor: TripKitMapContributor? = null

    // There doesn't seem to be a way to show an info window when a POI is clicked, so work-around that
    // by using an invisible marker on the map that is moved to the POI's location when clicked.
    private var poiMarker: Marker? = null

    private var transportModes: List<TransportMode>? = null

    var enablePinLocationOnClick: Boolean = false
    var pinnedDepartureLocationOnClickMarker: Marker? = null
    var pinnedDepartureLocation: Location? = null
    var pinnedOriginLocationOnClickMarker: Marker? = null
    var pinnedOriginLocation: Location? = null
    var pinLocationSelectedListener: ((Location, Int) -> Unit)? =
        null //for type, 0 = from and 1 = to
    var appDeactivatedListener: (() -> Unit)? = null

    /**
     * When an icon in the map is clicked, an information window is displayed. When that information window
     * is clicked, this interface is used as a callback to notify the app of the click.
     *
     */
    interface OnInfoWindowClickListener {
        /**
         * Called when an info window is clicked.
         *
         * @param location The location represented by the info window that was clicked
         */
        fun onInfoWindowClick(location: Location?)
    }

    private var onInfoWindowClickListener: OnInfoWindowClickListener? = null
    fun setOnInfoWindowClickListener(listener: OnInfoWindowClickListener?) {
        onInfoWindowClickListener = listener
    }

    fun setOnInfoWindowClickListener(listener: (Location?) -> Unit) {
        onInfoWindowClickListener = object : TripKitMapFragment.OnInfoWindowClickListener {
            override fun onInfoWindowClick(location: Location?) {
                listener(location)
            }
        }
    }

    fun refreshMap(map: GoogleMap) {
        initMap(map, false)
    }

    interface OnZoomLevelChangedListener {
        fun onZoomLevelChanged(zoomLevel: Float)
    }

    private var onZoomLevelChangedListener: OnZoomLevelChangedListener? = null
    fun setOnZoomLevelChangedListener(listener: OnZoomLevelChangedListener?) {
        onZoomLevelChangedListener = listener
    }

    fun setOnZoomLevelChangedListener(listener: (Float) -> Unit) {
        onZoomLevelChangedListener = object : OnZoomLevelChangedListener {
            override fun onZoomLevelChanged(zoomLevel: Float) {
                listener(zoomLevel)
            }

        }
    }

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().homeMapFragmentComponent(HomeMapFragmentModule(this)).inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //TODO remove on future releases(version > 75).
        val prefs = requireContext().getSharedPreferences(
            KEY_APP_PREF,
            Context.MODE_PRIVATE
        )
        if (context?.getVersionCode() == 75L && !prefs.getBoolean(
                APP_PREF_CLEAR_CAR_PODS_ONCE,
                false
            )
        ) {
            viewModel.clearCarPods()
            prefs.edit().putBoolean(APP_PREF_CLEAR_CAR_PODS_ONCE, true).apply()
        }

        geocoder = AndroidGeocoder(requireContext())

        // Restore state if available
        savedInstanceState?.let { bundle ->
            restoreMapState(bundle)
        }

        getMapAsync { map ->
            initFromAndToMarkers(map)
            map.setOnCameraIdleListener(this)
            lastZoomLevel = map.cameraPosition.zoom
        }

        whenSafeToUseMap(Consumer { map: GoogleMap ->
            this.map = map
            map.uiSettings.isCompassEnabled = true
            initMarkerCollections(map)
            initMap(map)
            contributor?.safeToUseMap(requireContext(), map)
        })
        initStuff()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        
        // Save map camera state if available
        map?.let { googleMap ->
            val cameraPosition = googleMap.cameraPosition
            outState.putDouble(KEY_MAP_CAMERA_LAT, cameraPosition.target.latitude)
            outState.putDouble(KEY_MAP_CAMERA_LNG, cameraPosition.target.longitude)
            outState.putFloat(KEY_MAP_CAMERA_ZOOM, cameraPosition.zoom)
            outState.putFloat(KEY_MAP_CAMERA_BEARING, cameraPosition.bearing)
            outState.putFloat(KEY_MAP_CAMERA_TILT, cameraPosition.tilt)
            
            // Save visible region bounds
            try {
                val visibleRegion = googleMap.projection.visibleRegion.latLngBounds
                outState.putDouble(KEY_MAP_VISIBLE_BOUNDS_NE_LAT, visibleRegion.northeast.latitude)
                outState.putDouble(KEY_MAP_VISIBLE_BOUNDS_NE_LNG, visibleRegion.northeast.longitude)
                outState.putDouble(KEY_MAP_VISIBLE_BOUNDS_SW_LAT, visibleRegion.southwest.latitude)
                outState.putDouble(KEY_MAP_VISIBLE_BOUNDS_SW_LNG, visibleRegion.southwest.longitude)
            } catch (e: Exception) {
                // Projection may not be available yet
            }
        }
        
        // Save marker state
        outState.putBoolean(KEY_SHOW_MARKERS, viewModel.showMarkers.get())
        
        // Save last zoom level
        outState.putFloat(KEY_LAST_ZOOM_LEVEL, lastZoomLevel)
        
        // Save contributor state if available
        contributor?.let { contributor ->
            outState.putString(KEY_CONTRIBUTOR_CLASS, contributor.javaClass.simpleName)
        }
    }

    /**
     * Restore map state from saved instance state
     */
    private fun restoreMapState(bundle: Bundle) {
        // Store saved state for restoration when map is ready
        if (bundle.containsKey(KEY_MAP_CAMERA_LAT)) {
            val latitude = bundle.getDouble(KEY_MAP_CAMERA_LAT)
            val longitude = bundle.getDouble(KEY_MAP_CAMERA_LNG)
            val zoom = bundle.getFloat(KEY_MAP_CAMERA_ZOOM)
            val bearing = bundle.getFloat(KEY_MAP_CAMERA_BEARING)
            val tilt = bundle.getFloat(KEY_MAP_CAMERA_TILT)
            
            // Restore camera position when map is ready
            whenSafeToUseMap(Consumer { map: GoogleMap ->
                val cameraPosition = CameraPosition.Builder()
                    .target(LatLng(latitude, longitude))
                    .zoom(zoom)
                    .bearing(bearing)
                    .tilt(tilt)
                    .build()
                map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            })
        }
        
        // Restore marker visibility
        val showMarkers = bundle.getBoolean(KEY_SHOW_MARKERS, true)
        viewModel.showMarkers.set(showMarkers)
        
        // Restore last zoom level
        lastZoomLevel = bundle.getFloat(KEY_LAST_ZOOM_LEVEL, lastZoomLevel)
    }

    fun setContributor(newContributor: TripKitMapContributor?) {
        contributor?.cleanup()
        contributor = newContributor
        contributor?.let { contributor ->
            // Check if map is already ready and call safeToUseMap immediately
            if (map != null) {
                contributor.safeToUseMap(requireContext(), map!!)
            } else {
                // Map is not ready yet, wait for it
                whenSafeToUseMap(Consumer { map: GoogleMap ->
                    contributor.safeToUseMap(requireContext(), map)
                })
            }
        }
    }

    fun getContributor(): TripKitMapContributor? = contributor

    override fun onResume() {
        super.onResume()
        viewModel.getOriginPinUpdate()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWithErrorHandling { pinUpdate: PinUpdate -> updateDepartureMarker(pinUpdate) }
            .addTo(autoDisposable)
        viewModel.getDestinationPinUpdate()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWithErrorHandling { pinUpdate: PinUpdate -> updateArrivalMarker(pinUpdate) }
            .addTo(autoDisposable)
        viewModel.myLocation
            .subscribeOn(AndroidSchedulers.mainThread())
            .subscribe({ myLocation: Location -> showMyLocation(myLocation) }) { error: Throwable? ->
                errorLogger!!.trackError(
                    error!!
                )
            }
            .addTo(autoDisposable)

        viewModel.myLocationError
            .subscribeOn(AndroidSchedulers.mainThread())
            .subscribe({ _: Throwable? -> showMyLocationError() }) { error: Throwable? ->
                errorLogger!!.trackError(
                    error!!
                )
            }
            .addTo(autoDisposable)

        if (viewModel.showMarkers.get()) {
            loadMarkers()
        }

        if (!requireContext().isNetworkConnected() &&
            appPreferences.getBoolean(APP_PREF_DEACTIVATED, false)
        ) {
            appDeactivatedListener?.invoke()
        }
    }

    private fun loadMarkers() {
        viewModel.markers
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ (first, second) ->
                for ((first1, second1) in first) {
                    val marker = poiMarkers!!.addMarker(first1)
                    marker.tag = second1
                }
            }, {
                errorLogger.logError(it)
            })
            .addTo(autoDisposable)
    }

    override fun onPause() { //    bus.unregister(this);
        super.onPause()
        // Warning: If we obtain GoogleMap via getMapAsync() right here, when onPause() is called in
        // the case of removing the fragment, the callback of getMapAsync() won't be invoked.
        // However, for the case of switching to a different Activity, the callback of getMapAsync()
        // will be invoked.
        if (map != null) {
            viewModel.putCameraPosition(map!!.cameraPosition).subscribe()
        }
    }


    override fun onDestroy() {
        if (currentLocationMarkers != null) {
            currentLocationMarkers!!.clear()
        }
        viewModel.onCleared()
        super.onDestroy()
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        return markerManager!!.onMarkerClick(marker)
    }


    //null means remove all
    fun removePinnedLocationMarker(markers: List<Marker>? = null) {
        markers?.forEach {
            it.remove()
        } ?: kotlin.run {
            pinnedDepartureLocationOnClickMarker?.remove()
            pinnedOriginLocationOnClickMarker?.remove()

            pinnedDepartureLocationOnClickMarker = null
            pinnedDepartureLocation = null
            pinnedOriginLocationOnClickMarker = null
            pinnedOriginLocation = null
            pinForType = 1
        }
    }

    var pinForType = 1

    override fun onMapLongClick(latLng: LatLng) {
        if (enablePinLocationOnClick) {

            geocoder.getAddress(latLng.latitude, latLng.longitude)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .take(1)
                .subscribe({
                    val location = Location(
                        latLng.latitude,
                        latLng.longitude
                    ).apply {
                        address = it
                    }
                    if (pinForType == 0) {
                        pinnedOriginLocation = location
                    } else {
                        pinnedDepartureLocation = location
                    }
                    pinLocationSelectedListener
                        ?.invoke(
                            location,
                            pinForType
                        )
                }, { it.printStackTrace() })
                .addTo(autoDisposable)
        }
    }

    fun updatePinForType() {
        if (pinForType == 0) {
            pinForType = 1
        } else {
            pinForType = 0
        }
    }

    //0 = from, 1 = to
    fun addOriginDestinationMarker(type: Int, location: Location) {
        if (type == 0) {

            if (pinnedOriginLocationOnClickMarker != null &&
                pinnedOriginLocationOnClickMarker?.isVisible == true
            ) {
                removePinnedLocationMarker(listOf(pinnedOriginLocationOnClickMarker!!))
            }

            pinnedOriginLocationOnClickMarker = map?.addMarker(
                MarkerOptions()
                    .position(LatLng(location.lat, location.lon))
                    .icon(
                        BitmapDescriptorFactory.fromBitmap(
                            requireContext().getFromAndToMarkerBitmap(type)
                        )
                    )
            )
        } else {

            if (pinnedDepartureLocationOnClickMarker != null &&
                pinnedDepartureLocationOnClickMarker?.isVisible == true
            ) {
                removePinnedLocationMarker(listOf(pinnedDepartureLocationOnClickMarker!!))
            }

            pinnedDepartureLocationOnClickMarker = map?.addMarker(
                MarkerOptions()
                    .position(LatLng(location.lat, location.lon))
                    .icon(
                        BitmapDescriptorFactory.fromBitmap(
                            requireContext().getFromAndToMarkerBitmap(type)
                        )
                    )
            )
        }

        if ((pinnedOriginLocationOnClickMarker != null && pinnedOriginLocationOnClickMarker!!.isVisible)
            && pinnedDepartureLocationOnClickMarker != null && pinnedDepartureLocationOnClickMarker!!.isVisible
        ) {
            zoomOuToShowMarkers(
                pinnedOriginLocationOnClickMarker!!,
                pinnedDepartureLocationOnClickMarker!!
            )
        }
    }

    private fun zoomOuToShowMarkers(vararg markers: Marker) {
        val boundsBuilder = LatLngBounds.Builder()
        markers.forEach {
            boundsBuilder.include(it.position)
        }
        val width = resources.displayMetrics.widthPixels
        val height = resources.displayMetrics.heightPixels
        val padding = (height * 0.2).toInt()
        val cameraUpdate =
            CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), width, height, padding)
        map?.animateCamera(cameraUpdate)
    }

    override fun onPoiClick(pointOfInterest: PointOfInterest) {
        poiMarker?.let { marker ->
            marker.title = pointOfInterest.name
            marker.position = pointOfInterest.latLng
            marker.tag = GenericIMapPoiLocation(
                pointOfInterest,
                pointOfInterest.placeId,
                ViewableInfoWindowAdapter(layoutInflater)
            )
            if (markerManager != null) {
                marker.showInfoWindow()
            }
        }
    }

    /**
     * If we do not specify our own implementation,
     * GoogleMap will fall back to its default implementation for InfoWindowAdapter.
     */
    fun setInfoWindowAdapter(infoWindowAdapter: InfoWindowAdapter?) {
        this.infoWindowAdapter = infoWindowAdapter
    }

    override fun onInfoWindowClick(marker: Marker) {
        markerManager!!.onInfoWindowClick(marker)
    }

    override fun onCameraChange(position: CameraPosition) {
        if (!isAdded) { // To investigate further this scenario.
            return
        }
        //    boolean tipZoomToSeeTimetable = mPrefUtils.get(TooltipFragment.PREF_ZOOM_TO_SEE_TIMETABLE, false);
//    boolean tipTapPublicStops = mPrefUtils.get(TooltipFragment.PREF_TAP_PUBLIC_STOPS, false);
        val tipZoomToSeeTimetable = false
        val tipTapPublicStops = true
        if (map == null) {
            return
        }
        val visibleBounds = map!!.projection.visibleRegion.latLngBounds
        //    bus.post(new CameraChangeEvent(position, visibleBounds));
//reason to keep zoomLevel is because it's used in so many loader classes
        val zoomLevel = ZoomLevel.fromLevel(position.zoom)
        if (zoomLevel != null) {
            if (!tipZoomIsDeleted && tipTapPublicStops && checkZoomOutFlag) {
                //        bus.post(new TooltipFragment.TooltipClose(TooltipFragment.PREF_ZOOM_TO_SEE_TIMETABLE));
                tipZoomIsDeleted = true
            }
            if (!tipTapPublicStops) {
                //        bus.post(new RequestShowTip(TooltipFragment.PREF_TAP_PUBLIC_STOPS, getString(R.string.tap_public_transport_stops_for_access_to_timetable)));
            }
        } else {
            if (!tipTapIsDeleted) {
                //        bus.post(new TooltipFragment.TooltipClose(TooltipFragment.PREF_TAP_PUBLIC_STOPS));
                tipTapIsDeleted = true
            }
            if (!tipZoomToSeeTimetable) {
                //        bus.post(new RequestShowTip(TooltipFragment.PREF_ZOOM_TO_SEE_TIMETABLE, getString(R.string.zoom_into_map_to_view_public_transport_stops)));
                checkZoomOutFlag = true
            }
        }
        if (zoomLevel != null) {
            viewModel.onViewPortChanged(
                CloseEnough(
                    position.zoom,
                    visibleBounds.convertToDomainLatLngBounds()
                )
            )
        } else {
            viewModel.onViewPortChanged(
                NotCloseEnough(
                    position.zoom,
                    visibleBounds.convertToDomainLatLngBounds()
                )
            )
        }

        if (position.zoom <= ZoomLevel.ZOOM_VALUE_TO_SHOW_CITIES) {
            showCities(map!!, regions)
        } else {
            removeAllCities()
        }
    }

    fun moveToLatLng(latLng: com.skedgo.geocoding.LatLng) {
        whenSafeToUseMap(Consumer { map ->
            cameraController.moveToLatLng(map, LatLng(latLng.lat, latLng.lng))
        })
    }

    fun fetchCurrentPositionMarkers() {
        map?.let {
            val position = it.cameraPosition
            val visibleBounds = it.projection.visibleRegion.latLngBounds
            viewModel.prefetchMarkersForRegion(
                position.zoom,
                visibleBounds.convertToDomainLatLngBounds()
            )
        }
    }

    override fun animateToMyLocation() {
        goToMyLocation()
    }

    fun animateToCity(city: Location) {
        whenSafeToUseMap(Consumer { map: GoogleMap ->
            val position = CameraPosition.Builder()
                .zoom(ZoomLevel.OUTER.level)
                .target(LatLng(city.lat, city.lon))
                .build()
            map.animateCamera(CameraUpdateFactory.newCameraPosition(position))
        })
    }

    private fun initStuff() {
        regionService.getRegionsAsync()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ regions: List<Region> ->
                appPreferences
                    .edit().putBoolean(APP_PREF_DEACTIVATED, false)
                    .apply()
                this.regions = regions
                whenSafeToUseMap { m: GoogleMap -> showCities(m, regions) }
            }) { error: Throwable? ->
                error?.let {
                    it.printStackTrace()
                    errorLogger.logError(it)
                }
            }.addTo(autoDisposable)
    }

    private fun updateArrivalMarker(pinUpdate: PinUpdate) {
        whenSafeToUseMap { map: GoogleMap? ->
            pinUpdate.match(
                { arrivalMarkers!!.clear() },
                { (type) ->
                    val marker = arrivalMarkers!!.addMarker(
                        tripLocationMarkerCreator.call(type.toLocation())
                            .icon(asMarkerIcon(SelectionType.ARRIVAL))
                    )
                    marker.tag = type
                    marker.showInfoWindow()
                }
            )
        }
    }

    private fun updateDepartureMarker(pinUpdate: PinUpdate) {
        whenSafeToUseMap { map: GoogleMap? ->
            pinUpdate.match(
                { departureMarkers!!.clear() },
                { (type) ->
                    val marker = departureMarkers!!.addMarker(
                        tripLocationMarkerCreator.call(type.toLocation())
                            .icon(asMarkerIcon(SelectionType.DEPARTURE))
                    )
                    marker.tag = type
                    marker.showInfoWindow()
                }
            )
        }
    }

    private fun removeAllCities() {
        cityMarkers!!.clear()
        cityMarkerMap.clear()
    }

    private fun showCities(map: GoogleMap, regions: List<Region>?) {
        if (regions != null) {
            val bounds = map.projection.visibleRegion.latLngBounds
            var i = 0
            val regionsSize = regions.size
            while (i < regionsSize) {
                val region = regions[i]
                val cities = region.cities
                if (cities != null) {
                    var j = 0
                    val citiesSize = cities.size
                    while (j < citiesSize) {
                        val city = cities[j]
                        // If the city is in viewport, add markers if hasn't added.
                        if (bounds.contains(LatLng(city.lat, city.lon))) {
                            if (cityMarkerMap[city.name] == null) { // Marker for this city hasn't been added yet.
                                addCityMarker(city)
                            }
                        } else {
                            removeCity(city)
                        }
                        j++
                    }
                }
                i++
            }
        }
    }

    private fun removeCity(city: City) {
        cityMarkers!!.remove(cityMarkerMap[city.name])
        cityMarkerMap.remove(city.name)
    }

    @SuppressLint("MissingPermission")
    private fun setupMap(map: GoogleMap) {
        map.setOnMapLongClickListener(this)
        map.setOnInfoWindowClickListener(this)
        map.setInfoWindowAdapter(object : InfoWindowAdapter {
            override fun getInfoWindow(marker: Marker): View? {
                return markerManager!!.getInfoWindow(marker)
            }

            override fun getInfoContents(marker: Marker): View? {
                return when (val result = contributor?.getInfoContents(marker)) {
                    null -> markerManager!!.getInfoContents(marker)
                    else -> result
                }
            }
        })
        map.setOnCameraChangeListener(this)
        map.isIndoorEnabled = false
        map.setOnMarkerClickListener(this)
        map.setOnPoiClickListener(this)
        map.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style))
    }

    private fun addCityMarker(city: City): Marker {
        val markerOptions = MapMarkerUtils.createCityMarker(city, cityIcon)
        val marker = cityMarkers!!.addMarker(markerOptions)
        cityMarkerMap[city.displayName] = marker
        marker.tag = city
        return marker
    }

    @SuppressLint("MissingPermission")
    private fun goToMyLocation() {
        ExcuseMe.couldYouGive(this)
            .permissionFor(android.Manifest.permission.ACCESS_FINE_LOCATION) {
                if (it.granted.contains(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                    map?.isMyLocationEnabled = true
                    viewModel.goToMyLocation()
                }
            }
    }

    private fun showMyLocation(myLocation: Location) { // Prepare marker for my location.
        if (myLocationMarker == null) {
            val markerOptions = tripLocationMarkerCreator!!.call(myLocation)
            markerOptions.icon(
                MapMarkerUtils.createTransparentSquaredIcon(
                    resources,
                    R.dimen.spacing_small
                )
            )
            myLocationMarker = currentLocationMarkers!!.addMarker(markerOptions)
        } else {
            myLocationMarker!!.position = LatLng(myLocation.lat, myLocation.lon)
        }
        cameraController!!.moveTo(map!!, myLocationMarker!!)
        myLocationMarker!!.showInfoWindow()
    }

    private fun initMap(map: GoogleMap, moveCamera: Boolean = true) {
        cityIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_map_city)
        setupMap(map)
        if (moveCamera) {
            viewModel.getInitialCameraUpdate()
                .subscribe(
                    { cameraUpdate: CameraUpdate? ->
                        map.moveCamera(cameraUpdate)
                        val position = map.cameraPosition
                        val visibleBounds = map.projection.visibleRegion.latLngBounds
                        viewModel.prefetchMarkersForRegion(
                            position.zoom,
                            visibleBounds.convertToDomainLatLngBounds()
                        )
                    }) { error: Throwable? ->
                    errorLogger.trackError(
                        error!!
                    )
                }
                .addTo(autoDisposable)
        }
    }

    private fun showMyLocationError() {
        Toast.makeText(
            activity,
            R.string.could_not_determine_your_current_location_dot,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun initMarkerCollections(map: GoogleMap) {
        markerManager = MarkerManager(map)
        setUpCityMarkers(markerManager!!)
        setUpTripLocationMarkers(markerManager!!)
        setUpDepartureAndArrivalMarkers(markerManager!!)
        setUpCurrentLocationMarkers(markerManager!!)
        setUpPOIMarkers(markerManager!!, map)
    }

    fun focusOnLocation(location: LatLng) {
        whenSafeToUseMap(Consumer { map: GoogleMap ->
            val position = CameraPosition.Builder()
                .zoom(ZoomLevel.OUTER.level)
                .target(LatLng(location.latitude, location.longitude))
                .build()
            map.moveCamera(CameraUpdateFactory.newCameraPosition(position))
        })
    }

    fun setFromMarkerLocation(location: LatLng?) {
        if (location == null) {
            fromMarker?.isVisible = false
        } else {
            fromMarker?.let {
                it.position = location
                it.isVisible = true
            }
        }
    }

    fun setToMarkerLocation(location: LatLng?) {
        if (location == null) {
            toMarker?.isVisible = false
        } else {
            toMarker?.let {
                it.position = location
                it.isVisible = true
            }
        }
    }

    private fun initFromAndToMarkers(map: GoogleMap) {
        val fromBitmap = requireContext().getFromAndToMarkerBitmap(0)

        val toBitmap = requireContext().getFromAndToMarkerBitmap(1)

        fromMarker = map.addMarker(
            MarkerOptions()
                .position(LatLng(0.0, 0.0))
                .visible(false)
                .icon(BitmapDescriptorFactory.fromBitmap(fromBitmap))
        )

        toMarker = map.addMarker(
            MarkerOptions()
                .position(LatLng(0.0, 0.0))
                .visible(false)
                .icon(BitmapDescriptorFactory.fromBitmap(toBitmap))
        )

    }

    private fun setUpCurrentLocationMarkers(markerManager: MarkerManager) {
        currentLocationMarkers = markerManager.newCollection("CurrentLocationMarkers")
        currentLocationMarkers!!.setInfoWindowAdapter(myLocationWindowAdapter)
    }

    private fun setUpDepartureAndArrivalMarkers(markerManager: MarkerManager) {
        departureMarkers = markerManager.newCollection("DepartureMarkers")
        departureMarkers!!.setInfoWindowAdapter(infoWindowAdapter)
        departureMarkers!!.setOnInfoWindowClickListener(OnInfoWindowClickListener { marker: Marker ->
            val tag = marker.tag
            if (tag is NonCurrentType) {
                val type = tag
                //        bus.post(new InfoWindowClickEvent(toLocation(type), true));
            }
        })
        arrivalMarkers = markerManager.newCollection("ArrivalMarkers")
        arrivalMarkers!!.setInfoWindowAdapter(infoWindowAdapter)
        arrivalMarkers!!.setOnInfoWindowClickListener(OnInfoWindowClickListener { marker: Marker ->
            val tag = marker.tag
            if (tag is NonCurrentType) {
                val type = tag
                //        bus.post(new InfoWindowClickEvent(toLocation(type), false));
            }
        })
    }

    private fun setUpTripLocationMarkers(markerManager: MarkerManager) {
        tripLocationMarkers = markerManager.newCollection("TripLocationMarkers")
        tripLocationMarkers!!.setInfoWindowAdapter(infoWindowAdapter)
        tripLocationMarkers!!.setOnInfoWindowClickListener(OnInfoWindowClickListener { marker: Marker ->
            val tag = marker.tag
        })
    }

    private fun setUpCityMarkers(markerManager: MarkerManager) {
        cityMarkers = markerManager.newCollection("CityMarkers")
        cityMarkers!!.setInfoWindowAdapter(cityInfoWindowAdapter)
        cityMarkers!!.setOnInfoWindowClickListener(OnInfoWindowClickListener { marker: Marker ->
            val tag = marker.tag
            if (tag is City) {
                animateToCity(tag)
            }
        })
    }

    private fun setUpPOIMarkers(markerManager: MarkerManager, map: GoogleMap) {
        poiMarkers = markerManager.newCollection("poiMarkers")
        val poiMarkers = poiMarkers

        // This invisible marker is used to show the InfoWindow when a user clicks on a Google POI or long-presses somewhere
        poiMarker = poiMarkers!!.addMarker(MarkerOptions().position(LatLng(0.0, 0.0))).apply {
            alpha = 0F
        }

        longPressMarker = poiMarkers.addMarker(
            MarkerOptions()
                .position(LatLng(0.0, 0.0))
                .infoWindowAnchor(0.5f, 1f)
                .visible(false)
        ).apply {
            alpha = 0F
        }
        val poiLocationInfoWindowAdapter = POILocationInfoWindowAdapter(requireContext())
        poiMarkers.setInfoWindowAdapter(poiLocationInfoWindowAdapter)
        map.setOnInfoWindowCloseListener { marker: Marker ->
            if (marker.tag is IMapPoiLocation) {
                poiLocationInfoWindowAdapter.onInfoWindowClosed(marker)
            }
        }
        poiMarkers.setOnInfoWindowClickListener { marker: Marker ->
            if (onInfoWindowClickListener != null) {
                val poiLocation = marker.tag as IMapPoiLocation?
                if (poiLocation != null && isPoiWindowAdapterClickable(poiLocation)) {
                    onInfoWindowClickListener!!.onInfoWindowClick(poiLocation.toLocation())
                }
            }
        }

        poiMarkers.setOnMarkerClickListener { marker: Marker ->
            val view = view ?: return@setOnMarkerClickListener true
            val poiLocation = marker.tag as IMapPoiLocation?
            poiLocation?.let {
                poiLocation.onMarkerClick(bus, eventTracker)
                marker.showInfoWindow()
                val scrollY = ((resources.getDimensionPixelSize(R.dimen.routing_card_height)
                    + resources.getDimensionPixelSize(R.dimen.spacing_huge)
                    + poiLocationInfoWindowAdapter.windowInfoHeightInPixel(marker))
                    - view.height)
                map.moveCamera(CameraUpdateFactory.newLatLng(marker.position))
                if (scrollY > 0) { // center the map to 64dp above the bottom of the fragment
                    map.moveCamera(CameraUpdateFactory.scrollBy(0f, scrollY * -1.toFloat()))
                }
                onInfoWindowClickListener!!.onInfoWindowClick(poiLocation.toLocation())
            }
            true
        }

    }

    // Keep track of the last zoom level since we don't want to misleadingly call the OnZoomLevelChangedListener.
    private var lastZoomLevel = 0f
    override fun onCameraIdle() {
        map?.let {
            if (it.cameraPosition.zoom != lastZoomLevel) {
                lastZoomLevel = it.cameraPosition.zoom
                onZoomLevelChangedListener?.onZoomLevelChanged(lastZoomLevel)
            }
        }
    }

    fun setShowPoiMarkers(show: Boolean, modes: List<TransportMode>?) {
        viewModel.transportModes = modes
        modes?.let {
            transportModes = it
        }

        viewModel.showMarkers.set(show)
        if (show) {
            tripLocationMarkers?.showAll()
            poiMarkers?.showAll()
            loadMarkers()
        } else {
            tripLocationMarkers?.hideAll()
            poiMarkers?.hideAll()
        }
    }

    fun moveToCameraPosition(cameraPosition: CameraPosition) {
        map?.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    fun moveCameraToPolygonBounds(polygon: Polygon) {
        map?.let { cameraController.moveToPolygonBounds(it, polygon) }
    }

    /**
     * Check if a POI location should be clickable based on its type
     */
    private fun isPoiWindowAdapterClickable(poiLocation: IMapPoiLocation): Boolean {
        return when (poiLocation) {
            is StopPOILocation -> false
            is CarParkPOILocation -> false
            is FacilityPOILocation -> false
            else -> true
        }
    }

    companion object {
        // Camera state keys
        private const val KEY_MAP_CAMERA_LAT = "map_camera_lat"
        private const val KEY_MAP_CAMERA_LNG = "map_camera_lng"
        private const val KEY_MAP_CAMERA_ZOOM = "map_camera_zoom"
        private const val KEY_MAP_CAMERA_BEARING = "map_camera_bearing"
        private const val KEY_MAP_CAMERA_TILT = "map_camera_tilt"
        
        // Visible bounds keys
        private const val KEY_MAP_VISIBLE_BOUNDS_NE_LAT = "map_visible_bounds_northeast_lat"
        private const val KEY_MAP_VISIBLE_BOUNDS_NE_LNG = "map_visible_bounds_northeast_lng"
        private const val KEY_MAP_VISIBLE_BOUNDS_SW_LAT = "map_visible_bounds_southwest_lat"
        private const val KEY_MAP_VISIBLE_BOUNDS_SW_LNG = "map_visible_bounds_southwest_lng"
        
        // Other state keys
        private const val KEY_SHOW_MARKERS = "show_markers"
        private const val KEY_LAST_ZOOM_LEVEL = "last_zoom_level"
        private const val KEY_CONTRIBUTOR_CLASS = "contributor_class"
        
        private fun asMarkerIcon(mode: SelectionType): BitmapDescriptor {
            return if (mode === SelectionType.DEPARTURE) {
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
            } else {
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
            }
        }
    }
}