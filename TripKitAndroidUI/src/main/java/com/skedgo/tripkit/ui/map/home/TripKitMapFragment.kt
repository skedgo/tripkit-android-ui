package com.skedgo.tripkit.ui.map.home

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
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
import com.skedgo.rxtry.printThrowableStackTrace
import com.skedgo.rxtry.subscribeWithErrorHandling
import com.skedgo.tripkit.AndroidGeocoder
import com.skedgo.tripkit.TripKitConstants.Companion.PREF_NAME_APP
import com.skedgo.tripkit.account.data.Polygon
import com.skedgo.tripkit.common.model.TransportMode
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.common.model.region.Region
import com.skedgo.tripkit.common.model.region.Region.City
import com.skedgo.tripkit.common.model.stop.ScheduledStop
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
import com.skedgo.tripkit.ui.map.LocationEnhancedMapFragment
import com.skedgo.tripkit.ui.map.MapCameraController
import com.skedgo.tripkit.ui.map.MapMarkerUtils
import com.skedgo.tripkit.ui.map.StopMarkerIconFetcher
import com.skedgo.tripkit.ui.map.StopPOILocation
import com.skedgo.tripkit.ui.map.TripLocationMarkerCreator
import com.skedgo.tripkit.ui.map.adapter.CityInfoWindowAdapter
import com.skedgo.tripkit.ui.map.adapter.NoActionWindowAdapter
import com.skedgo.tripkit.ui.map.adapter.POILocationInfoWindowAdapter
import com.skedgo.tripkit.ui.map.adapter.StopInfoWindowAdapter
import com.skedgo.tripkit.ui.map.adapter.ViewableInfoWindowAdapter
import com.skedgo.tripkit.ui.map.convertToDomainLatLngBounds
import com.skedgo.tripkit.ui.map.home.ViewPort.CloseEnough
import com.skedgo.tripkit.ui.map.home.ViewPort.NotCloseEnough
import com.skedgo.tripkit.ui.tracking.EventTracker
import com.skedgo.tripkit.ui.tripresult.TripResultMapContributor
import com.skedgo.tripkit.ui.trip.options.SelectionType
import com.skedgo.tripkit.ui.utils.APP_PREF_CLEAR_CAR_PODS_ONCE
import com.skedgo.tripkit.ui.utils.APP_PREF_DEACTIVATED
import com.skedgo.tripkit.ui.utils.KEY_APP_PREF
import com.skedgo.tripkit.ui.utils.MARKER_COLLECTION_ARRIVAL
import com.skedgo.tripkit.ui.utils.MARKER_COLLECTION_CITY
import com.skedgo.tripkit.ui.utils.MARKER_COLLECTION_CURRENT_LOCATION
import com.skedgo.tripkit.ui.utils.MARKER_COLLECTION_DEPARTURE
import com.skedgo.tripkit.ui.utils.MARKER_COLLECTION_POI
import com.skedgo.tripkit.ui.utils.MARKER_COLLECTION_TRIP_LOCATION
import com.skedgo.tripkit.ui.utils.getOrNewCollection
import com.skedgo.tripkit.ui.utils.getVersionCode
import com.skedgo.tripkit.ui.utils.isNetworkConnected
import com.skedgo.tripkit.ui.utils.showConfirmationPopUpDialog
import com.skedgo.tripkit.checkIfLocationProviderIsEnabled
import com.squareup.otto.Bus
import com.squareup.picasso.Picasso
import java.util.WeakHashMap
import dagger.Lazy
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import java.util.LinkedList
import javax.inject.Inject
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import timber.log.Timber
import java.util.*


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
    private var lastZoomLevel: Float = 0f

    private var fromMarker: Marker? = null
    private var toMarker: Marker? = null
    private var longPressMarker: Marker? = null
    private lateinit var geocoder: AndroidGeocoder

    private var contributor: TripKitMapContributor? = null

    // Track viewport bounds for performance optimization
    private var lastViewportBounds: LatLngBounds? = null

    // There doesn't seem to be a way to show an info window when a POI is clicked, so work-around that
    // by using an invisible marker on the map that is moved to the POI's location when clicked.
    private var poiMarker: Marker? = null
    private var transportModes: List<TransportMode>? = null
    private val infoWindowHandler = Handler(Looper.getMainLooper())
    private val markerHideCallbacks = WeakHashMap<Marker, Runnable>()
    private var selectedStopMarkerPosition: LatLng? = null

    @Inject
    lateinit var stopInfoWindowAdapter: StopInfoWindowAdapter

    @Inject
    lateinit var picasso: Picasso

    var enablePinLocationOnClick: Boolean = false
    var pinnedDepartureLocationOnClickMarker: Marker? = null
    var pinnedDepartureLocation: Location? = null
    var pinnedOriginLocationOnClickMarker: Marker? = null
    var pinnedOriginLocation: Location? = null
    var pinLocationSelectedListener: ((Location, Int) -> Unit)? =
        null //for type, 0 = from and 1 = to
    var appDeactivatedListener: (() -> Unit)? = null

    // Track POI markers state before entering trip details to restore it later
    private var previousPoiMarkersState: Boolean = true
    private var previousTransportModes: List<TransportMode>? = null

    // Track existing marker positions to prevent duplicates
    private val existingMarkerPositions = mutableSetOf<LatLng>()

    /**
     * Check if a marker with the given position already exists
     * @param position The LatLng position to check
     * @return true if a marker with this position already exists, false otherwise
     */
    private fun isMarkerPositionExists(position: LatLng): Boolean {
        return existingMarkerPositions.contains(position)
    }

    /**
     * Add a marker position to the tracking set
     * @param position The LatLng position to add
     */
    private fun addMarkerPosition(position: LatLng) {
        existingMarkerPositions.add(position)
    }

    private fun removeMarkerPosition(position: LatLng) {
        existingMarkerPositions.remove(position)
    }

    /**
     * Clear all tracked marker positions
     */
    private fun clearMarkerPositions() {
        existingMarkerPositions.clear()
    }

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

    fun setContributor(newContributor: TripKitMapContributor?) {
        Timber.d("[StateRestore] TripKitMapFragment - setContributor called: newContributor=${newContributor?.javaClass?.simpleName}, mapReady=${map != null}, sameInstance=${contributor === newContributor}")
        
        // Only cleanup if it's a different contributor instance
        // Cleaning up the same instance would remove polylines unnecessarily
        if (contributor !== newContributor) {
            Timber.d("[StateRestore] TripKitMapFragment - Different contributor, calling cleanup on old one")
            contributor?.cleanup()
        } else if (contributor === newContributor && newContributor != null) {
            Timber.d("[StateRestore] TripKitMapFragment - Same contributor instance, skipping cleanup to preserve polylines")
        }
        
        contributor = newContributor
        contributor?.let { contributor ->
            // If the contributor is a TripResultMapContributor, share the MarkerManager
            when (contributor) {
                is TripResultMapContributor -> {
                    contributor.markerManager = this.markerManager
                    Timber.d("[StateRestore] TripKitMapFragment - TripResultMapContributor detected, shared MarkerManager")
                }
            }
            // Check if map is already ready and call safeToUseMap immediately
            if (map != null) {
                Timber.d("[StateRestore] TripKitMapFragment - Map ready, calling safeToUseMap to (re)initialize contributor")
                contributor.safeToUseMap(requireContext(), map!!)
            } else {
                // Map is not ready yet, wait for it
                Timber.d("[StateRestore] TripKitMapFragment - Map not ready, deferring safeToUseMap")
                whenSafeToUseMap(Consumer { map: GoogleMap ->
                    Timber.d("[StateRestore] TripKitMapFragment - Map now ready, calling safeToUseMap")
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

        // Set up the throttle for clearing non-regional markers
        clearNonRegionalMarkersThrottle.debounce(500, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    clearNonRegionalMarkers()
                },
                { e ->
                    e.printStackTrace()
                }
            ).addTo(autoDisposable)
    }

    /**
     * Load POI markers from the view model, preventing duplicate markers at the same position
     */
    private fun loadMarkers() {
        viewModel.markers
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ (first, second) ->
                for ((first1, second1) in first) {
                    // Check if a marker with the same position already exists
                    if (!isMarkerPositionExists(first1.position)) {
                        val marker = poiMarkers!!.addMarker(first1)
                        marker.tag = second1
                        addMarkerPosition(first1.position)
                    }
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

    override fun onDestroyView() {
        infoWindowHandler.removeCallbacksAndMessages(null)
        markerHideCallbacks.clear()
        super.onDestroyView()
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

            viewModel.onViewPortChanged(
                CloseEnough(
                    position.zoom,
                    visibleBounds.convertToDomainLatLngBounds()
                )
            )
        } else {
            if (!tipTapIsDeleted) {
                //        bus.post(new TooltipFragment.TooltipClose(TooltipFragment.PREF_TAP_PUBLIC_STOPS));
                tipTapIsDeleted = true
            }
            if (!tipZoomToSeeTimetable) {
                //        bus.post(new RequestShowTip(TooltipFragment.PREF_ZOOM_TO_SEE_TIMETABLE, getString(R.string.zoom_into_map_to_view_public_transport_stops)));
                checkZoomOutFlag = true
            }

            viewModel.onViewPortChanged(
                NotCloseEnough(
                    position.zoom,
                    visibleBounds.convertToDomainLatLngBounds()
                )
            )
        }

        if (position.zoom <= ZoomLevel.ZOOM_VALUE_TO_SHOW_CITIES) {
            toggleLocationMarkers(show = false)
            showCities(map!!, regions)
        } else {
            toggleLocationMarkers(show = viewModel.showMarkers.get())
            removeAllCities()
        }

        if(position.zoom > ZoomLevel.ZOOM_VALUE_TO_SHOW_CITIES) {
            Timber.i("========== ${position.zoom} ============")
            if (position.zoom > ZoomLevel.ZOOM_START_VALUE_TO_SHOW_REGIONAL && position.zoom <= 12.0f) {
                clearNonRegionalMarkersThrottle.onNext(System.currentTimeMillis())
            } else {
                hideMarkersOutsideViewport()
            }
        }
    }

    private fun toggleLocationMarkers(show: Boolean) {
        val mapRef = map ?: return

        if (show) {
            val viewportBounds = mapRef.projection.visibleRegion.latLngBounds
            val zoom = mapRef.cameraPosition.zoom
            val isPOIZoom = zoom > 12.1f && zoom < 14.5f

            // Show non-POI collections first (these can use showAll safely)
            tripLocationMarkers?.showAll()
            arrivalMarkers?.showAll()
            departureMarkers?.showAll()

            // POIs: avoid showAll during POI zoom to prevent the flash
            if (isPOIZoom) {
                poiMarkers?.let { collection ->
                    // Authoritatively set per marker in the same frame
                    for (marker in collection.markers) {
                        val inViewport = viewportBounds.contains(marker.position)
                        val shouldBeVisible = inViewport && (marker.tag is StopPOILocation)
                        if (marker.isVisible != shouldBeVisible) {
                            marker.isVisible = shouldBeVisible
                        }
                    }
                }
            } else {
                // Outside the POI zoom band we can safely showAll
                poiMarkers?.showAll()
            }

            // Apply viewport filtering to all collections immediately (same frame, no blink)
            hideMarkersInCollection(poiMarkers, viewportBounds)
            hideMarkersInCollection(cityMarkers, viewportBounds)
            hideMarkersInCollection(tripLocationMarkers, viewportBounds)
            hideMarkersInCollection(departureMarkers, viewportBounds)
            hideMarkersInCollection(arrivalMarkers, viewportBounds)
            hideMarkersInCollection(currentLocationMarkers, viewportBounds)
            hideIndividualMarkers(viewportBounds)

        } else {
            // Hiding is unchanged
            tripLocationMarkers?.hideAll()
            poiMarkers?.hideAll()
            arrivalMarkers?.hideAll()
            departureMarkers?.hideAll()
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
                .zoom(ZoomLevel.REGIONAL.level)
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
                { arrivalMarkers?.clear() },
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
                { departureMarkers?.clear() },
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
        cityMarkers?.clear()
        cityMarkerMap.clear()
        // Note: City markers are managed by MarkerManager collections, so we don't need to untrack them individually
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
        // First check if device location is enabled
        if (!requireContext().checkIfLocationProviderIsEnabled()) {
            requireContext().showConfirmationPopUpDialog(
                title = getString(R.string.location_services_required),
                message = getString(R.string.device_location_is_turned_off),
                positiveLabel = getString(R.string.settings),
                positiveCallback = {
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                }
            )
            return
        }

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
                .zoom(ZoomLevel.ZOOM_START_VALUE_TO_SHOW_REGIONAL)
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
        currentLocationMarkers = markerManager.getOrNewCollection(MARKER_COLLECTION_CURRENT_LOCATION)
        currentLocationMarkers!!.setInfoWindowAdapter(myLocationWindowAdapter)
    }

    private fun setUpDepartureAndArrivalMarkers(markerManager: MarkerManager) {
        departureMarkers = markerManager.getOrNewCollection(MARKER_COLLECTION_DEPARTURE)
        departureMarkers!!.setInfoWindowAdapter(infoWindowAdapter)
        departureMarkers!!.setOnInfoWindowClickListener(OnInfoWindowClickListener { marker: Marker ->
            val tag = marker.tag
            if (tag is NonCurrentType) {
                val type = tag
                //        bus.post(new InfoWindowClickEvent(toLocation(type), true));
            }
        })
        arrivalMarkers = markerManager.getOrNewCollection(MARKER_COLLECTION_ARRIVAL)
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
        tripLocationMarkers = markerManager.getOrNewCollection(MARKER_COLLECTION_TRIP_LOCATION)
        tripLocationMarkers!!.setInfoWindowAdapter(infoWindowAdapter)
        tripLocationMarkers!!.setOnInfoWindowClickListener(OnInfoWindowClickListener { marker: Marker ->
            val tag = marker.tag
        })
    }

    private fun setUpCityMarkers(markerManager: MarkerManager) {
        cityMarkers = markerManager.getOrNewCollection(MARKER_COLLECTION_CITY)
        cityMarkers!!.setInfoWindowAdapter(cityInfoWindowAdapter)
        cityMarkers!!.setOnInfoWindowClickListener(OnInfoWindowClickListener { marker: Marker ->
            val tag = marker.tag
            if (tag is City) {
                animateToCity(tag)
            }
        })
    }

    private fun setUpPOIMarkers(markerManager: MarkerManager, map: GoogleMap) {
        poiMarkers = markerManager.getOrNewCollection(MARKER_COLLECTION_POI)
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
                if (poiLocation is StopPOILocation) {
                    selectedStopMarkerPosition = marker.position
                }
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
    override fun onCameraIdle() {
        map?.let {
            if (it.cameraPosition.zoom != lastZoomLevel) {
                lastZoomLevel = it.cameraPosition.zoom
                onZoomLevelChangedListener?.onZoomLevelChanged(lastZoomLevel)
            }
        }
    }

    fun setShowMarkers(
        show: Boolean,
        notIncludedModes: List<TransportMode>?,
        fromTripList: Boolean = false
    ) {
        // Save current state before making changes (only if we're disabling markers)
        if (!show && viewModel.showMarkers.get()) {
            savePoiMarkersState()
        }

        //viewModel.notIncludedTransportModes = notIncludedModes
        notIncludedModes?.let {
            transportModes = it
        }

        viewModel.showMarkers.set(show)
        if (show) {
            tripLocationMarkers?.showAll()
            poiMarkers?.showAll()
            loadMarkers()
        } else {
            if (fromTripList) {
                tripLocationMarkers?.clear()
                poiMarkers?.clear()
                clearMarkerPositions()
            } else {
                tripLocationMarkers?.hideAll()
                poiMarkers?.hideAll()
            }

        }
    }

    /**
     * Save the current POI markers state before disabling them
     */
    private fun savePoiMarkersState() {
        previousPoiMarkersState = viewModel.showMarkers.get()
        previousTransportModes = transportModes
    }

    /**
     * Restore POI markers to their previous state
     */
    fun restorePoiMarkersState() {
        if (previousPoiMarkersState) {
            setShowMarkers(true, previousTransportModes)
        }
    }

    fun ensureStopMarker(
        stop: ScheduledStop,
        shouldHideInfoWindow: Boolean = false
    ) {
        selectedStopMarkerPosition = if (shouldHideInfoWindow) null else LatLng(stop.lat, stop.lon)
        if (stop.lat.isNaN() || stop.lon.isNaN()) {
            return
        }
        if (stop.lat == 0.0 && stop.lon == 0.0) {
            return
        }
        val targetPosition = LatLng(stop.lat, stop.lon)
        if (isMarkerPositionExists(targetPosition)) {
            showExistingStopMarkerInfoWindow(targetPosition, shouldHideInfoWindow)
            return
        }

        fun addMarkerIfNeeded() {
            val collection = poiMarkers ?: return
            if (isMarkerPositionExists(targetPosition)) {
                return
            }
            val poiLocation = StopPOILocation(stop, stopInfoWindowAdapter)
            poiLocation.createMarkerOptions(resources, picasso)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ markerOptions ->
                    val position = markerOptions.position
                    if (isMarkerPositionExists(position)) {
                        return@subscribe
                    }
                    val marker = collection.addMarker(markerOptions)
                    marker.tag = poiLocation
                    addMarkerPosition(position)
                    marker.showInfoWindow()
                    if (shouldHideInfoWindow) {
                        hideInfoWindowLater(marker)
                    }
                    map?.moveToMarkerWithInfoWindow(marker)
                }, { error ->
                    errorLogger.logError(error)
                })
                .addTo(autoDisposable)
        }

        if (map != null && poiMarkers != null) {
            addMarkerIfNeeded()
        } else {
            whenSafeToUseMap {
                addMarkerIfNeeded()
            }
        }
    }

    private fun showExistingStopMarkerInfoWindow(
        position: LatLng,
        shouldHideInfoWindow: Boolean = false
    ) {
        fun showInfoWindow() {
            val collection = poiMarkers ?: return
            val marker = collection.markers.firstOrNull { it.position == position } ?: return
            marker.isVisible = true
            marker.showInfoWindow()
            if (shouldHideInfoWindow) {
                hideInfoWindowLater(marker)
            }
            map?.moveToMarkerWithInfoWindow(marker)
        }

        if (map != null && poiMarkers != null) {
            showInfoWindow()
        } else {
            whenSafeToUseMap {
                showInfoWindow()
            }
        }
    }

    private fun hideInfoWindowLater(marker: Marker) {
        markerHideCallbacks[marker]?.let { infoWindowHandler.removeCallbacks(it) }

        val hideRunnable = Runnable {
            markerHideCallbacks.remove(marker)

            if (!isAdded || !isVisible) {
                return@Runnable
            }

            try {
                if (marker.isInfoWindowShown) {
                    marker.hideInfoWindow()
                }
            } catch (throwable: Exception) {
                Timber.v(throwable, "Unable to hide marker info window safely.")
            }
        }

        markerHideCallbacks[marker] = hideRunnable
        infoWindowHandler.postDelayed(hideRunnable, INFO_WINDOW_AUTO_HIDE_DELAY_MS)
    }

    private fun GoogleMap.moveToMarkerWithInfoWindow(marker: Marker, offsetPx: Int = 50) {
        var done = false

        setOnCameraIdleListener {
            if (done) return@setOnCameraIdleListener
            done = true

            animateCamera(CameraUpdateFactory.scrollBy(0f, -offsetPx.toFloat()))
            marker.showInfoWindow()

            setOnCameraIdleListener(null)
        }

        try {
            lifecycleScope.launch {
                delay(500)
                launch(Dispatchers.Main) {
                    animateCamera(CameraUpdateFactory.newLatLng(marker.position))
                }
            }
        } catch (e: Exception) {
            e.printThrowableStackTrace()
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

    /**
     * Clear all LOCAL level markers when transitioning to regional level
     * This ensures that existing LOCAL markers are removed when zooming out
     */
    val clearNonRegionalMarkersThrottle = PublishSubject.create<Long>()

    private fun clearNonRegionalMarkers() {
        val mapRef = map ?: return
        val zoom = mapRef.cameraPosition.zoom
        val isCityZoom = zoom <= ZoomLevel.ZOOM_VALUE_TO_SHOW_CITIES
        
        // Don't re-add markers when at city zoom level
        if(viewModel.showMarkers.get() && !isCityZoom) {
            poiMarkers?.clear()
            clearMarkerPositions()
            // Re-add cached regional stop markers and restore StopPOILocation tags
            MapData.getRegionalStops().forEach { cached ->
                val marker = poiMarkers?.addMarker(cached.markerOptions)
                marker?.let { m ->
                    m.tag = com.skedgo.tripkit.ui.map.StopPOILocation(
                        cached.stop,
                        stopInfoWindowAdapter
                    )
                    addMarkerPosition(m.position)
                }
            }
        }
    }

    /**
     * Hide markers that are outside the current camera viewport for performance optimization
     * Uses MarkerManager collections for efficient marker management
     */
    private fun hideMarkersOutsideViewport() {
        val map = this.map ?: return
        val currentBounds = map.projection.visibleRegion.latLngBounds

        // Only update if viewport has changed significantly
        if (lastViewportBounds != null && boundsAreSimilar(lastViewportBounds!!, currentBounds)) {
            return
        }

        if(!viewModel.showMarkers.get()) {
            tripLocationMarkers?.hideAll()
            poiMarkers?.hideAll()
            arrivalMarkers?.hideAll()
            departureMarkers?.hideAll()
            return
        }

        lastViewportBounds = currentBounds

        // Hide/show markers in each collection based on viewport
        hideMarkersInCollection(poiMarkers, currentBounds)
        hideMarkersInCollection(cityMarkers, currentBounds)
        hideMarkersInCollection(tripLocationMarkers, currentBounds)
        hideMarkersInCollection(departureMarkers, currentBounds)
        hideMarkersInCollection(arrivalMarkers, currentBounds)
        hideMarkersInCollection(currentLocationMarkers, currentBounds)

        // Handle individual markers that aren't in collections
        hideIndividualMarkers(currentBounds)
    }

    /**
     * Hide/show markers in a collection based on viewport and zoom.
     * - For zoom in (12.1f, 14.5f): only show markers with tag is StopPOILocation AND in viewport.
     * - Otherwise: standard viewport-based visibility.
     */
    private fun hideMarkersInCollection(
        collection: MarkerManager.Collection?,
        viewportBounds: LatLngBounds
    ) {
        collection ?: return

        val zoom = map?.cameraPosition?.zoom ?: 0f
        val isPOIZoom = zoom > 12.1f && zoom < 14.5f

        // Iterate once and set visibility based on the rule for this zoom level
        for (marker in collection.markers) {
            val inViewport = viewportBounds.contains(marker.position)

            val shouldBeVisible =
                viewModel.showMarkers.get() && if (isPOIZoom) {
                    // Show only StopPOILocation markers within viewport
                    inViewport && (marker.tag is StopPOILocation)
                } else {
                    // Standard: any marker within viewport
                    inViewport
                }

            if (marker.isVisible != shouldBeVisible) {
                marker.isVisible = shouldBeVisible
            }
            if (
                shouldBeVisible &&
                marker.tag is StopPOILocation &&
                selectedStopMarkerPosition != null &&
                marker.position == selectedStopMarkerPosition &&
                !marker.isInfoWindowShown
            ) {
                marker.showInfoWindow()
            }
        }
    }


    /**
     * Extract Location from marker tag
     */
    private fun getLocationFromMarker(marker: Marker): Location? {
        return when (val tag = marker.tag) {
            is Location -> tag
            is StopPOILocation -> tag.toLocation()
            else -> null
        }
    }

    /**
     * Hide individual markers that aren't managed by collections
     */
    private fun hideIndividualMarkers(bounds: LatLngBounds) {
        // Handle from/to markers
        fromMarker?.let { marker ->
            marker.isVisible = bounds.contains(marker.position)
        }
        toMarker?.let { marker ->
            marker.isVisible = bounds.contains(marker.position)
        }

        // Handle pinned location markers
        pinnedOriginLocationOnClickMarker?.let { marker ->
            marker.isVisible = bounds.contains(marker.position)
        }
        pinnedDepartureLocationOnClickMarker?.let { marker ->
            marker.isVisible = bounds.contains(marker.position)
        }

        // Handle POI and long press markers (these are usually invisible anyway)
        poiMarker?.let { marker ->
            marker.isVisible = bounds.contains(marker.position)
        }
        longPressMarker?.let { marker ->
            marker.isVisible = bounds.contains(marker.position)
        }
    }

    /**
     * Check if two bounds are similar enough to avoid unnecessary updates
     */
    private fun boundsAreSimilar(bounds1: LatLngBounds, bounds2: LatLngBounds): Boolean {
        val latDiff = kotlin.math.abs(bounds1.northeast.latitude - bounds2.northeast.latitude) +
                     kotlin.math.abs(bounds1.southwest.latitude - bounds2.southwest.latitude)
        val lngDiff = kotlin.math.abs(bounds1.northeast.longitude - bounds2.northeast.longitude) +
                     kotlin.math.abs(bounds1.southwest.longitude - bounds2.southwest.longitude)

        // Consider bounds similar if the difference is less than 0.001 degrees (roughly 100m)
        return latDiff < 0.001 && lngDiff < 0.001
    }

    /**
     * Save the current map state for restoration after configuration changes or app resume.
     * This includes camera position, visible bounds, and map contributor state.
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Save camera position
        map?.cameraPosition?.let { position ->
            outState.putDouble(KEY_MAP_CAMERA_LAT, position.target.latitude)
            outState.putDouble(KEY_MAP_CAMERA_LNG, position.target.longitude)
            outState.putFloat(KEY_MAP_CAMERA_ZOOM, position.zoom)
            outState.putFloat(KEY_MAP_CAMERA_BEARING, position.bearing)
            outState.putFloat(KEY_MAP_CAMERA_TILT, position.tilt)
        }

        // Save visible bounds
        map?.projection?.visibleRegion?.latLngBounds?.let { bounds ->
            outState.putDouble(KEY_MAP_VISIBLE_BOUNDS_NE_LAT, bounds.northeast.latitude)
            outState.putDouble(KEY_MAP_VISIBLE_BOUNDS_NE_LNG, bounds.northeast.longitude)
            outState.putDouble(KEY_MAP_VISIBLE_BOUNDS_SW_LAT, bounds.southwest.latitude)
            outState.putDouble(KEY_MAP_VISIBLE_BOUNDS_SW_LNG, bounds.southwest.longitude)
        }

        // Save marker visibility state
        outState.putBoolean(KEY_SHOW_MARKERS, viewModel.showMarkers.get())

        // Save last zoom level
        lastZoomLevel?.let { zoom ->
            outState.putFloat(KEY_LAST_ZOOM_LEVEL, zoom)
        }

        // Save contributor class name for restoration
        contributor?.let { contributor ->
            outState.putString(KEY_CONTRIBUTOR_CLASS, contributor::class.java.name)
        }
    }

    /**
     * Restore the map state from saved instance state.
     * This restores camera position, visible bounds, and marker visibility.
     * The map contributor must be set externally after restoration.
     */
    private fun restoreMapState(savedInstanceState: Bundle) {
        Timber.d("[StateRestore] TripKitMapFragment - restoreMapState called")
        
        // Restore camera position when map is ready
        val hasCamera = savedInstanceState.containsKey(KEY_MAP_CAMERA_LAT) &&
                savedInstanceState.containsKey(KEY_MAP_CAMERA_LNG) &&
                savedInstanceState.containsKey(KEY_MAP_CAMERA_ZOOM)

        if (hasCamera) {
            val lat = savedInstanceState.getDouble(KEY_MAP_CAMERA_LAT)
            val lng = savedInstanceState.getDouble(KEY_MAP_CAMERA_LNG)
            val zoom = savedInstanceState.getFloat(KEY_MAP_CAMERA_ZOOM)
            val bearing = savedInstanceState.getFloat(KEY_MAP_CAMERA_BEARING, 0f)
            val tilt = savedInstanceState.getFloat(KEY_MAP_CAMERA_TILT, 0f)
            
            Timber.d("[StateRestore] TripKitMapFragment - Restoring camera: lat=$lat, lng=$lng, zoom=$zoom")

            val cameraPosition = CameraPosition.Builder()
                .target(LatLng(lat, lng))
                .zoom(zoom)
                .bearing(bearing)
                .tilt(tilt)
                .build()

            whenSafeToUseMap(Consumer { map ->
                map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                Timber.d("[StateRestore] TripKitMapFragment - Camera position restored")
            })
        } else {
            Timber.d("[StateRestore] TripKitMapFragment - No saved camera position found")
        }

        // Restore marker visibility
        if (savedInstanceState.containsKey(KEY_SHOW_MARKERS)) {
            val showMarkers = savedInstanceState.getBoolean(KEY_SHOW_MARKERS)
            viewModel.showMarkers.set(showMarkers)
            Timber.d("[StateRestore] TripKitMapFragment - Marker visibility restored: $showMarkers")
        }

        // Restore last zoom level
        if (savedInstanceState.containsKey(KEY_LAST_ZOOM_LEVEL)) {
            lastZoomLevel = savedInstanceState.getFloat(KEY_LAST_ZOOM_LEVEL)
            Timber.d("[StateRestore] TripKitMapFragment - Last zoom level restored: $lastZoomLevel")
        }
        
        // Check for saved contributor
        if (savedInstanceState.containsKey(KEY_CONTRIBUTOR_CLASS)) {
            val contributorClassName = savedInstanceState.getString(KEY_CONTRIBUTOR_CLASS)
            Timber.d("[StateRestore] TripKitMapFragment - Saved contributor class: $contributorClassName (will be restored by parent fragment)")
        }

        // Note: Contributor restoration must be handled by the parent fragment
        // since it requires context about which contributor to create
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

        private const val INFO_WINDOW_AUTO_HIDE_DELAY_MS = 3_000L
    }
}