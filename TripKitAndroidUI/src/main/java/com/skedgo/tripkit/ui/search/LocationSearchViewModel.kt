package com.skedgo.tripkit.ui.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableList
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.common.model.Region
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.ui.BR
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.core.SchedulerFactory
import com.skedgo.tripkit.ui.core.UnableToFindPlaceCoordinatesError
import com.skedgo.tripkit.ui.core.isExecuting
import com.skedgo.tripkit.ui.core.rxproperty.asObservable
import com.skedgo.tripkit.ui.data.places.Place
import com.skedgo.tripkit.ui.data.places.PlaceSearchRepository
import com.skedgo.tripkit.ui.geocoding.HasResults
import com.skedgo.tripkit.ui.geocoding.NoConnection
import com.skedgo.tripkit.ui.geocoding.NoResult
import com.squareup.otto.Bus
import com.squareup.picasso.Picasso
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.rxkotlin.Observables
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.Schedulers.io
import io.reactivex.subjects.PublishSubject
import me.tatarka.bindingcollectionadapter2.ItemBinding
import me.tatarka.bindingcollectionadapter2.collections.MergeObservableList
import com.skedgo.tripkit.logging.ErrorLogger
import java.util.Collections.min
import java.util.concurrent.TimeUnit
import javax.inject.Inject

const val KEY_BOUNDS = "bounds"
const val KEY_CENTER = "center"

