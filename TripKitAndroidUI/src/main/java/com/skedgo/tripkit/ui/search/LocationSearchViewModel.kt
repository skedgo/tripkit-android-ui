package com.skedgo.tripkit.ui.search

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableList
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.analytics.SearchResultItemSource
import com.skedgo.tripkit.common.model.LOCATION_CLASS_SCHOOL
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.common.model.Region
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.logging.ErrorLogger
import com.skedgo.tripkit.ui.BR
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.core.SchedulerFactory
import com.skedgo.tripkit.ui.core.UnableToFindPlaceCoordinatesError
import com.skedgo.tripkit.ui.core.isExecuting
import com.skedgo.tripkit.ui.core.rxproperty.asObservable
import com.skedgo.tripkit.ui.data.places.Place
import com.skedgo.tripkit.ui.data.places.PlaceSearchRepository
import com.skedgo.tripkit.ui.database.location_history.LocationHistoryRepository
import com.skedgo.tripkit.ui.geocoding.AutoCompleteResult
import com.skedgo.tripkit.ui.geocoding.HasResults
import com.skedgo.tripkit.ui.geocoding.NoConnection
import com.skedgo.tripkit.ui.geocoding.NoResult
import com.skedgo.tripkit.ui.utils.LocationUtil
import com.skedgo.tripkit.ui.utils.TransportModeSharedPreference
import com.squareup.picasso.Picasso
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.rxkotlin.Observables
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.Schedulers.io
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.launch
import me.tatarka.bindingcollectionadapter2.ItemBinding
import me.tatarka.bindingcollectionadapter2.collections.MergeObservableList
import timber.log.Timber
import java.util.Collections.min
import java.util.concurrent.TimeUnit
import javax.inject.Inject

const val KEY_BOUNDS = "bounds"
const val KEY_CENTER = "center"

