package com.skedgo.tripkit.ui.map.home

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.*
import com.google.android.gms.maps.model.*
import com.google.maps.android.MarkerManager
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.common.model.Region
import com.skedgo.tripkit.common.model.Region.City
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.logging.ErrorLogger
import com.skedgo.tripkit.tripplanner.NonCurrentType
import com.skedgo.tripkit.tripplanner.PinUpdate
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.module.HomeMapFragmentModule
import com.skedgo.tripkit.ui.core.permissions.*
import com.skedgo.tripkit.ui.core.permissions.PermissionResult.Granted
import com.skedgo.tripkit.ui.data.toLocation
import com.skedgo.tripkit.ui.map.*
import com.skedgo.tripkit.ui.map.adapter.CityInfoWindowAdapter
import com.skedgo.tripkit.ui.map.adapter.NoActionWindowAdapter
import com.skedgo.tripkit.ui.map.adapter.POILocationInfoWindowAdapter
import com.skedgo.tripkit.ui.map.adapter.ViewableInfoWindowAdapter
import com.skedgo.tripkit.ui.map.home.ViewPort.CloseEnough
import com.skedgo.tripkit.ui.map.home.ViewPort.NotCloseEnough
import com.skedgo.tripkit.ui.model.LocationTag
import com.skedgo.tripkit.ui.tracking.EventTracker
import com.skedgo.tripkit.ui.trip.options.SelectionType
import com.squareup.otto.Bus
import dagger.Lazy
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Action
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import java.io.InvalidClassException
import java.util.*
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
class TripKitMapFragment : LocationEnhancedMapFragment(), OnInfoWindowClickListener, OnMapLongClickListener, OnPoiClickListener, OnCameraChangeListener, OnMarkerClickListener {
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
    lateinit var errorLogger: ErrorLogger
    @Inject
    lateinit var eventTracker: EventTracker
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
    // There doesn't seem to be a way to show an info window when a POI is clicked, so work-around that
    // by using an invisible marker on the map that is moved to the POI's location when clicked.
    private var poiMarker: Marker? = null
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
    fun setOnInfoWindowClickListener(listener:(Location?) -> Unit) {
        onInfoWindowClickListener = object: TripKitMapFragment.OnInfoWindowClickListener {
            override fun onInfoWindowClick(location: Location?) {
                listener(location)
            }
        }
    }

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().homeMapFragmentComponent(HomeMapFragmentModule(this)).inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        whenSafeToUseMap(Consumer { map: GoogleMap ->
            this.map = map
            initMarkerCollections(map)
            initMap(map)
        })
        initStuff()
        setMyLocationEnabled()
        viewModel!!.getOriginPinUpdate()
                .compose(bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { pinUpdate: PinUpdate -> updateDepartureMarker(pinUpdate) }
        viewModel!!.getDestinationPinUpdate()
                .compose(bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { pinUpdate: PinUpdate -> updateArrivalMarker(pinUpdate) }
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

    override fun onResume() {
        super.onResume()
        bus.register(this)
    }

    override fun onDestroy() {
        if (currentLocationMarkers != null) {
            currentLocationMarkers!!.clear()
        }
        viewModel.onCleared()
        super.onDestroy()
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        //    bus.post(new TooltipFragment.TooltipClose(TooltipFragment.PREF_TAP_PUBLIC_STOPS ));

        return markerManager!!.onMarkerClick(marker)
    }

    override fun onPoiClick(pointOfInterest: PointOfInterest) {
        poiMarker?.let { marker ->
            marker.title = pointOfInterest.name
            marker.position = pointOfInterest.latLng
            marker.tag = GenericIMapPoiLocation(pointOfInterest, ViewableInfoWindowAdapter(layoutInflater))
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

    override fun onMapLongClick(point: LatLng) { //    bus.post(new MapLongClickEvent(point.latitude, point.longitude));
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
    override fun onCameraChange(position: CameraPosition) {
        if (!isAdded) { // To investigate further this scenario.
//      Crashlytics.logException(new IllegalStateException(
//          "onCameraChange() when !isAdded()"
//      ));
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
            viewModel!!.onViewPortChanged(CloseEnough(
                    position.zoom,
                    visibleBounds.convertToDomainLatLngBounds()))
        } else {
            viewModel!!.onViewPortChanged(NotCloseEnough(
                    position.zoom,
                    visibleBounds.convertToDomainLatLngBounds()))
        }
        if (position.zoom <= ZoomLevel.ZOOM_VALUE_TO_SHOW_CITIES) {
            showCities(map!!, regions)
        } else {
            removeAllCities()
        }
    }

    fun onLocationSelected(locationTag: LocationTag?) {
        if (locationTag != null) {
            val selectionType = locationTag.type
            val location = locationTag.location
            whenSafeToUseMap(Consumer { map: GoogleMap? ->
                tripLocationMarkers!!.clear()
                val marker = tripLocationMarkers!!.addMarker(
                        tripLocationMarkerCreator!!.call(location)
                                .icon(asMarkerIcon(selectionType))
                )
                marker.tag = locationTag
                marker.showInfoWindow()
            })
        }
    }

    //  @Subscribe
//  public void onEvent(LocationSelectedEvent event) {
//    whenSafeToUseMap(map -> cameraController.moveToLatLng(map, toLatLng(event.getLocation())));
//  }
    fun onLocationAddressDecoded(locationTag: LocationTag) {
        val selectionType = locationTag.type
        val location = locationTag.location
        whenSafeToUseMap(Consumer { map: GoogleMap? ->
            tripLocationMarkers!!.clear()
            val marker = tripLocationMarkers!!.addMarker(
                    tripLocationMarkerCreator!!.call(location)
                            .icon(asMarkerIcon(selectionType))
            )
            marker.tag = locationTag
            marker.showInfoWindow()
        })
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
        regionService!!.getRegionsAsync()
                .compose(bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ regions: List<Region> ->
                    this.regions = regions
                    whenSafeToUseMap(Consumer { m: GoogleMap -> showCities(m, regions) })
                }) { error: Throwable? -> errorLogger!!.logError(error!!) }
    }

    private fun updateArrivalMarker(pinUpdate: PinUpdate) {
        whenSafeToUseMap(Consumer { map: GoogleMap? ->
            pinUpdate.match(
                    Action { arrivalMarkers!!.clear() },
                    Consumer { (type) ->
                        val marker = arrivalMarkers!!.addMarker(
                                tripLocationMarkerCreator!!.call(type.toLocation())
                                        .icon(asMarkerIcon(SelectionType.ARRIVAL))
                        )
                        marker.tag = type
                        marker.showInfoWindow()
                    }
            )
        })
    }

    private fun updateDepartureMarker(pinUpdate: PinUpdate) {
        whenSafeToUseMap(Consumer { map: GoogleMap? ->
            pinUpdate.match(
                    Action { departureMarkers!!.clear() },
                    Consumer { (type) ->
                        val marker = departureMarkers!!.addMarker(
                                tripLocationMarkerCreator.call(type.toLocation())
                                        .icon(asMarkerIcon(SelectionType.DEPARTURE))
                        )
                        marker.tag = type
                        marker.showInfoWindow()
                    }
            )
        })
    }

    private fun setMyLocationEnabled() {
        if (activity is CanRequestPermission) {
            whenSafeToUseMap(Consumer { map: GoogleMap -> map.isMyLocationEnabled = true })
        }
        //    ((BaseActivity) getActivity())
//        .checkSelfPermissionReactively(Manifest.permission.ACCESS_FINE_LOCATION)
//        .filter(result -> result)
//            .compose(bindToLifecycle())
//        .subscribe(__ -> whenSafeToUseMap(map -> map.setMyLocationEnabled(true)));
    }

    private fun requestLocationPermission(): Single<PermissionResult> {
        if (activity is CanRequestPermission) {
            return (activity as CanRequestPermission)
                    .requestPermissions(
                            PermissionsRequest.Location(),
                            activity!!.showGenericRationale(null, getString(R.string.access_to_location_services_required_dot)),
                            activity!!.dealWithNeverAskAgainDenial(getString(R.string.access_to_location_services_required_dot))
                    )
        } else {
            return Single.error(InvalidClassException("Parent activity must implement CanRequestPermission"))
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
                return markerManager!!.getInfoContents(marker)
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
        cityMarkerMap[city.name] = marker
        marker.tag = city
        return marker
    }

    private fun goToMyLocation() {
        if (activity is CanRequestPermission) {
            requestLocationPermission()
                    .toObservable()
                    .compose(bindToLifecycle())
                    .subscribe({ result: PermissionResult? ->
                        if (result is Granted) {
                            viewModel!!.goToMyLocation()
                        }
                    }) { error: Throwable? -> errorLogger!!.trackError(error!!) }
        }
    }

    private fun showMyLocation(myLocation: Location) { // Prepare marker for my location.
        if (myLocationMarker == null) {
            val markerOptions = tripLocationMarkerCreator!!.call(myLocation)
            markerOptions.icon(MapMarkerUtils.createTransparentSquaredIcon(
                    resources,
                    R.dimen.spacing_small
            ))
            myLocationMarker = currentLocationMarkers!!.addMarker(markerOptions)
        } else {
            myLocationMarker!!.position = LatLng(myLocation.lat, myLocation.lon)
        }
        cameraController!!.moveTo(map!!, myLocationMarker!!)
        myLocationMarker!!.showInfoWindow()
    }

    private fun initMap(map: GoogleMap) {
        cityIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_map_city)
        setupMap(map)
        val requestLocationPermission = Single
                .defer {
                    requestLocationPermission()
                            .map { it: PermissionResult? -> it is Granted }
                }
                .toObservable()
                .compose(bindToLifecycle())
        viewModel!!.getInitialCameraUpdate { requestLocationPermission }
                .compose(bindToLifecycle())
                .subscribe({ cameraUpdate: CameraUpdate? -> map.moveCamera(cameraUpdate) }) { error: Throwable? -> errorLogger!!.trackError(error!!) }
        viewModel!!.myLocation
                .compose(bindToLifecycle())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe({ myLocation: Location -> showMyLocation(myLocation) }) { error: Throwable? -> errorLogger!!.trackError(error!!) }
        viewModel!!.myLocationError
                .compose(bindToLifecycle())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe({ _: Throwable? -> showMyLocationError() }) { error: Throwable? -> errorLogger!!.trackError(error!!) }
        viewModel!!.markers
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { (first, second) ->
                    val toRemove: MutableList<Marker> = ArrayList()
                    for (marker in poiMarkers!!.markers) {
                        marker.tag?.let {tag ->
                            val identifier = (tag as IMapPoiLocation?)!!.identifier
                            if (second.contains(identifier)) {
                                toRemove.add(marker)
                            }
                        }
                    }
                    for (marker in toRemove) {
                        poiMarkers!!.remove(marker)
                    }
                    for ((first1, second1) in first) {
                        val marker = poiMarkers!!.addMarker(first1)
                        marker.tag = second1
                    }
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

    private fun setUpCurrentLocationMarkers(markerManager: MarkerManager) {
        if (activity is CanRequestPermission) {
            currentLocationMarkers = markerManager.newCollection("CurrentLocationMarkers")
            currentLocationMarkers!!.setOnInfoWindowAdapter(myLocationWindowAdapter)
        }
        //    currentLocationMarkers.setOnInfoWindowClickListener(marker -> bus.post(
//        new CurrentLocationInfoWindowClickEvent(marker.getPosition())
//    ));
    }

    private fun setUpDepartureAndArrivalMarkers(markerManager: MarkerManager) {
        departureMarkers = markerManager.newCollection("DepartureMarkers")
        departureMarkers!!.setOnInfoWindowAdapter(infoWindowAdapter)
        departureMarkers!!.setOnInfoWindowClickListener(OnInfoWindowClickListener { marker: Marker ->
            val tag = marker.tag
            if (tag is NonCurrentType) {
                val type = tag
                //        bus.post(new InfoWindowClickEvent(toLocation(type), true));
            }
        })
        arrivalMarkers = markerManager.newCollection("ArrivalMarkers")
        arrivalMarkers!!.setOnInfoWindowAdapter(infoWindowAdapter)
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
        tripLocationMarkers!!.setOnInfoWindowAdapter(infoWindowAdapter)
        tripLocationMarkers!!.setOnInfoWindowClickListener(OnInfoWindowClickListener { marker: Marker ->
            val tag = marker.tag
            if (tag is LocationTag) {
                val location = tag.location
                if (location != null) { //          bus.post(new InfoWindowClickEvent(location));
                }
            }
        })
    }

    private fun setUpCityMarkers(markerManager: MarkerManager) {
        cityMarkers = markerManager.newCollection("CityMarkers")
        cityMarkers!!.setOnInfoWindowAdapter(cityInfoWindowAdapter)
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

        // This invisible marker is used to show the InfoWindow when a user clicks on a Google POI.
        poiMarker = poiMarkers!!.addMarker(MarkerOptions().position(LatLng(0.0,0.0))).apply {
            alpha = 0F
        }

        val poiLocationInfoWindowAdapter = POILocationInfoWindowAdapter(context!!)
        poiMarkers!!.setOnInfoWindowAdapter(poiLocationInfoWindowAdapter)
        map.setOnInfoWindowCloseListener { marker: Marker ->
            if (marker.tag is IMapPoiLocation) {
                poiLocationInfoWindowAdapter.onInfoWindowClosed(marker)
            }
        }
        poiMarkers.setOnInfoWindowClickListener { marker: Marker ->
            if (onInfoWindowClickListener != null) {
                val poiLocation = marker.tag as IMapPoiLocation?
                if (poiLocation != null) {
                    onInfoWindowClickListener!!.onInfoWindowClick(poiLocation.toLocation())
                }
            }
        }
        poiMarkers.setOnMarkerClickListener { marker: Marker ->
            val view = view ?: return@setOnMarkerClickListener true
            val poiLocation = marker.tag as IMapPoiLocation?
            poiLocation!!.onMarkerClick(bus!!, eventTracker!!)
            marker.showInfoWindow()
            val scrollY = ((resources.getDimensionPixelSize(R.dimen.routing_card_height)
                    + resources.getDimensionPixelSize(R.dimen.spacing_huge)
                    + poiLocationInfoWindowAdapter.windowInfoHeightInPixel(marker))
                    - view.height / 2)
            map.moveCamera(CameraUpdateFactory.newLatLng(marker.position))
            if (scrollY > 0) { // center the map to 64dp above the bottom of the fragment
                map.moveCamera(CameraUpdateFactory.scrollBy(0f, scrollY * -1.toFloat()))
            }
            true
        }

    }

    companion object {
        private fun asMarkerIcon(mode: SelectionType): BitmapDescriptor {
            return if (mode === SelectionType.DEPARTURE) {
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
            } else {
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
            }
        }
    }
}