class LocationSearchViewModel @Inject constructor(private val context: Context,
                                                  private val regionService: RegionService,
                                                  private val bus: Bus,
                                                  private val placeSearchRepository: PlaceSearchRepository,
                                                  private val fetchSuggestions: FetchSuggestions,
                                                  private val errorLogger: ErrorLogger,
                                                  private val picasso: Picasso,
                                                  private val schedulerFactory: SchedulerFactory,
                                                  val errorViewModel: LocationSearchErrorViewModel)
    : RxViewModel() {
    val unableToFindPlaceCoordinatesError: Observable<Throwable> get() = _unableToFindPlaceCoordinatesError.hide()
    val dismiss: PublishRelay<Unit> = PublishRelay.create<Unit>()
    val locationChosen: PublishRelay<Location> = PublishRelay.create<Location>()
    val currentLocationChosen: PublishRelay<Unit> = PublishRelay.create<Unit>()
    val pinDropChosen: PublishRelay<Unit> = PublishRelay.create<Unit>()

    val showRefreshing = ObservableBoolean()
    val showList = ObservableBoolean()
    val showGoogleAttribution = ObservableBoolean(false)
    val showError = ObservableBoolean()
    val showMiddleProgressBar = ObservableBoolean()

    val chosenCityName = ObservableField<String>()

    val fixedSuggestions: ObservableList<SuggestionViewModel> = ObservableArrayList()
    val googleSuggestions: ObservableList<GoogleSuggestionViewModel> = ObservableArrayList()
    val allSuggestions = MergeObservableList<SuggestionViewModel>()

    val itemBinding = ItemBinding.of<SuggestionViewModel>(BR.viewModel, R.layout.list_item_search_result_item)
    val queries: PublishRelay<FetchLocationsParameters> = PublishRelay.create<FetchLocationsParameters>()

    private val _unableToFindPlaceCoordinatesError: PublishRelay<Throwable> = PublishRelay.create()
    private lateinit var bounds: LatLngBounds
    private lateinit var center: LatLng
    private var canOpenTimetable: Boolean = false
    private var showCurrentLocation: Boolean = false
    private var showDropPin: Boolean = false
    private val onQueryTextChangeEventThrottle = PublishSubject.create<String>()
    private val isFetchingPlaceDetails = ObservableBoolean(false)
    private val isSearchingSuggestion = ObservableBoolean(false)

    init {
        allSuggestions.insertList(fixedSuggestions)
        allSuggestions.insertList(googleSuggestions)
        allSuggestions.asObservable()
                .map {
                    it.mapIndexed { index, vm ->
                        vm.onItemClicked.observable
                                .map { viewModel -> viewModel to index }
                    }
                }
                .switchMap {
                    Observable.merge(it)
                }
                .subscribe ({
                    when (it.first) {
                        is CurrentLocationSuggestionViewModel -> onSuggestionItemClick(SearchSuggestionChoice.FixedChoice(it.second))
                        is DropNewPinSuggestionViewModel -> onSuggestionItemClick(SearchSuggestionChoice.FixedChoice(it.second))
                        is GoogleSuggestionViewModel -> onSuggestionItemClick(SearchSuggestionChoice.PlaceChoice(
                                (it.first as GoogleSuggestionViewModel).place, it.second))
                    }
                }, errorLogger::trackError)
                .autoClear()

        queries
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( {
                    fixedSuggestions.clear()
                    if (it.term().isNullOrEmpty()) {
                        if (showCurrentLocation) fixedSuggestions.add(CurrentLocationSuggestionViewModel(context))
                        if (showDropPin) fixedSuggestions.add(DropNewPinSuggestionViewModel(context))
                    }
                }, errorLogger::trackError)
                .autoClear()

        val googlePlaces = queries.hide()
                .switchMap { query ->
                    fetchSuggestions.query(query)
                            .isExecuting { isSearchingSuggestion.set(it) }
                            .repeatWhen { errorViewModel.retryObservable }
                }
                .subscribeOn(schedulerFactory.ioScheduler)
                .share()
                .debounce(500, TimeUnit.MILLISECONDS)

        Observables.combineLatest(
                isFetchingPlaceDetails.asObservable(),
                isSearchingSuggestion.asObservable(),
                googlePlaces)
        { fetchingPlaceDetails, fetchingSuggestions, googlePlaceResult ->
            when {
                fetchingPlaceDetails -> VisibilityState.FetchingPlaceDetails
                fetchingSuggestions -> VisibilityState.HasSuggestionsAndFetchingSuggestions
                googlePlaceResult == NoConnection -> VisibilityState.Error
                googlePlaceResult is NoResult -> VisibilityState.Error
                else -> VisibilityState.HasSuggestions
            }
        }
                .subscribe {
                    showMiddleProgressBar.set(it == VisibilityState.FetchingPlaceDetails)
                    showRefreshing.set(it == VisibilityState.HasSuggestionsAndFetchingSuggestions)
                    showError.set(it == VisibilityState.Error)
                    showList.set(it == VisibilityState.HasSuggestionsAndFetchingSuggestions || it == VisibilityState.HasSuggestions)
                }
                .autoClear()

        googlePlaces
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    when (it) {
                        is NoConnection -> {
                            googleSuggestions.clear()
                            errorViewModel.updateError(SearchErrorType.NoConnection)
                        }
                        is HasResults -> {
                            googleSuggestions.clear()
                            googleSuggestions.addAll(it.suggestions.map { GoogleSuggestionViewModel(context, picasso, it, canOpenTimetable) })
                            errorViewModel.updateError(null)
                        }
                        is NoResult -> {
                            errorViewModel.updateError(SearchErrorType.NoResults(it.query))
                        }
                    }
                }, errorLogger::trackError)
                .autoClear()

        onQueryTextChangeEventThrottle.debounce(500, TimeUnit.MILLISECONDS)
                .subscribe(
                        { this.searchResults(it) },
                        { errorLogger.trackError(it) })
                .autoClear()

        observeGoogleAttribution(googleSuggestions.asObservable().map { it.map { it.place } })
                .subscribe({}, { errorLogger.trackError(it) })
                .autoClear()

        errorViewModel.chooseOnMapObservable
                .subscribe({
                    pinDropChosen.accept(Unit)
                }, { errorLogger.trackError(it) })
                .autoClear()
    }

    fun observeGoogleAttribution(suggestions: Observable<List<Place>>): Observable<Unit> =
            suggestions.map { it.any { it.source() == Location.GOOGLE } }
                    .subscribeOn(schedulerFactory.computationScheduler)
                    .observeOn(schedulerFactory.mainScheduler)
                    .doOnNext { showGoogleAttribution.set(it) }
                    .map { Unit }

    fun loadCity() {
        val center = this.center
        val location = Location(center.latitude, center.longitude)
        regionService.getRegionByLocationAsync(location)
                .flatMap<Region.City> { region ->
                    val cities = region.cities
                    if (cities != null) {
                        val city = min<Region.City>(cities) { lhs, rhs ->
                            lhs.distanceTo(location).toDouble().compareTo(rhs.distanceTo(location).toDouble())
                        }
                        Observable.just(city)
                    } else {
                        Observable.empty()
                    }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ city -> chosenCityName.set(city.name) }, { errorLogger.trackError(it) })
                .autoClear()
    }

    fun changeCity(data: Intent) {
        bounds = data.getParcelableExtra("bounds")
        center = data.getParcelableExtra("center")
        val city = data.getParcelableExtra<Location>("city")
        chosenCityName.set(city.name)
        // TODO
//        bus.post(ImmutableCitySelectedEvent.of(bounds))
    }

    fun onQueryTextChanged(query: String) {
        showRefreshing.set(true)
        onQueryTextChangeEventThrottle.onNext(query)
    }
    fun onTextSubmit(): Boolean = when {
        googleSuggestions.isNotEmpty() -> {
            onSuggestionItemClick(SearchSuggestionChoice.PlaceChoice(googleSuggestions.first().place, 0))
            true
        }
        else -> false
    }

    private fun onPlaceClicked(place: Place): Observable<Location> = when (place) {
        is Place.TripGoPOI -> Observable.just(place.location)
        is Place.WithoutLocation -> placeSearchRepository
                .getPlaceDetails(place.prediction.placeId)
                .map { (name: String, lat: Double, lng: Double, address: String) ->
                    val location = Location(lat, lng)
                    location.address = address
                    location.name = name
                    location.source = Location.GOOGLE
                    location
                }
                .subscribeOn(io())
    }

    private fun searchResults(term: CharSequence) {
        val bounds = bounds()
        val center = center()
        val fetchLocationsParameters = FetchLocationsParameters.builder()
                .northeastLat(bounds.northeast.latitude)
                .northeastLon(bounds.northeast.longitude)
                .southwestLat(bounds.southwest.latitude)
                .southwestLon(bounds.southwest.longitude)
                .nearbyLat(center.latitude)
                .nearbyLon(center.longitude)
                .term(term.toString())
                .build()
        queries.accept(fetchLocationsParameters)
    }

    fun onSuggestionItemClick(choice: SearchSuggestionChoice) {
        when (choice) {
            is SearchSuggestionChoice.FixedChoice ->
                onFixedLocationSuggestionItemClick(choice.position)
            is SearchSuggestionChoice.PlaceChoice -> {
                val (place, position) = choice
                onPlaceClicked(place)
                        .observeOn(mainThread())
                        .subscribe({
                            locationChosen.accept(it)
                        }, {
                            val error = UnableToFindPlaceCoordinatesError(it)
                            errorLogger.trackError(error)
                            _unableToFindPlaceCoordinatesError.accept(error)
                        })
                        .autoClear()
            }
        }
    }

    private fun onFixedLocationSuggestionItemClick(itemPosition: Int) {
        when (itemPosition) {
            0 -> {
                currentLocationChosen.accept(Unit)
            }
            else -> {
                pinDropChosen.accept(Unit)
            }
        }
        dismiss.accept(Unit)
    }

    fun goBack() {
        dismiss.accept(Unit)
    }

    fun handleArgs(args: Bundle?) {
        args?.let {
            if (it.containsKey(KEY_BOUNDS)) {
                bounds = it.getParcelable(KEY_BOUNDS)!!
            }

            if (it.containsKey(KEY_CENTER)) {
                center = it.getParcelable(KEY_CENTER)!!
            }
            canOpenTimetable = it.getBoolean(ARG_CAN_OPEN_TIMETABLE, false)
            showCurrentLocation = it.getBoolean(ARG_WITH_CURRENT_LOCATION, false)
            showDropPin = it.getBoolean(ARG_WITH_DROP_PIN, false)
        }
    }

    fun bounds(): LatLngBounds = bounds

    fun center(): LatLng = center

}