@SuppressLint("StaticFieldLeak")
class LocationSearchViewModel @Inject constructor(
    private val context: Context,
    private val regionService: RegionService,
    private val placeSearchRepository: PlaceSearchRepository,
    private val fetchSuggestions: FetchSuggestions,
    private val errorLogger: ErrorLogger,
    private val picasso: Picasso,
    private val schedulerFactory: SchedulerFactory,
    private val locationHistoryRepository: LocationHistoryRepository,
    val errorViewModel: LocationSearchErrorViewModel,
    private val transportModeSharedPreference: TransportModeSharedPreference
) : RxViewModel() {

    companion object {
        const val DEBOUNCE_TEXT_CHANGE_THROTTLE = 800L
    }

    var legacyLocationSearchIconProvider: LegacyLocationSearchIconProvider? = null
    var locationSearchIconProvider: LocationSearchIconProvider? = null
    var fixedSuggestionsProvider: FixedSuggestionsProvider? = null
    var locationSearchProvider: LocationSearchProvider? = null

    val unableToFindPlaceCoordinatesError: Observable<Throwable> get() = _unableToFindPlaceCoordinatesError.hide()
    val dismiss: PublishRelay<Unit> = PublishRelay.create()
    val locationChosen: PublishRelay<Location> = PublishRelay.create()
    val cityLocationChosen: PublishRelay<Location> = PublishRelay.create()
    val fixedLocationChosen: PublishRelay<Any> = PublishRelay.create()
    val infoChosen: PublishRelay<Location?> = PublishRelay.create()
    val suggestionActionClick: PublishRelay<Location?> = PublishRelay.create()
    val showRefreshing = ObservableBoolean()
    val showList = ObservableBoolean()
    val showGoogleAttribution = ObservableBoolean(false)
    val showError = ObservableBoolean()
    val showMiddleProgressBar = ObservableBoolean()
    val showBackButton = ObservableBoolean(true)
    val showSearchBox = ObservableBoolean(true)
    val onFinishLoad: PublishRelay<Boolean> = PublishRelay.create<Boolean>()

    var scrollResultsOfQuery = false
    val scrollListToTop = ObservableBoolean(true)

    val chosenCityName = ObservableField<String>()

    val citiesSuggestions: ObservableList<SuggestionViewModel> = ObservableArrayList()
    val historySuggestions: ObservableList<SuggestionViewModel> = ObservableArrayList()
    val fixedSuggestions: ObservableList<SuggestionViewModel> = ObservableArrayList()
    val providedSuggestions: ObservableList<SuggestionViewModel> = ObservableArrayList()

    val googleAndTripGoSuggestions: ObservableList<GoogleAndTripGoSuggestionViewModel> =
        ObservableArrayList()
    val allSuggestions = MergeObservableList<SuggestionViewModel>()

    val itemBinding: ItemBinding<SuggestionViewModel> by lazy {
        ItemBinding.of<SuggestionViewModel>(BR.viewModel, R.layout.list_item_search_result_item)
    }

    val queries: PublishRelay<FetchLocationsParameters> =
        PublishRelay.create<FetchLocationsParameters>()

    private val _unableToFindPlaceCoordinatesError: PublishRelay<Throwable> = PublishRelay.create()
    private lateinit var bounds: LatLngBounds
    private lateinit var center: LatLng
    private var canOpenTimetable: Boolean = false
    private var showCurrentLocation: Boolean = false
    private var showDropPin: Boolean = false
    private var isRouting: Boolean = false
    private val onQueryTextChangeEventThrottle = PublishSubject.create<String>()
    private val isFetchingPlaceDetails = ObservableBoolean(false)
    private val isSearchingSuggestion = ObservableBoolean(false)
    private val queryCache = mutableMapOf<String, AutoCompleteResult>()

    init {
        allSuggestions.insertList(fixedSuggestions)
        allSuggestions.insertList(historySuggestions)
        allSuggestions.insertList(providedSuggestions)
        allSuggestions.insertList(googleAndTripGoSuggestions)
        allSuggestions.insertList(citiesSuggestions)
        handleItemClick()
        handleItemInfoClick()
        handleSuggestionActionClick()

        observeQueries()

        val suggestionFetcher = queries.hide()
            .switchMap { query ->
                // TODO MIKE Everything dealing with getting results, including this caching, needs to be refactored
                if (queryCache.containsKey(query.term())) {
                    Observable.just(queryCache[query.term()]!!)
                } else {
                    fetchSuggestions.query(query)
                        .map {
                            if (it is HasResults) {
                                queryCache[query.term()] = it
                            }
                            it
                        }
                        .isExecuting { isSearchingSuggestion.set(it) }
                        .repeatWhen { errorViewModel.retryObservable }
                }
            }
            .subscribeOn(schedulerFactory.ioScheduler)
            .share()
            .debounce(500, TimeUnit.MILLISECONDS)


        Observables.combineLatest(
            isFetchingPlaceDetails.asObservable(),
            isSearchingSuggestion.asObservable(),
            suggestionFetcher
        )
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

        suggestionFetcher
            .observeOn(mainThread())
            .subscribe({ result ->
                when (result) {
                    is NoConnection -> {
                        googleAndTripGoSuggestions.clear()
                        errorViewModel.updateError(SearchErrorType.NoConnection)
                    }

                    is HasResults -> {
                        googleAndTripGoSuggestions.clear()
                        val initialSuggestions = result.suggestions.map { place ->
                            GoogleAndTripGoSuggestionViewModel(
                                context,
                                picasso,
                                place,
                                canOpenTimetable,
                                iconProvider(),
                                result.query
                            )
                        }

                        // Filter to identify all school items
                        val schoolItems = initialSuggestions.filter { it.location.locationClass == LOCATION_CLASS_SCHOOL }

                        // Using a set to avoid duplicates
                        val uniqueItemsToAdd = mutableSetOf<GoogleAndTripGoSuggestionViewModel>()

                        // Add school items directly since they should always be included
                        uniqueItemsToAdd.addAll(schoolItems)

                        // Filter and add other items based on their distance and relevance to any school item
                        initialSuggestions.forEach { item ->
                            if (item.location.locationClass != "SchoolLocation" && schoolItems.none { school ->
                                    LocationUtil.getRelevancePoint(school.location.name, item.location.name) > 0.7
                                }) {
                                uniqueItemsToAdd.add(item)
                            }
                        }

                        // Add all unique filtered items to the main suggestions list
                        googleAndTripGoSuggestions.addAll(uniqueItemsToAdd)

                        errorViewModel.updateError(null)
                    }

                    is NoResult -> {
                        errorViewModel.updateError(SearchErrorType.NoResults(result.query))
                    }
                }
            }, errorLogger::trackError)
            .autoClear()

        setTextChangeThrottle()
        observeGoogleAttr()
    }

    private fun observeQueries() {
        queries
            .observeOn(mainThread())
            .subscribe({ params ->
                fixedSuggestions.clear()
                if (params.term().isEmpty()) {
                    fixedSuggestionsProvider().fixedSuggestions(context, iconProvider())
                        .forEach { suggestion ->
                            fixedSuggestions.add(
                                FixedSuggestionViewModel(context, suggestion)
                            )

                            if (scrollResultsOfQuery) {
                                scrollListToTop.set(true)
                                scrollResultsOfQuery = false
                            }
                        }
                    loadFromHistory()
                } else {
                    historySuggestions.clear()

                    fixedSuggestionsProvider().specificSuggestions(
                        context,
                        listOf(
                            DefaultFixedSuggestionType.CHOOSE_ON_MAP,
                            DefaultFixedSuggestionType.CURRENT_LOCATION
                        ),
                        iconProvider()
                    ).forEach { suggestion ->
                        fixedSuggestions.add(
                            FixedSuggestionViewModel(context, suggestion)
                        )
                    }
                }
                if (!isRouting && params.term().isNotEmpty()) {
                    loadFromRegions(params.term())
                } else if (params.term().isEmpty()) {
                    citiesSuggestions.clear()
                }
                providedSuggestions.clear()
                viewModelScope.launch {
                    locationSearchProvider?.query(context, iconProvider(), params.term())
                        ?.forEach { suggestion ->
                            providedSuggestions.add(
                                SearchProviderSuggestionViewModel(
                                    context,
                                    suggestion,
                                    params.term()
                                )
                            )
                        }
                }

            }, errorLogger::trackError)
            .autoClear()
    }

    private fun observeGoogleAttr() {
        observeGoogleAttribution(
            googleAndTripGoSuggestions.asObservable().map { it.map { it.place } })
            .subscribe({}, { errorLogger.trackError(it) })
            .autoClear()
    }

    private fun handleItemClick() {
        allSuggestions.asObservable()
            .map {
                onFinishLoad.accept(false)
                it.mapIndexed { index, vm ->
                    vm.onItemClicked.observable
                        .map { viewModel -> viewModel to index }
                }
            }
            .switchMap {
                onFinishLoad.accept(true)
                Observable.merge(it)
            }
            .subscribe({
                when (it.first) {
                    is SearchProviderSuggestionViewModel -> onSuggestionItemClick(
                        SearchSuggestionChoice.SearchProviderChoice((it.first as SearchProviderSuggestionViewModel).suggestion.location())
                    )

                    is CityProviderSuggestionViewModel -> onSuggestionItemClick(
                        SearchSuggestionChoice.CityProviderChoice((it.first as CityProviderSuggestionViewModel).suggestion.location())
                    )

                    is FixedSuggestionViewModel -> onSuggestionItemClick(
                        SearchSuggestionChoice.FixedChoice(
                            (it.first as FixedSuggestionViewModel).id
                        )
                    )

                    is GoogleAndTripGoSuggestionViewModel -> onSuggestionItemClick(
                        SearchSuggestionChoice.PlaceChoice(
                            (it.first as GoogleAndTripGoSuggestionViewModel).place
                        )
                    )
                }
            }, errorLogger::trackError)
            .autoClear()
    }

    private fun handleItemInfoClick() {
        allSuggestions.asObservable()
            .map {
                onFinishLoad.accept(false)
                it.mapIndexed { index, vm ->
                    vm.onInfoClicked.observable
                        .map { viewModel ->
                            viewModel to index
                        }
                }
            }
            .switchMap {
                onFinishLoad.accept(true)
                Observable.merge(it)
            }
            .subscribe({
                when (val suggestionViewModel = it.first) {
                    is FixedSuggestionViewModel -> {
                        onInfoClicked(suggestionViewModel.suggestion.location())
                    }

                    is GoogleAndTripGoSuggestionViewModel -> {
                        onInfoClicked(suggestionViewModel.location)
                    }
                }
            }, errorLogger::trackError)
            .autoClear()
    }

    private fun handleSuggestionActionClick() {
        allSuggestions.asObservable()
            .map {
                onFinishLoad.accept(false)
                it.mapIndexed { index, vm ->
                    vm.onSuggestionActionClicked.observable
                        .map { viewModel -> viewModel to index }
                }
            }
            .switchMap {
                onFinishLoad.accept(true)
                Observable.merge(it)
            }
            .subscribe({
                when (val suggestionViewModel = it.first) {
                    is FixedSuggestionViewModel -> {
                        onSuggestionActionClicked(suggestionViewModel.suggestion.location())
                    }

                    is GoogleAndTripGoSuggestionViewModel -> {
                        onSuggestionActionClicked(suggestionViewModel.location)
                    }
                }
            }, errorLogger::trackError)
            .autoClear()
    }

    private fun setTextChangeThrottle() {
        onQueryTextChangeEventThrottle.debounce(
            DEBOUNCE_TEXT_CHANGE_THROTTLE,
            TimeUnit.MILLISECONDS
        )
            .subscribe({
                searchResults(if (it == SearchResultItemSource.CurrentLocation.value) "" else it)
            }, { errorLogger.trackError(it) })
            .autoClear()
    }

    private fun loadFromHistory() {
        historySuggestions.clear()
        val historyStartCalendarMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(12L)

        locationHistoryRepository
            .getLatestLocationHistory(historyStartCalendarMillis)
            .observeOn(mainThread())
            .subscribeOn(io())
            .subscribe({
                fixedSuggestionsProvider().locationsToSuggestion(
                    context,
                    it.toSet().reversed(),
                    legacyIconProvider()
                ).forEach { suggestion ->
                    historySuggestions.add(SearchProviderSuggestionViewModel(context, suggestion))
                }
            }, {
                it.printStackTrace()
            }).autoClear()
    }

    private fun loadFromRegions(query: String) {
        citiesSuggestions.clear()

        regionService.getRegionsAsync()
            .observeOn(mainThread())
            .subscribe({
                val newList = ArrayList<Region.City>()
                it.forEach { region ->
                    region.cities?.let { list ->
                        list.forEach { city ->
                            if (city.name.contains(query, true)) {
                                newList.add(city)
                            }
                        }
                    }
                }
                fixedSuggestionsProvider().citiesToSuggestion(
                    context,
                    newList,
                    legacyIconProvider()
                ).forEach { suggestion ->
                    citiesSuggestions.add(
                        CityProviderSuggestionViewModel(
                            context,
                            suggestion,
                            query
                        )
                    )
                }
            }, { Timber.e(it) }).autoClear()
    }

    private fun legacyIconProvider(): LegacyLocationSearchIconProvider {
        if (legacyLocationSearchIconProvider == null) {
            legacyLocationSearchIconProvider = LegacyLocationSearchIconProvider()
        }
        return legacyLocationSearchIconProvider!!
    }

    private fun iconProvider(): LocationSearchIconProvider {
        if (locationSearchIconProvider == null) {
            locationSearchIconProvider = LegacyLocationSearchIconProvider()
        }
        return locationSearchIconProvider!!
    }

    private fun fixedSuggestionsProvider(): FixedSuggestionsProvider {
        if (fixedSuggestionsProvider == null) {
            fixedSuggestionsProvider =
                DefaultFixedSuggestionsProvider(showCurrentLocation, showDropPin)
        }

        return fixedSuggestionsProvider!!
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
                        lhs.distanceTo(location).toDouble()
                            .compareTo(rhs.distanceTo(location).toDouble())
                    }
                    Observable.just(city)
                } else {
                    Observable.empty()
                }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(mainThread())
            .subscribe({ city -> chosenCityName.set(city.name) }, { errorLogger.trackError(it) })
            .autoClear()
    }

    fun onQueryTextChanged(query: String, isRouting: Boolean = false) {
        this.isRouting = isRouting
        showRefreshing.set(true)
        onQueryTextChangeEventThrottle.onNext(query)
        if (query.isBlank()) {
            scrollResultsOfQuery = true
        }
    }

    fun onTextSubmit(): Boolean = when {
        googleAndTripGoSuggestions.isNotEmpty() -> {
            onSuggestionItemClick(SearchSuggestionChoice.PlaceChoice(googleAndTripGoSuggestions.first().place))
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
            is SearchSuggestionChoice.SearchProviderChoice ->
                choice.location?.let { locationChosen.accept(it) }

            is SearchSuggestionChoice.CityProviderChoice ->
                choice.location?.let { cityLocationChosen.accept(it) }

            is SearchSuggestionChoice.FixedChoice ->
                onFixedLocationSuggestionItemClick(choice.id)

            is SearchSuggestionChoice.PlaceChoice -> {
                val (place) = choice
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

    fun onInfoClicked(location: Location?) {
        infoChosen.accept(location)
    }

    fun onSuggestionActionClicked(location: Location?) {
        suggestionActionClick.accept(location)
    }

    private fun onLocationSearchSuggestionItemClick(id: Any) {
        viewModelScope.launch {
            locationSearchProvider?.onClick(id)
        }
    }

    private fun onFixedLocationSuggestionItemClick(id: Any) {
        fixedLocationChosen.accept(id)
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
            showBackButton.set(it.getBoolean(ARG_SHOW_BACK_BUTTON, true))
            showSearchBox.set(it.getBoolean(ARG_SHOW_SEARCH_FIELD, true))
        }
    }

    fun bounds(): LatLngBounds = bounds

    fun center(): LatLng = center

    fun saveLocationToHistory(location: Location) {
        if (location.name != context.getString(R.string.home) && location.name != context.getString(
                R.string.work
            ) && !location.isFavourite
        ) {
            locationHistoryRepository.saveLocationsToHistory(
                listOf(location)
            ).subscribeOn(io())
                .subscribe({
                    //Do nothing
                }, {
                    it.printStackTrace()
                }).autoClear()
        }
    }

}

